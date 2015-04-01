/**
 * Hough_Circles.java:
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * This class roughly implements the imfindcircle function from matlab. Several steps
 * in this function are edited, changed or not implemented. Still, the functionality 
 * is similar to the imfindcircle function.
 * 
 * @author Peter J. Schüffler
 * 12.03.2015
 *
 */
package cish;

import ij.*;
import ij.process.*;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import tmarker.misc.Misc;

/**
 * This ImageJ plugin shows the Hough Transform Space and search for circles in
 * a binary image. The image must have been passed through an edge detection
 * module and have edges marked in white (background must be in black).
 */
public class Hough_Circles {

    public int radiusMin;  // Find circles with radius grater or equal radiusMin
    public int radiusMax;  // Find circles with radius less or equal radiusMax
    ImagePlus I; // original image
    Point[] centerPoints; // Center Points of the Circles Found.

    // Sobel Filter
    static final int sobel_x[][] = {{-1, 0, 1},
                                    {-2, 0, 2},
                                    {-1, 0, 1}};
    static final int sobel_y[][] = {{-1, -2, -1},
                                    {0, 0, 0},
                                    {1, 2, 1}};

    /**
     * Run this method to get the circle center points directly (without ij
     * plugin).
     *
     * @param ip The image to be considered, as ImagePlus imageprocessor.
     * @param radiusMin The min radius of circles.
     * @param radiusMax The max radius of circles.
     * @param darkcircles If true, dark circles on light background are
     * detected. If false, light circles on dark background.
     * @return All found center points, or null if there was a failure.
     */
    public Point[] run(ImageProcessor ip, int radiusMin, int radiusMax, boolean darkcircles) {

        this.radiusMin = radiusMin;
        this.radiusMax = radiusMax;

        this.I = new ImagePlus("", ip.createImage());

        // grayscale Image
        ImageConverter ic = new ImageConverter(I);
        ic.convertToGray8();

        // find the edges in the image (hopefully circular edges)
        double[][][] gradientImages = SobelFilter(I.getProcessor());

        //Misc.writeImage(gradientImages.get(0), true, "sobelH.png");
        //Misc.writeImage(gradientImages.get(1), true, "sobelV.png");
        //Misc.writeImage(gradientImages.get(2), true, "sobel.png");

        int edge_threshold = Otsu_Threshold.getOtsuThreshold(gradientImages[2]); // Otsu's threshold
        Logger.getLogger(Hough_Circles.class.getName()).log(Level.INFO, "Edge Threshold = " + edge_threshold);

        double[][] accum_matrix = AccumulationMatrix(gradientImages[0], gradientImages[1], gradientImages[2], ip.getWidth(), ip.getHeight(), this.radiusMin, this.radiusMax, darkcircles, edge_threshold);
        //Misc.writeImage(accum_matrix, true, "accumulatorMatrix.png");

        // perform median filtering
        accum_matrix = MedianFilter(accum_matrix, 5, 5);
        Misc.writeImage(accum_matrix, true, "accumulatorMatrixMedian.png");

        double sensitivity = 0.85;
        double accumThresh = 1.0 - sensitivity;
        this.centerPoints = findCenters(accum_matrix, 255.0 * accumThresh, this.radiusMax);

        return this.centerPoints;
    }

    /**
     * The parametric equation for a circle centered at (a,b) with radius r is:
     *
     * a = x - r*cos(theta) b = y - r*sin(theta)
     *
     * In order to speed calculations, we first construct a lookup table (lut)
     * containing the rcos(theta) and rsin(theta) values, for theta varying from
     * 0 to 2*PI with increments equal to 1/8*r. As of now, a fixed increment is
     * being used for all different radius (1/8*radiusMin). This should be
     * corrected in the future.
     *
     * Return value = Number of angles for each radius
     *
     */
    private int buildLookUpTable() {

        int i = 0;
        int incDen = Math.round(8F * radiusMin);  // increment denominator

        int radiusInc = 1;
        int depth = 3; // radius_max - radius_min / radiusInc;
        int[][][] lut = new int[2][incDen][depth];

        for (int radius = radiusMin; radius <= radiusMax; radius = radius + radiusInc) {
            i = 0;
            for (int incNun = 0; incNun < incDen; incNun++) {
                double angle = (2 * Math.PI * (double) incNun) / (double) incDen;
                int indexR = (radius - radiusMin) / radiusInc;
                int rcos = (int) Math.round((double) radius * Math.cos(angle));
                int rsin = (int) Math.round((double) radius * Math.sin(angle));
                if ((i == 0) | (rcos != lut[0][i][indexR]) & (rsin != lut[1][i][indexR])) {
                    lut[0][i][indexR] = rcos;
                    lut[1][i][indexR] = rsin;
                    i++;
                }
            }
        }

        return i;
    }

    /**
     * Calculates the accumulation matrix for estimated circle center points.
     *
     * @param Gx Gradient image in horizontal direction.
     * @param Gy Gradient image in vertical direction.
     * @param G Gradient image in both directions.
     * @param radius_min The min radius of the radius range of detected circles.
     * @param radius_max The max radius of the radius range of detected circles.
     * @param darkcircles If true, dark circles on light background are
     * detected. If false, light circles on dark background.
     * @param edge_threshold Threshold above which an edge is considered.
     * @return A matrix of size width x height which can be considered as a
     * probability map of found circles with radius within the specified radius
     * range.
     */
    private static double[][] AccumulationMatrix(double[][] Gx, double[][] Gy, double[][] G, int width, int height, int radius_min, int radius_max, boolean darkcircles, int edge_threshold) {

        // weights of circles
        double[] w0 = new double[radius_max - radius_min + 1];
        for (int i = 0; i < w0.length; i++) {
            w0[i] = 1.0 / (2.0 * Math.PI * (radius_min + i));
        }

        // correct for darkcircles
        int[] RR = new int[radius_max - radius_min + 1];
        for (int i = 0; i < RR.length; i++) {
            if (darkcircles) {
                RR[i] = radius_min + i;
            } else {
                RR[i] = -(radius_min + i);
            }
        }

        // initialize accumulator matrix
        double[][] accum = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                accum[i][j] = 0;
            }
        }

        // fill accumulator matrix
        int xc, yc;
        double value;
        for (int r = 0; r < RR.length; r++) {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    value = G[i][j];
                    if (value > edge_threshold) {
                        xc = (int) Math.round(i - RR[r] * (Gx[i][j] / value));
                        yc = (int) Math.round(j - RR[r] * (Gy[i][j] / value));
                        if (xc >= 0 && xc < width && yc >= 0 && yc < height) {
                            accum[xc][yc] += w0[r];
                        }
                    }
                }
            }
        }
        //writeCSV(accum, "test.csv");

        // normalize Image
        double max_value = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                max_value = Math.max(max_value, accum[i][j]);
            }
        }
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                accum[i][j] = Math.round(255.0 * accum[i][j] / max_value);
            }
        }

        return accum;
    }

    /**
     * Sobel Filter: generate a 3 x width x height matrix for the horizontal,
     * vertical and combined gradient.
     *
     * @param ip The original image to read from.
     * @return The gradient "images" (values can be negative or positive). 
     * SobelFilter[0][][] contains the horizontal gradient.
     * SobelFilter[1][][] contains the vertical gradient.
     * SobelFilter[2][][] contains the combined gradient.
     */
    private static double[][][] SobelFilter(ImageProcessor ip) {
        int w = ip.getWidth();
        int h = ip.getHeight();
        double[][][] F = new double[3][w][h];
        double pixel_x = 0, pixel_y = 0;
        for (int x = 1; x < w - 2; x++) {
            for (int y = 1; y < h - 2; y++) {
                pixel_x = (sobel_x[0][0] * ip.getPixel(x - 1, y - 1)) + (sobel_x[0][1] * ip.getPixel(x, y - 1)) + (sobel_x[0][2] * ip.getPixel(x + 1, y - 1))
                        + (sobel_x[1][0] * ip.getPixel(x - 1, y)) + (sobel_x[1][1] * ip.getPixel(x, y)) + (sobel_x[1][2] * ip.getPixel(x + 1, y))
                        + (sobel_x[2][0] * ip.getPixel(x - 1, y + 1)) + (sobel_x[2][1] * ip.getPixel(x, y + 1)) + (sobel_x[2][2] * ip.getPixel(x + 1, y + 1));
                pixel_y = (sobel_y[0][0] * ip.getPixel(x - 1, y - 1)) + (sobel_y[0][1] * ip.getPixel(x, y - 1)) + (sobel_y[0][2] * ip.getPixel(x + 1, y - 1))
                        + (sobel_y[1][0] * ip.getPixel(x - 1, y)) + (sobel_y[1][1] * ip.getPixel(x, y)) + (sobel_y[1][2] * ip.getPixel(x + 1, y))
                        + (sobel_y[2][0] * ip.getPixel(x - 1, y + 1)) + (sobel_y[2][1] * ip.getPixel(x, y + 1)) + (sobel_y[2][2] * ip.getPixel(x + 1, y + 1));

                F[0][x][y] = pixel_x;
                F[1][x][y] = pixel_y;
                F[2][x][y] = Math.sqrt((pixel_x * pixel_x) + (pixel_y * pixel_y));
            }
        }
        return F;
    }

    /**
     * For Debugging: Write a double[][] into a .csv file
     *
     * @param array The array to be written.
     * @param filename The filename.
     */
    static void writeCSV(double[][] array, String filename) {
        try {
            File file = new File(filename);
            file.deleteOnExit();

            String out = "";
            for (int i = 0; i < array.length; i++) {
                for (int j = 0; j < array[i].length; j++) {
                    out += array[i][j] + ";";
                }
                out += "\n";
            }

            FileWriter fw = new FileWriter(file);
            fw.write(out);
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Hough_Circles.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * For Debugging: Write a int[][] into a .csv file
     *
     * @param array The array to be written.
     * @param filename The filename.
     */
    static void writeCSV(int[][] array, String filename) {
        try {
            File file = new File(filename);
            file.deleteOnExit();

            String out = "";
            for (int i = 0; i < array.length; i++) {
                for (int j = 0; j < array[i].length; j++) {
                    out += array[i][j] + ";";
                }
                out += "\n";
            }

            FileWriter fw = new FileWriter(file);
            fw.write(out);
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Hough_Circles.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Strategy: Go from 255 - threshold, for every value find the points on the
     * image with this value. These are the center points. Set everything 0
     * within the radius around these points.
     *
     * @param img A gray value image whose local centers are found.
     * @param threshold A threshold below which intesities are not considered.
     * @param r A radius which separates two centers.
     * @return A sorted array of found center points (brightest center in the
     * beginning).
     */
    private Point[] findCenters(double[][] img, double threshold, int r) {
        List<Point> centers = new ArrayList<>();

        int r_half = (int) (r / 2.0);

        for (int g = 255; g >= threshold; g--) {
            for (int i = 0; i < img.length; i++) {
                for (int j = 0; j < img[i].length; j++) {
                    if (img[i][j] == g) {
                        // add this center
                        centers.add(new Point(i, j));
                        // clear neighbors
                        for (int m = i - r_half; m <= i + r_half; m++) {
                            if (m >= 0 && m < img.length) {
                                for (int n = j - r_half; n <= j + r_half; n++) {
                                    if (n >= 0 && n < img[i].length) {
                                        img[m][n] = 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return centers.toArray(new Point[centers.size()]);
    }

    /**
     * Performs a median filter to the 2D matrix.
     *
     * @param accum_matrix The image matrix to be filtered.
     * @param m The width of the filter.
     * @param n The height of the filter.
     * @return The filtered image matrix.
     */
    private static double[][] MedianFilter(double[][] accum_matrix, int m, int n) {
        int m_half = (int) Math.floor(m / 2.0);
        int n_half = (int) Math.floor(n / 2.0);

        double[][] accum_matrix_new = new double[accum_matrix.length][accum_matrix[0].length];

        double[] window;
        int k;
        for (int i = 0; i < accum_matrix.length; i++) {
            for (int j = 0; j < accum_matrix[i].length; j++) {
                window = new double[(2 * m_half + 1) * (2 * n_half + 1)];
                k = 0;
                for (int x = i - m_half; x <= i + m_half; x++) {
                    for (int y = j - n_half; y <= j + n_half; y++) {
                        if (x < 0 || y < 0 || x >= accum_matrix.length || y >= accum_matrix[i].length) {
                            window[k] = 0;
                        } else {
                            window[k] = accum_matrix[x][y];
                        }
                        k++;
                    }
                }
                accum_matrix_new[i][j] = Median(window);
            }
        }
        return accum_matrix_new;
    }

    /**
     * Returns the median of a number array.
     *
     * @param numArray The number array to be processed.
     * @return The median of this number array.
     */
    private static double Median(double[] numArray) {
        if (numArray.length == 0) {
            return Double.NaN;
        }
        if (numArray.length == 1) {
            return numArray[0];
        }
        Arrays.sort(numArray);
        double median;
        if (numArray.length % 2 == 0) {
            median = ((double) numArray[numArray.length / 2] + (double) numArray[numArray.length / 2 - 1]) / 2;
        } else {
            median = (double) numArray[numArray.length / 2];
        }

        return median;
    }

}

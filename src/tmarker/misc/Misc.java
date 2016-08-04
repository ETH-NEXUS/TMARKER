/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker.misc;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.Histogram;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.apache.commons.lang3.ArrayUtils;
import tmarker.tmarker;


/**
 * A collection of useful helper functions (all static).
 * @author Peter J. Schueffler
 */
public class Misc {
    
    /**
     * Returns the extension of the file.
     * @param filepath The file (either with or without full file path).
     * @return The extension, i.e. the part behind the last "." of the String.
     */
    public static String FilePathStringtoExtension(String filepath) {
        int dot = filepath.lastIndexOf(".");
        return filepath.substring(dot + 1);
    }
    
    /**
     * Returns the filename in a full file path.
     * @param filepath The full file path.
     * @return The filename, i.e. the part of the String after the last separator.
     */
    public static String FilePathStringtoFilename(String filepath) {
        return new File(filepath).getName();
    }
    
    /**
     * Returns the filename in a full file path without extension.
     * @param filepath The full file path.
     * @return The filename, i.e. the part of the String after the last separator and before the last ".".
     */
    public static String FilePathStringtoFilenameWOExtension(String filepath) {
        filepath = FilePathStringtoFilename(filepath);
        int dot2 = filepath.lastIndexOf(".");
        if (dot2>=0) {
            return filepath.substring(0, dot2);
        }
        return filepath;
    }
    
    /**
    * Reallocates an array with a new size, and copies the contents
    * of the old array to the new array.
    * @param oldArray  the old array, to be reallocated.
    * @param newSize   the new array size.
    * @return          A new array with the same contents.
    */
    public static Object resizeArray (Object oldArray, int newSize) {
       int oldSize = java.lang.reflect.Array.getLength(oldArray);
       Class elementType = oldArray.getClass().getComponentType();
       Object newArray = java.lang.reflect.Array.newInstance(
             elementType,newSize);
       int preserveLength = Math.min(oldSize,newSize);
       if (preserveLength > 0) {
            System.arraycopy(oldArray,0,newArray,0,preserveLength);
        }
       return newArray; 
    }
    
    /**
     * Adds an element to a given array.
     * @param oldArray The array to be augmented by 1.
     * @param element The element to be inserted at the end of the old array.
     */
    public static void pushbackArray (Object oldArray, Object element) {
        oldArray = resizeArray(oldArray, java.lang.reflect.Array.getLength(oldArray)+1);
        Class elementType = oldArray.getClass().getComponentType();
        ((Object[])oldArray)[((Object[])oldArray).length-1] = element;
    }
    
    /**
     * Returns the order of an array as a new int[].
     * E.g. orderArray([4.0, 3.0, 1.0, 6.0, 5.0, 4.0], true) will return 
     * [2, 1, 0, 5, 4, 3]
     * @param array The array to be ordered. It is not changed.
     * @param reverse If true, it is ordered from highest to smallest value, and vice versa.
     * @return The order of the double array.
     */
    public static int[] orderArray(double[] array, boolean reverse) {
        double[] tmp = array.clone();
        Arrays.sort(tmp);
        if (reverse) {
            ArrayUtils.reverse(tmp);
        }
        // for duplicate entries in array: store the last found position index
        // (initiate with 0 for all values in the beginning)
        Hashtable<Double, Integer> hash = new Hashtable<Double, Integer>(array.length);
        for (int i=0; i<array.length; i++) {
            hash.put(array[i], 0);
        }
        int[] order = new int[array.length];
        for (int i=0; i<order.length; i++) {
            order[i] = ArrayUtils.indexOf(tmp, array[i], hash.get(array[i]));
            hash.put(array[i], order[i]+1);
        }
        return order;
    }
    
    /**
     * Returns the mean of a double array.
     * @param a The double array.
     * @return The mean value of a. Double.NaN if a.length==0.
     */
    public static double mean(double[] a) {
        if (a.length == 0) {
            return Double.NaN;
        }
        if (a.length == 1) {
            return a[0];
        }
        double sum = 0.0;
        for (double d:a) {
            sum += d;
        }
        return sum/a.length;
    }
    
    /**
     * Returns the mean of a Double array.
     * @param a The Double array.
     * @return The mean value of a. Double.NaN if a.length==0.
     */
    public static double mean(Double[] a) {
        if (a.length == 0) {
            return Double.NaN;
        }
        if (a.length == 1) {
            return a[0];
        }
        double sum = 0.0;
        for (double d:a) {
            sum += d;
        }
        return sum/a.length;
    }
    
    /**
     * Returns the maximum of an integer array.
     * @param a The array.
     * @return The maximum value of a. 0 if a.length==0.
     */
    public static int max(int[] a) {
        if (a.length == 0) {
            return 0;
        }
        if (a.length == 1) {
            return a[0];
        }
        int max = Integer.MIN_VALUE;
        for (int d:a) {
            max = Math.max(max, d);
        }
        return max;
    }
    
    /**
     * Returns the maximum of a double array.
     * @param a The double array.
     * @return The maximum value of a. Double.NaN if a.length==0.
     */
    public static double max(double[] a) {
        if (a.length == 0) {
            return Double.NaN;
        }
        if (a.length == 1) {
            return a[0];
        }
        double max = Double.MIN_VALUE;
        for (double d:a) {
            max = Math.max(max, d);
        }
        return max;
    }
    
    /**
     * Returns the maximum of a 2D double array.
     * @param a The double array.
     * @return The maximum value of a. Double.NaN if a.length==0.
     */
    public static double max(double[][] a) {
        if (a.length == 0) {
            return Double.NaN;
        }
        double max = Double.MIN_VALUE;
        for (int i=0; i<a.length; i++) {
            for (int j=0; j<a[i].length; j++) {
                max = Math.max(max, a[i][j]);
            }
        }
        return max;
    }
    
    /**
     * Returns the minimum of a 2D double array.
     * @param a The double array.
     * @return The minimum value of a. Double.NaN if a.length==0.
     */
    public static double min(double[][] a) {
        if (a.length == 0) {
            return Double.NaN;
        }
        double min = Double.MAX_VALUE;
        for (int i=0; i<a.length; i++) {
            for (int j=0; j<a[i].length; j++) {
                min = Math.min(min, a[i][j]);
            }
        }
        return min;
    }
    
    /**
     * Returns the minimum of an int array.
     * @param a The array.
     * @return The minimum value of a. 0 if a.length==0.
     */
    public static int min(int[] a) {
        if (a.length == 0) {
            return 0;
        }
        if (a.length == 1) {
            return a[0];
        }
        int min = Integer.MAX_VALUE;
        for (int d:a) {
            min = Math.min(min, d);
        }
        return min;
    }
    
    /**
     * Returns the minimum of a double array.
     * @param a The double array.
     * @return The minimum value of a. Double.NaN if a.length==0.
     */
    public static double min(double[] a) {
        if (a.length == 0) {
            return Double.NaN;
        }
        if (a.length == 1) {
            return a[0];
        }
        double min = Double.MAX_VALUE;
        for (double d:a) {
            min = Math.min(min, d);
        }
        return min;
    }
    
    /**
     * Returns the variance of a double array.
     * @param a The double[].
     * @return The variance of the double array. Double.NaN if a.length==0.
     */
    public static double var(double[] a) {
        if (a.length == 0) {
            return Double.NaN;
        }
        if (a.length == 1) {
            return 0;
        }
        double m = mean(a);
        double sum = 0.0;
        for (double d:a) {
            sum += (d - m) * (d - m);
        }
        return sum/(a.length - 1);
    }
    
    /**
     * Calculates the Eucledian length of a double array.
     * @param a The double array.
     * @return  The Eucledian length of this double array. Double.NaN, if a has length 0.
     */
    public static double Euclidean_length(double[] a) {
        if (a.length == 0) {
            return Double.NaN;
        }
        if (a.length == 1) {
            return a[0];
        }
        double sum = 0.0;
        for (double d:a) {
            sum += d*d;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Calculates the Eucledian distance of two double arrays.
     * @param a The first double array.
     * @param b The second double array, same length as a.
     * @return  The Eucledian distance of these double arrays. Double.NaN, if a has length 0, or a has different length than b.
     */
    public static double Euclidean_distance(double[] a, double[] b) {
        if (a.length == 0 || a.length != b.length) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int i=0; i<a.length; i++) {
            sum += Math.pow(a[i]-b[i], 2);
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Calculates the standard deviation of vector a.
     * @param a The double array.
     * @return Math.sqrt(var(a))
     */
    public static double std(double[] a) {
        return Math.sqrt(var(a));
    }
    
    /**
     * Calculates the standard deviation of vector a.
     * @param a The double array.
     * @return Math.sqrt(var(a))
     */
    public static double std(Double[] a) {
        double[] a2 = new double[a.length];
        for (int i=0; i<a.length; i++) {
            a2[i] = a[i];
        }
        return Math.sqrt(var(a2));
    }
    
    /**
     * Calculates the AUC of a curve represented in x and y coordinates.
     * @param xs The x-coordinates of the curve.
     * @param ys The y-coordinates of the curve.
     * @return The AUC value.
     */
    public static double auc(double[] xs, double[] ys) {
        if (xs.length == 0) {
            return Double.NaN;
        }
        if (xs.length == 1) {
            return 0;
        }
        if (xs.length != xs.length) {
            Logger.getLogger(Misc.class.getName()).log(Level.WARNING, "Error at Misc.auc: xs and ys have to have the same length!");
            return Double.NaN; 
        }
        double A = 0;
        for (int i=1; i<xs.length; i++) {
            A += (xs[i]-xs[i-1]) * (ys[i]+ys[i-1])/2;
        }
        return A;
    }
    
    /**
     * Calculates the entropy of a double[].
     * @param vector The double[] to be investigated.
     * @return The vector's entropy.
     */
    public static double entropy(double[] vector) {
        double sum = 0;
        for (double d:vector) {
            sum += d;
        }
        for (int i=0; i<vector.length; i++) {
            vector[i] /= sum;
        }
        double H = 0;
        for (double d:vector) {
            H += d > 0 ? d * (Math.log(d)/Math.log(2)) : 0;
        }
        return -H;
    }
    
    /**
     * A hand-written function of the RGB to gray conversion.
     * @param R The red value.
     * @param G The green value.
     * @param B The blue value.
     * @return The gray value = 0.299 * R + 0.587 * G + 0.114 * B.
     */
    public static double RGBToGray(double R, double G, double B) {
        //return (0.21*R + 0.72*G + 0.07*B);
        return (0.299 * R + 0.587 * G + 0.114 * B);
    }
    
    /**
     * Writes a BufferedImage on hard disk.
     * @param bi The image to be saved.
     * @param filename The image file to be written.
     */
    public static void writeImage(BufferedImage bi, String filename) {
        try {
            File outputfile = new File(filename);
            outputfile.deleteOnExit();
            ImageIO.write(bi, FilePathStringtoExtension(filename), outputfile);
        } catch (IOException e) {
            Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Writes an Image on hard disk.
     * @param I The image to be saved.
     * @param filename The image file to be written.
     */
    public static void writeImage(Image I, String filename) {
        try {
            BufferedImage bi = toBufferedImage(I);
            File outputfile = new File(filename);
            outputfile.deleteOnExit();
            ImageIO.write(bi, FilePathStringtoExtension(filename), outputfile);
        } catch (IOException ex) {
            Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Converts an Image to BufferedImage.
     * @param image The image to be converted.
     * @return A new BufferedImage which looks same as the image.
     */
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }
    
    /**
     * Returns whether or not alpha is supported in this image.
     * @param image The image to be tested.
     * @return True, if alpha is supported in this image.
     */
    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }

    
    /**
     * Writes an Image on hard disk.
     * @param I The image data represented as double[][].
     * @param normalize If true, I will be scaled from min and max to 0 and 255. If false, values are interpreted to lie between 0 and 255.
     * @param filename The image file to be written.
     */
    public static void writeImage(double[][] I, boolean normalize, String filename) {
        double min = 0;
        double max = 255.0;
        if (normalize) {
            min = Double.MAX_VALUE;
            max = Double.MIN_VALUE;
            for (double[] I1 : I) {
                for (int y = 0; y < I1.length; y++) {
                    min = Math.min(min, I1[y]);
                    max = Math.max(max, I1[y]);
                }
            }
        }
        BufferedImage BI = new BufferedImage(I.length, I[0].length, BufferedImage.TYPE_BYTE_GRAY);
        int value; 
        for (int x=0; x<I.length; x++) {
            for (int y=0; y<I[x].length; y++) {
                value = (int) ((I[x][y]-min)*255/(max-min));
                BI.setRGB(x, y, (int)(Math.pow(2,16)*value + Math.pow(2,8)*value + value));
            }
        }
        writeImage(BI, filename);
    }
    
    /**
     * Writes the shape of the ROI into an image on harddisk. Only the ROI will be white, everything else black.
     * @param roi The ROI to be saved.
     * @param filename The file to be saved.
     */
    public static void writeROI(ROI roi, String filename) {
        if (roi!=null) {
            BufferedImage bi = roi.getAsImage().getAsBufferedImage();
            Misc.writeImage(bi, filename);
        }
    }
    
    /**
     * Writes the shape of the Roi into an image on harddisk. The image has the size of the underlying image. Only the ROI will be white, everything else black.
     * @param roi The ROI to be saved.
     * @param filename The file to be saved.
     */
    public static void writeROI(Roi roi, String filename) {
        if (roi!=null) {
            BufferedImage bi = new BufferedImage(roi.getImage().getWidth(), roi.getImage().getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            for (int i=0; i<bi.getWidth(); i++) {
                for (int j=0; j<bi.getHeight(); j++) {
                    if (roi.contains(i,j)) {
                        bi.setRGB(i, j, 0xFFFFFFFF);
                    }
                }
            }
            Misc.writeImage(bi, filename);
        }
    }

    /**
     * Returns the sum of a double[].
     * @param darray The array to be summed up.
     * @return The sum of the array.
     */
    public static double sum(double[] darray) {
        double sum = 0;
        for (double d: darray) {
            if (!Double.isNaN(d)) sum += d;
        }
        return sum;
    }
    
    /**
     * Returns the sum of an int[].
     * @param darray The array to be summed up.
     * @return The sum of the array.
     */
    public static int sum(int[] darray) {
        int sum = 0;
        for (int d: darray) {
            sum += d;
        }
        return sum;
    }
    
    /**
     * Multiplies each element in the int[] with k and returns the result as new int[].
     * @param a The source int[].
     * @param k The factor to be multiplied.
     * @return A new int[] of size a.length.
     */
    public static int[] ElemProduct(int[] a, double k) {
        int[] b = new int[a.length];
        for (int i=0; i<a.length; i++) {
            b[i] = (int) (k*a[i]);
        }
        return b;
    }
    
    /**
     * Returns a random sample of n elements out of 0 (inclusive) to k (exclusive), with or
     * without replacement.
     * @param n The number of items to draw.
     * @param k The upper limit (exclusive) integer to draw.
     * @param replacement If yes, drawing is done with replacement. If no, no replacement 
     * is done (then, n has to be smaller than k).
     * @return A int array of length n
     */
    public static int[] sample(int n, int k, boolean replacement) {
        if (!replacement && n>=k) {
            try {
                throw(new Exception("Random Sampling error: Cannot draw n>=k elements out of k without replacement."));
            } catch (Exception ex) {
                Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        int[] sample = new int[n]; 
        Random random = new Random();
        int d;
        for (int i=0; i<n; i++) {
            d = random.nextInt(k);
            if (!replacement) {
                if (Arrays.asList(sample).contains(d)) {
                    i--;
                    continue;
                }
            } 
            sample[i] = d;
        }
        return sample;
    }

    /**
     * Converts an x and y coordinate of a 2D data matrix into a running index.
     * @param x The x coordinate of the 2D data matrix.
     * @param y The y coordinate of the 2D data matrix.
     * @param width The with of the data matrix.
     * @return The running index of the coordinates.
     */
    public static int Sub2ind(int x, int y, int width) {
        return y * width + x;
    }

    /**
     * Converts an running index of a 2D Matrix into the x and y indices.
     * @param sub The container of size 2 for the x and y coordinate.
     * @param ind The running index of the data matrix.
     * @param width The width of the data matrix.
     */
    public static void Ind2sub(int[] sub, int ind, int width) {
        sub[1] = ind / width;
        sub[0] = ind - sub[1] * width;
    }
    
    /**
     * Returns the index of the integer in the given array.
     * @param array The array of integers.
     * @param a The integer to be searched.
     * @return The index of a in the array. -1, if the array is empty or a is not found.
     */
    public static int IndexOf(int[] array, int a) {
        if (array!=null) {
            for (int i=0; i<array.length; i++) {
                if (array[i]==a) return i;
            }
        }
        return -1;
    }

    /**
     * Grayscales an image.
     * @param source The colored source image.
     * @return A new grayscaled BufferedImage.
     */
    public static BufferedImage convertToGrayscale(BufferedImage source) {
        BufferedImageOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return op.filter(source, null);
    }

    /**
     * Translates a javax.​media.​jai.ROI (which supports Affine Transformations)
     * to a ij.​gui.Roi (which can be handled by ImagePlus). The Roi (output) is
     * the convex hull of the ROI (input).
     * @param roi the input javax.​media.​jai.ROI.
     * @return the convex hull of the roi as a new ij.​gui.Roi.
     */
    public static Roi ROIToRoi(ROI roi) {
        if (roi == null) return null;
        
        List<Integer> xpts_list = new ArrayList<>();
        List<Integer> ypts_list = new ArrayList<>();
        for (int i=0; i<roi.getAsImage().getWidth(); i++) {
            for (int j=0; j<roi.getAsImage().getHeight(); j++) {
                if (roi.contains(i,j)) {
                    xpts_list.add(i);
                    ypts_list.add(j);
                }
            }
        }
        int[] xpts = new int[xpts_list.size()];
        int[] ypts = new int[ypts_list.size()];
        for (int i=0; i<xpts_list.size(); i++) {
            xpts[i] = xpts_list.get(i);
            ypts[i] = ypts_list.get(i);
        }
        
        Polygon p = new Polygon(xpts, ypts, xpts.length);
        Roi roi2 = new PolygonRoi(p, Roi.FREEROI);
        if (xpts_list.size()>1) {
            try {
                // get the convexHull, otherwise it is a non meaningfull ziczac of polygon lines
                roi2 = new PolygonRoi(roi2.getConvexHull(), Roi.POLYGON);
            } catch (Exception e) {
                if (tmarker.DEBUG>0) {
                    Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, e);
                }
                roi2 = null;
            }
        }
        return roi2;
    }
    
    
    /**
     * Translates a javax.​media.​jai.ROI (which supports Affine Transformations)
     * to a ij.​gui.Roi (which can be handled by ImagePlus). The Roi (output) is
     * the convex hull of the ROI (input). The Roi is also translated to the right coordinates (roi.offset_x and roi.offset_y).
     * @param roi the input javax.​media.​jai.ROI.
     * @return the convex hull of the roi as a new ij.​gui.Roi.
     */
    public static Roi ROIToRoi(lROI roi) {
        if (roi == null) return null;
        
        List<Integer> xpts_list = new ArrayList<>();
        List<Integer> ypts_list = new ArrayList<>();
        for (int i=0; i<roi.getAsImage().getWidth(); i++) {
            for (int j=0; j<roi.getAsImage().getHeight(); j++) {
                if (roi.contains(i,j)) {
                    xpts_list.add(i);
                    ypts_list.add(j);
                }
            }
        }
        int[] xpts = new int[xpts_list.size()];
        int[] ypts = new int[ypts_list.size()];
        for (int i=0; i<xpts_list.size(); i++) {
            xpts[i] = xpts_list.get(i);
            ypts[i] = ypts_list.get(i);
        }
        
        Polygon p = new Polygon(xpts, ypts, xpts.length);
        p.translate(roi.offset_x, roi.offset_y);
        Roi roi2 = new PolygonRoi(p, Roi.FREEROI);
        if (xpts_list.size()>1) {
            try {
                // get the convexHull, otherwise it is a non meaningfull ziczac of polygon lines
                roi2 = new PolygonRoi(roi2.getConvexHull(), Roi.POLYGON);
            } catch (Exception e) {
                if (tmarker.DEBUG>0) {
                    Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, e);
                }
                roi2 = null;
            }
        }
        return roi2;
    }
    
    
   /**
    * Concatenates two int arrays.
    * @param A Array 1.
    * @param B Array 2.
    * @return A new int array of length A.length+B.length.
    */
   public static int[] concat(int[] A, int[] B) {
       int[] C= new int[A.length+B.length];
       System.arraycopy(A, 0, C, 0, A.length);
       System.arraycopy(B, 0, C, A.length, B.length);

       return C;
   }
   
   /**
    * Concatenates two double arrays.
    * @param A Array 1.
    * @param B Array 2.
    * @return A new double array of length A.length+B.length.
    */
   public static double[] concat(double[] A, double[] B) {
       double[] C= new double[A.length+B.length];
       System.arraycopy(A, 0, C, 0, A.length);
       System.arraycopy(B, 0, C, A.length, B.length);

       return C;
   }
   
   /**
    * Concatenates two String arrays.
    * @param A Array 1.
    * @param B Array 2.
    * @return A new String array of size A.length + B.length.
    */
   public static String[] concat(String[] A, String[] B) {
       String[] C= new String[A.length+B.length];
       System.arraycopy(A, 0, C, 0, A.length);
       System.arraycopy(B, 0, C, A.length, B.length);

       return C;
   }
   
   /**
    * Concatenates a String array to a string.
    * @param ss The string array to be concatenated.
    * @param delim The delimiter between two strings in the array (e.g. ";" or "").
    * @return A new String.
    */
   public static String ArrayToString(String[] ss, String delim) {
       String s2 = "";
       if (ss!=null) {
            for (int i=0; i<ss.length; i++) {
                s2 += ss[i];
                if (i<ss.length-1) s2 += delim;
            }
       }
       return s2;
   }
   
   /**
    * Calculates a histogram from a double array.
    * @param data The data array.
    * @param min The defined min value of the histogram (for the histogram range).
    * @param max The defined max value of the histogram (for the histogram range).
    * @param numBins The number of bins of the histogram.
    * @return A new int array with length numBins and the counts of the data in each bin.
    */
   public static int[] histogram(double[] data, double min, double max, int numBins) {
      final int[] result = new int[numBins];
      final double binSize = (max - min)/numBins;

      for (double d : data) {
        int bin = (int) ((d - min) / binSize); // changed this from numBins
        if (bin < 0) { /* this data is smaller than min */ }
        else if (bin >= numBins) { /* this data point is bigger than max */ }
        else {
          result[bin] += 1;
        }
      }
      return result;
   }
   
   /**
    * Calculates the similarty of two vectors = 1 / (e^(dist(u,v)/sqrt(n))), where dist is the Euclidean distance and n is the length of u and v.
    * @param u Vector 1.
    * @param v Vector 2.
    * @return The similarity of two vectors. 0 if the vectors have different lengths.
    */
   public static double CalculateSimilarity(double[] u, double[] v) {
       if (u.length==0) return 0;
       double sim = 1.0/Math.exp(Euclidean_distance(u,v)/Math.sqrt(u.length));
       return sim;
   }
   
   /**
    * Returns the text of the currently selected JButton in the JButtonGroup.
    * @param buttonGroup The JButtonGroup to be investigated.
    * @return The text of the selected JButton. Null if non is selected.
    */
   public static String getSelectedButtonText(ButtonGroup buttonGroup) {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return button.getText();
            }
        }

        return null;
    }
   
   /**
    * Selects the JButton with Text text in the buttonGroup.
    * @param buttonGroup The JButtonGroup in which a button should be selected.
    * @param text The text of the JButton to be selected.
    */
   public static void selectButtonWithText(ButtonGroup buttonGroup, String text) {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.getText().equals(text)) {
                button.setSelected(true);
                return;
            }
        }
    }
   
   /**
    * Returns an image screenshot of a JPanel.
    * @param panel The panel to be drawn.
    * @return The panel as BufferedImage. Null, if the panel is null.
    */
   public static BufferedImage getScreenShot(JPanel panel){
        if (panel==null) return null;
        BufferedImage bi = new BufferedImage(
            panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        panel.paint(bi.getGraphics());
        return bi;
    }
   
   /**
    * Transforms a List<Double> to a new double[].
    * @param list The Double list (is not changed).
    * @return A new double[] with the same value as list.
    */
   public static double[] DoubleListToArray(List<Double>list) {
       double[] array = new double[list.size()];
       for (int i=0; i<list.size(); i++) {
           array[i] = list.get(i);
       } 
       return array;
   }
   
   /**
    * Converts a list of Boolean to a int array. True becomes 1, False becomes 0.
    * @param list The Boolean list to be converted.
    * @return A new int[] with zeros and ones.
    */
   public static int[] BooleanListToIntArray(List<Boolean>list) {
       int[] array = new int[list.size()];
       for (int i=0; i<list.size(); i++) {
           array[i] = list.get(i) ? 1 : 0;
       } 
       return array;
   }
   
   /**
    * Transforms a List<Integer> to a new int[].
    * @param list The Integer list (is not changed).
    * @return A new int[] with the same value as list.
    */
   public static int[] IntegerListToArray(List<Integer> list) {
       int[] array = new int[list.size()];
       for (int i=0; i<list.size(); i++) {
           array[i] = list.get(i);
       } 
       return array;
   }
   
   
   /**
    * Calculates the expected and observed events per group and the variances.
    * It is adopted from the function Csurvfiff2.c from the R-project Package
    * "survival" (http://cran.r-project.org/web/packages/survival/).
    * This function implements the G-rho family of Harrington and Fleming (1982), 
    * with weights on each death of S(t)^rho, where S is the Kaplan-Meier estimate of survival. 
    * With rho = 0 this is the log-rank or Mantel-Haenszel test, and 
    * with rho = 1 it is equivalent to the Peto & Peto modification of the Gehan-Wilcoxon test.
    * Return Values are stored in obs, exp and var.
    * @param nn Number of observations.
    * @param nngroup Number of groups.
    * @param nstrat Number of stratification per group (mostly 1).
    * @param rho Parameter for weightning the deaths. 0 for LogRank or Mantel-Haenszel test OR 
    * 1 for Peto & Peto modification of the Gehan-Wilcoxon Test.
    * @param time Sorted time array of all n observations.
    * @param status Array of events (1) or censors (0) of all observations. Ordered by time.
    * @param group Array of group assignment of all observations. Ordered by time. Group numbers start at 0 and are continuously increased (0, 1, 2, ...).
    * @param strata In R: "strata = c(1 * (diff(strat[ord]) != 0), 1)", where strat is the assignment of the observations to the strata, ordered by time.
    * When there is no statification used, strata should be an array of (nn-1)x0 followed by a 1 ([0, 0, 0, 0, ..., 1]).
    * @param obs Observed events per group. An array of 0's with size = ngroup * nstrat. It is filled in the code.
    * @param exp Expected events per group. An array of 0's with size = ngroup * nstrat. It is filled in the code.
    * @param var Variances among the groups. An array of 0's with size = ngroup * ngroup. It is filled in the code.
    * @param risk Number of individuals under risk. Array of 0's with size nngroup. It is changed in the code.
    * @param kaplan Kaplan estimates. Array of 0's with size nn. It is changed in the code.
    */
   public static void survdiff2(int nn, int nngroup, int nstrat,
            double rho, double[] time, int[] status,
            int[] group, int[] strata, double[] obs,
            double[] exp, double[] var, double[] risk,
            double[] kaplan) {
        int i, j, k;
        int kk;
        int n, ngroup, ntot;
        int istart, koff;
        double km, nrisk, wt, tmp;
        double deaths;

        ntot = nn;
        ngroup = nngroup;
        istart = 0;
        koff = 0;
        for (i = 0; i < ngroup * ngroup; i++) {
            var[i] = 0;
        }
        for (i = 0; i < nstrat * ngroup; i++) {
            obs[i] = 0;
            exp[i] = 0;
        }

        while (istart < ntot) {  /* loop over the strata */
            for (i = 0; i < ngroup; i++) {
                risk[i] = 0;
            }

            /* last obs of this strata */
            for (i = istart; i < ntot; i++) {
                if (strata[i] == 1) {
                    break;
                }
            }
            n = i + 1;


            /*
             ** Compute the k-m, which is only needed if rho!=0
             **   We want it set up as a left-continuous function (unusual)
             */
            if (rho != 0) {
                km = 1;
                for (i = istart; i < n;) {
                    kaplan[i] = km;
                    nrisk = n - i;
                    deaths = status[i];
                    for (j = i + 1; j < n && time[j] == time[i]; j++) {
                        kaplan[j] = km;
                        deaths += status[j];
                    }
                    km = km * (nrisk - deaths) / nrisk;
                    i = j;
                }
            }

            /*
             ** Now for the actual test
             */
            for (i = n - 1; i >= istart; i--) {
                if (rho == 0) {
                    wt = 1;
                } else {
                    wt = Math.pow(kaplan[i], rho);
                }

                deaths = 0;
                for (j = i; j >= istart && time[j] == time[i]; j--) {
                    k = group[j]; // - 1;
                    deaths += status[j];
                    risk[k] += 1;
                    obs[k + koff] += status[j] * wt;
                }
                i = j + 1;
                nrisk = n - i;

                if (deaths > 0) {  /* a death time */
                    for (k = 0; k < ngroup; k++) {
                        exp[k + koff] += wt * deaths * risk[k] / nrisk;
                    }

                    if (nrisk == 1) {
                        continue;  /*only 1 subject, so no variance */
                    }
                    kk = 0;
                    wt = wt * wt;
                    for (j = 0; j < ngroup; j++) {
                        tmp = wt * deaths * risk[j] * (nrisk - deaths) / (nrisk * (nrisk - 1));
                        var[kk + j] += tmp;
                        for (k = 0; k < ngroup; k++) {
                            var[kk] -= tmp * risk[k] / nrisk;
                            kk++;
                        }
                    }
                }
            }
            istart = n;
            koff += ngroup;
        }
    }

   /**
    * Returns the 2D center of a given roi.
    * @param container The two center coordinates x, y are written here.
    * @param roi The ROI to be investigated.
    */
    public static void getROICentroid(double[] container, Roi roi) {
        //container[0] = Misc.sum(roi.getPolygon().xpoints)/(1.0*roi.getPolygon().npoints);
        //container[1] = Misc.sum(roi.getPolygon().ypoints)/(1.0*roi.getPolygon().npoints);
        container[0] = roi.getBounds().width/2;
        container[1] = roi.getBounds().height/2;
    }
    
    /**
     * A fast version to load a BufferedImage from a file.
     * @param file The image location on harddisk.
     * @return A new BufferedImage.
     */
    public static BufferedImage loadImageFast(String file) {
      try {
        FileInputStream in = new FileInputStream(file);
        FileChannel channel = in.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate((int)channel.size());
        channel.read(buffer);
        BufferedImage img = loadImage(buffer.array());
        
        //clean up
        buffer.clear();
        channel.close();
        in.close();
        //img.flush();
        
        return img;
        
        /*BufferedImage argbImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = argbImg.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        img.flush();
        return argbImg;*/
      }
      catch (Exception e) {
        if (tmarker.DEBUG>0) {
            Logger.getLogger(Misc.class.getName()).log(Level.INFO, "The image could not be loaded as ByteBuffer. Try to load it with ImageIO...", e);
        }
        try {
              return ImageIO.read(new File(file));
          } catch (IOException ex) {
              Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, ex);
              return null;
          }
      }
    }
    
    /**
     * A fast version to create a BufferedImage from a byte[].
     * @param data The image data.
     * @return A new BufferedImage with the data in there.
     * @throws IOException If data can't be read.
     */
    static BufferedImage loadImage(byte[] data) throws IOException {
        Image image = null;
        SeekableStream stream = new ByteArraySeekableStream(data);
        String[] names = ImageCodec.getDecoderNames(stream);
        ImageDecoder dec = ImageCodec.createImageDecoder(names[0], stream, null);
        RenderedImage im = dec.decodeAsRenderedImage();
        image = PlanarImage.wrapRenderedImage(im).getAsBufferedImage();
        return (BufferedImage) image;
        
    }

    /**
     * Tests if two polygons are the same.
     * @param p1 The polygon 1.
     * @param p2 The polygon 2.
     * @return True, if the polygons have same number of points, same order of points and same points.
     */
    public static boolean SamePolygons(Polygon p1, Polygon p2) {
        boolean same = p1.npoints==p2.npoints;
        if (same) {
            for (int i=0; i<p1.npoints; i++) {
                same &= p1.xpoints[i]==p2.xpoints[i] && p1.ypoints[i]==p2.ypoints[i];
            }
        }
        return same;
    }

    /**
     * Return a new scaled image with max width maxX and max height maxY.
     * @param I The original image (will not be changed).
     * @param maxX Max width of the scaled version.
     * @param maxY Max height of the scaled version.
     * @param factor If not null and not empty, the rezise factor is written into factor[0].
     * @return A new, scaled image. If I is smaller than maxX or maxY, a new Image with original size will be returned.
     */
    public static Image getScaledImageWithMaxSize(BufferedImage I, int maxX, int maxY, double[] factor) {
        int w = I.getWidth();
        int h = I.getHeight();
        if (w<maxX && h<maxY) {
            if (factor!=null && factor.length>0) factor[0] = 1.0;
            return (I.getScaledInstance(-1, -1, BufferedImage.SCALE_DEFAULT));
        } else {
            double f = Math.min(1.0*maxX/w, 1.0*maxY/h);
            if (factor!=null&& factor.length>0) factor[0] = f;
            return (I.getScaledInstance((int)(f*w), (int)(f*h), BufferedImage.SCALE_DEFAULT));
        }
    }
    
    /**
     * Returns the index of the min value in ds.
     * @param ds The double array.
     * @return The index of the min value. -1 if no min value has been found or ds is null or empty.
     */
    public static int min_arg(double[] ds) {
        if (ds!=null && ds.length>0) {
            double min = Misc.min(ds);
            for (int i=0; i<ds.length; i++) {
                if (ds[i]==min) return i;
            }
        }
        return -1;
    }
    
    /**
     * Creates a histogram of the color or intensity values in the given image. The histogram has
     * numBins bins per Band. Usually, gray images have one band, RGB images have three bands.
     * @param bi The specified image. Can be colored or gray scaled.
     * @param roi A region of interest within the image. Null for the whole image.
     * @param numBins Number of bins per band.
     * @return The histogram, normalized to unit sum.
     */
    public static double[] image2histogram(BufferedImage bi, ROI roi, int numBins) {
        Histogram hist = new Histogram(numBins, 0.0D, 256.0D, bi.getData().getNumBands());
        hist.countPixels(bi.getRaster(), roi, 0, 0, 1, 1);
                
        /*ParameterBlock pb = new ParameterBlock();
        
        int[] bins = { numBins };
        double[] low = { 0.0D };
        double[] high = { 256.0D };
        
        pb.addSource(bi);
        pb.add(null);
        pb.add(1);
        pb.add(1);
        pb.add(bins);
        pb.add(low);
        pb.add(high);
        
        RenderedOp op = JAI.create("histogram", pb, null);
        Histogram hist = (Histogram) op.getProperty("histogram");
        
        int numBands = hist.getNumBands();
        */
        
        double[] fv = new double[(bi.getData().getNumBands())*numBins];
        for (int i=0; i<bi.getData().getNumBands(); i++) {
            for (int j=0; j<hist.getNumBins(i); j++) {
                fv[(i*numBins+j)] = hist.getBins(i)[j];
            }
        }
        normalize(fv);
        return fv;
    }
    
    /**
     * Normalizes a vector to unit sum.
     * @param fv The vector is normalized (and altered).
     */
    public static void normalize(double[] fv) {
        double sum = Misc.sum(fv);
        if (sum > 0) {
            for (int i=0; i<fv.length; i++) {
                fv[i] /= sum;
            }
        }
    }
    
    /**
     * Converts the color represented in RGB format (0-255, 0-255, 0-255) to HTML format (000000 - FFFFFF).
     *
     * @param rgb The color RGB format, as <code>int[]</code> with three integers (0-255).
     * @return A <code>String</code> representing the HTML color (000000 - FFFFFF).
     */
    public static String Color2HTML(int[] rgb) {
        int r = rgb[0];
        r = Math.max(r, 0);
        r = Math.min(r, 255);
        int g = rgb[1];
        g = Math.max(g, 0);
        g = Math.min(g, 255);
        int b = rgb[2];
        b = Math.max(b, 0);
        b = Math.min(b, 255);
        String rHTML = Integer.toHexString(r);
        String gHTML = Integer.toHexString(g);
        String bHTML = Integer.toHexString(b);
        if (rHTML.length() < 2) {
            rHTML = "0" + rHTML;
        }
        if (gHTML.length() < 2) {
            gHTML = "0" + gHTML;
        }
        if (bHTML.length() < 2) {
            bHTML = "0" + bHTML;
        }
        return rHTML + gHTML + bHTML;
    }
    
    /**
     * Converts the color to HTML format (000000 - FFFFFF).
     *
     * @param color The color.
     * @return A <code>String</code> representing the HTML color (000000 - FFFFFF).
     */
    public static String Color2HTML(Color color) {
        return Color2HTML(new int[] {color.getRed(), color.getGreen(), color.getBlue()});
    }
    
    /**
     * Returns whether a given color is on the darker side of the color spectrum or on the
     * lighter side.
     * @param c The color to be tested.
     * @return True, if the color is quite dark and can be displayed on white background.
     * False, if the color is light and should not be displayed on white background (poor contrast). 
     */
    public static boolean getContrast50(Color c) {
        int r = c.getRed();
	int g = c.getGreen();
	int b = c.getBlue();
	int yiq = ((r*299)+(g*587)+(b*114))/1000;
	return (yiq < 190);
    }
}

package cish;

import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

/* 
 * Java_Otsu_Threshold
 * ImageJ PlugInFilter which implements Otsu's method for
 * thresholding an image.  Works with 8-Bit gray-scale images.
 *
 * Otsu's method: maximize inter-class variance between object and
 * background.  Define function to be optimized:
 *   Var = w_1(t)*w_2(t)*[\mu_1(t) - \mu_2(t)]**2
 *
 * Algorithm: (wikipedia)
 *   1. Compute histogram and probabilities of each gray level.
 *   2. Set up initial w_i and \mu_i
 *   3. Step through all possible thresholds t=1,...
 *      1. Update w_i and \mu_i
 *      2. Compute Var
 *   4. Desired threshold corresponds to maximum Var.
 *
 */
public class Otsu_Threshold implements PlugInFilter {
	
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }
        this.imp = imp;
        return DOES_8G;
    }

    // run
    public void run(ImageProcessor ip) {
        byte[] pixels = (byte[]) ip.getPixels();
        int numPixels = pixels.length;

        int bestThreshold = 0;
        double bestValue = 0.0;
        double currentValue = 0.0;

        double[] histogram = computeHist(pixels);

        // Maximize otsu function.
        for (int i = 0; i < 255; i++) {
            currentValue = otsu(histogram, i);
            if (currentValue > bestValue) {
                bestValue = currentValue;
                bestThreshold = i;
            }
        }

        //	IJ.showMessage("Best Threshold", Integer.toString(bestThreshold));
        System.out.println("Otsu_Threshold = " + bestThreshold);
                        //bestThreshold = 112;

		// Perform thresholding operation.
        int width = ip.getWidth();
        int height = ip.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (ip.get(x, y) < bestThreshold) {
                    ip.set(x, y, 0);
                } else {
                    ip.set(x, y, 255);
								}
						}
        }

    } // end run

    /**
     * Returns the Otsu-Threshold of a given image.
     * @param img The gray image to be computed. The image is normalized between 0 and 255.
     * @return The Otsu-Threshold of a given image.
     */
    public static int getOtsuThreshold(double[][] img) {
        int bestThreshold = 0;
        double bestValue = 0.0;
        double currentValue = 0.0;

        // normalize the image to the range 0-255
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double[] I1 : img) {
            for (int y = 0; y < I1.length; y++) {
                min = Math.min(min, I1[y]);
                max = Math.max(max, I1[y]);
            }
          }
            double[] histogram = computeHist(img, min, max);

        // Maximize otsu function.
        for (int i = 0; i < 255; i++) {
            currentValue = otsu(histogram, i);
            if (currentValue > bestValue) {
                bestValue = currentValue;
                bestThreshold = i;
            }
        }
            
            // normalize the threshold back to original img range
        bestThreshold = (int) ((bestThreshold + min) * (max - min) / 255.0);

        return bestThreshold;
    }

	 /**
     * Compute the histogram of pixel array.
     */
    private static double[] computeHist(double[][] pixels, double min, double max) {
        double[] histogram = new double[256];
        int i, j;
        for (i = 0; i <= 255; i++) {
            histogram[i] = 0;
        }
        for (i = 0; i < pixels.length; i++) {
            for (j = 0; j < pixels[i].length; j++) {
                histogram[(int) ((pixels[i][j] - min) * 255 / (max - min))]++;
            }
        }
        /*double s = Misc.sum(histogram);
         for (i=0; i<histogram.length; i++) {
         histogram[i] /= s;
         }*/
        return histogram;

    } // end computeHist
        
   /**
    * compute histogram (256 bins) of pixel array.
    */
    private static double[] computeHist(byte[] pixels) {
        int numPixels = pixels.length;
        double[] histogram = new double[256];
        int i, j;
        for (i = 0; i <= 255; i++) {
            histogram[i] = 0;
        }
        for (j = 0; j < numPixels; j++) {
            histogram[pixels[j] & 0x000000FF]++;
        }

        /*double s = Misc.sum(histogram);
         for (i=0; i<histogram.length; i++) {
         histogram[i] /= s;
         }*/
        return histogram;

    } // end computeHist

    /**
     * Otsu function.
     */
    private static double otsu(double[] histogram, int t) {

        double meanBg = 0;
        double meanFg = 0;
        long numBg = 0;
        long numFg = 0;
        double probBg, probFg;
        double variance;
        int i;

        // compute background mean
        for (i = 0; i < t; i++) {
            numBg += histogram[i];
            meanBg += i * histogram[i];
        }

        if (numBg > 0) {
            meanBg = meanBg / numBg;
        }

        // compute foreground mean
        for (i = t; i < 256; i++) {
            numFg += histogram[i];
            meanFg += i * histogram[i];
        }
        if (numFg > 0) {
            meanFg = meanFg / numFg;
        }

        // Compute otsu function.
        probBg = (double) numBg / (numBg + numFg);
        probFg = 1.0 - probBg;
        variance = probBg * probFg * Math.pow((meanBg - meanFg), 2);

        return variance;
    }

    private void showAbout() {
        IJ.showMessage("About Otsu Threshold", "Performs an Otsu threshold on 8-bit images.");
    }

}
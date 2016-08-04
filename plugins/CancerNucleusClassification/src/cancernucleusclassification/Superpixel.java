/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleusclassification;

import de.lmu.ifi.dbs.jfeaturelib.features.LocalBinaryPatterns;
import de.lmu.ifi.dbs.jfeaturelib.features.PHOG;
import de.lmu.ifi.dbs.jfeaturelib.features.SURF;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageStatistics;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.Histogram;
import javax.media.jai.ROI;
import tmarker.TMAspot.TMALabel;
import tmarker.TMAspot.TMApoint;
import tmarker.delaunay.ArraySet;
import tmarker.misc.Misc;
import tmarker.tmarker;

/**
 *
 * @author Peter J. Schueffler
 */
public class Superpixel implements Comparable {

    private final static int[] lbp_rotinv = {0, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 37, 39, 43, 45, 47, 51, 53, 55, 59, 61, 63, 85, 87, 91, 95, 111, 119, 127, 255};

    private final String id;
    private byte label;
    private boolean stained = false;
    private double probability = 0.5;
    private double entropy = -1;
    private double similarityToUnlabeledPoints_detection = Double.NaN;
    private double similarityToUnlabeledPoints_classification = Double.NaN;
    private double[] fv = null;
    
    private final SLICsuperpixels s;
    
    private int[] indices_inside;
    private int[] indices_border;
    
    SuperpixelPolygon sppoly = null;
            
    private int ins_i = 0;
    private int bor_i = 0;
    
    ArraySet<Superpixel> neighbours = new ArraySet<>();
    
    Superpixel(String id, byte label, SLICsuperpixels s, int innersize, int outersize) {
        this.id = id;
        this.label = label;
        this.s = s;
        indices_inside = new int[innersize];
        indices_border = new int[outersize];
    }
    
    public byte getLabel() {
        return label;
    }
    
    public boolean isStained() {
        return stained;
    } 
    
    public void setStained(boolean stained) {
        this.stained = stained;
    }
    
    boolean containsOnBorder(int j) {
        for (int i=0; i<indices_border.length; i++) {
            if (indices_border[i]==j) {
                return true;
            }
        }
        return false;
    }
    
    public boolean contains(int j) {
        for (int i=0; i<indices_border.length; i++) {
            if (indices_border[i]==j) {
                return true;
            }
        }
        for (int i=0; i<indices_inside.length; i++) {
            if (indices_inside[i]==j) {
                return true;
            }
        }
        return false;
    }

    public void setLabel(byte new_value) {
        label = new_value;
    }
    
    public void addBorderPoint(int i) {
        indices_border[bor_i++]=i;
        
        if (sppoly!=null) {
            int[] xy = new int[2];
            Misc.Ind2sub(xy, i, s.width);
            sppoly.addAreaPoint(xy[0], xy[1]);
        }
    }
    
    public void addInsidePoint(int i) {
        indices_inside[ins_i++]=i;
        
        if (sppoly!=null) {
            int[] xy = new int[2];
            Misc.Ind2sub(xy, i, s.width);
            sppoly.addAreaPoint(xy[0], xy[1]);
        }
    }
    
    public void addBorderPoints(int[] i) {
        indices_border = Misc.concat(indices_border, i);
        bor_i+=i.length;
        
        if (sppoly!=null) {
            int[] xy = new int[2];
            for (int j:i) {
                Misc.Ind2sub(xy, j, s.width);
                sppoly.addAreaPoint(xy[0], xy[1]);
            }
        }
    }
    
    public void addInsidePoints(int[] i) {
        indices_inside = Misc.concat(indices_inside, i);
        ins_i+=i.length;
        
        if (sppoly!=null) {
            int[] xy = new int[2];
            for (int j:i) {
                Misc.Ind2sub(xy, j, s.width);
                sppoly.addAreaPoint(xy[0], xy[1]);
            }
        }
    }
    
    /**
     * BorderPoints are border points of this superpixel. They are painted blueish
     * in the TMA image. However, only the "left" borderline points are included,
     * to make the bluish lines as thin as possible.
     * @return The borderline points of this superpixel which can be drawn blueish
     * in the TMA image. The coordinates refer to the whole image (not only the
     * superpixel area).
     */
    public int[] getBorderPoints() {
        return indices_border;
    }
    
    public int[] getInsidePoints() {
        return indices_inside;
    }
    
    /**
     * Returns the cicumference of this SP. It has been calculated during superpixel object creation.
     * @return The circumference of this superpixel in pixels.
     */
    public int getCircumference() {
        return indices_border.length;
    }
    
    /**
     * Returns a list of the neighbours of this superpixel.
     * @return A list of the neighbours of this superpixel.
     */
    public Set<Superpixel> getNeighbours() {
        return neighbours;
    }
    
    /**
     * Adds a superpixel to the neighbours list.
     */
    public void addNeighbour(Superpixel sp) {
        this.neighbours.add(sp);
    }
    
    /**
     * Sets the probability of this superpixel. Note that 0.5 is "neutral", 
     * 1 is perfect the one class and 0 is perfect the other class. If you want
     * to set a "normal" probability between [0;1], than use @see #setScaledPobability(double probability)
     * @param probability A probability in [0;1]. 0.5 is neutral,
     * 0 is class "negative", 1 is class "positive".
     */
    public void setProbability(double probability) {
        this.probability = probability;
    }
    
    /**
     * Returns the Classification certainty of this superpixel. 1 means class is certain,
     * 0 means class is unsure.
     * @return Value between 0 and 1.
     */
    public double getCertainty() {
        /*
        double c = 2.0*Math.abs(probability-0.5);
        /*/
        double c = 1.0-getClassEntropy();
        //*/
        return c;
    }
    
    /**
     * Returns the Classification entropy of this superpixel, normalized by
     * maximum entropy for two classes (log(2)). 1 means high entropy,
     * 0 means low entropy.
     * @return Value between 0 and 1.
     */
    public double getClassEntropy() {
        return probability == 1.0 ? 0 : probability == 0.0 ? 0 : -(probability*Math.log(probability)+(1.0-probability)*Math.log(1.0-probability)) / (Math.log(2.0));
    }
    
    public double getInformativeness(boolean classification) {
        double info = 1.0-getCertainty();
        if (getSimilarityToNeighbours(classification)!=Double.NaN) info+=getSimilarityToNeighbours(classification);
        return info;
    }
    
    public double getSimilarityToNeighbours(boolean classification) {
        return (classification ? similarityToUnlabeledPoints_classification : similarityToUnlabeledPoints_detection);
    }
    
    public void setSimilarityToNeighbours(double sim, boolean classification) {
        if (classification) {
            similarityToUnlabeledPoints_classification = sim;
        } else {
            similarityToUnlabeledPoints_detection = sim;
        }
    }
    
    public int getMinX() {
        int minx = s.getWidth();
        int[] txy = new int[2];
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(txy, getInsidePoints()[i], s.getWidth());
            minx = Math.min(minx, txy[0]);
        }
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(txy, getBorderPoints()[i], s.getWidth());
            minx = Math.min(minx, txy[0]);
        }
        return minx;
    }
    
    public int getMaxX() {
        int maxx = 0;
        int[] txy = new int[2];
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(txy, getInsidePoints()[i], s.getWidth());
            maxx = Math.max(maxx, txy[0]);
        }
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(txy, getBorderPoints()[i], s.getWidth());
            maxx = Math.max(maxx, txy[0]);
        }
        return maxx;
    }
    
    public int getMinY() {
        /*int[] txy = new int[2];
        int[] txy2 = new int[2];
        SLICsuperpixels.ind2sub(txy, getInsidePoints()[0], s.getWidth());
        SLICsuperpixels.ind2sub(txy2, getBorderPoints()[0], s.getWidth());
        return Math.min(txy2[1], txy[1]);*/
        int miny = s.getHeight();
        int[] txy = new int[2];
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(txy, getInsidePoints()[i], s.getWidth());
            miny = Math.min(miny, txy[1]);
        }
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(txy, getBorderPoints()[i], s.getWidth());
            miny = Math.min(miny, txy[1]);
        }
        return miny;
    }
    
    public int getMaxY() {
        /*int[] txy = new int[2];
        int[] txy2 = new int[2];
        SLICsuperpixels.ind2sub(txy, getInsidePoints()[getInsidePoints().length-1], s.getWidth());
        SLICsuperpixels.ind2sub(txy2, getBorderPoints()[getBorderPoints().length-1], s.getWidth());
        return Math.min(txy2[1], txy[1]);
        */
        int maxy = 0;
        int[] txy = new int[2];
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(txy, getInsidePoints()[i], s.getWidth());
            maxy = Math.max(maxy, txy[1]);
        }
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(txy, getBorderPoints()[i], s.getWidth());
            maxy = Math.max(maxy, txy[1]);
        }
        return maxy;
    }
    
    /**
     * Label is on first place, rest is feature.
     * @param img The image out of which the patches are drawn.
     * @return 
     */
    public double[] getFeaturevectorWithLabel(BufferedImage img) {
        return getFeaturevectorWithLabel(s.CNC, getPatches(img), this);
    }
    
    /**
     * Label is on first place, rest is feature.
     * @param patches A list of three objects (Patch_rgb, Patch_gray, ROI).
     * @return 
     */
    public static double[] getFeaturevectorWithLabel(CancerNucleusClassification CNC, List<Object> patches, Superpixel sp) {
        
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        
        // the featureVector
        int fv_size = 1;
        if (CNC.getParam_useFeature_ColorHist()) {
            fv_size += 24;
        }
        if (CNC.getParam_useFeature_IMGStatistics()) {
            fv_size += 8;
        }
        if (CNC.getParam_useFeature_LBP()) {
            fv_size += 36; // 36 for rotational inveariant version; 256 for raw version;
        }
        if (CNC.getParam_useFeature_LBP2()) {
            fv_size += CNC.getParam_useFeature_LBP2_NumBins(); // LPB for jFeaturelib LBP Version;
        }
        if (CNC.getParam_useFeature_PHOG()) {
            fv_size += 80;
        }
        if (CNC.getParam_useFeature_SURF()) {
            fv_size += 64;
        }
        if (CNC.getParam_useFeature_Gabor()) {
            fv_size += 60;
        }
        if (CNC.getParam_useFeature_Entropy()) {
            fv_size += 1;
        }
        if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_FGBGColor()) {
            fv_size += 9;
        }
        if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_1DSignature()) {
            fv_size += 16;
        }
        if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_PathSegmentCode()) {
            fv_size += 8;
        }
        if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_Circularity()) {
            fv_size += 1;
        }
        if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_Roundness()) {
            fv_size += 1;
        }
        if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_AreaSize()) {
            fv_size += 1;
        }
        
        double[] fvl = new double[fv_size];
        
        if (patches!=null) {
            ImagePlus bi_col = new ImagePlus(" ", (BufferedImage) patches.remove(0));
            ImagePlus bi_gray = new ImagePlus(" ", (BufferedImage) patches.remove(0));
            ROI roi = (ROI) patches.remove(0);
            Roi roi2 = null;
            
            if (roi!=null) {
                roi2 = Misc.ROIToRoi(roi);
                bi_col.setRoi(roi2);
                bi_gray.setRoi(roi2);
//                if (tmarker.DEBUG>5) {
//                    // Draw Polygons
//                    try {
//                        Misc.writeROI(roi, t.getTmpDir() + File.separator + "ROI_vorher.png");
//                        Misc.writeROI(roi2, t.getTmpDir() + File.separator + "Roi_nachher.png");
//                    } catch (Exception e){
//                       e.printStackTrace();
//                    }
//                }
            }
            
            int pos = 1;
            double[] signature = null; // used for 1D signature and for Roundness.
            // Image Histogramm as featurevector
            if (CNC.getParam_useFeature_ColorHist()) {
                int binSize = 8;
                double[] fv_tmp = image2histogram(bi_col.getBufferedImage(), roi, binSize, 3); // 8 bin * 3 colors = 24. 4th band for alpha, but this will be cut out.
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 24;
            }

            // Skewness, Kurtosis and Moments as featurevector
            if (CNC.getParam_useFeature_IMGStatistics()) {
                double[] fv_tmp = image2Statistics(bi_gray);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 8;
            }

            // LPB as featurevector
            if (CNC.getParam_useFeature_LBP()) {
                double[] fv_tmp = image2LBP(bi_col, true);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 36; // 36 for rotational inveariant version; 256 for raw version;
            }
            
            // LPB2 as featurevector
            if (CNC.getParam_useFeature_LBP2()) {
                double[] fv_tmp = image2LBP2(bi_col, CNC.getParam_useFeature_LBP2_NumPoints(),
                        CNC.getParam_useFeature_LBP2_Radius(),
                        CNC.getParam_useFeature_LBP2_NeighborhoodSize(),
                        CNC.getParam_useFeature_LBP2_Offset(),
                        CNC.getParam_useFeature_LBP2_NumBins());
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += CNC.getParam_useFeature_LBP2_NumBins(); // 
            }

            // PHOG as featurevector
            if (CNC.getParam_useFeature_PHOG()) {
                int numbins = 8;
                int depth = 1; // 8/1: length 80, 8/2: length 336, 8/3: length 1360
                double[] fv_tmp = image2PHOG(bi_col, numbins, depth);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 80;
            }

            // SURF as featurevector
            if (CNC.getParam_useFeature_SURF()) {
                double[] fv_tmp = image2SURF(bi_col, sp);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 64;
            }
            // Gabor Texture as featurevector
            if (CNC.getParam_useFeature_Gabor()) {
                double[] fv_tmp = image2Gabor(bi_gray);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 60;
            }
            // Entropy as featurevector
            if (CNC.getParam_useFeature_Entropy()) {
                double[] fv_tmp = image2Entropy(bi_gray.getBufferedImage(), roi, sp);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 1;
            }
            // FG / BG Color Ratio as featurevector
            if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_FGBGColor()) {
                double[] fv_tmp = image2FGBGColor(bi_col, roi);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 9;
            }
            // 1D-Signature as featurevector
            if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_1DSignature()) {
                signature = Signature1D(roi, roi2, CNC.getParam_useFeature_1DSignature_scaleInvariant(), CNC.getParam_useFeature_1DSignature_rotationInvariant(), CNC.getParam_useFeature_1DSignature_derivative());
                System.arraycopy(signature, 0, fvl, pos, signature.length);
                pos += 16;
            }
            // PathSegmentCode as featurevector
            if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_PathSegmentCode()) {
                double[] fv_tmp = PathSegmentCode(bi_gray.getRoi());
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 8;
            }
            // Circularity
            if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_Circularity()) {
                double[] fv_tmp = Circularity(sp, roi2);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 1;
            }
            // Roundness
            if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_Roundness()) {
                //if (signature==null) {
                //    signature = Signature1D(roi, bi_gray.getRoi(), CNC.getParam_useFeature_1DSignature_scaleInvariant(), CNC.getParam_useFeature_1DSignature_rotationInvariant(), CNC.getParam_useFeature_1DSignature_derivative());
                //}
                double[] fv_tmp = Roundness(roi2);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 1;
            }
            // Area/Size
            if (CNC.getParam_useFeature_Segmentation() && CNC.getParam_useFeature_AreaSize()) {
                double[] fv_tmp = AreaSize(roi);
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 1;
            }
            
            if (sp!=null) {
                fvl[0] = sp.getLabel();
                sp.fv = fvl;
            }
        }
        return fvl;
    }
    
    /**
     * Calculates the Featurevector without label (useful for clustering).
     * @param patches A list of three objects (Patch_rgb, Patch_gray, ROI).
     * @return 
     */
    public static double[] getClusteringFeaturevector(tmarker t, List<Object> patches) {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        
        // the featureVector
        int fv_size = 0;
        if (true){//CNC.getParam_useFeature_ColorHist()) {
            fv_size += 8;
        }
        if (true){//CNC.getParam_useFeature_IMGStatistics()) {
            fv_size += 8;
        }
        double[] fvl = new double[fv_size];
        
        if (patches!=null) {
            ImagePlus bi_col = new ImagePlus(" ", (BufferedImage) patches.remove(0));
            ImagePlus bi_gray = new ImagePlus(" ", (BufferedImage) patches.remove(0));
            ROI roi = (ROI) patches.remove(0);
            
            if (roi!=null) {
                Roi roi2 = null;
                roi2 = Misc.ROIToRoi(roi);
                bi_col.setRoi(roi2);
                bi_gray.setRoi(roi2);
                if (tmarker.DEBUG>5) {
                    // Draw Polygons
                    Misc.writeROI(roi, t.getTmpDir() + File.separator + "ROI_vorher.png");
                    Misc.writeROI(roi2, t.getTmpDir() + File.separator + "Roi_nachher.png");
                }
            }
            
            int pos = 0;
            // Image Histogramm as featurevector
            if (true){//CNC.getParam_useFeature_ColorHist()) {
                int binSize = 8;
                double[] fv_tmp = image2histogram(bi_gray.getBufferedImage(), roi, binSize, 1); // 8 bin * 1 colors = 24. 4th band for alpha, but this will be cut out.
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 8;
            }
            if (true){//CNC.getParam_useFeature_IMGStatistics()) {
                double[] fv_tmp = image2Statistics(bi_gray); 
                System.arraycopy(fv_tmp, 0, fvl, pos, fv_tmp.length);
                pos += 8;
            }

        }
        return fvl;
    }
    
    /**
     * Label is on first place, rest is feature. Null, if getFeatureVectorWithLabel()
     * hasn't be called before.
     * @return Featurevector of Superpixel.
     */
    public double[] getCalculatedFeatureVectorWithLabel() {
        return fv;
    }
    
    public static double[] image2histogram(BufferedImage bi, ROI roi, int numBins, int numBands) {
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
        
        double[] fv = new double[(numBands)*numBins];
        for (int i=0; i<numBands; i++) {
            for (int j=0; j<hist.getNumBins(i); j++) {
                fv[(i*numBins+j)] = hist.getBins(i)[j];
            }
        }
        normalize(fv);
        return fv;
    }
    
    public static double[] image2Statistics(ImagePlus bi) {
        ImageStatistics stats = bi.getStatistics(ImageStatistics.MEAN
                + ImageStatistics.STD_DEV
                + ImageStatistics.SKEWNESS
                + ImageStatistics.KURTOSIS
                + ImageStatistics.MIN_MAX
                + ImageStatistics.MEDIAN
                + ImageStatistics.AREA);
        return new double[] {stats.mean/255.0, stats.stdDev, stats.skewness, stats.kurtosis, stats.min, stats.max, stats.median, stats.area};
    }
    
    /**
     * Calculates the Local Binary Pattern of the superpixel. This is a histogram
     * over the LPBs of every pixels in the SP. The Local Binary Pattern of a pixel
     * is a 8-bit binary number. Starting from the neighbor pixel on top of the source pixel,
     * a bit of the number is set to 1, if the neighbor pixel's intensity is smaller than the
     * intensity of the source pixel, and 0 otherwise. The resulting 8-bit binary from the 8 
     * neighborpixels is transformed to a decimal number and a 256-bin histogram (for 256 possible
     * 8-bit numbers) is increased by one on this position.
     * 
     * If rotational_invariant is true, there are only 36 possible 8-bit numbers, which can be
     * converted by bit-shifting (rotation of the neighbors) to each other.
     * @param bi a gray-scaled image (only the intensity is interesting for LPB)
     * @param roi the ROI of the superpixel boundaries
     * @param rotational_invariant If true, the rotational invariant version of LBP is returned,
     * otherwise the raw version.
     * @return if rotational_invariant is true, 36 bin feature vector with the LBP, else a
     * 256 bin feature vector.
     */
    public static double[] image2LBP(ImagePlus bi, boolean rotational_invariant) {
        int[] dx8 = {-1, -1,  0,  1, 1, 1, 0, -1};
        int[] dy8 = { 0, -1, -1, -1, 0, 1, 1,  1};
        double [] fvl;
        
        if (rotational_invariant) {
            fvl = new double[lbp_rotinv.length];
        } else {
            fvl = new double[256];
        }
        Arrays.fill(fvl, 0);
        int lbp;
        int k=0;
        int min;
        int ind;
        for (int i=1; i<bi.getWidth()-1; i++) {
            for (int j=1; j<bi.getHeight()-1; j++) {
                lbp = 0;
                if (bi.getRoi()==null ||bi.getRoi().contains(i, j)) {
                    for (int z=0; z<8; z++) {
                        lbp = lbp<<1;
                        if (bi.getPixel(i, j)[0]>=bi.getPixel(i+dx8[z], j+dy8[z])[0]) {
                            lbp++;
                        }
                    }
                    if (rotational_invariant) {
                        // rotational invariance, 8 rotations possible
                        min = lbp;
                        for (int z=0; z<7; z++) {
                            if (lbp%2==0) {
                                lbp /= 2;
                            } else {
                                lbp = (lbp+255)/2;
                            }
                            min = Math.min(min, lbp);
                        }
                        ind = Arrays.binarySearch(lbp_rotinv, min);
                        if (ind >= 0) {
                            fvl[ind]++;
                            k++;
                        }
                    } else {
                        fvl[lbp]++;
                        k++;
                    }
                }
            }
        }
        // to find out the lbp_rotinv variable
        /*int[][] rows = new int[256][8];
        for (int i=0; i<256; i++) {
            for (int j=0; j<8; j++) {
                rows[i][j] = (i * (int)Math.pow(2, j))%255;
            }
        }
        for (int i=0; i<256; i++) {
            Arrays.sort(rows[i]);
            for (int j=0; j<8; j++) {
                System.out.print(Integer.toString(rows[i][j]) + " ");
            }
            System.out.println();
        }
        Set set = new ArraySet();
        for (int i=0; i<256; i++) {
            set.add(rows[i]);
        }*/
        
        if (k>0) {
            normalize(fvl);
        }
        return fvl;
    }
    
    // Calculates the local binary patterns via the jFeatureLib
    public static double[] image2LBP2(ImagePlus imp, int numPoints, int radius, int sizeNeighborhood, int offset, int numbins) {
        LocalBinaryPatterns lbp = new LocalBinaryPatterns();
        lbp.setNumPoints(numPoints);
        lbp.setRadius(radius);
        lbp.setConstant(offset);
        lbp.setNeighborhoodSize(sizeNeighborhood);
        lbp.setNumberOfHistogramBins(numbins);
        lbp.run(imp.getProcessor());
        List<double[]> data = lbp.getFeatures();
        double[] fv = new double[data.get(0).length-2];
        for (double[] datapx: data) {
            for (int i=2; i<datapx.length; i++) {
                fv[i-2] += datapx[i];
            }
        }
        normalize(fv);
        return fv;
    }
    
    // Calculates the local binary patterns via the jFeatureLib
    public static double[] image2LBP2(ImagePlus imp, int numPoints, int radius, int sizeNeighborhood, int offset, int numbins, int x, int y) {
        LocalBinaryPatterns lbp = new LocalBinaryPatterns();
        lbp.setNumPoints(numPoints);
        lbp.setRadius(radius);
        lbp.setConstant(offset);
        lbp.setNeighborhoodSize(sizeNeighborhood);
        lbp.setNumberOfHistogramBins(numbins);
        lbp.run(imp.getProcessor(), x, y);
        double[] fv = Arrays.copyOfRange(lbp.getFeatures().get(0), 2, lbp.getFeatures().get(0).length) ;
        normalize(fv);
        return fv;
    }
    
    public static double[] image2PHOG(ImagePlus imp, int numbins, int depth) {
        PHOG phog = new PHOG();
        phog.setBins(numbins);
        phog.setRecursions(depth);
        phog.run(imp.getProcessor());
        //List<double[]> data = phog.getFeatures();
        double[] fv = phog.getFeatures().get(0);
        //normalize(fv);
        return fv;
    }
    
    public static double[] image2GLCM(ImagePlus imp) {
        GLCM_Texture glcm = new GLCM_Texture();
        glcm.run(imp.getProcessor());
        double[] fv = null;// = glcm.getFeatures().get(0); 
        normalize(fv);
        return fv;
    }
    
    public static double[] image2SIFT(ImagePlus imp) {
        //TODO implement SIFT features
        double[] fv = null;
        return fv;
    }
    
    public static double[] image2SURF(ImagePlus imp, Superpixel sp) {
        /*Class<ImageFloat32> integralType = GIntegralImageOps.getIntegralType(ImageFloat32.class);
        DescribePointSurf<ImageFloat32> descriptor = FactoryDescribePointAlgs.<ImageFloat32>msurf(integralType);
        ImageFloat32 img = new ImageFloat32(bi.getWidth(), bi.getHeight());
        descriptor.setImage(ConvertBufferedImage.convertFrom(bi, img));
        SurfFeature desc = descriptor.describe((sp.getMaxX()+sp.getMinX())/2, (sp.getMaxY()+sp.getMinY())/2, 1, 0, null);
        if (desc != null) {
            return desc.getValue();
        } else {
            double[] fvec = new double[64];
            for (int i=0; i<64; i++) {
                fvec[i] = 0;                
            }
            return fvec;
        }*/
        SURF surf = new SURF();
        surf.runDescription(imp.getProcessor());
        double[] fv = Arrays.copyOfRange(surf.getFeatures().get(0), 6, 70);
        //normalize(fv);
        return fv;
    }
    
    /*public static double[] image2SIFT(ImagePlus imp) {
        // TODO Implement SIFT in Java.
        double[] fv;
        Param p = new Param();
        FloatArray2DSIFT fa = new FloatArray2DSIFT(p);
        SIFT sift = new SIFT(fa);
        Collection<Feature> features = sift.extractFeatures(imp.getProcessor());
        for (Feature f: features) {
            
        }
        return fv;
    }*/
    
    public static double[] image2Gabor(ImagePlus imp) {
        //Gabor g = new Gabor();
        //g.run(imp.getProcessor());
        //double[] fv = g.getFeatures().get(0);
        net.semanticmetadata.lire.imageanalysis.Gabor g = new net.semanticmetadata.lire.imageanalysis.Gabor();
        double[] fv = g.getNormalizedFeature(imp.getBufferedImage());
        //normalize(fv);
        return fv;
    }
    
    public static double[] image2Entropy(BufferedImage bi, ROI roi, Superpixel sp) {
        double[] fvl = new double[1];
        if (sp!=null && sp.entropy>=0) {
            fvl[0] = sp.entropy;
        } else {
            double[] hist = image2histogram(bi, roi, 32, 1);
            fvl[0] = Misc.entropy(hist);
            if (sp!=null) {
                fvl[0] = sp.entropy = fvl[0];
            }
        }
        return fvl;
    }
    
    /**
     * Calculates the feature vector which sets the foregound color in relation to the
     * background color. This can be used for cytoplasm staining classification.
     * The median R, G and B values are calculated for the ROI (Foreground FG) 
     * and the region not in ROI (Background BG). These values are concatenated:
     * [ R_FG, G_FG, B_FG, R_BG, G_BG, B_BG ]
     * @param bi_col The colored nucleus patch.
     * @param roi The segmented nucleus as region of interest.
     * @return The feature vector of the foreground (nucleus) and background color.
     */
    private static double[] image2FGBGColor(ImagePlus bi_col, ROI roi) {
        
        // get the number of pixels in FG and BG
        int n_FG = 0;
        int n_BG = 0;
        for (int i=0; i<bi_col.getWidth(); i++) {
            for (int j=0; j<bi_col.getHeight(); j++) {
                if (roi != null && roi.contains(i, j)) {
                    n_FG++;
                } else {
                    n_BG++;
                }
            }
        }
        
        int[] R_FG = new int[n_FG];
        int[] G_FG = new int[n_FG];
        int[] B_FG = new int[n_FG];
        
        int[] R_BG = new int[n_BG];
        int[] G_BG = new int[n_BG];
        int[] B_BG = new int[n_BG];
        
        int i_FG = 0;
        int i_BG = 0;
        int[] col;
        for (int i=0; i<bi_col.getWidth(); i++) {
            for (int j=0; j<bi_col.getHeight(); j++) {
                col = bi_col.getPixel(i, j);
                if (roi != null && roi.contains(i, j)) {
                    R_FG[i_FG] = col[0];
                    G_FG[i_FG] = col[1];
                    B_FG[i_FG] = col[2];
                    i_FG++;
                } else {
                    R_BG[i_BG] = col[0];
                    G_BG[i_BG] = col[1];
                    B_BG[i_BG] = col[2];
                    i_BG++;
                }
            }
        }
        
        Arrays.sort(R_FG);
        Arrays.sort(R_BG);
        Arrays.sort(G_FG);
        Arrays.sort(G_BG);
        Arrays.sort(B_FG);
        Arrays.sort(B_BG);
        
        double mRFG = median(R_FG);
        double mGFG = median(G_FG);
        double mBFG = median(B_FG);
        double mRBG = median(R_BG);
        double mGBG = median(G_BG);
        double mBBG = median(B_BG);
        
        return new double[]{mRFG, mGFG, mBFG, mRBG, mGBG, mBBG, mRFG/mRBG, mGFG/mGBG, mBFG/mBBG};
    }
    
    /**
     * Calculates the median from an int[].
     * @param m The int[] MUST BE SORTED.
     * @return The median of the array.
     */
    public static double median(int[] m) {
        if (m.length == 0) return Double.NaN;
        if (m.length == 1) return m[0];
        int middle = m.length/2;
        if (m.length%2 == 1) {
            return m[middle];
        } else {
            return (m[middle-1] + m[middle]) / 2.0;
        }
    }
    
    /**
     * Generates a 1D Signature Featurevector of a superpixel (ROI). It is a 16-bin
     * vector for 16 directions (e, nee, ne, nne, n, nnw, nw, nww, w, sww, sw, ssw
     * s, sse, se, see). For each direction, the 1D Signature contains the distance
     * from the ROI centerpoint to the border. This is a shape decriptor.
     * @param roi The ROI from which the 1D Signature is to be calculated.
     * @param scale_invariant If true, the distances are devided by the longest distance.
     * @param rotation_invariant If true, the longest distance appears first in the array, followed by counterclockwise distances.
     * @param derivative Of true, not the distances are returned, but the differences between neighbored distances.
     * @return A 16-direction 1D Signature.
     */
    public static double[] Signature1D(ROI roi, Roi roi2, boolean scale_invariant, boolean rotation_invariant, boolean derivative) {
        
        double s_e_x=0, s_ne_x=0, s_n_x=0, s_nw_x=0, s_w_x=0, s_sw_x=0, s_s_x=0, s_se_x=0;
        double s_nee_x=0, s_nne_x=0, s_nnw_x=0, s_nww_x=0, s_sww_x=0, s_ssw_x=0, s_sse_x=0, s_see_x=0;
        double s_e_y=0, s_ne_y=0, s_n_y=0, s_nw_y=0, s_w_y=0, s_sw_y=0, s_s_y=0, s_se_y=0;
        double s_nee_y=0, s_nne_y=0, s_nnw_y=0, s_nww_y=0, s_sww_y=0, s_ssw_y=0, s_sse_y=0, s_see_y=0;
        
        if (roi!=null && ((PolygonRoi)roi2).getNCoordinates()>0) {
            double roi_mx = roi.getAsImage().getWidth()/2; //centroid[0];
            double roi_my = roi.getAsImage().getHeight()/2; //centroid[1];
            // if centroid is not in roi, shift the centroid to the nearest point in roi.
            if (!roi.contains(roi_mx, roi_my)) {
                int delta = 1; 
                while(!roi.contains(roi_mx+delta, roi_my) && !roi.contains(roi_mx, roi_my-delta) && !roi.contains(roi_mx-delta, roi_my) && !roi.contains(roi_mx, roi_my+delta)) {
                    delta++;
                }
                if (roi.contains(roi_mx+delta, roi_my)) {
                    roi_mx+=delta;
                } else if (roi.contains(roi_mx, roi_my-delta)) {
                    roi_my-=delta;
                } else if (roi.contains(roi_mx-delta, roi_my)) {
                    roi_mx-=delta;
                } else if (roi.contains(roi_mx, roi_my+delta)) {
                    roi_my+=delta;
                }
            }
            while (roi.contains(roi_mx+s_e_x,  roi_my       ))   { s_e_x++;           }
            while (roi.contains(roi_mx+s_nee_x, roi_my-s_nee_y)) { s_nee_x++; s_nee_y=s_nee_y+0.4;}
            while (roi.contains(roi_mx+s_ne_x, roi_my-s_ne_y))   { s_ne_x++; s_ne_y++;}
            while (roi.contains(roi_mx+s_nne_x, roi_my-s_nne_y)) { s_nne_x=s_nne_x+0.4; s_nne_y++;}
            while (roi.contains(roi_mx       , roi_my-s_n_y ))   { s_n_y++;           }
            while (roi.contains(roi_mx+s_nnw_x, roi_my-s_nnw_y)) { s_nnw_x=s_nnw_x+0.4; s_nnw_y++;}
            while (roi.contains(roi_mx-s_nw_x, roi_my-s_nw_y))   { s_nw_x++; s_nw_y++;}
            while (roi.contains(roi_mx+s_nww_x, roi_my-s_nww_y)) { s_nww_x++; s_nww_y=s_nww_y+0.4;}
            while (roi.contains(roi_mx-s_w_x,  roi_my       ))   { s_w_x++;           }
            while (roi.contains(roi_mx+s_sww_x, roi_my-s_sww_y)) { s_sww_x++; s_sww_y=s_sww_y+0.4;}
            while (roi.contains(roi_mx-s_sw_x, roi_my+s_sw_y))   { s_sw_x++; s_sw_y++;}
            while (roi.contains(roi_mx+s_ssw_x, roi_my-s_ssw_y)) { s_ssw_x=s_ssw_x+0.4; s_ssw_y++;}
            while (roi.contains(roi_mx,        roi_my+s_s_y ))   { s_s_y++;           }
            while (roi.contains(roi_mx+s_sse_x, roi_my-s_sse_y)) { s_sse_x=s_ssw_x+0.4; s_sse_y++;}
            while (roi.contains(roi_mx+s_se_x, roi_my+s_se_y))   { s_se_x++; s_se_y++;}
            while (roi.contains(roi_mx+s_see_x, roi_my-s_see_y)) { s_see_x++; s_see_y=s_see_y+0.4;}
        }
        
        double[] sig = new double[]{
            Math.sqrt(Math.pow(s_e_x,2) + Math.pow(s_e_y,2)),
            Math.sqrt(Math.pow(s_nee_x,2) + Math.pow(s_nee_y,2)),
            Math.sqrt(Math.pow(s_ne_x,2) + Math.pow(s_ne_y,2)),
            Math.sqrt(Math.pow(s_nne_x,2) + Math.pow(s_nne_y,2)),
            Math.sqrt(Math.pow(s_n_x,2)  + Math.pow(s_n_y,2)),
            Math.sqrt(Math.pow(s_nnw_x,2) + Math.pow(s_nnw_y,2)),
            Math.sqrt(Math.pow(s_nw_x,2) + Math.pow(s_nw_y,2)),
            Math.sqrt(Math.pow(s_nww_x,2) + Math.pow(s_nww_y,2)),
            Math.sqrt(Math.pow(s_w_x,2)  + Math.pow(s_w_y,2)),
            Math.sqrt(Math.pow(s_sww_x,2) + Math.pow(s_sww_y,2)),
            Math.sqrt(Math.pow(s_sw_x,2) + Math.pow(s_sw_y,2)),
            Math.sqrt(Math.pow(s_ssw_x,2) + Math.pow(s_ssw_y,2)),
            Math.sqrt(Math.pow(s_s_x,2)  + Math.pow(s_s_y,2)),
            Math.sqrt(Math.pow(s_sse_x,2) + Math.pow(s_sse_y,2)),
            Math.sqrt(Math.pow(s_se_x,2) + Math.pow(s_se_y,2)),
            Math.sqrt(Math.pow(s_see_x,2) + Math.pow(s_see_y,2))
        };
        
        // derivative - do not take the raw values but their differences.
        double[] sig_derivate = new double[sig.length];
        if (derivative) {
            for (int i=0; i<sig_derivate.length-1; i++) {
                sig_derivate[i] = sig[i+1]-sig[i];
            }
            sig_derivate[sig_derivate.length-1] = sig[0]-sig[sig.length-1];
        } else {
            System.arraycopy(sig, 0, sig_derivate, 0, sig.length);
        }
        
        // scale invariance - normalize by max 
        if (scale_invariant) {
            double max = Misc.max(sig_derivate);
            if (max>0) {
                for (int i=0; i<sig_derivate.length; i++) {
                    sig_derivate[i] /= max;
                }
            }
        }
        
        // rotation invariance - align to the biggest signature first, then counterclockwise.
        double[] sig_rotinv = new double[sig_derivate.length];
        if (rotation_invariant) {
            //find arg max
            double max = 0;
            int amax = 0;
            for (int i=0; i<sig_derivate.length; i++) {
                if (sig_derivate[i]>max) {
                    amax = i;
                    max = sig_derivate[i];
                }
            }
            for (int i=amax; i<sig_rotinv.length+amax; i++) {
                sig_rotinv[i-amax] = sig_derivate[i%sig_derivate.length];
            }
        } else {
            System.arraycopy(sig_derivate, 0, sig_rotinv, 0, sig_derivate.length);
        }
        
        return sig_rotinv;
    }
    
    /**
     * Returns a path segment code for the given roi. The roi is described as polygon and an iterator
     * walks along the polygon. The walk is described as going north, north-west, west, south-west, south, 
     * south-east, east or north-east (8 directions). A normalized histogram (sum 1) over the 8 directions is returned.
     * @param roi The roi to be described.
     * @return A 8 bin histogram of the PathSegmentCode (or [0,0,0,0,0,0,0,0] if roi is null or its polygon contains less than 3 points).
     */
    public static double[] PathSegmentCode(Roi roi) {
        double[] psc = new double[] {0, 0, 0, 0, 0, 0, 0, 0};
        if (roi!=null) {
            Polygon p = roi.getPolygon();
            if (p!=null && p.npoints>2) {
                int old_x = p.xpoints[0];
                int old_y = p.ypoints[0];
                int k;
                double dx,dy,a;
                for (int i=1; i<=p.npoints; i++) {
                    k = i % p.npoints;
                    dx = p.xpoints[k]-old_x;
                    dy = p.ypoints[k]-old_y;
                    a = Math.toDegrees(Math.atan2(dx, dy));
                    if (a<0) {
                        a = 360 + a;
                    }
                    a = Math.abs(a/45);
                    psc[(int)a]++;
                    
                    old_x = p.xpoints[k];
                    old_y = p.ypoints[k];
                }
            }
            Misc.normalize(psc);
        }
        return psc;
    }
    
    /**
     * The circularity of a Superpixel is defined by
     * 2.0*Math.sqrt(Math.PI*A)/U, where A is the area and U is the circumference,
     * both in pixels. If sp==null or U==0, [Double.NaN] is returned.
     * @param sp The superpixel fromt which the circularity is calculated.
     * @param roi If the superpixel sp is null, the circularity is calculated from the Roi.
     * @return the circularity of this superpixel. One double in an array.
     */
    public static double[] Circularity(Superpixel sp, Roi roi) {
        double[] c = new double[]{Double.NaN};
        if (sp!=null) {
            double A = sp.getSize();
            double U = sp.getCircumference();
            c[0] = (2.0*Math.sqrt(Math.PI*A)/U);
        } else if (roi!=null) {
            double A = getArea(roi);
            double U = 0;
            boolean isIn_old;
            Rectangle r = roi.getBounds();
            for (int i=r.x; i<r.x+r.width; i++) {
                isIn_old = false;
                for (int j=r.y; j<r.y+r.height; j++) {
                    if (roi.contains(i, j)!=isIn_old) {
                        U++;
                        isIn_old = roi.contains(i, j);
                    }
                }
            }
            c[0] = (2.0*Math.sqrt(Math.PI*A)/U);
        }
        return c;
    }
    
    /**
     * Returns the area in pixels of a given Roi.
     * @param roi The Roi to be measured.
     * @return Area of this Roi (= number of points within or on border of ROI).
     * @author Peter Schüffler
     */
    public static double getArea(Roi roi) {
        if (roi == null) {
            return Double.NaN;
        }
        double A = 0.0;
        Rectangle rect = roi.getBounds();
        for (int i=rect.x; i<rect.x+rect.width; i++) {
            for (int j=rect.y; j<rect.y+rect.height; j++) {
                if (roi.contains(i, j)) A++;
            }
        }
        return A;
    }
    
    /**
     * Returns the area in pixels of a given Roi.
     * @param roi The Roi to be measured.
     * @return Area of this Roi (= number of points within or on border of ROI).
     * @author Peter Schüffler
     */
    public static double getArea(ROI roi) {
        if (roi == null) {
            return Double.NaN;
        }
        double A = 0.0;
        Rectangle rect = roi.getBounds();
        for (int i=rect.x; i<rect.x+rect.width; i++) {
            for (int j=rect.y; j<rect.y+rect.height; j++) {
                if (roi.contains(i, j)) A++;
            }
        }
        return A;
    }
    
    /**
     * Returns the roundness of the signature of a given ROI. Roundness is the ratio of smallest
     * diameter to largest diameter. If signature is null or the largest diameter is 0, [Double.NaN] is returned.
     * @param roi The 1D-signature as calculated around this roi.
     * @return The roundness of the 1DSignature. One double in an array.
     */
    public static double[] Roundness(Roi roi) {
        double[] r = new double[]{Double.NaN};
        /*if (signature!=null) {
            double[] diams = new double[signature.length/2];
            for (int i=0; i<diams.length; i++) {
                diams[i] = signature[i] + signature[i+signature.length/2];
            }
            double max_diam = Misc.max(diams);
            double min_diam = Misc.min(diams);
            r[0] = min_diam/max_diam;
        }
        return r;
        */
        try {
            double[] feret = roi.getFeretValues();
            r[0] = feret[2]/feret[0];
        } catch (Exception e) {
            
        }
        return r;
    }
    
    /**
     * Returns the size of the Area of the ROI.
     * @param roi The Roi to be processed.
     * @return The size of this roi in pixels.
     */
    public static double[] AreaSize(ROI roi) {
        double[] r = new double[]{Double.NaN};
        try {
            r[0] = getArea(roi);
        } catch (Exception e) {
            
        }
        return r;
    }

    public Point calculateMidPoint() {
        int x = (int) (getMinX() + (getMaxX()-getMinX())/2);
        int y = (int) (getMinY() + (getMaxY()-getMinY())/2);
        if (!contains(Misc.Sub2ind(x, y, s.getWidth()))) {
            int[] xy = new int[2];
            Misc.Ind2sub(xy, getInsidePoints()[(getInsidePoints().length/2)], s.getWidth());
            x = xy[0];
            y = xy[1];
        }
        return new Point(x,y);
    }
    
    public TMApoint mergeToTMAPoint(double confidence) {
        if (getCertainty() > confidence) {
            Point p = calculateMidPoint();
            TMApoint tp = new TMApoint(s.getTMAspot(), p.x, p.y, label);
            return tp;
        }
        return null;
    }

    boolean touchesRectangle(Rectangle r) {
        return touchesRectangle(r, 1);
    }
    
    boolean touchesRectangle(Rectangle r, double zoom) {
        return (int)(getMaxX()*zoom)>=r.x && (int)(getMinX()*zoom)<=r.x+r.width && (int)(getMaxY()*zoom)>=r.y && (int)(getMinY()*zoom)<=r.y+r.height;
    }

    public void flipLabel() {
        if (label==TMALabel.LABEL_POS) {
            label = TMALabel.LABEL_NEG;
        } else if (label==TMALabel.LABEL_NEG) {
            label = TMALabel.LABEL_UNK;
        } else if (label==TMALabel.LABEL_UNK) {
            label = TMALabel.LABEL_BG;
        } else if (label==TMALabel.LABEL_BG) {
            label = TMALabel.LABEL_POS;
        }
    }
    
    public boolean touchesRectangleBorder(Rectangle r) {
        return touchesRectangleBorder(r, 1);
    }
    
    public boolean touchesRectangleBorder(Rectangle r, double zoom) {
        int maxX = (int)(getMaxX()*zoom);
        int maxY = (int)(getMaxY()*zoom);
        int minX = (int)(getMinX()*zoom);
        int minY = (int)(getMinY()*zoom);
        return maxX>=r.x && minX<=r.x ||
               maxX>=r.x+r.width-1 && minX<=r.x+r.width-1 ||
               maxY>=r.y && minY<=r.y ||
               maxY>=r.y+r.height-1 && minY<=r.y+r.height-1;
    }


    @Override
    public int compareTo(Object o) {
        Superpixel tmp = (Superpixel) o;
        return (int) (Math.signum(this.getCertainty() - tmp.getCertainty()));
    }

    public static void normalize(double[] fv) {
        double sum = Misc.sum(fv);
        if (sum > 0) {
            for (int i=0; i<fv.length; i++) {
                fv[i] /= sum;
            }
        }
    }
    
    public double[] getIntensities(BufferedImage img) {
        double[] intensities = new double[getBorderPoints().length + getInsidePoints().length];
        int[] sub = new int[2];
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(sub, getBorderPoints()[i], s.getWidth());
            intensities[i] = img.getRGB(sub[0], sub[1]);
        }
        for (int i=getBorderPoints().length; i<getBorderPoints().length+getInsidePoints().length; i++) {
            Misc.Ind2sub(sub, getInsidePoints()[i-getBorderPoints().length], s.getWidth());
            intensities[i] = img.getRGB(sub[0], sub[1]);
        }
        return intensities;
    }
    
    public double getEntropy() {
        return entropy;
    }
    
    /**
     * Returns the numeric ID of this superpixel.
     * @return The superpixel unique ID.
     */
    public String getID() {
        return id;
    }
    
    public void calculateEntropy(BufferedImage img) {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        
        List<Object> patches = getPatches(img);
        BufferedImage bi_gray = (BufferedImage) patches.get(1);
        ROI roi = (ROI) patches.get(2);
        
        image2Entropy(bi_gray, roi, this);
    }
    
    /**
     * Returns the rectangular patch around the superpixel as color buffered image, 
     * gray buffered image and the javax.​media.​jai.​ROI describing the superpixel.
     * @param img The whole TMA image as BufferedImage.
     * @return A list with three objects: bi_col (buffered image), bi_gray (buffered image)
     * and roi (javax.​media.​jai.​ROI)
     */
    public List<Object> getPatches (BufferedImage img) {
        List<Object> out = new ArrayList<Object>();
        
        int maxx = getMaxX();
        int maxy = getMaxY();
        int minx = getMinX();
        int miny = getMinY();
        BufferedImage bi_col = new BufferedImage(maxx-minx+1, maxy-miny+1, BufferedImage.TYPE_INT_ARGB);
        // initialize with -1
        for (int i=0; i<bi_col.getWidth(); i++) {
            for (int j=0; j<bi_col.getHeight(); j++) {
                bi_col.setRGB(i, j, 0);
            }
        }
        
        // fill in image info within the superpixel shape
        int[] sub = new int[2];
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(sub, getBorderPoints()[i], s.getWidth());
            bi_col.setRGB(sub[0]-minx, sub[1]-miny, img.getRGB(sub[0], sub[1]));
        }
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(sub, getInsidePoints()[i], s.getWidth());
            bi_col.setRGB(sub[0]-minx, sub[1]-miny, img.getRGB(sub[0], sub[1]));
        }
        
        // ROI is the shape of the superpixel
        BufferedImage bi_gray = new BufferedImage(bi_col.getWidth(), bi_col.getHeight(), BufferedImage.TYPE_BYTE_GRAY);  
        Graphics g = bi_gray.getGraphics();  
        g.drawImage(bi_col, 0, 0, null);  
        g.dispose(); 
        if (tmarker.DEBUG>5) {
            Misc.writeImage(bi_col, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_col1.png");
            Misc.writeImage(bi_gray, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_gray1.png");
        }        
        ROI roi = new ROI(bi_gray, 1);
        // run this otherwise roi.contains() returns true always.
        for (int i=0; i<bi_gray.getWidth(); i++) {
            for (int j=0; j<bi_gray.getHeight(); j++) {
                if (roi.contains(i,j)) {
                }
            }            
        }
        // END run this
        
        if (tmarker.DEBUG>5) {
            for (int i=0; i<bi_gray.getWidth(); i++) {
                for (int j=0; j<bi_gray.getHeight(); j++) {
                    System.out.print(bi_gray.getData().getSample(i, j, bi_gray.getData().getNumBands()-1)); System.out.print(" ");
                }
                System.out.println("num Bands: " + Integer.toString(bi_gray.getData().getNumBands()));
            }
        }
        if (tmarker.DEBUG>5) {
            for (int i=0; i<bi_gray.getWidth(); i++) {
                for (int j=0; j<bi_gray.getHeight(); j++) {
                    if (roi.contains(i,j)) {
                            System.out.print("1");
                        } else {
                            System.out.print(".");
                        }
                }
                System.out.println("num Bands: " + Integer.toString(bi_gray.getData().getNumBands()));
            }
        }
        
        // complete the bi_col and bi_gray
        for (int i=0; i<bi_col.getWidth(); i++) {
            for (int j=0; j<bi_col.getHeight(); j++) {
                if (bi_col.getRGB(i, j)==0) {
                    bi_col.setRGB (i, j, img.getRGB(i+minx, j+miny));
                }
            }
        }
        g = bi_gray.getGraphics();  
        g.drawImage(bi_col, 0, 0, null);  
        g.dispose(); 
        if (tmarker.DEBUG>5) {
            Misc.writeImage(bi_col, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_col2.png");
            Misc.writeImage(bi_gray, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_gray2.png");
        }    
        
        if (tmarker.DEBUG>5) {
            for (int i=0; i<bi_gray.getWidth(); i++) {
                for (int j=0; j<bi_gray.getHeight(); j++) {
                    if (roi.contains(i,j)) {
                        bi_gray.setRGB(i, j, Color.WHITE.getRGB());
                    } else {
                        bi_gray.setRGB(i, j, Color.BLACK.getRGB());
                    }
                }
            }
            Misc.writeImage(bi_gray, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_gray3.png");
        }    
        
        out.add(bi_col);
        out.add(bi_gray);
        out.add(roi);
        
        return out;
    }

    /**
     * Return the size (area) of this superpixel, including borderline points.
     * @return The size of this superpixel in pixels.
     */
    public int getSize() {
        return indices_border.length + indices_inside.length;
    }
    
    /**
     * Same as getSize().
     * @return getSize().
     */
    public int getArea() {
        return getSize();
    }
    
    /**
     * Get the width of the superpixel
     * @return The Width of the superpixel
     */
    public int getWidth() {
        return getMaxX() - getMinX();
    }

    /**
     * Get the height of the superpixel
     * @return The height of the superpixel
     */
    public int getHeight() {
        return getMaxY() - getMinY();
    }

    /**
     * Debug Function: Save the SP as PNG image.
     * @param name Filename with extension.
     */
    public void saveAsPNG(String filename) {
        try {
            BufferedImage b = new BufferedImage(getWidth()+1, getHeight()+1, BufferedImage.TYPE_INT_ARGB);
            int[] sub = new int[2];
            int minX = getMinX();
            int minY = getMinY();
//            System.out.println();
//            System.out.println("bw: " + b.getWidth());
//            System.out.println("bh: " + b.getHeight());
//            System.out.println("minx: " + minX);
//            System.out.println("miny: " + minY);
            for (int ind: getBorderPoints()) {
                Misc.Ind2sub(sub, ind, s.getWidth());
//                System.out.println("  Koordinates: " + sub[0] + " " + sub[1]);
//                System.out.println("  Koordinates corr: " + (sub[0]-minX) + " " + (sub[1]-minY));
                b.setRGB(sub[0]-minX, sub[1]-minY, 0xFFFFFFFF);
            }
            ImageIO.write(b, "PNG", new File(filename));
        } catch (IOException ex) {
            Logger.getLogger(SLICsuperpixels.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}

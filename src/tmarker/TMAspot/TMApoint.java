/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker.TMAspot;

import ij.gui.Roi;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.media.jai.ROI;
import tmarker.misc.lROI;
import tmarker.tmarker;

/**
 * A TMApoint is an instance of a nucleus. It has a certain label (malignant = LABEL_POS, benign = LABEL_NEG, unknown = LABEL_UNK). Also background
 * points are TMApoints (LABEL_BG) for foreground/background classification. A TMApoint also has a certain staining (STAINING_0-3). STAINING_0 means clear nucleus.
 * STAINING_1-3 means mild to marked staining. A TMApoint can be hand-drawn by a pathologist or user (goldStandard > 0) or estimated by the computer (goldStandard==0).
 * @author Peter J. Schueffler
 */
public class TMApoint extends TMALabel {
    
    public static transient final byte CONSENSUS = Byte.MAX_VALUE; // for consensus points
    public static transient final byte ESTIMATED = 0; // for consensus points
    
    private byte label = TMALabel.LABEL_UNK;
    private byte staining = TMALabel.STAINING_0;
    private byte goldStandard = 0;
    private boolean isTrainingsPoint_classification = false;
    private boolean isTrainingsPoint_detection = false;
    
    /**
     * The segmented nucleus. Null, if none.
     */
    private lROI lROI = null;
    
    /**
     * The convex hull of the segmentation. Null, if none.
     */
    private Roi lroi = null;

    
    
    private TMAspot ts = null;
   
    /**
     * Constructor for TMApoint.
     * @param ts The parent TMAspot.
     * @param x The point's x-coord.
     * @param y The point's y-coord.
     * @param label The point's label (e.g. TMAspot.LABEL_POS).
     */
    public TMApoint(TMAspot ts, int x, int y, byte label) {
        this.ts = ts;
        this.x = x;
        this.y = y;
        this.label = label;
    }
    
    /**
     * Constructor for TMApoint.
     * @param ts The parent TMAspot.
     * @param x The point's x-coord.
     * @param y The point's y-coord.
     * @param label The point's label (e.g. TMAspot.LABEL_POS).
     * @param stained The point's staining (e.g. TMAspot.STAINING_0).
     */
    public TMApoint(TMAspot ts, int x, int y, byte label, byte stained) {
        this.ts = ts;
        this.x = x;
        this.y = y;
        this.label = label;
        this.staining = stained;
    }
    
    @Override
    public TMApoint clone() {
        TMApoint tp = new TMApoint(ts, x, y, label, staining);
        tp.goldStandard = this.goldStandard;
        tp.isTrainingsPoint_classification = this.isTrainingsPoint_classification;
        tp.isTrainingsPoint_detection = this.isTrainingsPoint_detection;
        
        return tp;
    }
    
    /**
     * Returns this point's label.
     * @return This point's label.
     */
    public byte getLabel() {
        return label;
    } 
    
    /**
     * Sets this point's label.
     * @param label This point's label.
     */
    public void setLabel(byte label) {
        this.label = label;
    }
    
    /**
     * Returns whether or not this point is stained.
     * @return True if this point's staining != Staining_0.
     */
    public boolean isStained() {
        return staining>TMALabel.STAINING_0;
    } 
    
    /**
     * Returns this point's staining.
     * @return This point's staining (e.g. STAINING_0).
     */
    public byte getStaining() {
        return staining;
    } 
    
    /**
     * Sets this point's staining.
     * @param staining This point's staining (e.g. STAINING_0).
     */
    public void setStaining(byte staining) {
        this.staining = staining;
    }
    
    /**
     * Sets the parent TMAspot.
     * @param ts The parent TMAspot.
     */
    public void setTMAspot(TMAspot ts) {
        this.ts = ts;
    }
    
    /**
     * Returns the parent TMAspot.
     * @return The parent TMAspot.
     */
    public TMAspot getTMAspot() {
        return ts;
    }
    
    /**
     * Returns the segmentation of this TMApoint (localized ROI).
     * ROIs support affine transforms, but they are not supported by ImagePlus.
     * This ROI can be transformed to a convex hull roi by Misc.ROIToRoi(ROI).
     * @return The pixelwise segmentation of this spot, null if not generated yet.
     */
    public lROI getROI() {
        return lROI;
    }

    /**
     * Sets the pixelwise segmentation of this TMApoint (ROI).
     * ROIs are stored as rectangular ROI patches centered at the TMApoint. Usually,
     * the ROI patch is of size 2*radius in x and y direction.
     * See javax.​media.​jai.ROI documentation for more details.
     * This function does not set the corresponding convex hull roi, but you can do that by calling setRoi(Misc.ROIToRoi(ROI)).
     * @param lROI The segmentation of this TMApoint.
     */
    public void setROI(lROI lROI) {
        this.lROI = lROI;
    }
    
    /**
     * Returns the convex hull roi of this TMApoint (Roi).
     * @return The convex hull of the segmentation of this spot, null if not generated yet.
     */
    public Roi getRoi() {
        return lroi;
    }

    /**
     * Sets the convex hull segmentation of this TMApoint (Roi)
     * Rois are stored as polygons like objects and can be used by ImagePlus. The coordinate space is the image coordinate space.
     * This function does not set the pixelwise ROI.
     * @param lroi The convex hull of the segmentation of this TMApoint.
     */
    public void setRoi(Roi lroi) {
        this.lroi = lroi;
    }
    
    
    /**
     * Flips the label of this TMApoint.
     * @param binary If true, LABEL_POS swaps to LABEL_NEG and vice versa. If false,
     * the flipping is LABEL_POS -> LABEL_NEG -> LABEL_UNK -> LABEL_BG.
     */
    public void flipLabel(boolean binary) {
        if (binary) {
            if (label==TMALabel.LABEL_POS) {
                label = TMALabel.LABEL_NEG;
            }
            else if (label==TMALabel.LABEL_NEG) {
                label = TMALabel.LABEL_POS;
            }
        } else {
            if (label==TMALabel.LABEL_POS) {
                label = TMALabel.LABEL_NEG;
            }
            else if (label==TMALabel.LABEL_NEG) {
                label = TMALabel.LABEL_UNK;
            }
            else if (label==TMALabel.LABEL_UNK) {
                label = TMALabel.LABEL_BG;
            }
            else if (label==TMALabel.LABEL_BG) {
                label = TMALabel.LABEL_POS;
            }
        }
    }
    
    /**
     * Flips the Staining Intensity of this TMApoint.
     * @param binary If true, STAINING_0 swaps to STAINING_3 and vice versa. If false,
     * the flipping is STAINING_0 -> STAINING_1 -> STAINING_2 -> STAINING_3.
     */
    public void flipStaining(boolean binary) {
        if (binary) {
            if (staining!=TMALabel.STAINING_3) {
                staining = TMALabel.STAINING_3;
            }
            else {
                staining = TMALabel.STAINING_0;
            }
        } else {
            if (staining==TMALabel.STAINING_0) {
                staining = TMALabel.STAINING_1;
            }
            else if (staining==TMALabel.STAINING_1) {
                staining = TMALabel.STAINING_2;
            }
            else if (staining==TMALabel.STAINING_2) {
                staining = TMALabel.STAINING_3;
            }
            else if (staining==TMALabel.STAINING_3) {
                staining = TMALabel.STAINING_0;
            }
        }
    }
    
    /**
     * Gold Standard is an expert's label, in contrast to the estimated/predicted
     * computer's label. Since several labels of experts are possible
     * (goldStandard 1, 2, ...), this function returns goldStandard>0.
     * @return True, if this point has been created by an expert. False, if this
     * point has been created by the computer.
     */
    public boolean isGoldStandard() {
        return goldStandard > 0;
    }
    
    /**
     * Gold Standard is an expert's label, in contrast to the estimated/predicted
     * computer's label. Since several labels of experts are possible
     * (goldStandard 1, 2, ...), this function returns a byte.
     * @return 0 for computer's label (estimated/predicted), 1,2,3... for 
     * expert's number (gold standard 1,2,3...).
     */
    public byte getGoldStandard() {
        return goldStandard;
    }

    /**
     * Sets the gold-standard parameter.
     * @param goldStandard The gold-standard number of the labeler who created this point.
     * 0 if this point is estimated by the computer.
     */
    public void setGoldStandard(byte goldStandard) {
        this.goldStandard = goldStandard;
    }
    
    /**
     * Returns whether this point is used as training a classifier.
     * @param classification If true the classifier is for benign/cancerous classification. Otherwise for foreground/background classification (=detection).
     * @return True if this point is used for training the corresponding classifier.
     */
    public boolean isTrainingPoint(boolean classification) {
        if (classification) {
            return isTrainingsPoint_classification;
        }
        else {
            return isTrainingsPoint_detection;
        }
    }

    /**
     * Sets whether this point is used as training a classifier.
     * @param classification If true the classifier is for benign/cancerous classification. Otherwise for foreground/background classification (=detection).
     * @param isTrainingsPoint True if this point is used for training the corresponding classifier.
     */
    public void setTrainingsPoint(boolean classification, boolean isTrainingsPoint) {
        if (classification) {
            this.isTrainingsPoint_classification = isTrainingsPoint;
        }
        else {
            this.isTrainingsPoint_detection = isTrainingsPoint;
        }
    }
    
    /**
     * Returns the rectangular patch around this TMApoint as color buffered image, 
     * gray buffered image and the javax.​media.​jai.​ROI describing the whole patch.
     * @param I The TMA image.
     * @param patchsize The patch size.
     * @return A list with three objects: bi_col (buffered image patch), bi_gray (buffered image patch)
     * and roi (javax.​media.​jai.​ROI)
     */
    public List<Object> getPatches (BufferedImage I, int patchsize) {
        List<Object> out = new ArrayList<Object>();
        
        BufferedImage bi_col = new BufferedImage(I.getWidth(), I.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi_col.createGraphics();
        try {
            g.drawImage(I.getSubimage(x-patchsize/2, y-patchsize/2, patchsize, patchsize), x-patchsize/2, y-patchsize/2, null);
        } catch (RasterFormatException ex) {
            System.err.println("Image Patch for point " + x + ", " + y + " out of borders of image " + ts.getName());
            return out;
        }
        bi_col = bi_col.getSubimage(x-patchsize/2, y-patchsize/2, patchsize, patchsize);
        BufferedImage bi_gray = new BufferedImage(bi_col.getWidth(), bi_col.getHeight(), BufferedImage.TYPE_BYTE_GRAY);  
        Graphics gr = bi_gray.getGraphics();  
        gr.drawImage(bi_col, 0, 0, null);  
           
        
        if (tmarker.DEBUG>5) {
            try {
                File bi_col1 = new File(getTMAspot().getTmpDir() + File.separator + x + "_" + y + "_patch_col1.png"); bi_col1.deleteOnExit();
                File bi_gray1 = new File(getTMAspot().getTmpDir() + File.separator + x + "_" + y + "_patch_gray1.png"); bi_gray1.deleteOnExit();
                ImageIO.write(bi_col, "PNG", bi_col1);
                ImageIO.write(bi_gray, "PNG", bi_gray1);
                
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }    
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");

        ROI roi = new ROI(bi_gray, 0);
        // run this otherwise roi.contains() returns true always.
        for (int i=0; i<bi_gray.getWidth(); i++) {
            for (int j=0; j<bi_gray.getHeight(); j++) {
                if (roi.contains(i,j)) {
                }
            }            
        }
        // END run this
        
        if (tmarker.DEBUG>5) {
            String info = "";
            for (int i=0; i<bi_gray.getWidth(); i++) {
                for (int j=0; j<bi_gray.getHeight(); j++) {
                    info += bi_gray.getData().getSample(i, j, bi_gray.getData().getNumBands()-1) + " ";
                }
                info += "num Bands: " + Integer.toString(bi_gray.getData().getNumBands()) + "\n";
            }
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, info);
        }
        if (tmarker.DEBUG>5) {
            String info = "";
            for (int i=0; i<bi_gray.getWidth(); i++) {
                for (int j=0; j<bi_gray.getHeight(); j++) {
                    if (roi.contains(i,j)) {
                            info += "1";
                        } else {
                            info += ".";
                        }
                }
                info += "num Bands: " + Integer.toString(bi_gray.getData().getNumBands()) + "\n";
            }
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, info);
        }
        
        g.dispose(); 
        gr.dispose(); 
        
        if (tmarker.DEBUG>5) {
            try {                
                for (int i=0; i<bi_gray.getWidth(); i++) {
                    for (int j=0; j<bi_gray.getHeight(); j++) {
                        if (roi.contains(i,j)) {
                            bi_gray.setRGB(i, j, Color.WHITE.getRGB());
                        } else {
                            bi_gray.setRGB(i, j, Color.BLACK.getRGB());
                        }
                    }
                }
                File bi_gray3 = new File(getTMAspot().getTmpDir() + File.separator + x + "_" + y + "_patch_gray3.png"); bi_gray3.deleteOnExit();
                ImageIO.write(bi_gray, "PNG", bi_gray3);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }    
        
        out.add(bi_col);
        out.add(bi_gray);
        out.add(roi);
        
        return out;
    }

    
    
}

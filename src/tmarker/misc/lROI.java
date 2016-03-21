/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker.misc;

import ij.gui.Roi;
import java.awt.image.RenderedImage;
import javax.media.jai.ROI;

/**
 *
 * @author Peter J. Schueffler
 */
public class lROI extends ROI {
    
    public int offset_x;
    
    public int offset_y;
    
    
    public lROI (RenderedImage RI, int threshold, int offset_x, int offset_y) {
        super(RI, threshold);
        this.offset_x = offset_x;
        this.offset_y = offset_y;
        //createRoi();
    }
    
    public lROI (RenderedImage RI, int offset_x, int offset_y) {
        super(RI);
        this.offset_x = offset_x;
        this.offset_y = offset_y;
        //createRoi();
    }
    
    public void setOffset(int offset_x, int offset_y) {
        this.offset_x = offset_x;
        this.offset_y = offset_y;
        //createRoi();
    }
    
    /**
     * Return this ROI as roi. This is mostly the convex hull and can be used 
     * for ImagePlus.
     * @return This ROI as Roi (different format).
     */
    //public Roi getRoi() {
    //    return roi;
    //}
    
    //private void createRoi() {
    //    roi = Misc.ROIToRoi(this);
    //}
    
}

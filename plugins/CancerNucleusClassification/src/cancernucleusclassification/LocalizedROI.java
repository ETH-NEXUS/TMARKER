/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleusclassification;

import java.awt.Polygon;
import javax.media.jai.ROI;
import tmarker.TMAspot.TMALabel;
import tmarker.TMAspot.TMAspot;
import tmarker.misc.Misc;
import tmarker.misc.lROI;

/**
 *
 * @author Peter J. Schueffler
 */
public final class LocalizedROI {
    
    /**
     * The actual region of interest, pixelwise.
     */
    ROI roi;
    /**
     * The convex hull (generated with Misc.ROIToRoi()).
     */
    Polygon p;
    /**
     * The TMALabel with the coordinates.
     */
    TMALabel tp;
    /**
     * The TMAspot on which the TMALabel is.
     */
    TMAspot ts;
    /**
     * The radius of the patch of the ROI.
     */
    int r;
    
    /**
     * Creates a localized ROI. It is localized on a specific TMAspot and on a 
     * specific TMALabel. A Polygon p is created as the convex hull of the ROI.
     * P can be used for faster access (e.g. drawing) or to get the boundary of 
     * the ROI.
     * @param ts The TMAspot on which the TMALabel is.
     * @param tp The TMALabel with the coordinates.
     * @param roi The actual region of interest, pixelwise.
     * @param r The radius of the patch of the ROI.
     */
    LocalizedROI(TMAspot ts, TMALabel tp, ROI roi, int r) {
        this.ts = ts;
        this.tp = tp;
        this.roi = roi;
        this.r = r;
        p = Misc.ROIToRoi(roi).getPolygon();
        p.translate(tp.x-r, tp.y-r);
    }
    
    /**
     * Creates a localized ROI. It is localized on a specific TMAspot and on a 
     * specific TMALabel. A Polygon p is created as the convex hull of the ROI.
     * P can be used for faster access (e.g. drawing) or to get the boundary of 
     * the ROI.
     * @param ts The TMAspot on which the TMALabel is.
     * @param tp The TMALabel with the coordinates.
     * @param roi The actual region of interest, pixelwise.
     * @param r The radius of the patch of the ROI.
     */
    LocalizedROI(TMAspot ts, TMALabel tp, lROI roi, int r) {
        this.ts = ts;
        this.tp = tp;
        this.roi = roi;
        this.r = r;
        p = Misc.ROIToRoi(roi).getPolygon();
    }
    
}

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

/**
 *
 * @author Peter J. Schueffler
 */
public final class LocalizedROI {
    
    ROI roi;
    Polygon p;
    TMALabel tp;
    TMAspot ts;
    int r;
    
    LocalizedROI(TMAspot ts, TMALabel tp, ROI roi, int r) {
        this.ts = ts;
        this.tp = tp;
        this.roi = roi;
        this.r = r;
        p = Misc.ROIToRoi(roi).getPolygon();
        p.translate(tp.x-r, tp.y-r);
    }
    
}

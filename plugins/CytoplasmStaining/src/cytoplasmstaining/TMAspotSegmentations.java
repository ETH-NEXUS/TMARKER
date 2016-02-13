/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cytoplasmstaining;

import java.util.ArrayList;
import java.util.List;
import tmarker.TMAspot.TMALabel;
import tmarker.TMAspot.TMAspot;

/**
 *
 * @author Peter J. Schueffler
 */
public class TMAspotSegmentations {
    
    
    /**
     * Segmentations after nucleus segmentation are drawn to the visible TMAspot.
     */
    final List<LocalizedROI> segmentations = new ArrayList<>();
    
    TMAspot ts;
    
    public TMAspotSegmentations(TMAspot ts) {
        this.ts = ts;
    }
    
    LocalizedROI getLocalizedROI(TMALabel tp) {
        for (LocalizedROI lroi : segmentations) {
            if (lroi.tp == tp) {
                return lroi;
            }
        }
        return null;
    }
}

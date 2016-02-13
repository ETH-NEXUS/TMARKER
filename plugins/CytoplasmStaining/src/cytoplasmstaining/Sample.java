/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cytoplasmstaining;

import java.awt.Polygon;
import tmarker.TMAspot.TMAspot;

/**
 *
 * @author Peter J. Schueffler
 */
public class Sample {
    
    double[] feature_vector;
    TMAspot ts;
    Polygon roi = null;
    
    public Sample(TMAspot ts, Polygon roi, double[] f) {
        this.ts = ts;
        this.roi = roi;
        this.feature_vector=f;
    }
    
}

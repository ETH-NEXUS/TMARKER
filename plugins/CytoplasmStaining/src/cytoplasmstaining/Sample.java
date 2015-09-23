/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cytoplasmstaining;

import tmarker.TMAspot.TMAspot;

/**
 *
 * @author Peter J. Schueffler
 */
public class Sample {
    
    double[] feature_vector;
    TMAspot ts;
    
    public Sample(TMAspot ts, double[] f) {
        this.ts = ts;
        this.feature_vector=f;
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package intensityclustering;

import java.awt.Color;

/**
 *
 * @author Peter J. Schueffler
 */
public class NucleusIntensity3DSinglet {
    Color c;
    double[] xs;
    double[] ys;
    double[] zs;
    
    NucleusIntensity3DSinglet(Color c, float[] feature) {
        this.c = c;
        this.xs = new double[]{feature[0]};
        this.ys = new double[]{feature[1]};
        this.zs = new double[]{feature[2]};
    }
}

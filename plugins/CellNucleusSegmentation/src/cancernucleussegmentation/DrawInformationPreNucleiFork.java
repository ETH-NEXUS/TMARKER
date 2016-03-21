/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleussegmentation;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMAspot;
import static java.util.concurrent.ForkJoinTask.invokeAll;
import tmarker.TMAspot.TMApoint;

/**
 *
 * @author Peter J. Schueffler
 */
public class DrawInformationPreNucleiFork extends RecursiveAction {    
    
    private final TMARKERPluginManager tpm;
    private final CancerNucleusSegmentation cns;
    private final TMAspot ts;
    private final List<TMApoint> tps;
    private final BufferedImage I;
    private final Graphics g;
    private final double z;
    private final int x_min;
    private final int y_min;
    private final int x_max;
    private final int y_max;
    private final int mStart;
    private final int mLength;
    private final boolean doFork;
    private final int[] progress_container;
    private final long startTime;
    private boolean interrupt = false;
    
    
    
    /**
     * Creates a new StainingEstimationCoreFork.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param cns The StainingEstimation instance, used for User chosen colors which are stored in the StainingEstimation instance.
     * @param ts The TMAspot to be processed.
     * @param mStart Starting point of this fork (default: 0).
     * @param mLength Length of this fork (default: tss.size()).
     * @param doFork If true, the fork is run in parallel. Otherwise, not parallel (no fork).
     * @param progress_value A container for the progress bar over the forks.
     */
    public DrawInformationPreNucleiFork(TMARKERPluginManager tpm, CancerNucleusSegmentation cns, TMAspot ts, List<TMApoint> tps, BufferedImage I, Graphics g, double z, int x_min, int y_min, int x_max, int y_max, int mStart, int mLength, boolean doFork, int[] progress_value) {
        this.tpm = tpm;
        this.cns = cns;
        this.ts = ts;
        this.tps = tps;
        this.I = I;
        this.g = g;
        this.z = z;
        this.x_min = x_min;
        this.y_min = y_min;
        this.x_max = x_max;
        this.y_max = y_max;
        this.mStart = mStart;
        this.mLength = mLength;
        this.doFork = doFork;
        this.progress_container = progress_value;
        this.startTime = System.currentTimeMillis();
    }
    
    
    @Override
    protected void compute() {
        if (!doFork) {
            computeDirectly();
            return;
        }
        
        int n_proc = tpm.getNumberProcessors();
        int split = (int) Math.ceil((1.0*tps.size()) / n_proc);
        int split_adj;
        
        List<ForkJoinTask> fjt = new ArrayList<>();
        for (int i=0; i<n_proc; i++) {
            split_adj = Math.min(split, tps.size()-(mStart + i*split));
            if (split_adj>0) {
                fjt.add(new DrawInformationPreNucleiFork(tpm, cns, ts, tps, I, g, z, x_min, y_min, x_max, y_max, mStart + i*split, split_adj, false, progress_container));
            }
        }
        invokeAll(fjt);
    }
    
    
    protected void computeDirectly() {
        for (int i=mStart; i<mStart+mLength; i++) {
            if (!interrupt) {
                CancerNucleusSegmentation.drawInformationPreNucleus(cns, ts, tps.get(i), I, g, z, x_min, y_min, x_max, y_max);
                
                progress_container[0]++;
            }
        }
    }
    
    void interrupt() {
        interrupt = true;
    }
       
    
    
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleussegmentation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import static java.util.concurrent.ForkJoinTask.invokeAll;
import java.util.concurrent.RecursiveAction;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMApoint;
import tmarker.TMAspot.TMAspot;

/**
 *
 * @author Peter J. Schueffler
 */
public class SegmentNucleiFork extends RecursiveAction {    

    
    
    private final TMARKERPluginManager tpm;
    private final CancerNucleusSegmentation cns;
    private final TMAspot aTMAspot;
    private final List<TMApoint> TMApoints;
    private final BufferedImage I_col;
    private final BufferedImage I_gray;
    private final int mStart;
    private final int mLength;
    private final boolean doFork;
    private final long startTime;
    private final int[] progress_container;
    
    /**
     * Creates a new nucleus segmentation Fork.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param cns The CancerNucleusSegmentation instance.
     * @param aTMAspot The TMAspot to be processed.
     * @param TMApoints The TMApoints to be processed in parallel.
     * @param I_col The colored BufferedImage of this spot, used for segmentation.
     * @param I_gray The grayed BufferedImage of this spot, used for segmentation.
     * @param mStart Starting point of this fork (default: 0).
     * @param mLength Length of this fork (default: tss.size()).
     * @param doFork If true, the fork is run in parallel. Otherwise, not parallel (no fork).
     * @param progress_value A container for the progress bar over the forks.
     */
    public SegmentNucleiFork(TMARKERPluginManager tpm, CancerNucleusSegmentation cns, TMAspot aTMAspot, List<TMApoint> TMApoints, BufferedImage I_col, BufferedImage I_gray, int mStart, int mLength, boolean doFork, int[] progress_value) {
        this.tpm = tpm;
        this.cns = cns;
        this.aTMAspot = aTMAspot;
        this.TMApoints = TMApoints;
        this.I_col = I_col;
        this.I_gray = I_gray;
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
        
        int n_proc = Runtime.getRuntime().availableProcessors();
        int split = (int) Math.ceil((1.0*TMApoints.size()) / n_proc);
        int split_adj;
        
        List<ForkJoinTask> fjt = new ArrayList<>();
        for (int i=0; i<n_proc; i++) {
            split_adj = Math.min(split, TMApoints.size()-(mStart + i*split));
            fjt.add(new SegmentNucleiFork(tpm, cns, aTMAspot, TMApoints, I_col, I_gray, mStart + i*split, split_adj, false, progress_container));
        }
        invokeAll(fjt);
    }
    
    
    protected void computeDirectly() {
        for (int i=mStart; i<mStart+mLength; i++) {
            cns.segmentOneNucleus(aTMAspot, TMApoints.get(i), I_col, I_gray);
            cns.setProgressNumber_2(progress_container[0]++, TMApoints.size(), startTime);
        }
    }
    

}

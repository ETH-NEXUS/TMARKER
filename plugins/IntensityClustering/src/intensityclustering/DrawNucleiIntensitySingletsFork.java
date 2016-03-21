/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package intensityclustering;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import static java.util.concurrent.ForkJoinTask.invokeAll;
import java.util.concurrent.RecursiveAction;
import org.math.plot.Plot3DPanel;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMApoint;
import tmarker.TMAspot.TMAspot;

/**
 *
 * @author Peter J. Schueffler
 */
public class DrawNucleiIntensitySingletsFork extends RecursiveAction {   
    
    private final TMARKERPluginManager tpm;
    private final IntensityClustering ic;
    private final TMAspot ts;
    private final BufferedImage img;
    private final List<TMApoint> tps;
    private final String colorSpace;
    private final Plot3DPanel plot;
    private final int mStart;
    private final int mLength;
    private final boolean doFork;
    private final int[] progress_container;
    private final NucleusIntensity3DSinglet[] singlets;
    private final long startTime;
    
    /**
     * Creates a new StainingEstimationFork.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param mStart Starting point of this fork (default: 0).
     * @param mLength Length of this fork (default: tps.size()).
     * @param doFork If true, the fork is run in parallel. Otherwise, not parallel (no fork).
     * @param progress_value A container for the progress bar over the forks.
     */
    public DrawNucleiIntensitySingletsFork(TMARKERPluginManager tpm, IntensityClustering ic, TMAspot ts, BufferedImage img, List<TMApoint> tps, String colorSpace, Plot3DPanel plot, int mStart, int mLength, boolean doFork, int[] progress_value, NucleusIntensity3DSinglet[] singlets) {
        this.tpm = tpm;
        this.ic = ic;
        this.ts = ts;
        this.img = img;
        this.tps = tps;
        this.colorSpace = colorSpace;
        this.plot = plot;
        this.mStart = mStart;
        this.mLength = mLength;
        this.doFork = doFork;
        this.progress_container = progress_value;
        this.startTime = System.currentTimeMillis();
        this.singlets = singlets;
    }
    
    
    @Override
    protected void compute() {
        if (!doFork) {
            computeDirectly();
            return;
        }
        
        int n_proc = Runtime.getRuntime().availableProcessors();
        int split = (int) Math.ceil((1.0*tps.size()) / n_proc);
        int split_adj;
        
        List<ForkJoinTask> fjt = new ArrayList<>();
        for (int i=0; i<n_proc; i++) {
            split_adj = Math.min(split, tps.size()-(mStart + i*split));
            if (split_adj>0) {
                fjt.add(new DrawNucleiIntensitySingletsFork(tpm, ic, ts, img, tps, colorSpace, plot, mStart + i*split, split_adj, false, progress_container, singlets));
            }
        }
        invokeAll(fjt);
    }
    
    
    protected void computeDirectly() {
        for (int i=mStart; i<mStart+mLength; i++) {
            TMApoint tp = tps.get(i);
            singlets[i] = IntensityClustering.drawNucleiIntensity3DSingletsCore(ts, img, tp, colorSpace, plot);
            
            progress_container[0]++;

        }
    }
    
    
    public static void DrawNucleiIntensitySinglets_Fork(TMARKERPluginManager tpm, IntensityClustering ic, TMAspot ts, BufferedImage img, List<TMApoint> tps, String colorSpace, Plot3DPanel plot) {
        int[] progress_container = new int[]{1};
                
        NucleusIntensity3DSinglet[] singlets = new NucleusIntensity3DSinglet[tps.size()];
        
        DrawNucleiIntensitySingletsFork fb = new DrawNucleiIntensitySingletsFork(tpm, ic, ts, img, tps, colorSpace, plot, 0, tps.size(), tpm.getNumberProcessors()>1, progress_container, singlets);

        ForkJoinPool pool = new ForkJoinPool();

        long startTime = System.currentTimeMillis();
        pool.invoke(fb);
        long endTime = System.currentTimeMillis();
        pool.shutdown();
        
        
        for (NucleusIntensity3DSinglet singlet: singlets) {
            plot.addScatterPlot(null, singlet.c, singlet.xs, singlet.ys, singlet.zs);
        }

        
        System.out.println("DrawNucleiIntensitySinglets_Fork took " + (endTime - startTime) + 
                " milliseconds.");
        
    }
    
    
}

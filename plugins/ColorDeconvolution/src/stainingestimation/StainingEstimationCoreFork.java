/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stainingestimation;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
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
public class StainingEstimationCoreFork extends RecursiveAction {    
    
    private final TMARKERPluginManager tpm;
    private final StainingEstimation se;
    private final TMAspot ts;
    private final BufferedImage ts_img;
    private final int radius;
    private final double blur;
    private final int tolerance;
    private final int TMblur_hema;
    private final int TMblur_dab;
    private final int TMblur_ch3;
    private final int t_hema;
    private final int t_dab;
    private final int t_ch3;
    private final boolean hide_legend;
    private final String myStain;
    private final boolean substractChannels;
    private final boolean invertCH1;
    private final boolean invertCH2;
    private final boolean invertCH3;
    private final boolean useThresholdMap;
    private final boolean respectAreas;
    private final List<TMApoint> brown_spots_total;
    private final List<Point> offsets;
    private final List<Point> sizes;
    private final int maxsize;
    private final int mStart;
    private final int mLength;
    private final boolean doFork;
    private final int[] progress_container;
    private final long startTime;
    private boolean interrupt = false;
    
    
    /**
     * Creates a new StainingEstimationCoreFork.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param se The StainingEstimation instance, used for User chosen colors which are stored in the StainingEstimation instance.
     * @param ts The TMAspot to be processed.
     * @param ts_img The BufferedImage of the TMAspot (if not NDPI). Can be null.
     * @param radius The radius of the nuclei.
     * @param blur The blurring applied to the channels prior to local maxima finding.
     * @param tolerance The tolerance used for local maxima finding.
     * @param TMblur_hema The blurring for the dynamic threshold map for the channel 1 (if any).
     * @param TMblur_dab The blurring for the dynamic threshold map for the channel 2 (if any).
     * @param TMblur_ch3 The blurring for the dynamic threshold map for the channel 3 (if any).
     * @param t_hema The fixed channel 1 threshold (between 0-255).
     * @param t_dab The fixed channel 2 threshold (between 0-255).
     * @param t_ch3 The fixed channel 3 threshold (between 0-255).
     * @param hide_legend If false, a legend of the color deconvolution algorithm will appear.
     * @param myStain String of the staining protocol (e.g. "H&E" or "H DAB").
     * @param substractChannels If true, channel 2 will be substracted from channel 1.
     * @param invertCH1 If true, channel 1 is inverted.
     * @param invertCH2 If true, channel 2 is inverted.
     * @param invertCH3 If true, channel 3 is inverted.
     * @param mStart Starting point of this fork (default: 0).
     * @param mLength Length of this fork (default: tss.size()).
     * @param doFork If true, the fork is run in parallel. Otherwise, not parallel (no fork).
     * @param progress_value A container for the progress bar over the forks.
     * @param useThresholdMap If true, a dynamic threshold map is used for maximum acceptance instead of a fixed value.
     * @param respectAreas If true, including and excluding areas (ROI) on the images are respected and found nuclei are filtered accordingly.
     * @param brown_spots_total The resulting list of found TMApoints. These are added to this list.
     * @param offsets The offsets of the processed sub-patches.
     * @param sizes The sizes of the processed sub-patches, expressed as points. Must be same size as offsets.
     * @param maxsize The maximum size of the sub-patches (edge length, only needed for NDPI, might be equal to the maximum patch-edge-size).
     */
    public StainingEstimationCoreFork(TMARKERPluginManager tpm, StainingEstimation se, TMAspot ts, BufferedImage ts_img, int radius, double blur, int tolerance, int TMblur_hema, int TMblur_dab, int TMblur_ch3, int t_hema, int t_dab, int t_ch3, boolean hide_legend, String myStain, boolean substractChannels, boolean invertCH1, boolean invertCH2, boolean invertCH3, boolean useThresholdMap, boolean respectAreas, List<TMApoint> brown_spots_total, List<Point> offsets, List<Point> sizes, int maxsize, int mStart, int mLength, boolean doFork, int[] progress_value) {
        this.tpm = tpm;
        this.se = se;
        this.ts = ts;
        this.ts_img = ts_img;
        this.radius = radius;
        this.blur = blur;
        this.tolerance = tolerance;
        this.TMblur_hema = TMblur_hema;
        this.TMblur_dab = TMblur_dab;
        this.TMblur_ch3 = TMblur_ch3;
        this.t_hema = t_hema;
        this.t_dab = t_dab;
        this.t_ch3 = t_ch3;
        this.hide_legend = hide_legend;
        this.myStain = myStain;
        this.substractChannels = substractChannels;
        this.invertCH1 = invertCH1;
        this.invertCH2 = invertCH2;
        this.invertCH3 = invertCH3;
        this.useThresholdMap = useThresholdMap;
        this.respectAreas = respectAreas;
        this.brown_spots_total = brown_spots_total;
        this.offsets = offsets;
        this.sizes = sizes;
        this.maxsize = maxsize;
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
        int split = (int) Math.ceil((1.0*offsets.size()) / n_proc);
        int split_adj;
        
        List<ForkJoinTask> fjt = new ArrayList<>();
        for (int i=0; i<n_proc; i++) {
            split_adj = Math.min(split, offsets.size()-(mStart + i*split));
            if (split_adj>0) {
                fjt.add(new StainingEstimationCoreFork(tpm, se, ts, ts_img, radius, blur, tolerance, TMblur_hema, TMblur_dab, TMblur_ch3, t_hema, t_dab, t_ch3, hide_legend, myStain, substractChannels, invertCH1, invertCH2, invertCH3, useThresholdMap, respectAreas, brown_spots_total, offsets, sizes, maxsize, mStart + i*split, split_adj, false, progress_container));
            }
        }
        invokeAll(fjt);
    }
    
    
    protected void computeDirectly() {
        for (int i=mStart; i<mStart+mLength; i++) {
            if (!interrupt) {
                Point offset = offsets.get(i);
                Point size = sizes.get(i);
                //tpm.setStatusMessageLabel("Performing Staining Estimation ...");
                //tpm.setProgressbar((int)(1.0*progress_container[0]/mLength));
                se.setProgressNumber_2(progress_container[0], offsets.size(), startTime);

                StainingEstimation.tma_stainCore(se, ts, ts_img, radius, blur, tolerance, TMblur_hema, TMblur_dab, TMblur_ch3, t_hema, t_dab, t_ch3, hide_legend, myStain, substractChannels, invertCH1, invertCH2, invertCH3, useThresholdMap, respectAreas, doFork, brown_spots_total, offset, size, maxsize);

                progress_container[0]++;
            }
        }
    }
    
    void interrupt() {
        interrupt = true;
    }
       
    
    
    
}

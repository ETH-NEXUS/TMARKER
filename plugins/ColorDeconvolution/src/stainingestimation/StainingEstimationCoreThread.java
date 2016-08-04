/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stainingestimation;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMApoint;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;

/**
 *
 * @author Peter J. Schueffler
 */
public class StainingEstimationCoreThread extends Thread {    
    
    TMARKERPluginManager tpm;
    StainingEstimation se;
    TMAspot aTMAspot;
    BufferedImage ts_img;
    int radius;
    double blur;
    int tolerance;
    int TMblur_hema;
    int TMblur_dab;
    int TMblur_ch3;
    int t_hema;
    int t_dab;
    int t_ch3;
    boolean hide_legend;
    String myStain;
    boolean substractChannels;
    boolean invertCH1;
    boolean invertCH2;
    boolean invertCH3;
    boolean useThresholdMap;
    boolean respectAreas;
    List<TMApoint> brown_spots_total;
    List<Point> offsets;
    List<Point> sizes;
    int maxsize;
    public boolean continu = true;
    StainingEstimationCoreFork fb;
    
    
    /**
     * Performs the staining estimation of all given TMAspots in a separate thread.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param se The StainingEstimation instance, used for User chosen colors which are stored in the StainingEstimation instance.
     * @param aTMAspot The TMAspots to be processed.
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
     * @param useThresholdMap If true, a dynamic threshold map is used for maximum acceptance instead of a fixed value.
     * @param respectAreas If true, including and excluding areas (ROI) on the images are respected and found nuclei are filtered accordingly.
     * @param brown_spots_total The resulting list of found TMApoints. These are added to this list.
     * @param offsets The offsets of the processed sub-patches.
     * @param sizes The sizes of the processed sub-patches, expressed as points. Must be same size as offsets.
     * @param maxsize The maximum size of the sub-patches (edge length, only needed for NDPI, might be equal to the maximum patch-edge-size).
     */
    public StainingEstimationCoreThread (TMARKERPluginManager tpm, StainingEstimation se, TMAspot aTMAspot, BufferedImage ts_img, int radius, double blur, int tolerance, int TMblur_hema, int TMblur_dab, int TMblur_ch3, int t_hema, int t_dab, int t_ch3, boolean hide_legend, String myStain, boolean substractChannels, boolean invertCH1, boolean invertCH2, boolean invertCH3, boolean useThresholdMap, boolean respectAreas, List<TMApoint> brown_spots_total, List<Point> offsets, List<Point> sizes, int maxsize) {
        this.tpm = tpm;
        this.se = se;
        this.aTMAspot = aTMAspot;
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
    }
    
    @Override
    public void run() {
        ForkJoinPool pool = null;
        try{ 
            se.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            int[] progress_container = new int[]{1};
            fb = new StainingEstimationCoreFork(tpm, se, aTMAspot, ts_img, radius, blur, tolerance, TMblur_hema, TMblur_dab, TMblur_ch3, t_hema, t_dab, t_ch3, hide_legend, myStain, substractChannels, invertCH1, invertCH2, invertCH3, useThresholdMap, respectAreas, brown_spots_total, offsets, sizes, maxsize, 0, offsets.size(), tpm.useParallelProgramming(), progress_container);

            pool = new ForkJoinPool();

            long startTime = System.currentTimeMillis();
            pool.invoke(fb);
            long endTime = System.currentTimeMillis();
            pool.shutdown();

            se.setProgressNumber_2(0, 0, 0);

            System.out.println("StainingEstimationCore_Fork took " + (endTime - startTime) + 
                    " milliseconds.");
            
            
            tpm.setStatusMessageLabel("Performing Staining Estimation ... Done.");
            tpm.setProgressbar(0);
            se.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            continu = false;
            fb.interrupt();
            if (pool!=null) {
                pool.shutdownNow();
            }
            tpm.setStatusMessageLabel("Staining Estimation Stopped."); tpm.setProgressbar(0);
            se.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (tmarker.DEBUG>0) {
                Logger.getLogger(StainingEstimationThread.class.getName()).log(Level.WARNING, e.getMessage(), e);
            }
        }
    }
}

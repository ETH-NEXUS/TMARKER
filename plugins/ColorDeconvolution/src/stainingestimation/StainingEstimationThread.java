/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stainingestimation;

import java.awt.Cursor;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;

/**
 *
 * @author Peter J. Schueffler
 */
public class StainingEstimationThread extends Thread {    
    
    TMARKERPluginManager tpm;
    StainingEstimation se;
    List<TMAspot> aTMAspots;
    int radius;
    double blur;
    int tolerance;
    int t_hema;
    int t_dab;
    int t_ch3;
    boolean delete_cur_gs_spots;
    boolean delete_cur_es_spots;
    boolean hide_legend;
    boolean markCancerous;
    String myStain;
    boolean substractChannels;
    boolean invertCH1;
    boolean invertCH2;
    boolean invertCH3;
    public boolean continu = true;
    int TMblur_hema;
    int TMblur_dab;
    int TMblur_ch3;
    boolean respectAreas;
    StainingEstimationFork fb = null;
    
    /**
     * Performs the staining estimation of all given TMAspots in a separate thread.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param se The StainingEstimation instance, used for User chosen colors which are stored in the StainingEstimation instance.
     * @param aTMAspots The TMAspots to be processed.
     * @param radius The radius of the nuclei.
     * @param blur The blurring applied to the channels prior to local maxima finding.
     * @param tolerance The tolerance used for local maxima finding.
     * @param TMblur_hema The blurring for the dynamic threshold map for the channel 1 (if any).
     * @param TMblur_dab The blurring for the dynamic threshold map for the channel 2 (if any).
     * @param TMblur_ch3 The blurring for the dynamic threshold map for the channel 3 (if any).
     * @param t_hema The fixed channel 1 threshold (between 0-255).
     * @param t_dab The fixed channel 2 threshold (between 0-255).
     * @param t_ch3 The fixed channel 3 threshold (between 0-255).
     * @param delete_cur_gs_spots If true, current gold standard nuclei will be deleted.
     * @param delete_cur_es_spots If true, current estimated nuclei will be deleted.
     * @param hide_legend If false, a legend of the color deconvolution algorithm will appear.
     * @param markCancerous If true, all found nuclei will be labeled as "cancerous" nuclei
     * @param myStain String of the staining protocol (e.g. "H&E" or "H DAB").
     * @param substractChannels If true, channel 2 will be substracted from channel 1.
     * @param invertCH1 If true, channel 1 is inverted.
     * @param invertCH2 If true, channel 2 is inverted.
     * @param invertCH3 If true, channel 3 is inverted.
     */
    public StainingEstimationThread (TMARKERPluginManager tpm, StainingEstimation se, List<TMAspot> aTMAspots, int radius, double blur, int tolerance, int TMblur_hema, int TMblur_dab, int TMblur_ch3, int t_hema, int t_dab, int t_ch3, boolean delete_cur_gs_spots, boolean delete_cur_es_spots, boolean hide_legend, boolean markCancerous, String myStain, boolean substractChannels, boolean invertCH1, boolean invertCH2, boolean invertCH3, boolean respectAreas) {
        this.tpm = tpm;
        this.se = se;
        this.aTMAspots = aTMAspots;
        this.radius = radius;
        this.blur = blur;
        this.tolerance = tolerance;
        this.t_hema = t_hema;
        this.t_dab = t_dab;
        this.t_ch3 = t_ch3;
        this.delete_cur_gs_spots = delete_cur_gs_spots;
        this.delete_cur_es_spots = delete_cur_es_spots;
        this.hide_legend = hide_legend;
        this.markCancerous = markCancerous;
        this.myStain = myStain;
        this.substractChannels = substractChannels;
        this.invertCH1 = invertCH1;
        this.invertCH2 = invertCH2;
        this.invertCH3 = invertCH3;
        this.TMblur_hema = TMblur_hema;
        this.TMblur_dab = TMblur_dab;
        this.TMblur_ch3 = TMblur_ch3;
        this.respectAreas = respectAreas;
    }
    
    @Override
    public void run() {
        ForkJoinPool pool = null;
        try{ 
            se.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                //StainingEstimationFork.StainingEstimation_Fork(tpm, se, aTMAspots, radius, blur, tolerance, TMblur_hema, TMblur_dab, TMblur_ch3, t_hema, t_dab, t_ch3, delete_cur_gs_spots, delete_cur_es_spots, hide_legend, markCancerous, myStain, substractChannels, invertCH1, invertCH2, invertCH3, respectAreas);
            
            int[] progress_container = new int[]{1};
            fb = new StainingEstimationFork(tpm, se, aTMAspots, radius, blur, tolerance, TMblur_hema, TMblur_dab, TMblur_ch3, t_hema, t_dab, t_ch3, delete_cur_gs_spots, delete_cur_es_spots, hide_legend, markCancerous, myStain, substractChannels, invertCH1, invertCH2, invertCH3, respectAreas, 0, aTMAspots.size(), tpm.useParallelProgramming(), progress_container);

            pool = new ForkJoinPool();
            
            long startTime = System.currentTimeMillis();
            pool.invoke(fb);
            long endTime = System.currentTimeMillis();
            pool.shutdown();

            se.setProgressNumber(0, 0, 0);
            
            System.out.println("StainingEstimation_Fork took " + (endTime - startTime) + 
                    " milliseconds.");
            
            
            tpm.setStatusMessageLabel("Performing Staining Estimation ... Done.");
            tpm.setProgressbar(0);
            se.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            continu = false;
            
            // search for all CORE Threads, and stop them
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            for (Thread thread: threadSet) {
                if (thread.getClass().equals(StainingEstimationCoreThread.class)) {
                    thread.interrupt();
                }
            }
            
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
    
    @Override
    public void interrupt() {
        if (fb!=null) {
            fb.cancel(true);
        }
        super.interrupt();
    }
}

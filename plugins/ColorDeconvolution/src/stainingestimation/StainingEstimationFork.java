/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stainingestimation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import static java.util.concurrent.ForkJoinTask.invokeAll;
import java.util.concurrent.RecursiveAction;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMAspot;

/**
 *
 * @author Peter J. Schueffler
 */
public class StainingEstimationFork extends RecursiveAction {    
    
    private final TMARKERPluginManager tpm;
    private final StainingEstimation se;
    private final List<TMAspot> tss;
    private final int radius;
    private final double blur;
    private final int tolerance;
    private final int t_hema;
    private final int t_dab;
    private final boolean delete_cur_gs_spots;
    private final boolean delete_cur_es_spots;
    private final boolean hide_legend;
    private final boolean markCancerous;
    private final String myStain;
    private final int TMblur_hema;
    private final int TMblur_dab;
    private final int mStart;
    private final int mLength;
    private final boolean doFork;
    private final int[] progress_container;
    
    /**
     * Creates a new StainingEstimationFork.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param se The StainingEstimation instance, used for User chosen colors which are stored in the StainingEstimation instance.
     * @param tss The TMAspots to be processed.
     * @param radius The radius of the nuclei.
     * @param blur The blurring applied to the channels prior to local maxima finding.
     * @param tolerance The tolerance used for local maxima finding.
     * @param TMblur_hema The blurring for the dynamic threshold map for the channel 1 (if any).
     * @param TMblur_dab The blurring for the dynamic threshold map for the channel 2 (if any).
     * @param t_hema The fixed channel 1 threshold (between 0-255).
     * @param t_dab The fixed channel 2 threshold (between 0-255).
     * @param delete_cur_gs_spots If true, current gold standard nuclei will be deleted.
     * @param delete_cur_es_spots If true, current estimated nuclei will be deleted.
     * @param hide_legend If false, a legend of the color deconvolution algorithm will appear.
     * @param markCancerous If true, all found nuclei will be labeled as "cancerous" nuclei
     * @param myStain String of the staining protocol (e.g. "H&E" or "H DAB").
     * @param mStart Starting point of this fork (default: 0).
     * @param mLength Length of this fork (default: tss.size()).
     * @param doFork If true, the fork is run in parallel. Otherwise, not parallel (no fork).
     * @param progress_value A container for the progress bar over the forks.
     */
    public StainingEstimationFork(TMARKERPluginManager tpm, StainingEstimation se, List<TMAspot> tss, int radius, double blur, int tolerance, int TMblur_hema, int TMblur_dab, int t_hema, int t_dab, boolean delete_cur_gs_spots, boolean delete_cur_es_spots, boolean hide_legend, boolean markCancerous, String myStain, int mStart, int mLength, boolean doFork, int[] progress_value) {
        this.tpm = tpm;
        this.se = se;
        this.tss = tss;
        this.radius = radius;
        this.blur = blur;
        this.tolerance = tolerance;
        this.t_hema = t_hema;
        this.t_dab = t_dab;
        this.delete_cur_gs_spots = delete_cur_gs_spots;
        this.delete_cur_es_spots = delete_cur_es_spots;
        this.hide_legend = hide_legend;
        this.markCancerous = markCancerous;
        this.myStain = myStain;
        this.TMblur_hema = TMblur_hema;
        this.TMblur_dab = TMblur_dab;
        this.mStart = mStart;
        this.mLength = mLength;
        this.doFork = doFork;
        this.progress_container = progress_value;
    }
    
    
    @Override
    protected void compute() {
        if (!doFork) {
            computeDirectly();
            return;
        }
        
        int n_proc = Runtime.getRuntime().availableProcessors();
        int split = (int) Math.ceil((1.0*tss.size()) / n_proc);
        int split_adj;
        
        List<ForkJoinTask> fjt = new ArrayList<>();
        for (int i=0; i<n_proc; i++) {
            split_adj = Math.min(split, tss.size()-(mStart + i*split));
            fjt.add(new StainingEstimationFork(tpm, se, tss, radius, blur, tolerance, TMblur_hema, TMblur_dab, t_hema, t_dab, delete_cur_gs_spots, delete_cur_es_spots, hide_legend, markCancerous, myStain, mStart + i*split, split_adj, false, progress_container));
        }
        invokeAll(fjt);
    }
    
    
    protected void computeDirectly() {
        for (int i=mStart; i<mStart+mLength; i++) {
            TMAspot ts = tss.get(i);
            tpm.setStatusMessageLabel("Performing Staining Estimation ...");
            tpm.setProgressbar((int)(1.0*progress_container[0]/mLength));
            
            StainingEstimation.doStainingEstimation(se, ts, radius, blur, tolerance, TMblur_hema, TMblur_dab, t_hema, t_dab, delete_cur_gs_spots, delete_cur_es_spots, hide_legend, markCancerous, myStain, true);
            
            progress_container[0]++;
        }
    }
    
    
    public static void StainingEstimation_Fork(TMARKERPluginManager tpm, StainingEstimation se, List<TMAspot> tss, int radius, double blur, int tolerance, int TMblur_hema, int TMblur_dab, int t_hema, int t_dab, boolean delete_cur_gs_spots, boolean delete_cur_es_spots, boolean hide_legend, boolean markCancerous, String myStain) {
        int[] progress_container = new int[]{1};
        StainingEstimationFork fb = new StainingEstimationFork(tpm, se, tss, radius, blur, tolerance, TMblur_hema, TMblur_dab, t_hema, t_dab, delete_cur_gs_spots, delete_cur_es_spots, hide_legend, markCancerous, myStain, 0, tss.size(), tpm.useParallelProgramming(), progress_container);

        ForkJoinPool pool = new ForkJoinPool();

        long startTime = System.currentTimeMillis();
        pool.invoke(fb);
        long endTime = System.currentTimeMillis();

        System.out.println("StainingEstimation_Fork took " + (endTime - startTime) + 
                " milliseconds.");
        
    }
    
    
}

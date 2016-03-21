/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleussegmentation;

import java.awt.Cursor;
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
public class SegmentNucleiThread extends Thread {    
    
    TMARKERPluginManager tpm;
    CancerNucleusSegmentation cns;
    TMAspot aTMAspot;
    List<TMApoint> TMApoints;
    BufferedImage I_col;
    BufferedImage I_gray;
    public boolean continu = true;
    
    /**
     * Performs the nucleus segmentation off all given nuclei in a separate thread.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param cns The CancerNucleusSegmentation instance.
     * @param aTMAspot The TMAspot to be processed.
     * @param TMApoints The TMApoints to be processed in parallel.
     * @param I_col The colored BufferedImage of this spot, used for segmentation.
     * @param I_gray The grayed BufferedImage of this spot, used for segmentation.
     */
    public SegmentNucleiThread(TMARKERPluginManager tpm, CancerNucleusSegmentation cns, TMAspot aTMAspot, List<TMApoint> TMApoints, BufferedImage I_col, BufferedImage I_gray) {
        this.tpm = tpm;
        this.cns = cns;
        this.aTMAspot = aTMAspot;
        this.TMApoints = TMApoints;
        this.I_col = I_col;
        this.I_gray = I_gray;
    }
    
    @Override
    public void run() {
        try{ 
            cns.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            int[] progress_container = new int[]{1};
        
            SegmentNucleiFork fb = new SegmentNucleiFork(tpm, cns, aTMAspot, TMApoints, I_col, I_gray, 0, TMApoints.size(), tpm.useParallelProgramming(), progress_container);

            ForkJoinPool pool = new ForkJoinPool();

            long startTime = System.currentTimeMillis();
            pool.invoke(fb);
            long endTime = System.currentTimeMillis();
            pool.shutdown();

            System.out.println("SegmentNuclei_Fork took " + (endTime - startTime) + 
                    " milliseconds.");

            tpm.setStatusMessageLabel("Performing Nucleus Segmentation ... Done.");
            tpm.setProgressbar(0);
            cns.setProgressNumber_2(0, 0, 0);
            cns.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            tpm.repaintVisibleTMAspot();
        } catch (Exception e) {
            continu = false;
            tpm.setStatusMessageLabel("CNucleus Segmentation Stopped."); tpm.setProgressbar(0);
            cns.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (tmarker.DEBUG>0) {
                Logger.getLogger(SegmentNucleiThread.class.getName()).log(Level.WARNING, e.getMessage(), e);
            }
        }
    }
}

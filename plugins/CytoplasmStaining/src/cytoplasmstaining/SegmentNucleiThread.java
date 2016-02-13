/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cytoplasmstaining;

import java.awt.Cursor;
import java.awt.image.BufferedImage;
import java.util.List;
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
    CytoplasmStaining cs;
    TMAspot aTMAspot;
    List<TMApoint> TMApoints;
    TMAspotSegmentations tsseg;
    BufferedImage I_col;
    BufferedImage I_gray;
    public boolean continu = true;
    
    /**
     * Performs the nucleus segmentation off all given nuclei in a separate thread.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param cs The CytoplasmStaining instance.
     * @param aTMAspot The TMAspot to be processed.
     * @param TMApoints The TMApoints to be processed in parallel.
     * @param tsseg The TMAspotSegmentations where this TMAspot belongs to.
     * @param I_col The colored BufferedImage of this spot, used for segmentation.
     * @param I_gray The grayed BufferedImage of this spot, used for segmentation.
     */
    public SegmentNucleiThread(TMARKERPluginManager tpm, CytoplasmStaining cs, TMAspot aTMAspot, List<TMApoint> TMApoints, TMAspotSegmentations tsseg, BufferedImage I_col, BufferedImage I_gray) {
        this.tpm = tpm;
        this.cs = cs;
        this.aTMAspot = aTMAspot;
        this.TMApoints = TMApoints;
        this.tsseg = tsseg;
        this.I_col = I_col;
        this.I_gray = I_gray;
    }
    
    @Override
    public void run() {
        try{ 
            cs.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            SegmentNucleiFork.SegmentNuclei_Fork(tpm, cs, aTMAspot, TMApoints, tsseg, I_col, I_gray);
            
            tpm.setStatusMessageLabel("Performing Nucleus Segmentation ... Done.");
            tpm.setProgressbar(0);
            cs.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            continu = false;
            tpm.setStatusMessageLabel("CNucleus Segmentation Stopped."); tpm.setProgressbar(0);
            cs.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (tmarker.DEBUG>0) {
                Logger.getLogger(SegmentNucleiThread.class.getName()).log(Level.WARNING, e.getMessage(), e);
            }
        }
    }
}

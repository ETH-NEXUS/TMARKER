/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker;

import java.awt.Cursor;
import java.util.List;
import tmarker.TMAspot.TMAspot;

/**
 * Performs the automatic white balance (background correction) of all selected TMAspots in a separate thread.
 * @author Peter J. Schueffler
 */
public class BgCorrectionThread extends Thread {
    List<TMAspot> tss = null;
    
    /**
     * Performs the automatic white balance (background correction) of all selected TMAspots in a separate thread.
     * @param tss The TMAspots whose background should be corrected. Performs doBgCorrectionAutomatic() on each TMAspot.
     */
    public BgCorrectionThread(List<TMAspot> tss) {
        this.tss = tss;
    }
    
    @Override
    public void run() {
        try { 
            if (tss!=null && !tss.isEmpty()) {
                tss.get(0).getCenter().getBgCorrectionDialog().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                for (TMAspot ts:tss) {
                    ts.doBgCorrectionAutomatic();
                }

                tss.get(0).getCenter().getBgCorrectionDialog().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                tss.get(0).getCenter().getBgCorrectionDialog().dispose();
            }
        } catch (OutOfMemoryError e) {
           java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, " Out of memory... Retry...");
           run();
        } catch (Exception e) {
            if (!tss.isEmpty()) {
                tss.get(0).getCenter().setStatusMessageLabel("Automatic Background Correction Stopped."); tss.get(0).getCenter().setProgressbar(0);
            }
            if (tmarker.DEBUG>0) {
                e.printStackTrace();
            }
        }
    }
}

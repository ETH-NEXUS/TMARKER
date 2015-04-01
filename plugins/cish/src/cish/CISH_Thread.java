/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cish;

import java.awt.Cursor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 *
 * @author Peter J. Schueffler
 */
public class CISH_Thread extends Thread {    
    
    private final TMARKERPluginManager tpm;
    private final CISH cish;
    private final List<TMAspot> tss;
    private final int psr;
    private final int nPts;
    private final boolean darkpoints;
    private final double[] gRatios;
    private final double[] lRatios;
    private final int[][][] ps;
    private final Classifier classifier;
    private final Instances dataset;
    public boolean continu = true;
    
    /**
     * Performs the CISH calculation of all given TMAspots in a separate thread.
     * @param tpm The TMARKERPluginManager to access the main program.
     * @param cish The main CISH instance.
     * @param tss The TMAspots to be processed.
     * @param psr The point signal radius used by the cish method.
     * @param nPts The number of local points used for local ratio calculation.
     * @param darkpoints True, if dark points on white background are found, false for light points on dark background.
     * @param gRatios A container for the calculated result global CISH Ratios. Has to have equal length as tss.
     * @param lRatios A container for the calculated result local CISH Ratios. Has to have equal length as tss.
     * @param ps A container for the calculated CISH points. 1th dim has to have equal length as tss. 2nd dim has length of found CISH points. 3rd dim has size 5 (x-coord, y-coord, r, g, b of RGB color of label).
     * @param classifier The classifier to distinguish different cish points.
     * @param dataset The dataset with which the classifier was trained.
     */
    public CISH_Thread(TMARKERPluginManager tpm, CISH cish, List<TMAspot> tss, int psr, int nPts, boolean darkpoints, double[] gRatios, double[] lRatios, int[][][] ps, Classifier classifier, Instances dataset) {
        this.tpm = tpm;
        this.cish = cish;
        this.tss = tss;
        this.psr = psr;
        this.nPts = nPts;
        this.darkpoints = darkpoints;
        this.gRatios = gRatios;
        this.lRatios = lRatios;
        this.ps = ps;
        this.classifier = classifier;
        this.dataset = dataset;
    }
    
    @Override
    public void run() {
        try{ 
            cish.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            CISH_Fork.doCISH_Fork(tpm, cish, tss, psr, nPts, darkpoints, gRatios, lRatios, ps, classifier, dataset);
            
            cish.updateCISHTable();
            cish.drawRatioPlot();
            tpm.repaintVisibleTMAspot();
           
            tpm.setStatusMessageLabel("Performing CISH Computation ... Done.");
            tpm.setProgressbar(0);
            cish.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            continu = false;
            tpm.setStatusMessageLabel("CISH Computation Stopped."); tpm.setProgressbar(0);
            cish.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (tmarker.DEBUG>0) {
                Logger.getLogger(CISH_Thread.class.getName()).log(Level.WARNING, e.getMessage(), e);
            }
        }
    }
}

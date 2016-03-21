/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleusclassification;

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
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 *
 * @author Peter J. Schueffler
 */
public class CancerNucleusClassificationCoreThread extends Thread {    
    
    TMARKERPluginManager tpm;
    CancerNucleusClassification cnc;
    TMAspot aTMAspot;
    List<TMApoint> tps;
    BufferedImage I_col;
    BufferedImage I_gray;
    Classifier classifier;
    Instances dataset;
    boolean foregroundDetection;
    public boolean continu = true;
    
    /**
     * Performs the staining estimation of all given TMAspots in a separate thread.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param cnc The CancerNucleusClassification instance.
     * @param ts The TMAspot to be processed.
     * @param tps The TMApoints to be processed.
     * @param I_col The color image of the spot (can be null if ts is a NDPI image, otherwise it might be ts.getBufferedImage()).
     * @param I_gray A gray version of I_col (might be edited). Can be null if ts is a NDPI image.
     * @param classifier The classifier used.
     * @param dataset The prepared dataset for classification (defines the class labels etc). Can be null, but should be predefined for speed issues.
     * @param foregroundDetection If true, a nucleus/background classification, otherwise a malignant/benign nucleus classification is tried.
     */
    public CancerNucleusClassificationCoreThread(TMARKERPluginManager tpm, CancerNucleusClassification cnc, TMAspot ts, List<TMApoint> tps, BufferedImage I_col, BufferedImage I_gray, Classifier classifier, Instances dataset, boolean foregroundDetection) {
        this.tpm = tpm;
        this.cnc = cnc;
        this.aTMAspot = ts;
        this.tps = tps;
        this.I_col = I_col;
        this.I_gray = I_gray;
        this.classifier = classifier;
        this.dataset = dataset;
        this.foregroundDetection = foregroundDetection;
    }
    
    @Override
    public void run() {
        try{ 
            cnc.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            int[] progress_container = new int[]{1};
            CancerNucleusClassificationCoreFork fb = new CancerNucleusClassificationCoreFork(tpm, cnc, aTMAspot, tps, I_col, I_gray, classifier, dataset, foregroundDetection, 0, tps.size(), tpm.useParallelProgramming(), progress_container);

            ForkJoinPool pool = new ForkJoinPool();

            long startTime = System.currentTimeMillis();
            pool.invoke(fb);
            long endTime = System.currentTimeMillis();
            pool.shutdown();

            cnc.setProgressNumber(0, 0, 0);
            cnc.setProgressNumber_2(0, 0, 0);

            System.out.println("CancerNucleusClassification_Fork took " + (endTime - startTime) + 
                    " milliseconds.");

        
            
            tpm.setStatusMessageLabel("Performing Cancer Nucleus Classification ... Done.");
            tpm.setProgressbar(0);
            cnc.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            continu = false;
            tpm.setStatusMessageLabel("Cancer Nucleus Classification Stopped."); tpm.setProgressbar(0);
            cnc.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (tmarker.DEBUG>0) {
                Logger.getLogger(CancerNucleusClassificationCoreThread.class.getName()).log(Level.WARNING, e.getMessage(), e);
            }
        }
    }
}

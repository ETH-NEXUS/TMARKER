/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleusclassification;

import java.awt.Cursor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;
import weka.classifiers.Classifier;

/**
 *
 * @author Peter J. Schueffler
 */
public class CancerNucleusClassificationThread extends Thread {    
    
    TMARKERPluginManager tpm;
    CancerNucleusClassification se;
    List<TMAspot> aTMAspots;
    Classifier detector;
    Classifier classifier;
    boolean twoStepClassification;
    public boolean continu = true;
    
    /**
     * Performs the staining estimation of all given TMAspots in a separate thread.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param cnc The CancerNucleusClassification instance.
     * @param aTMAspots The TMAspots to be processed.
     * @param detector The detector used (1. classifier in 2-step classification). Can be null (then, 1-step classification is done).
     * @param classifier The classifier used.
     * @param twoStepClassification If true and detector not null, a 2-step classification is tried (1: nucleus/background classification, 2: malignant/benign nucleus classification).
     */
    public CancerNucleusClassificationThread(TMARKERPluginManager tpm, CancerNucleusClassification cnc, List<TMAspot> aTMAspots, Classifier detector, Classifier classifier, boolean twoStepClassification) {
        this.tpm = tpm;
        this.se = cnc;
        this.aTMAspots = aTMAspots;
        this.detector = detector;
        this.classifier = classifier;
        this.twoStepClassification = twoStepClassification;
    }
    
    @Override
    public void run() {
        try{ 
            se.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            CancerNucleusClassificationFork.CancerNucleusClassification_Fork(tpm, se, aTMAspots, detector, classifier, twoStepClassification);
            
            tpm.setStatusMessageLabel("Performing Cancer Nucleus Classification ... Done.");
            tpm.setProgressbar(0);
            se.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            continu = false;
            tpm.setStatusMessageLabel("Cancer Nucleus Classification Stopped."); tpm.setProgressbar(0);
            se.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (tmarker.DEBUG>0) {
                Logger.getLogger(CancerNucleusClassificationThread.class.getName()).log(Level.WARNING, e.getMessage(), e);
            }
        }
    }
}

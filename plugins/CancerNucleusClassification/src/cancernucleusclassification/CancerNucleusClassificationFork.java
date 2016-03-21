/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleusclassification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import static java.util.concurrent.ForkJoinTask.invokeAll;
import java.util.concurrent.RecursiveAction;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMAspot;
import weka.classifiers.Classifier;

/**
 *
 * @author Peter J. Schueffler
 */
public class CancerNucleusClassificationFork extends RecursiveAction {    
    
    private final TMARKERPluginManager tpm;
    private final CancerNucleusClassification cnc;
    private final List<TMAspot> tss;
    private final Classifier detector;
    private final Classifier classifier;
    private final boolean twoStepClassification;
    private final boolean respectROI;
    private final int mStart;
    private final int mLength;
    private final boolean doFork;
    private final long startTime;
    private final int[] progress_container;
    
    /**
     * Creates a new StainingEstimationFork.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param cnc The CancerNucleusClassification instance.
     * @param tss The TMAspots to be processed.
     * @param detector The detector used (1. classifier in 2-step classification). Can be null (then, 1-step classification is done).
     * @param classifier The classifier used.
     * @param twoStepClassification If true and detector not null, a 2-step classification is tried (1: nucleus/background classification, 2: malignant/benign nucleus classification).
     * @param respectROI If true, only nuclei in including or outside excluding ROI are classified (or all nuclei if there is no ROI). If false, all nuclei are classified.
     * @param mStart Starting point of this fork (default: 0).
     * @param mLength Length of this fork (default: tss.size()).
     * @param doFork If true, the fork is run in parallel. Otherwise, not parallel (no fork).
     * @param progress_value A container for the progress bar over the forks.
     */
    public CancerNucleusClassificationFork(TMARKERPluginManager tpm, CancerNucleusClassification cnc, List<TMAspot> tss, Classifier detector, Classifier classifier, boolean twoStepClassification, boolean respectROI, int mStart, int mLength, boolean doFork, int[] progress_value) {
        this.tpm = tpm;
        this.cnc = cnc;
        this.tss = tss;
        this.detector = detector;
        this.classifier = classifier;
        this.twoStepClassification = twoStepClassification;
        this.respectROI = respectROI;
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
        int split = (int) Math.ceil((1.0*tss.size()) / n_proc);
        int split_adj;
        
        List<ForkJoinTask> fjt = new ArrayList<>();
        for (int i=0; i<n_proc; i++) {
            split_adj = Math.min(split, tss.size()-(mStart + i*split));
            fjt.add(new CancerNucleusClassificationFork(tpm, cnc, tss, detector, classifier, twoStepClassification, respectROI, mStart + i*split, split_adj, false, progress_container));
        }
        invokeAll(fjt);
    }
    
    
    protected void computeDirectly() {
        for (int i=mStart; i<mStart+mLength; i++) {
            TMAspot ts = tss.get(i);
            tpm.setStatusMessageLabel("Performing Cancer Nucleus Classification ...");
            tpm.setProgressbar((int)(1.0*progress_container[0]/mLength));
            cnc.setProgressNumber(progress_container[0], tss.size(), startTime);
            
            if (detector != null && twoStepClassification) {
                CancerNucleusClassification.classifyNuclei(cnc, ts, detector, true, respectROI, cnc.manager, true);
            }
            CancerNucleusClassification.classifyNuclei(cnc, ts, classifier, false, respectROI, cnc.manager, true);
            
            progress_container[0]++;
            
        }
    }
    
    
}

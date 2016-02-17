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
import tmarker.TMAspot.TMApoint;
import tmarker.TMAspot.TMAspot;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 *
 * @author Peter J. Schueffler
 */
public class CancerNucleusClassificationCoreFork extends RecursiveAction {    
    
    private final TMARKERPluginManager tpm;
    private final CancerNucleusClassification cnc;
    private final TMAspot ts;
    private final List<TMApoint> tps;
    private final Classifier classifier;
    private final Instances dataset;
    private final boolean foregroundDetection;
    private final int mStart;
    private final int mLength;
    private final boolean doFork;
    private final long startTime;
    private final int[] progress_container;
    
    /**
     * Creates a new StainingEstimationFork.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param cnc The CancerNucleusClassification instance.
     * @param ts The TMAspot to be processed.
     * @param tps The TMApoints to be processed.
     * @param classifier The classifier used.
     * @param dataset The prepared dataset for classification (defines the class labels etc). Can be null, but should be predefined for speed issues.
     * @param foregroundDetection If true, a 2-step classification is tried (nucleus/background classification) otherwise a malignant/benign nucleus classification.
     * @param mStart Starting point of this fork (default: 0).
     * @param mLength Length of this fork (default: tps.size()).
     * @param doFork If true, the fork is run in parallel. Otherwise, not parallel (no fork).
     * @param progress_container A container for the progress bar over the forks.
     */
    public CancerNucleusClassificationCoreFork(TMARKERPluginManager tpm, CancerNucleusClassification cnc, TMAspot ts, List<TMApoint> tps, Classifier classifier, Instances dataset, boolean foregroundDetection, int mStart, int mLength, boolean doFork, int[] progress_container) {
        this.tpm = tpm;
        this.cnc = cnc;
        this.ts = ts;
        this.tps = tps;
        this.classifier = classifier;
        this.dataset = dataset;
        this.foregroundDetection = foregroundDetection;
        this.mStart = mStart;
        this.mLength = mLength;
        this.doFork = doFork;
        this.progress_container = progress_container;
        this.startTime = System.currentTimeMillis();
    }
    
    
    @Override
    protected void compute() {
        if (!doFork) {
            computeDirectly();
            return;
        }
        
        int n_proc = tpm.getNumberProcessors();
        int split = (int) Math.ceil((1.0*tps.size()) / n_proc);
        int split_adj;
        
        List<ForkJoinTask> fjt = new ArrayList<>();
        for (int i=0; i<n_proc; i++) {
            split_adj = Math.min(split, tps.size()-(mStart + i*split));
            fjt.add(new CancerNucleusClassificationCoreFork(tpm, cnc, ts, tps, classifier, dataset, foregroundDetection, mStart + i*split, split_adj, false, progress_container));
        }
        invokeAll(fjt);
    }
    
    
    protected void computeDirectly() {
        
        List<TMApoint> tps_ = new ArrayList<>();
        
        for (int i=mStart; i<mStart+mLength; i++) {
            tps_.add(tps.get(i));
        }
        
        CancerNucleusClassification.classifyNucleiCore(cnc, ts, tps_, classifier, dataset, foregroundDetection, cnc.manager, progress_container, tps.size(), startTime);
            
        }
    }
    

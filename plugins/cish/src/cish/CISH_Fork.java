/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cish;

import ij.ImagePlus;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import static java.util.concurrent.ForkJoinTask.invokeAll;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMAspot;
import tmarker.misc.Misc;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 *
 * @author Peter J. Schueffler
 */
public class CISH_Fork extends RecursiveAction {    
    
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
    private final int mStart;
    private final int mLength;
    private final boolean doFork;
    private final int[] progress_container;
    private final long startTime;
    
    /**
     * Creates a new CISH_Fork.
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
     * @param mStart Starting point of this fork (default: 0).
     * @param mLength Length of this fork (default: tss.size()).
     * @param doFork If true, the fork is run in parallel. Otherwise, not parallel (no fork).
     * @param progress_value A container for the progress bar over the forks.
     */
    public CISH_Fork(TMARKERPluginManager tpm, CISH cish, List<TMAspot> tss, int psr, int nPts, boolean darkpoints, double[] gRatios, double[] lRatios, int[][][] ps, Classifier classifier, Instances dataset, int mStart, int mLength, boolean doFork, int[] progress_value) {
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
        
        } else {
            int n_proc = Runtime.getRuntime().availableProcessors();
            Logger.getLogger(CISH_Fork.class.getName()).log(Level.INFO, "Using " + n_proc + " Processors.");

            int split = (int) Math.ceil((1.0*tss.size()) / n_proc);
            int split_adj;

            List<ForkJoinTask> fjt = new ArrayList<>();
            for (int i=0; i<n_proc; i++) {
                split_adj = Math.min(split, tss.size()-(mStart + i*split));
                if (split_adj>0) {
                    fjt.add(new CISH_Fork(tpm, cish, tss, psr, nPts, darkpoints, gRatios, lRatios, ps, classifier, dataset, mStart + i*split, split_adj, false, progress_container));
                }
            }
            invokeAll(fjt);
        }
    }
    
    /*
    protected void computeDirectly2() {
        try {
            MCISH mc = new MCISH();
            for (int i=mStart; i<mStart+mLength; i++) {
                progress_container[0]++;
                
                TMAspot ts = tss.get(i);
                tpm.setStatusMessageLabel("Performing CISH Analysis ...");
                tpm.setProgressbar((int)(100.0*(2*progress_container[0]-1)/(2*tss.size())));

                List<Object> in = new ArrayList<>();
                List<Object> out = new ArrayList<>();

                //in.add("E:\\svn\\SIMBAD\\cish\\A_5_3.jpg");
                in.add(ts.getOriginalImagename());
                in.add(psr);
                in.add(nPts);

                out.add(null);
                out.add(null);
                out.add(null);
                
                //MCISH mc = new MCISH();
                
                mc.doCISH(out, in);
                tpm.setProgressbar((int)(100.0*(2*progress_container[0])/(2*tss.size())));
                
                MWArray gRatioArray = (MWArray) out.get(0);
                double[][] gRatioArray_obj = (double[][]) gRatioArray.toArray();
                gRatios[i] = gRatioArray_obj[0][0];
                gRatioArray.dispose();

                MWArray lRatioArray = (MWArray) out.get(1);
                double[][] lRatioArray_obj = (double[][]) lRatioArray.toArray();
                lRatios[i] = lRatioArray_obj[0][0];
                lRatioArray.dispose();

                MWArray psArray = (MWArray) out.get(2);
                double[][] tmpd = ((double[][]) psArray.toArray());
                int[][] tmpi = new int[tmpd.length][tmpd[0].length];
                for (int m=0; m<tmpd.length; m++) {
                    for (int n=0; n<tmpd[m].length; n++) {
                        if (n>1) {
                            tmpi[m][n] = (int) (255.0*tmpd[m][n]);
                        } else {
                            tmpi[m][n] = (int) tmpd[m][n];
                        }
                    }
                }
                ps[i] = tmpi;
                psArray.dispose();
                
                //mc.dispose();
                
            }
            mc.dispose();
        } catch (MWException ex) {
            Logger.getLogger(CISH_Fork.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(CISH_Fork.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
    /**
     * Workhorse function to compute the CISH points.
     */
    protected void computeDirectly() {
        try {
            Hough_Circles hough = new Hough_Circles();
            for (int i=mStart; i<mStart+mLength; i++) {
                progress_container[0]++;
                
                TMAspot ts = tss.get(i);
                tpm.setStatusMessageLabel("Performing CISH Analysis ...");
                //tpm.setProgressbar((int)(100.0*(2*progress_container[0]-1)/(2*tss.size())));
                tpm.setProgressbar((int)(100.0*(progress_container[0])/(7*tss.size())));
                
                // get ImagePlus
                ImagePlus ip = new ImagePlus("", ts.getBufferedImage());
                progress_container[0]++;
                tpm.setProgressbar((int)(100.0*(progress_container[0])/(7*tss.size())));
                
                // get circles
                Point[] circ = hough.run(ip.getProcessor(), Math.max(1, psr-2), Math.max(1, psr+4), darkpoints);
                //Point[] circ = hough.run(ip.getProcessor(), psr, psr, 1, detectedPts, threshold, darkpoints);
                progress_container[0]++;
                tpm.setProgressbar((int)(100.0*(progress_container[0])/(7*tss.size())));
                
                // classifiy circles
                List<List> cepsgenes = CISH.getPoints(ip.getProcessor(), circ, psr, classifier, dataset);
                progress_container[0]++;
                tpm.setProgressbar((int)(100.0*(progress_container[0])/(7*tss.size())));
                
                // get final classified points as int[][]
                int[][] all_ps = new int[cepsgenes.get(0).size() + cepsgenes.get(1).size() + cepsgenes.get(2).size()][5];
                for (int j = 0; j<cepsgenes.get(0).size(); j++) {
                    all_ps[j] = (int[]) cepsgenes.get(0).get(j);
                }
                for (int j = 0; j<cepsgenes.get(1).size(); j++) {
                    all_ps[cepsgenes.get(0).size()+j] = (int[]) cepsgenes.get(1).get(j);
                }
                for (int j = 0; j<cepsgenes.get(2).size(); j++) {
                    all_ps[cepsgenes.get(0).size()+cepsgenes.get(1).size()+j] = (int[]) cepsgenes.get(2).get(j);
                }
                ps[i] = all_ps;
                progress_container[0]++;
                tpm.setProgressbar((int)(100.0*(progress_container[0])/(7*tss.size())));
                
                // get global ratio
                if (!cepsgenes.get(0).isEmpty() || !cepsgenes.get(2).isEmpty()) {
                    gRatios[i] = globalRatio(cepsgenes);
                } else if (!cepsgenes.get(1).isEmpty()) {
                    gRatios[i] = Double.POSITIVE_INFINITY;
                } else {
                    gRatios[i] = 1;
                }
                progress_container[0]++;
                tpm.setProgressbar((int)(100.0*(progress_container[0])/(7*tss.size())));
                
                // get local ratio
                int local_radius = 20*psr;
                if (!cepsgenes.get(0).isEmpty() || !cepsgenes.get(2).isEmpty()) {
                    lRatios[i] = localRatio(ip.getWidth(), ip.getHeight(), cepsgenes, local_radius, nPts);
                } else if (!cepsgenes.get(1).isEmpty()) {
                    lRatios[i] = Double.POSITIVE_INFINITY;
                } else {
                    lRatios[i] = 1;
                }
                progress_container[0]++;
                tpm.setProgressbar((int)(100.0*(progress_container[0])/(7*tss.size())));
                
                //tpm.setProgressbar((int)(100.0*(2*progress_container[0])/(2*tss.size())));
                cish.setProgressNumber(progress_container[0]/7, tss.size(), startTime);
            }
        } catch (Exception ex) {
            Logger.getLogger(CISH_Fork.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Workhorse function to parallely compute the CISH points.
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
    public static void doCISH_Fork(TMARKERPluginManager tpm, CISH cish, List<TMAspot> tss, int psr, int nPts, boolean darkpoints, double[] gRatios, double[] lRatios, int[][][] ps, Classifier classifier, Instances dataset) {
        int[] progress_container = new int[]{0};
        String text = tpm.getStatusMessageLabelText();
        
        //CISH_Fork fb = new CISH_Fork(t, cish, tss, psr, nPts, darkpoints, gRatios, lRatios, ps, classifier, dataset, 0, tss.size(), t.getOptionDialog().useParallelProgramming(), progress_container);
        CISH_Fork fb = new CISH_Fork(tpm, cish, tss, psr, nPts, darkpoints, gRatios, lRatios, ps, classifier, dataset, 0, tss.size(), false, progress_container);

        ForkJoinPool pool = new ForkJoinPool();

        long startTime = System.currentTimeMillis();
        pool.invoke(fb);
        long endTime = System.currentTimeMillis();
        
        cish.setProgressNumber(0, 0, 0);
        tpm.setStatusMessageLabel(text);
        tpm.setProgressbar(0);

        System.out.println("CISH_Fork took " + (endTime - startTime) + 
                " milliseconds.");
        
    }

    /**
     * Calculates and returns the global CISH ratio:
     *  cepsgenes.get(1).size() / cepsgenes.get(0).size()
     * 
     * @param cepsgenes The gene groups found (cepsgenes.get(0) = ceps, 
     * cepsgenes.get(1) = genes).
     * @return The global ceps to genes ratio.
     */
    private double globalRatio(List<List> cepsgenes) {
        return 1.0 * (cepsgenes.get(1).size()+cepsgenes.get(2).size()) / (cepsgenes.get(0).size()+cepsgenes.get(2).size());
    }
    
    /**
     * Calculates and returns the local CISH ratio. 
     * @param cepsgenes The gene groups found (cepsgenes.get(0) = ceps, 
     * cepsgenes.get(1) = genes).
     * @return The global ceps to genes ratio.
     */
    private double localRatio(int width, int height, List<List> cepsgenes, int local_radius, int nPts) {
        
        Random rand = new Random(1);
        
        if (cepsgenes.isEmpty() || (cepsgenes.get(0).isEmpty()&&cepsgenes.get(2).isEmpty())) {
            return 0;
        }
        
        // pre-compute the distance of all ceps to all ceps and all genes
        int n_ceps = cepsgenes.get(0).size();
        int n_genes = cepsgenes.get(1).size();
        int n_both = cepsgenes.get(2).size();
        double[][] distances = new double[n_ceps + n_both][n_ceps + n_both + n_genes];
        for (int i=0; i<n_ceps; i++) {
            int[] cep1 = (int[]) cepsgenes.get(0).get(i);
            for (int j=0; j<n_ceps; j++) {
                int[] cep2 = (int[]) cepsgenes.get(0).get(j);
                distances[i][j] = dist(cep1[0], cep1[1], cep2[0], cep2[1]);
            }
            for (int j=0; j<n_both; j++) {
                int[] cep2 = (int[]) cepsgenes.get(2).get(j);
                distances[i][n_ceps+j] = dist(cep1[0], cep1[1], cep2[0], cep2[1]);
            }
            for (int j=0; j<n_genes; j++) {
                int[] gene = (int[]) cepsgenes.get(1).get(j);
                distances[i][n_ceps+n_both+j] = dist(cep1[0], cep1[1], gene[0], gene[1]);
            }
        }
        for (int i=0; i<n_both; i++) {
            int[] cep1 = (int[]) cepsgenes.get(2).get(i);
            for (int j=0; j<n_ceps; j++) {
                int[] cep2 = (int[]) cepsgenes.get(0).get(j);
                distances[n_ceps+i][j] = dist(cep1[0], cep1[1], cep2[0], cep2[1]);
            }
            for (int j=0; j<n_both; j++) {
                int[] cep2 = (int[]) cepsgenes.get(2).get(j);
                distances[n_ceps+i][n_ceps+j] = dist(cep1[0], cep1[1], cep2[0], cep2[1]);
            }
            for (int j=0; j<n_genes; j++) {
                int[] gene = (int[]) cepsgenes.get(1).get(j);
                distances[n_ceps+i][n_ceps+n_both+j] = dist(cep1[0], cep1[1], gene[0], gene[1]);
            }
        }

        
        double[] lratios = new double[nPts];
        
        for (int i=0; i<nPts; i++) {
            
            // find a random point in the image
            int[] xyrand = cirrdnPt(width/2, height/2, (width+height)/5, rand);
            
            // find the closest cep/both to that point
            double closest_dist = Double.MAX_VALUE;
            double current_dist;
            int ind = 0;
            for (int k=0; k<cepsgenes.get(0).size(); k++) {
                int[] cep = (int[]) cepsgenes.get(0).get(k);
                current_dist = dist(xyrand[0], xyrand[1], cep[0], cep[1]);
                if (current_dist < closest_dist) {
                    closest_dist = current_dist;
                    ind = k;
                }
            }
            for (int k=0; k<cepsgenes.get(2).size(); k++) {
                int[] cep = (int[]) cepsgenes.get(2).get(k);
                current_dist = dist(xyrand[0], xyrand[1], cep[0], cep[1]);
                if (current_dist < closest_dist) {
                    closest_dist = current_dist;
                    ind = n_ceps + k;
                }
            }
            
            // from the closest cep/both, calculate the local ratio within local radius
            int n_lceps = 0;
            int n_lgenes = 0;
            for (int k=0; k<n_ceps; k++) {
                if (distances[ind][k]<local_radius) {
                    n_lceps++;
                }
            }
            for (int k=n_ceps; k<n_ceps+n_both; k++) {
                if (distances[ind][k]<local_radius) {
                    n_lceps++;
                    n_lgenes++;
                }
            }
            for (int k=n_ceps+n_both; k<n_ceps+n_both+n_genes; k++) {
                if (distances[ind][k]<local_radius) {
                    n_lgenes++;
                }
            }
            
            lratios[i] = 1.0*n_lgenes/n_lceps;
            
        }
        
        return Misc.mean(lratios);
    }
    
    /**
     * Create a random point within a radial area with center x0, y0 and radius R
     * @param x0 The center of the radial area (x).
     * @param y0 The center of the radial area (y).
     * @param R The radius of the radial area within which a random point is generated.
     * @param rand The random number generator. Can be null.
     * @return A random point x y.
     */
    static int[] cirrdnPt(int x0, int y0, double R, Random rand) {
        if (rand==null) {
            rand = new Random(1);
        }
        double t = 2*Math.PI*rand.nextDouble();
        double r = R*Math.sqrt(rand.nextDouble());
        int[] xy = new int[2];
        xy[0] = (int)(x0 + r*Math.cos(t));
        xy[1] = (int)(y0 + r*Math.sin(t));
        return xy;
    }
    
    /**
     * Calculate the Euclidean distance between two points.
     * @param x1 x-coord point 1.
     * @param y1 y-coord point 1.
     * @param x2 x-coord point 2.
     * @param y2 y-coord point 2.
     * @return The distance between the two points.
     */
    static double dist(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2-x1, 2.0) + Math.pow(y2-y1, 2.0));
    }
    
}

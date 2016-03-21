/*
 * Copyright (c) 2014, Peter J. Schueffler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package stainingestimation;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;

/**
 *
 * @author Peter J. Schueffler
 */
public class StainingEstimationParameterChangeThread extends Thread {
    
    TMARKERPluginManager tpm;
    StainingEstimation se;
    List<TMAspot> tss;
    int radius_start;
    int radius_end;
    int radius_d;
    int tol_start;
    int tol_end;
    int tol_d;
    int blur_start;
    int blur_end;
    int blur_d;
    int t_hema_start;
    int t_hema_end;
    int t_hema_d;
    int t_dab_start;
    int t_dab_end;
    int t_dab_d;
    int t_ch3_start;
    int t_ch3_end;
    int t_ch3_d;
    boolean lock_thresholds;
    boolean respectAreas;
    public boolean continu = true;

    /**
     * The StainingEstimationParameterChangeThread is a grid search troughout all parameters from given start to end values.
     * For each parameter setting, the color deconvolution is performed and the fscores and accuracies are recoreded. This
     * validation is only possible if manual full annotations are available to which the automated guesses can be compared.
     * @param tpm The TMARKERPluginManager with access to the main program.
     * @param se The StainingEstimation instance.
     * @param tss The TMAspots included in this testing.
     * @param radius_start Radius start value.
     * @param radius_end Radius end value.
     * @param radius_d Radius increment.
     * @param tol_start Tolerance start value.
     * @param tol_end Tolerance end value.
     * @param tol_d Tolerance increment.
     * @param blur_start Blue start value.
     * @param blur_end Blur end value.
     * @param blur_d Blur increment.
     * @param t_hema_start Threshold for channel 1 start value.
     * @param t_hema_end Threshold for channel 1 end value.
     * @param t_hema_d Threshold for channel 1 increment.
     * @param t_dab_start Threshold for channel 2 start value.
     * @param t_dab_end Threshold for channel 2 end value.
     * @param t_dab_d Threshold for channel 2 increment.
     * @param t_ch3_start Threshold for channel 3 start value.
     * @param t_ch3_end Threshold for channel 3 end value.
     * @param t_ch3_d Threshold for channel 3 increment.
     * @param lock_thresholds If true, t_dab will be kept identical to t_hema, such that both parameters shift equally.
     */
    StainingEstimationParameterChangeThread(TMARKERPluginManager tpm, StainingEstimation se, List<TMAspot> tss, int radius_start, int radius_end, int radius_d, int tol_start, int tol_end, int tol_d, int blur_start, int blur_end, int blur_d, int t_hema_start, int t_hema_end, int t_hema_d, int t_dab_start, int t_dab_end, int t_dab_d, int t_ch3_start, int t_ch3_end, int t_ch3_d, boolean lock_thresholds, boolean respectAreas) {
        this.tpm=tpm;
        this.se=se;
        this.tss=tss;
        this.radius_start = radius_start;
        this.radius_end = radius_end;
        this.radius_d = radius_d;
        this.tol_start=tol_start;
        this.tol_end=tol_end;
        this.tol_d = tol_d;
        this.blur_start=blur_start;
        this.blur_end=blur_end;
        this.blur_d = blur_d;
        this.t_hema_start=t_hema_start;
        this.t_hema_end=t_hema_end;
        this.t_hema_d = t_hema_d; 
        this.t_dab_start=t_dab_start;
        this.t_dab_end=t_dab_end;
        this.t_dab_d = t_dab_d;
        this.t_ch3_start = t_ch3_start;
        this.t_ch3_end = t_ch3_end;
        this.t_ch3_d = t_ch3_d;
        this.lock_thresholds = lock_thresholds;
        this.respectAreas = respectAreas;
    }
    
    @Override
    public void run() {
        try{ 
            //// COUNT HERE THE PARAMETERS
            int num_param = 0;
            for (int radius = radius_start; radius <= radius_end; radius += radius_d) {
                for (int tolerance = tol_start; tolerance <= tol_end; tolerance += tol_d) {
                    for (int blur = blur_start; blur <= blur_end; blur += blur_d) {
                        for (int t_hema = t_hema_start; t_hema <= t_hema_end; t_hema += t_hema_d) {
                            for (int t_dab = t_dab_start; t_dab <= t_dab_end; t_dab += t_dab_d) {
                                for (int t_ch3 = t_ch3_start; t_ch3 <= t_ch3_end; t_ch3 += t_ch3_d) {
                                    if (!lock_thresholds || (t_hema==t_dab && t_hema==t_ch3)) {
                                        num_param++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // END COUNT HERE  

            // to collect the f-scores:
            double[][][] fscores = new double[tss.size()][num_param][14];

            //initialize the double [][]
            double[] stats;
            List<double[][]> curves = new ArrayList<>(tss.size());
            for (TMAspot ts : tss) {
                curves.add(new double[2][num_param]);
            }
            //end initialize
            
            // go through the paramter list
            int staining_index = se.getParam_ColorChannelIndex();
            int s = 0;
            int TMblur = 10;
            String colorChannel = se.getParam_ColorChannel();
            outerloop:
            for (int radius = radius_start; radius <= radius_end; radius += radius_d) {
                tpm.setLabelRadius(radius);
                for (int tolerance = tol_start; tolerance <= tol_end; tolerance += tol_d) {
                    se.setParam_tolerance(tolerance); // param_tolerance
                    for (int blur = blur_start; blur <= blur_end; blur += blur_d) {
                        se.setParam_blur(blur);
                        for (int t_hema = t_hema_start; t_hema <= t_hema_end; t_hema += t_hema_d) {
                            se.setParam_t_hema(t_hema);
                            for (int t_dab = t_dab_start; t_dab <= t_dab_end; t_dab += t_dab_d) {
                                for (int t_ch3 = t_ch3_start; t_ch3 <= t_ch3_end; t_ch3 += t_ch3_d) {
                                    if (!lock_thresholds || (t_hema==t_dab && t_hema==t_ch3)) {
                                        se.setParam_t_dab(t_dab);
                                        se.setParam_t_dab(t_ch3);
                                        // do the Staining Estimation
                                        if (continu) se.performStainingEstimation(tss, radius, blur, tolerance, TMblur, TMblur, TMblur, t_hema, t_dab, t_ch3, false, true, true, false, colorChannel, respectAreas, false, false, false, false);
                                        // collect the Scores
                                        for (int i = 0; i < tss.size(); i++) {
                                            if (continu) {
                                                stats = tss.get(i).calculateMatchStatistics();
                                                curves.get(i)[0][s] = Math.max(0.0, stats[1 + 7]);
                                                curves.get(i)[1][s] = Math.max(0.0, stats[2 + 7]);

                                                // collect f-scores
                                                fscores[i][s][0] = tss.get(i).getID();
                                                fscores[i][s][1] = staining_index;
                                                fscores[i][s][2] = radius;
                                                fscores[i][s][3] = tolerance;
                                                fscores[i][s][4] = blur;
                                                fscores[i][s][5] = t_hema;
                                                fscores[i][s][6] = t_dab;
                                                fscores[i][s][7] = t_ch3;
                                                fscores[i][s][8] = TMblur;
                                                fscores[i][s][9] = TMblur;
                                                fscores[i][s][10] = TMblur;
                                                fscores[i][s][11] = Math.max(0.0, stats[1 + 7]);
                                                fscores[i][s][12] = Math.max(0.0, stats[2 + 7]);
                                                fscores[i][s][13] = Math.max(0.0, stats[0 + 7]);
                                            } else {
                                                break outerloop;
                                            }
                                        }
                                        if (tmarker.DEBUG > 0 && continu) {
                                            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Test " + s + " of " + num_param + " (" + Math.round((1000.0 * s) / num_param) / 10.0 + " %)");
                                        }
                                        s++;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // write fscores as table
             try {
                 se.writePrecisionRecallTable(fscores);
             } catch (Exception e) {
                e.printStackTrace();
            }
            
            //sort fscores accoroding to the actual fscore
            /*
            try {
                for (int i = 0; i < tss.size(); i++) {
                    double[] f = new double[fscores[i].length];
                    for (int j = 0; j < f.length; j++) {
                        f[j] = fscores[i][j][11];
                    }
                    int[] order = Misc.orderArray(f, true);
                    double[][] newFscores = new double[fscores[i].length][fscores[i][0].length];
                    for (int j = 0; j < newFscores.length; j++) {
                        System.arraycopy(fscores[i][j], 0, newFscores[order[j]], 0, newFscores[order[j]].length);
                        //for (int k=0; k<newFscores[order[j]].length; k++) {
                        //    newFscores[order[j]][k] = fscores[i][j][k];
                        //}
                    }
                    fscores[i] = newFscores;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            
            // write fscores as an external table file
            if (tmarker.DEBUG > 0) {
                try {
                    File file = new File(tpm.getTmpDir() + File.separator + "PrecicionRecall.csv");
                    file.deleteOnExit();
                    FileWriter writer = new FileWriter(file);
                    writer.append("id;staining;radius;tolerance;blur;t_hema;t_dab;TMblur_hema;TMblur_dab;precision;recall;FScore\n");
                    writer.append(Arrays.deepToString(fscores).replaceAll("\\]\\], \\[\\[", "\n \n").replaceAll("\\], \\[", "\n").replaceAll(", ", ";").replaceAll("\\[\\[\\[", "").replaceAll("\\]\\]\\]", ""));
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            XYSeriesCollection dataset = new XYSeriesCollection();

            for (int i = 0; i < tss.size(); i++) {
                XYSeries series = new XYSeries(tss.get(i).getID(), false, true);
                for (int j = 0; j < curves.get(i)[0].length; j++) {
                    series.add(curves.get(i)[1][j], curves.get(i)[0][j]);
                }
                dataset.addSeries(series);
            }

            //JFreeChart chart = ChartFactory.createXYLineChart("Precision Recall Plot", "Recall", "Precision", dataset, PlotOrientation.VERTICAL, true, true, false);
            JFreeChart chart = ChartFactory.createScatterPlot("Precision Recall Plot", "Recall", "Precision", dataset, PlotOrientation.VERTICAL, true, true, false);
            chart.getXYPlot().getDomainAxis().setRange(0, 1);
            chart.getXYPlot().getRangeAxis().setRange(0, 1);
            chart.setBackgroundPaint(null);
            chart.getPlot().setBackgroundPaint(null);
            //chart.getPlot().setBackgroundPaint(Color.WHITE);
          //  XYLineAndShapeRenderer las = new XYLineAndShapeRenderer(true, false);
          //  XYToolTipGenerator ttg = new StandardXYToolTipGenerator();
          //  las.setBaseToolTipGenerator(ttg);
          //  chart.getXYPlot().setRenderer(0, las);
            ChartPanel chartPanel = new ChartPanel(chart);
            se.setPrecisionRecallPlot(chartPanel);
        } catch (Exception e) {
            continu = false;
            tpm.setStatusMessageLabel("Staining Estimation Stopped."); tpm.setProgressbar(0);
            if (tmarker.DEBUG>0) {
                e.printStackTrace();
            }
        }
    }
    
}

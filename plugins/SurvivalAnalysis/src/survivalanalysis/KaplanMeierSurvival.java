/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package survivalanalysis;

import java.awt.BasicStroke;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javastat.survival.regression.CoxRegression;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import tmarker.misc.Misc;

/**
 * A class to create the Kaplan Meier Statistics.
 * @author Peter J. Schueffler
 */
public class KaplanMeierSurvival {
    
    /**
     * Creates a Kaplan Meier Survival plot with as many lines as groups of patients are existing. 
     * Every Group should have a name (for legend), and samples with time and censor information.
     * @param group_names The names of the different series (only used for legend).
     * @param times_groups Per Series the case-wise time of event or censor.
     * @param censored_groups Per Series the case-wise event variable. true:death/Event, false:censored
     * @param xLabel Label for the x-Axis, coming from the time variable selected by the user.
     * @return A ChartPanel with the JFreeChart.
     */
    public static JPanel createSurvivalChart(List<String> group_names, List<List<Double>> times_groups, List<List<Boolean>> censored_groups, String xLabel) {
        
        // number of samples per group
        int[] numberSamples_groups = new int[times_groups.size()];
        for (int i=0; i<times_groups.size(); i++) {
            numberSamples_groups[i] = times_groups.get(i).size();
        }
        
        // sort samples according to time per group
        for (int i=0; i<times_groups.size(); i++) {
            // create the ordering
            ListIndexComparator comparator = new ListIndexComparator(times_groups.get(i));
            Integer[] indexes = comparator.createIndexArray();
            Arrays.sort(indexes, comparator);
            
            // sort the times according to the ordering
            List<Double> times = new ArrayList<>(times_groups.get(i).size());
            for (int j=0; j<times_groups.get(i).size(); j++) {
                times.add(times_groups.get(i).get(indexes[j]));
            }
            times_groups.set(i, times);
            
            // sort the censores according to the ordering
            List<Boolean> censores = new ArrayList<>(censored_groups.get(i).size());
            for (int j=0; j<censored_groups.get(i).size(); j++) {
                censores.add(censored_groups.get(i).get(indexes[j]));
            }
            censored_groups.set(i, censores);
        }
        
        // create n(t) per group
        List<List<Integer>> nt_groups = new ArrayList<>();
        for (int i=0; i<times_groups.size(); i++) {
            List<Integer> nt = new ArrayList<>(censored_groups.get(i).size());
            int n = numberSamples_groups[i];
            for (Boolean get : censored_groups.get(i)) {
                nt.add(n--);
            }
            nt_groups.add(nt);
        }
        
        // create S(t) per group
        List<double[]> St_group = new ArrayList<>();
        for (int i=0; i<times_groups.size(); i++) {
            double[] St = new double[censored_groups.get(i).size()];
            for (int j=0; j<St.length; j++) {
                St[j] = KaplanMeierEstimate(times_groups.get(i).get(j), times_groups.get(i), censored_groups.get(i), nt_groups.get(i)); 
            }
            St_group.add(St);
        }
        
        // calculate the difference significance of the groups
        double p = SurvivalDifferenceSignificance(times_groups, censored_groups, nt_groups);
        
        //// plot the groups
        // create datasets per group
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (int i=0; i<times_groups.size(); i++) { 
            XYSeries xys = new XYSeries(group_names.get(i), false, true);
            XYSurvivalDataItem xyd = new XYSurvivalDataItem(0, 100, false);
            xys.add(xyd);
            for (int j=0; j<times_groups.get(i).size(); j++) {
                xyd = new XYSurvivalDataItem(times_groups.get(i).get(j), 100*St_group.get(i)[j], censored_groups.get(i).get(j));
                xys.add(xyd);
            }
            dataset.addSeries(xys);
        }
        JFreeChart chart = ChartFactory.createXYStepChart("Kaplan Meier Estimation", xLabel, "Survivals [%]", dataset, PlotOrientation.VERTICAL, true, true, false);
        
        // Differenciate date Axis (default) with Month Axis
        if (times_groups.size()>0 && times_groups.get(0).size()>0 && times_groups.get(0).get(0)<1000) {
            NumberAxis na = new NumberAxis(xLabel);
            na.setLabelFont(chart.getXYPlot().getDomainAxis().getLabelFont());
            na.setLabelPaint(chart.getXYPlot().getDomainAxis().getLabelPaint());
            na.setTickLabelFont(chart.getXYPlot().getDomainAxis().getTickLabelFont());
            na.setTickLabelPaint(chart.getXYPlot().getDomainAxis().getTickLabelPaint());
            chart.getXYPlot().setDomainAxis(na);
        }
        chart.addSubtitle(new TextTitle("LogRank Test (Survival Difference): p = " + (p==Double.NaN ? "NaN" : (Math.round(10000*p)/10000.0))));
        XYSurvivalStepRenderer xysr = new XYSurvivalStepRenderer();
        xysr.setBaseItemLabelsVisible(true);
        for (int i=0; i<dataset.getSeriesCount(); i++) {
            xysr.setSeriesStroke(i, new BasicStroke(2));
        }
        xysr.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        chart.getXYPlot().setRenderer(xysr);
        chart.getXYPlot().getRangeAxis().setRange(0, 100);
        chart.setBackgroundPaint(null);
        chart.getPlot().setBackgroundPaint(null);
        ChartPanel chartPanel = new ChartPanel(chart);
        return chartPanel;
        
    }
    
    /**
     * Calculates S(t) as the Kaplan Meier Estimator for a given time t.
     * @param t Time to which the S(t) should be calculated.
     * @param times Unique list of times to event or censor.
     * @param censor List d(t) of number of cases with event on time t.
     * @param nt N(t) is the number of cases left at the time point t. nt is the list of all N(t) for all timepoints in times.
     * @return S(t): the Kaplan Meier Estimate of time point t.
     */
    static double KaplanMeierEstimate(double t, List<Double> times, List<Boolean> censor, List<Integer> nt) {
        double st = 1;
        for (int i=0; i<times.size(); i++) {
            if (times.get(i)<=t) {
                st *= 1 - (censor.get(i) ? 1.0 : 0.0)/nt.get(i);
            }
        }
        return st;
    }
    
    /**
     * Calculates the statistical significance between two or more survival groups (Chi-Squared test).
     * @param times_groups The time entries of the groups.
     * @param censor_groups The censor entries of the groups. Has to have same size as time_groups. The first group has to have
     * same size as the first group in times_groups, and so on.
     * @param nt_groups n(t) per group.
     * @return The significance between the survival curve. The closer to zero the more significant the difference.
     */
    static double SurvivalDifferenceSignificance(List<List<Double>> times_groups, List<List<Boolean>> censor_groups, List<List<Integer>> nt_groups) {
        // calculate dataset size (number of cases)
        int n_cases = 0;
        for (int i=0; i<nt_groups.size(); i++) {
            try {
                n_cases += nt_groups.get(i).get(0);
            } catch (Exception e) {
            }
        }
        
        if (n_cases == 0) {
            return Double.NaN;
        }
        
        List<Double> times = new ArrayList<>();
        List<Integer> groups = new ArrayList<>();
        List<Boolean> censored = new ArrayList<>();
        List<Integer> nt = new ArrayList<>();
        for (int i=0; i<times_groups.size(); i++) {
            times.addAll(times_groups.get(i));
            censored.addAll(censor_groups.get(i));
            nt.addAll(nt_groups.get(i));
            for (int j=0; j<times_groups.get(i).size(); j++) {
                groups.add(i);
            }
        }
        // sort lists according to time (from all groups)
        List<Double> times_sorted = new ArrayList<>();
        List<Integer> groups_sorted = new ArrayList<>();
        List<Boolean> censor_sorted = new ArrayList<>();
        List<Integer> nt_sorted = new ArrayList<>();
        // create the ordering
        ListIndexComparator comparator = new ListIndexComparator(times);
        Integer[] indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);

        // sort the times according the ordering
        for (int i=0; i<times.size(); i++) {
            times_sorted.add(times.get(indexes[i]));
            groups_sorted.add(groups.get(indexes[i]));
            censor_sorted.add(censored.get(indexes[i]));
            nt_sorted.add(nt.get(indexes[i]));      
        }
        
        // create an overall sample number vector (over all groups)        
        List<Integer> n = new ArrayList<>();
        int k = times.size();
        for (int i=0; i<times.size(); i++) {
            n.add(k--);
        }
        
        // create the n vector per group
        // initialization
        List<List<Integer>> n_groups = new ArrayList<List<Integer>>();
        for (int i=0; i<times_groups.size(); i++) {
            n_groups.add(new ArrayList<Integer>());
            n_groups.get(i).add(nt_groups.get(i).get(0));
        }
        // filling with numbers of known alives on each timepoint
        for (int i=1; i<times_sorted.size(); i++) {
            for (int j=0; j<times_groups.size(); j++) {
                if (groups_sorted.get(i-1)==j) {
                    n_groups.get(j).add(n_groups.get(j).get(i-1)-1);
                } else {
                    n_groups.get(j).add(n_groups.get(j).get(i-1));
                }
            }
        }
        
        int ngroup = times_groups.size();
        int nstrat = 1;
        double rho = 0.0;
        double[] time = Misc.DoubleListToArray(times_sorted);
        int[] status = Misc.BooleanListToIntArray(censor_sorted);
        int[] group = Misc.IntegerListToArray(groups_sorted);
        int[] strata = new int[n_cases];
        Arrays.fill(strata, 0);
        strata[strata.length-1] = 1;
        double[] obs = new double[ngroup*nstrat]; Arrays.fill(obs, 0);
        double[] exp = new double[ngroup*nstrat]; Arrays.fill(exp, 0);
        double[] var = new double[ngroup*ngroup]; Arrays.fill(var, 0);
        double[] risk = new double[ngroup]; Arrays.fill(risk, 0);
        double[] kaplan = new double[n_cases]; Arrays.fill(kaplan, 0);
            
        Misc.survdiff2(n_cases, ngroup, nstrat, rho, time, status,
            group, strata, obs, exp, var, risk, kaplan);
        
        // calculate the chisquared logrank statistics
        double chisqlogrank = 0.0;
        double chisqlogrank2 = 0.0;
        for (int i=0; i<times_groups.size(); i++) {
            double o = obs[i];
            double e = exp[i];
            double v = var[i*(1+ngroup)];
            chisqlogrank += Math.pow(o-e, 2) / e;
            chisqlogrank2 += Math.pow(o-e, 2) / v;
        }
        // if chisqlogrank is devided by e instead of v, comment this out:
        chisqlogrank2 /= 1.0 * times_groups.size();
        
        int df = times_groups.size()-1;
        try {            
            ChiSquaredDistribution distribution = new ChiSquaredDistribution(df);
            return (1.0-distribution.cumulativeProbability(chisqlogrank2));
        } catch (Exception e) {
            return Double.NaN;
        }
    }
    
    /**
     * Creates a HTML summary of a coxregression. If covariatenames and skipped_samples are known,
     * they are included in the summary (the cox itself does not store this information).
     * @param cox The CoxRegression which provides the summary.
     * @param covariatenames The variable names of the regression, if known (null permitted).
     * @param skipped_samples The number of samples skipped for the regression, if known (-1 permitted).
     * @return A HTML description of cox. Can be printed on JLabels or JTextFields.
     */
    public static String CoxRegressionSummary(CoxRegression cox, String[] covariatenames, int skipped_samples) {
        int n = cox.time.length;
        int ne = 0;
        DecimalFormat df = new DecimalFormat("#.######");
        df.setRoundingMode(RoundingMode.HALF_UP);
        for (double c: cox.censor) {
            if (c>0) {
                ne++;
            }
        }
        JLabel jlabel = new JLabel();
        String out = "<html><head></head><body><h1>" + "COX Regression" + "</h1>";
        out += "&nbsp;&nbsp;n=" + n + ", number of events=" + ne + "<br>";
        if (skipped_samples>0) {
            out += "&nbsp;&nbsp;&nbsp;&nbsp;(" + skipped_samples + " observation skipped due to missingness)<br>";
        }
        out += "<br><table border=\"0\">";
        out += "<tr>"
                + "<th align=\"right\"></th> "
                + "<th align=\"right\">coef</th> "
                + "<th align=\"right\">exp(coef)</th> "
                + "<th align=\"right\">se(ceof)</th> "
                + "<th align=\"right\">z</th> "
                + "<th align=\"right\">CI-</th> "
                + "<th align=\"right\">CI+</th> "
                + "<th align=\"right\">p</th> "
                + "<th align=\"left\"></th></tr>";
        for (int i=0; i<cox.coefficients.length; i++) {
            out += "<tr>";
            if (covariatenames!=null) {
                out += "<td align=\"right\"><b>" + covariatenames[i] + "</b></td> ";
            }
            out += "<td align=\"right\">" + (Double.isNaN(cox.coefficients[i]) ? "NaN" : df.format(cox.coefficients[i])) + "</td> "
                    + "<td align=\"right\">" + (Double.isNaN(cox.coefficients[i]) ? "NaN" : df.format(Math.pow(Math.E, cox.coefficients[i]))) + "</td> "
                    + "<td align=\"right\">" + (Double.isNaN(cox.variance[i][i]) ? "NaN" : df.format(Math.sqrt(cox.variance[i][i]))) + "</td> "
                    + "<td align=\"right\">" + (Double.isNaN(cox.testStatistic[i]) ? "NaN" : df.format(cox.testStatistic[i])) + "</td> "
                    + "<td align=\"right\">" + (Double.isNaN(cox.confidenceInterval[i][0]) ? "NaN" : df.format(cox.confidenceInterval[i][0])) + "</td> "
                    + "<td align=\"right\">" + (Double.isNaN(cox.confidenceInterval[i][1]) ? "NaN" : df.format(cox.confidenceInterval[i][1])) + "</td> "
                    + "<td align=\"right\">" + (Double.isNaN(cox.pValue[i]) ? "NaN" : df.format(cox.pValue[i])) + "</td>";
            String significancelevel = "";
            if (!Double.isNaN(cox.pValue[i])) {
                if (cox.pValue[i]<0.0005) {
                    significancelevel = "***";
                } else if (cox.pValue[i]<0.005) {
                    significancelevel = "**";
                } else if (cox.pValue[i]<0.05) {
                    significancelevel = "*";
                }
            }
            out += "<td align=\"left\">" + significancelevel + "</td></tr>";
        }
        out += "</table>";
        out += "<br>" + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;* p&lt;0.05&nbsp;&nbsp;&nbsp;&nbsp;** p&lt;0.005&nbsp;&nbsp;&nbsp;&nbsp;*** p&lt;0.0005 ";
        out += "<br><br>" + "<i>&nbsp;&nbsp;You can change the layout of the plot(s) on the right with right mouse click.</i>";
        out += "</body></html>";
        return out;
    }
    
}

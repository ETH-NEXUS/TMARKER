/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmarker.misc;

import java.util.List;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This class is only for converting a biomarker to an ARFF format.
 * @author Peter Schueffler
 */
public class ARFF {
    
    
    /**
     * Creats a ARFF format from a given List of tss (e.g. ExperimentalSamples, entered by the user) and a given list of CpGNumbers.
     * These numbers are the attributes for the ARFF model.
     * The relation is 'biomarker_exp_(bc.name)_optimization', 
     * the class {unmethylated, methylated} for not methylated or methylated
     * and each instance in the data section is the list of methylation scores of every CpG group of bc
     * concerning on particular sample. This score is calculated from the sample's bisulfite methylation profile.
     * @param cpgNumberGroups The attributes of the ARFF file. These are the CpG number groups of the biomarker.
     * @param bc The biomarkerCandidate from which a ARFF format is created.
     * @param trainingSamples the tss with which the logistic is trained. if NULL, Trainingssamples from MethMarker
     * are used (loaded in the main window, rather than entered in the biomarker experimental validation table).
     * @return The arff is a 'Instances' (look WEKA package for more details).
     */
    /*public static Instances createARFFFromCpGNumbers(List<List<Integer>> cpgNumberGroups, BiomarkerCandidate bc, List<AbstractSample> trainigSamples) {
        FastVector      atts;
        FastVector      attVals;
        Instances       data;
        String          att;
        double[]        vals;
        int             i, j, n, m;

        // 1. set up attributes
        atts = new FastVector();
        // - CpG values -> numeric
        for (i=0, n=cpgNumberGroups.size(); i<n; i++) {
            List<Integer> cpgnumbers = cpgNumberGroups.get(i);
            att = "CpG-";
            for (j=0, m=cpgnumbers.size(); j<m; j++) {
                att += Integer.toString(cpgnumbers.get(j)) + "-";
            }
            atts.addElement(new Attribute(att));
        }

        // - Class -> nominal
        attVals = new FastVector();
        attVals.addElement("methylated");
        attVals.addElement("unmethylated");
        atts.addElement(new Attribute("class", attVals));

        // 2. create Instances object
        data = (bc.hasExperimentalSamples()) ? new Instances("biomarker_" + bc.getID() + "_optimization", atts, 0) :
            new Instances("biomarker_exp_" + bc.getID() + "_optimization", atts, 0);

        // 3. fill with data
        List<AbstractSample> sList = (trainigSamples == null) ? bc.getCenter().getTrainingSamples() : trainigSamples;
        for (i=0, n=sList.size(); i<n; i++) {
            // one instance
            vals = new double[data.numAttributes()];
            AbstractSample s = sList.get(i);
            for (j=0, m=bc.getAssays().size(); j<m; j++) {
                double markerScore = bc.getAssays().get(j).getMethGrade(s);

                // - Scores -> numeric
                vals[j] = markerScore;
            }
            // - Class -> nominal
            if (s.getClassC() == AbstractSample.UNKNOWN) {
                vals[m] = Instance.missingValue();
            }
            else {
                vals[m] = (s.getClassC() == AbstractSample.NEGATIVE) ? attVals.indexOf("unmethylated") : attVals.indexOf("methylated");
            }
            // add
            data.add(new Instance(1.0, vals));
        }

        // 4. set data class index (last attribute is the class)
        data.setClassIndex(data.numAttributes() - 1);

        if (Center.DEBUG > 4) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, data.toString());
        }
        
        return data;
    }*/
    
    /**
     * Creats a ARFF format from a given TMAspot list. The ARFF contains then the 
     * relation 'IntensityClustering_Features' and 
     * the attributes '1', '2', '3', ... (depending on how many attributes are give by (TMAspot).getFeatureVector_IntensityClustering()).
     * @param tss the TMAspot list from which the ARFF file is created.
     * @param n_cl number of classes.
     * @return The arff is a 'Instances' (look WEKA package for more details).
     */
    public static Instances createARFFForClustering(List<TMAspot> tss, int n_cl) {
        String text = tss.isEmpty()? "": tss.get(0).getCenter().getStatusMessageLabel().getText();
        FastVector      atts;
        Instances       data;
        double[]        vals;
        int             i, n;

        // 1. create arff data
        atts = new FastVector();
        
        vals = tss.get(0).getFeatureVector_IntensityClustering();
        for (i=0; i<vals.length; i++) {
            atts.addElement(new Attribute(Integer.toString(i)));
        }
        
        // 2. create Instances object
        data = new Instances("IntensityClustering_Features", atts, 0);
        
        // 3. fill with data
        for (TMAspot ts: tss) {
            ts.getCenter().setStatusMessageLabel(ts.getName() + ": Creating ARFF Instances ...");
            // add the instance
            Instance inst = new Instance(1.0, ts.getFeatureVector_IntensityClustering());
            inst.setDataset(data);
            data.add(inst);
        }

        // 4. set data class index (last attribute is the class)
        //data.setClassIndex(data.numAttributes() - 1); // not for weka 3.5.X

        if (tmarker.DEBUG > 4) {
            java.util.logging.Logger.getLogger(ARFF.class.getName()).log(java.util.logging.Level.INFO, data.toString());
        }
        
        if (!tss.isEmpty()) {
            tss.get(0).getCenter().setStatusMessageLabel(text);
        }
        
        return data;
    }

}

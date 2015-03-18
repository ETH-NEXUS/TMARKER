/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleusclassification;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import tmarker.TMAspot.TMALabel;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveUseless;

/**
 *
 * @author Peter J. Schueffler
 */
public class SLICsuperpixels {
    
    /** for serialization */
    private static final long serialVersionUID = 1L;

    public boolean verbose = true;
    public List<Superpixel> sps = new ArrayList<>();
    public int width;
    public int height;
    public int sz;
    public int numlabels;
    public TMAspot ts = null;
    private Instances instances = null; // features of superpixels
    CancerNucleusClassification CNC;
    
    /**
     * Debug Function: Save a int[] as image.
     * @param img The int array (INT_ARGB).
     * @param w Width of the image.
     * @param h Height of the image.
     * @param name Filename with extension.
     */
    public static void saveSPI(int[] img, int w, int h, String name) {
        try {
            BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    b.setRGB(x, y, (img[x*h+y] | 0xFF000000));
                }
            }
            ImageIO.write(b, "PNG", new File(name));
        } catch (IOException ex) {
            Logger.getLogger(SLICsuperpixels.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
   
    
    
    public List<Superpixel> getSuperpixels() {
        return sps;
    }
    
    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
    
    
    public static void run(SLICsuperpixels slicsp, BufferedImage I, int m_spcount, int m_compactness, boolean lab_space, int blur) {

        tmarker t = slicsp.getTMAspot().getCenter();
        try {
            // blur the immage
            if (blur>0) {
                GaussianBlur gb = new GaussianBlur();
                ImagePlus ip = new ImagePlus(null, I);
                gb.blurGaussian(ip.getProcessor(), blur, blur, 0.02);
                I = ip.getBufferedImage();
            }
            
            if (slicsp.verbose) {t.setStatusMessageLabel("Performing Superpixeling ..."); t.setProgressbar(10);}
            
            int width = I.getWidth();
            int height = I.getHeight();
            int [] img = new int[width*height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    img[y*width+x] = I.getRGB(x, y);
                }
            }
            I.flush();
            I=null;
            
            run(slicsp, img, width, height, m_spcount, m_compactness, lab_space);
        } catch (Exception e) {
            e.printStackTrace();
            if (slicsp.verbose) {t.setStatusMessageLabel("Performing Superpixeling: Failed. Maybe too large image."); t.setProgressbar(0 );}
        }
    }
    
    public static void run(SLICsuperpixels slicsp, int[] img, int width, int height, int m_spcount, int m_compactness, boolean lab_space) {

        tmarker t = slicsp.getTMAspot().getCenter();
        try {
            slicsp.sps.clear();
            slicsp.width = width;
            slicsp.height = height;
            slicsp.sz = width*height;
            if (slicsp.verbose) {t.setStatusMessageLabel("Performing Superpixeling: segmentation ..."); t.setProgressbar(20);}
            int[] labels = new int[slicsp.sz];
            SLIC slic = new SLIC();
            slicsp.numlabels = slic.DoSuperpixelSegmentation_ForGivenK(slicsp, img, slicsp.width, slicsp.height, labels, 0, m_spcount, m_compactness, lab_space);     
            slicsp.numlabels = uniformLabels(labels, slicsp.numlabels);
            
            System.gc();
            if (slicsp.verbose) {t.setStatusMessageLabel("Performing Superpixeling: make superpixel objects ..."); t.setProgressbar(95);}
            slicsp.sps = superpixelsToObjects(slicsp, labels);

            if (slicsp.verbose) {t.setStatusMessageLabel("Performing Superpixeling: done."); t.setProgressbar(100);}
            labels = null; 
            if (slicsp.verbose) {t.setStatusMessageLabel("Performing Superpixeling: done."); t.setProgressbar(0);}
        } catch (Exception e) {
            e.printStackTrace();
            if (slicsp.verbose) {t.setStatusMessageLabel("Performing Superpixeling: Failed. Maybe too large image."); t.setProgressbar(0 );}
        }
    }
    
    /**
     * Uniforms the labels as created by SLIC algorithmus. Afterwards, labels contains successive
     * label numbers from 0 to (n-1) for n superpixels.
     * @param labels The labels as calculated by SLIC algorithm.
     * @param numlabels The number of superpixels as calculated by SLIC algorithm.
     * @return Corrected number of superpixels (n).
     */
    public static int uniformLabels(int[] labels, int numlabels) {
        HashMap<Integer, Integer> sp_created = new HashMap<Integer, Integer>((int) (1.01*numlabels), 1);
        int ind=-1;
        int l;
        for (int i=0; i<labels.length; i++) {
            if (sp_created.containsKey(labels[i])) {
                l = sp_created.get(labels[i]);
            } else {
                ind++;
                l = ind;
                sp_created.put(labels[i], ind);
            }
            labels[i] = l;
        }
        return ind+1;
    }
    
    /**
     * Transforms the superpixels calculated by SLIC into Java objects "Superpixel".
     * SLIC stores the superpixels in labels.
     * @param slicsp The slicsp, which is the container of the superpixels.
     * @param labels The superpixels calculated by SLIC algorithm.
     * @return A list with created superpixel objects.
     */
    public static List<Superpixel> superpixelsToObjects(SLICsuperpixels slicsp, int[] labels) {
        // rough sp size in pixel is needed for sp initialization
        
        // pre-run: discover the number of pixels for each SP (needed for int array initialization of SP).
        int[] innerpx_num = new int[slicsp.numlabels];
        int[] borderpx_num = new int[slicsp.numlabels];
        for (int i=0; i<slicsp.numlabels; i++) {
            innerpx_num[i]=0;
            borderpx_num[i]=0;
        }
        for (int i=0; i<labels.length; i++) {
            if (isBorderPoint(i, labels, slicsp.width, slicsp.height, true)) {
                borderpx_num[labels[i]]++;
            } else {
                innerpx_num[labels[i]]++;
            }
        }
        // end pre-run
        
        List<Superpixel> spm = new ArrayList<>(slicsp.numlabels);
        for (int i=0; i<slicsp.numlabels; i++) {
            spm.add(null);
        }
        
        Superpixel sp;
        Superpixel sp_neighbour;
        for (int i=0; i<labels.length; i++) {
            sp = spm.get(labels[i]);
            if (sp == null) {
                sp = new Superpixel(slicsp.getTMAspot().getName()+"_"+labels[i], TMALabel.LABEL_BG, slicsp, innerpx_num[labels[i]], borderpx_num[labels[i]]);
                spm.set(labels[i], sp);
            } 
            if (isBorderPoint(i, labels, slicsp.width, slicsp.height, true)) {
                sp.addBorderPoint(i);
                int neighbour = getNeighbourLabel(i, labels, slicsp.width, slicsp.height);
                if (neighbour>=0) {
                    sp_neighbour = spm.get(neighbour);
                    if (sp_neighbour == null) {
                        sp_neighbour = new Superpixel(slicsp.getTMAspot().getName()+"_"+neighbour, TMALabel.LABEL_BG, slicsp, innerpx_num[neighbour], borderpx_num[neighbour]);
                        spm.set(neighbour, sp_neighbour);
                    }
                    sp.addNeighbour(sp_neighbour);
                }
            } else {
                sp.addInsidePoint(i);
            }
        }
        
        // post processing
        BufferedImage I = slicsp.ts.getBufferedImage();
        for (int i=spm.size()-1; i>=0; i--) {
            Superpixel spx = spm.get(i);
            if (spx!=null) {
                //if (spx.getNeighbours().size()==1) {
                    //((Superpixel)(spx.getNeighbours().toArray()[0])).addInsidePoints(spx.getInsidePoints());
                    //((Superpixel)(spx.getNeighbours().toArray()[0])).addInsidePoints(spx.getBorderPoints());
                    //spx.getNeighbours().remove(spx);
                    //spm.remove(i);
                //} else {
                    spx.calculateEntropy(I);
                    //spx.vt.calculateBorders();
                //}
            } else {
                spm.remove(i);
            }
        }
        // END post processing      
                
        return spm;
    }
    
    /**
     * Returns the label of a neighbour superpixel in left, top, right and bottom direction.
     * If there is no different superpixel, -1 is returned.
     * @param i The pixel index under consideration.
     * @param labels The label image, calculated by SLIC algorithm.
     * @param width The width of the full TMA image.
     * @param height The height of the full TMA image.
     * @return The label numbber of a neighbour superpixel. -1, if there is no different neighbour.
     */
    private static int getNeighbourLabel(int i, int[] labels, int width, int height) {
        int n = -1;
        if (i<=width                               // i is on top image border
            || i>=(height-1)*width                 // i is on bottom image border
            || Math.IEEEremainder(i+1, width)==0   // i is on left image border
            || Math.IEEEremainder(i, width)==0) {  // i is on rigth image border 
            return n;
        }
        // left neighbor 
        if (labels[i] != labels[i-1]) {
            n = labels[i-1];
        // top neighbor 
        } else if (labels[i] != labels[i-width]) {
            n = labels[i-width];
        // right neighbor 
        } else if (labels[i] != labels[i+1]) {
            n = labels[i+1];
        // bottom neighbor 
        } else if (labels[i] != labels[i+width]) {
            n = labels[i+width];
        } 
        return n;
    }
    
    
    
    public TMAspot getTMAspot() {
        return ts;
    }

    /**
     * Checks if the pixel i is a superpixel's border point. This is the case if it is
     * either on the top/left/bottom/right image border, or if a neigbour pixel has a
     * different label. 
     * @param i The pixel index under consideration.
     * @param labels The label image, calculated by SLIC algorithm.
     * @param width The width of the full TMA image.
     * @param height The height of the full TMA image.
     * @param alldirections If true, the 4 top/left/bottom/right neighbours 
     * are checked (results in the full border around the superpixel). Otherwise,
     * only the 3 top/topleft/left neighbours are checked (results in top and left
     * side of the border, which leads to a thinner line in the visualization).
     * @return True, if i is a borderline pixel of a superpixel. False, otherwise.
     */
    private static boolean isBorderPoint(int i, int[] labels, int width, int height, boolean alldirections) {
        return (// i is on image border
                   i<=width                            // i is on top image border
                || i>=(height-1)*width                 // i is on bottom image border
                || Math.IEEEremainder(i+1, width)==0   // i is on left image border
                || Math.IEEEremainder(i, width)==0     // i is on rigth image border
        
                // or i has a neighbor with different label
                || labels[i] != labels[i-1]                           // left neighbor
                || labels[i] != labels[i-width]                       // top neighbor
                || !alldirections && labels[i] != labels[i-width-1]   // top left neighbor
        
                // or i has a neighbor with different label in leftover directions.
                || alldirections && labels[i] != labels[i+1]          // right neighbor
                || alldirections && labels[i] != labels[i+width]      // bottom neighbor
        );
    }
    
    
    /**
     * Get the calculated WEKA instances of this SLICsuperpixels. This is the collection of 
     * the featurevectors of all superpixels.
     * @return The feature vectors with label as WEKA Instances.
     */
    public Instances getInstances() {
        return instances;
    }
    
    public void setInstances(Instances instances) {
        this.instances=instances;
    }

    public static Classifier trainClassifier(Instances data, String classifier, String[] options, boolean filterUseless) {
        // FOR DEBUGGING
        if (tmarker.DEBUG>4) {
            try {
                // write arff before filtering
                ArffSaver saver = new ArffSaver();
                saver.setInstances(data);
                File file = new File("superpixels1.arff");
                file.deleteOnExit();
                saver.setFile(file);
                saver.writeBatch();
                // write arff after filtering
                RemoveUseless filter = new RemoveUseless();
                filter.setInputFormat(data);
                Instances data2 = Filter.useFilter(data, filter);
                saver = new ArffSaver();
                saver.setInstances(data2);
                file = new File("superpixels2.arff");
                file.deleteOnExit();
                saver.setFile(file);
                saver.writeBatch();
            } catch (Exception ex) {
                Logger.getLogger(SLICsuperpixels.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        
        Classifier cl;
        //Instances data2 = data;
            
        // Filter data
        try {
            if (data.attribute("ID") != null) {
                Remove filter = new Remove();
                filter.setOptions(new String[]{"-R", Integer.toString(data.attribute("ID").index()+1)});
                filter.setInputFormat(data);
                data = Filter.useFilter(data, filter);
            }
            if (filterUseless) {
                RemoveUseless filter = new RemoveUseless();
                filter.setInputFormat(data);
                data = Filter.useFilter(data, filter);
            }
        } catch (Exception ex) {
            Logger.getLogger(SLICsuperpixels.class.getName()).log(Level.SEVERE, null, ex);
        }
            
            /*
            List<Filter> filters = new ArrayList<>();
            if (data2.attribute("ID") != null) {
                Remove filter = new Remove();
                filter.setOptions(new String[]{"-R", Integer.toString(data2.attribute("ID").index()+1)});
                filter.setInputFormat(data2);
                filters.add(filter);
            }
            if (filterUseless) {
                RemoveUseless filter = new RemoveUseless();
                filter.setInputFormat(data2);
                filters.add(filter);
            }
            if (filters.size()>0) {
                MultiFilter mf = new MultiFilter();
                mf.setInputFormat(data2);
                mf.setFilters(filters.toArray(new Filter[filters.size()]));
                cl.setFilter(mf);
            } else {
                cl.setFilter(new AllFilter());
            }*/
            
            if (classifier.equalsIgnoreCase("rf")) {
                // RANDOM FOREST CLASSIFIER
                //cl.setClassifier(new RandomForest());
                cl = new RandomForest();
                //options  = new String[] {
                //            "-I", "50",      // num trees
                //}; 
            } else if (classifier.equalsIgnoreCase("cf")) {
                // RANDOM COFOREST CLASSIFIER (SEMI-SUPERVISED)
                //cl.setClassifier(new CoForest());
                cl = new CoForest();
                //options  = new String[] {
                //            "-I", "50",      // num trees
                //};  
            } else if (classifier.equalsIgnoreCase("libsvm")) {
                // LIB SVM CLASSIFIER // former libsvm: WLSVM
                //cl.setClassifier(new LibSVM());
                cl = new LibSVM(); // original LIBSVM
                /*options  = new String[] {
                      //"-S", "0",      // 0 = C-SVC; 1 = nu-SVC; 2 = one-class SVM; 3 = epsilon-SVR; 4 = nu-SVR
                        "-K", "2",      // 0 = linear: u'*v; 1 = polynomial: (gamma*u'*v + coef0)^degree; 2 = radial basis function: exp(-gamma*|u-v|^2); 3 = sigmoid: tanh(gamma*u'*v + coef0)
                        "-G", "1",      // gamma (default 1/k)
                        "-D", "3",      // degree (default 3)
                        "-R", "0",      // coef0 (default 0)
                        "-C", "1",      // C for C-SVC, epsilon SVR and nu-SVR
                        "-N", "0.5",    // nu-SVM, one Class and nuSVR (default 0.5)
                        "-Z",           // turns on normalization of input data (default off)
                        "-J",           // Turn off nominal to binary conversion. WARNING: use only if your data is all numeric!
                        "-V",           // Turn off missing value replacement. WARNING: use only if your data has no missing values.
                        "-P", "0.1",    // Set the epsilon in loss function of epsilon-SVR (default: 0.1)</pre>
                        "-M", "40",     // Set cache memory size in MB (default: 40)</pre>
                        "-E", "0.001",  // Set tolerance of termination criterion (default: 0.001)</pre>
                        "-H"            // Turns the shrinking heuristics off (default: on)
                        "-W", "1 1",    // Set the parameters C of class i to weight[i]*C, for C-SVC. E.g., for a 3-class problem, you could use "1 1 1" for equally weighted classes. (default: 1 for all classes)
                        "-B",           // Generate probability estimates for classification
                        "-seed", "1",      // Random seed (default = 1)

                        };*/
            } else if (classifier.equalsIgnoreCase("linreg")) {
                // LINEAR REGRESSION
                //cl.setClassifier(new LinearRegression());
                cl = new LinearRegression();

            } else if (classifier.equalsIgnoreCase("logreg")) {
                // LOGISTIC REGRESSION
                //cl.setClassifier(new Logistic());
                cl = new Logistic();
            } else {
                // BAYES NET CLASSIFIER
                //cl.setClassifier(new BayesNet());
                cl =  new BayesNet();
                /*options  = new String[] {
                      //"-D",      // Do not use ADTree data structure
                        "-B", "<BIF file>", // BIF file to compare with
                        "-Q", "weka.classifiers.bayes.net.search.SearchAlgorithm",      // Search algorithm
                        "-E", "weka.classifiers.bayes.net.estimate.SimpleEstimator",      // Estimator algorithm
                        };*/
            }
        
        try {
            if (options.length>0) {
                if (tmarker.DEBUG>4) {
                    System.out.println("Classifier should have options");
                    for (String o:options) {System.out.print(o + " ");}
                    System.out.println();
                }
                //cl.getClassifier().setOptions(options);
                cl.setOptions(options);
            } else {
                if (tmarker.DEBUG>4) {
                    System.out.println("No options explicitly set.");
                }
            }
            if (tmarker.DEBUG>4) {
                System.out.println("Classifier has options");
                for (String o:cl.getOptions()) {System.out.print(o + " ");}
                System.out.println();
            }
            cl.buildClassifier(data);
        } catch (Exception ex) {
            Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
            cl = null;
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error Creating Classifier", JOptionPane.ERROR_MESSAGE);
        }
        return cl;
    }
    
    
    
   
    
    
    public double getMeanEntropy() {
        double H_mean = 0;
        for (Superpixel sp: getSuperpixels()) {
            H_mean += sp.getEntropy();
        }
        return H_mean/getSuperpixels().size();
    }

    public void setTMASpot(TMAspot ts) {
        this.ts = ts;
    }
    
    public static void drawSuperpixelsOnImage(List<Superpixel> superpixels, BufferedImage bi) {
        SLIC.DrawContoursAroundSegments(bi.getGraphics(), bi, superpixels, false, Color.BLACK, true, Color.BLACK, true, Color.BLACK, true, Color.BLACK, 1, new Rectangle(0, 0, bi.getWidth(), bi.getHeight()));
    }

}

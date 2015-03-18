package cancernucleusclassification;

/**
 * Description: CoForest is a semi-supervised algorithm, which exploits the power of ensemble learning and available
 *              large amount of unlabeled data to produce hypothesis with better performance.
 *
 * Reference:   M. Li, Z.-H. Zhou. Improve computer-aided diagnosis with machine learning techniques using undiagnosed
 *              samples. IEEE Transactions on Systems, Man and Cybernetics - Part A: Systems and Humans, 2007, 37(6).
 *
 * ATTN:        This package is free for academic usage. You can run it at your own risk.
 *	     	For other purposes, please contact Prof. Zhi-Hua Zhou (zhouzh@nju.edu.cn).
 *
 * Requirement: To use this package, the whole WEKA environment (ver 3.4) must be available.
 *	        refer: I.H. Witten and E. Frank. Data Mining: Practical Machine Learning
 *		Tools and Techniques with Java Implementations. Morgan Kaufmann,
 *		San Francisco, CA, 2000.
 *
 * Data format: Both the input and output formats are the same as those used by WEKA.
 *
 * ATTN2:       This package was developed by Mr. Ming Li (lim@lamda.nju.edu.cn). There
 *		is a ReadMe file provided for roughly explaining the codes. But for any
 *		problem concerning the code, please feel free to contact with Mr. Li.
 *
 */


import java.io.*;
import java.util.*;

import weka.core.*;
import weka.classifiers.*;
import weka.classifiers.trees.*;

public class CoForest extends Classifier
{
  /** Random Forest */
  protected Classifier[] m_classifiers = null;

  /** The number component */
  protected int m_numClassifiers = 10;

  /** The random seed */
  protected int m_seed = 1;

  /** Number of features to consider in random feature selection.
      If less than 1 will use int(logM+1) ) */
  protected int m_numFeatures = 0;

  /** Final number of features that were considered in last build. */
  protected int m_KValue = 0;

  /** confidence threshold */
  protected double m_threshold = 0.75;

  private int m_numOriginalLabeledInsts = 0;



  /**
   * The constructor
   */
  public CoForest()
  {
  }


  /**
   * Set the seed for initiating the random object used inside this class
   *
   * @param s int -- The seed
   */
  public void setSeed(int s)
  {
    m_seed = s;
  }
  
  /**
   * Get the seed for initiating the random object used inside this class
   *
   * @return int -- The seed
   */
  public int getSeed()
  {
    return m_seed;
  }

  /**
   * Set the number of trees used in Random Forest.
   *
   * @param n int -- Value to assign to numTrees.
   */
  public void setNumClassifiers(int n)
  {
    m_numClassifiers = n;
  }

  /**
   * Get the number of trees used in Random Forest
   *
   * @return int -- The number of trees.
   */
  public int getNumClassifiers()
  {
    return m_numClassifiers;
  }

  /**
   * Set the number of features to use in random selection.
   *
   * @param n int -- Value to assign to m_numFeatures.
   */
  public void setNumFeatures(int n)
  {
    m_numFeatures = n;
  }

  /**
   * Get the number of featrues to use in random selection.
   *
   * @return int -- The number of features
   */
  public int getNumFeatures()
  {
    return m_numFeatures;
  }

  /**
   * Resample instances w.r.t the weight
   *
   * @param data Instances -- the original data set
   * @param random Random -- the random object
   * @param sampled boolean[] -- the output parameter, indicating whether the instance is sampled
   * @return Instances
   */
  public final Instances resampleWithWeights(Instances data,
                                             Random random,
                                             boolean[] sampled)
  {


    double[] weights = new double[data.numInstances()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = data.instance(i).weight();
    }
    Instances newData = new Instances(data, data.numInstances());
    if (data.numInstances() == 0) {
      return newData;
    }
    double[] probabilities = new double[data.numInstances()];
    double sumProbs = 0, sumOfWeights = Utils.sum(weights);
    for (int i = 0; i < data.numInstances(); i++) {
      sumProbs += random.nextDouble();
      probabilities[i] = sumProbs;
    }
    Utils.normalize(probabilities, sumProbs / sumOfWeights);

    // Make sure that rounding errors don't mess things up
    probabilities[data.numInstances() - 1] = sumOfWeights;
    int k = 0; int l = 0;
    sumProbs = 0;
    while ((k < data.numInstances() && (l < data.numInstances()))) {
      if (weights[l] < 0) {
        throw new IllegalArgumentException("Weights have to be positive.");
      }
      sumProbs += weights[l];
      while ((k < data.numInstances()) &&
             (probabilities[k] <= sumProbs)) {
        newData.add(data.instance(l));
        sampled[l] = true;
        newData.instance(k).setWeight(1);
        k++;
      }
      l++;
    }
    return newData;
  }

  /**
   * Returns the probability label of a given instance
   *
   * @param inst Instance -- The instance
   * @return double[] -- The probability label
   * @throws Exception -- Some exception
   */
  public double[] distributionForInstance(Instance inst) throws Exception
  {
    double[] res = new double[inst.numClasses()];
    for(int i = 0; i < m_classifiers.length; i++)
    {
      double[] distr = m_classifiers[i].distributionForInstance(inst);
      for(int j = 0; j < res.length; j++)
        res[j] += distr[j];
    }
    Utils.normalize(res);
    return res;
  }

  /**
   * Classifies a given instance
   *
   * @param inst Instance -- The instance
   * @return double -- The class value
   * @throws Exception -- Some Exception
   */
  public double classifyInstance(Instance inst) throws Exception
  {
    double[] distr = distributionForInstance(inst);
    return Utils.maxIndex(distr);
  }

  /**
   * Build the classifiers using Co-Forest algorithm
   *
   * @param labeled Instances -- Labeled training set
   * @param unlabeled Instances -- unlabeled training set
   * @throws Exception -- certain exception
   */
  public void buildClassifier(Instances labeled, Instances unlabeled) throws Exception
  {
    double[] err = new double[m_numClassifiers];
    double[] err_prime = new double[m_numClassifiers];
    double[] s_prime = new double[m_numClassifiers];

    boolean[][] inbags = new boolean[m_numClassifiers][];

    Random rand = new Random(m_seed);
    m_numOriginalLabeledInsts = labeled.numInstances();

    RandomTree rTree = new RandomTree();

    // set up the random tree options
    m_KValue = m_numFeatures;
    if (m_KValue < 1) m_KValue = (int) Utils.log2(labeled.numAttributes())+1;
    rTree.setKValue(m_KValue);

    m_classifiers = Classifier.makeCopies(rTree, m_numClassifiers);
    Instances[] labeleds = new Instances[m_numClassifiers];
    int[] randSeeds = new int[m_numClassifiers];

    for(int i = 0; i < m_numClassifiers; i++)
    {
      randSeeds[i] = rand.nextInt();
      ((RandomTree)m_classifiers[i]).setSeed(randSeeds[i]);
      inbags[i] = new boolean[labeled.numInstances()];
      labeleds[i] = resampleWithWeights(labeled, rand, inbags[i]);
      m_classifiers[i].buildClassifier(labeleds[i]);
      err_prime[i] = 0.5;
      s_prime[i] = 0;
    }

    boolean bChanged = true;
    while(bChanged)
    {
      bChanged = false;
      boolean[] bUpdate = new boolean[m_classifiers.length];
      Instances[] Li = new Instances[m_numClassifiers];

      for(int i = 0; i < m_numClassifiers; i++)
      {
        err[i] = measureError(labeled, inbags, i);
        Li[i] = new Instances(labeled, 0);

        /** if (e_i < e'_i) */
        if(err[i] < err_prime[i])
        {
          if(s_prime[i] == 0)
            s_prime[i] = Math.min(unlabeled.sumOfWeights() / 10, 100);

          /** Subsample U for each hi */
          double weight = 0;
          unlabeled.randomize(rand);
          int numWeightsAfterSubsample = (int) Math.ceil(err_prime[i] * s_prime[i] / err[i] - 1);
          for(int k = 0; k < unlabeled.numInstances(); k++)
          {
            weight += unlabeled.instance(k).weight();
            if (weight > numWeightsAfterSubsample)
             break;
           Li[i].add((Instance)unlabeled.instance(k).copy());
          }

          /** for every x in U' do */
          for(int j = Li[i].numInstances() - 1; j > 0; j--)
          {
            Instance curInst = Li[i].instance(j);
            if(!isHighConfidence(curInst, i))       //in which the label is assigned
              Li[i].delete(j);
          }//end of j

          if(s_prime[i] < Li[i].numInstances())
          {
            if(err[i] * Li[i].sumOfWeights() < err_prime[i] * s_prime[i])
              bUpdate[i] = true;
          }
        }
      }//end of for i

      //update
      Classifier[] newClassifier = Classifier.makeCopies(rTree, m_numClassifiers);
      for(int i = 0; i < m_numClassifiers; i++)
      {
        if(bUpdate[i])
        {
          double size = Li[i].sumOfWeights();

          bChanged = true;
          m_classifiers[i] = newClassifier[i];
          ((RandomTree)m_classifiers[i]).setSeed(randSeeds[i]);
          m_classifiers[i].buildClassifier(combine(labeled, Li[i]));
          err_prime[i] = err[i];
          s_prime[i] = size;
        }
      }
    }//end of while
  }


  /**
   * To judege whether the confidence for a given instance of H* is high enough,
   * which is affected by the onfidence threshold. Meanwhile, if the example is
   * the confident one, assign label to it and weigh the example with the confidence
   *
   * @param inst Instance -- The instance
   * @param idExcluded int -- the index of the individual should be excluded from H*
   * @return boolean -- true for high
   * @throws Exception - some exception
   */
  protected boolean isHighConfidence(Instance inst, int idExcluded) throws Exception
  {
    double[] distr = distributionForInstanceExcluded(inst, idExcluded);
    double confidence = getConfidence(distr);
    if(confidence > m_threshold)
    {
      double classval = Utils.maxIndex(distr);
      inst.setClassValue(classval);    //assign label
      inst.setWeight(confidence);      //set instance weight
      return true;
    }
    else return false;
  }

  
  private Instances combine(Instances L, Instances Li)
  {
    for(int i = 0; i < L.numInstances(); i++)
      Li.add(L.instance(i));

    return Li;
  }

  private double measureError(Instances data, boolean[][] inbags, int id) throws Exception
   {
     double err = 0;
     double count = 0;
     for(int i = 0; i < data.numInstances() && i < m_numOriginalLabeledInsts; i++)
     {
       Instance inst = data.instance(i);
       double[] distr = outOfBagDistributionForInstanceExcluded(inst, i, inbags, id);

       if(getConfidence(distr) > m_threshold)
       {
         count += inst.weight();
         if(Utils.maxIndex(distr) != inst.classValue())
           err += inst.weight();
       }
     }
     err /= count;
     return err;
  }

  private double getConfidence(double[] p)
  {
    int maxIndex = Utils.maxIndex(p);
    return p[maxIndex];
  }

  private double[] distributionForInstanceExcluded(Instance inst, int idExcluded) throws Exception
  {
    double[] distr = new double[inst.numClasses()];
    for(int i = 0; i < m_numClassifiers; i++)
    {
      if(i == idExcluded)
        continue;

      double[] d = m_classifiers[i].distributionForInstance(inst);
      for(int iClass = 0; iClass < inst.numClasses(); iClass++)
        distr[iClass] += d[iClass];
    }
    Utils.normalize(distr);
    return distr;
  }

  private double[] outOfBagDistributionForInstanceExcluded(Instance inst, int idxInst, boolean[][] inbags, int idExcluded) throws Exception
  {
    double[] distr = new double[inst.numClasses()];
    for(int i = 0; i < m_numClassifiers; i++)
    {
      if(inbags[i][idxInst] == true || i == idExcluded)
        continue;

      double[] d = m_classifiers[i].distributionForInstance(inst);
      for(int iClass = 0; iClass < inst.numClasses(); iClass++)
        distr[iClass] += d[iClass];
    }
    if(Utils.sum(distr) != 0)
      Utils.normalize(distr);
    return distr;
  }




  /**
   * The main method only for demonstrating the simple use of this package
   *
   * @param args String[]
   */
  public static void main(String[] args)
  {
    try
    {
     int seed = 0;
     int numFeatures = 0;
     Random rand = new Random(seed);
     final int NUM_CLASSIFIERS = 6;

     BufferedReader r = new BufferedReader(new FileReader("labeled.arff"));
     Instances labeled = new Instances(r);
     labeled.setClassIndex(labeled.numAttributes()-1);
     r.close();

     r = new BufferedReader(new FileReader("unlabeled.arff"));
     Instances unlabeled = new Instances(r);
     unlabeled.setClassIndex(labeled.numAttributes()-1);
     r.close();

     r = new BufferedReader(new FileReader("test.arff"));
     Instances test = new Instances(r);
     test.setClassIndex(labeled.numAttributes()-1);
     r.close();

     CoForest forest = new CoForest();
     forest.setNumClassifiers(NUM_CLASSIFIERS);
     forest.setNumFeatures(numFeatures);
     forest.setSeed(rand.nextInt());
     forest.buildClassifier(labeled, unlabeled);

     double err = 0;
     for(int i = 0; i < test.numInstances(); i++)
     {
       if(forest.classifyInstance(test.instance(i)) != test.instance(i).classValue())
         err++;
     }

     java.util.logging.Logger.getLogger(CoForest.class.getName()).log(java.util.logging.Level.INFO, "Error Rate = " + (err/test.numInstances()));

   }
   catch(Exception e)
   {
     java.util.logging.Logger.getLogger(CoForest.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
   }
 }

 /**
  * Method required for the class to extend the WEKA Classifier interface. Input
  * Instances data with and without missing value. Data are split into labeled and 
  * unlabeled data (according to missing value) and then passed to
  * buildClassifier(Instances labeled, Instances unlabeled).
  * Author: Peter J. Schüffler.
  * @param data Labeled and unlabeled data. All instances with missing class value
  * are considered as unlabeled.
  * @throws Exception 
  * author Peter J. Schüffler
  */
    @Override
    public void buildClassifier(Instances data) throws Exception {
    Instances labeled = new Instances(data);
    labeled.deleteWithMissingClass();
    
    Instances unlabeled = new Instances(data);
    for (int i=unlabeled.numInstances()-1; i>=0; i--) {
      if (!unlabeled.instance(i).classIsMissing()) {
        unlabeled.delete(i);
      }
    }
    buildClassifier(labeled, unlabeled);
    }
    
    /**
   * Gets the current settings of the forest.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {
    Vector result;
    String[] options;
    int i;

    result = new Vector();

    result.add("-I");
    result.add("" + getNumClassifiers());

    result.add("-K");
    result.add("" + getNumFeatures());

    result.add("-S");
    result.add("" + getSeed());

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start --> 
   * Valid options are:
   * <p/>
   * 
   * <pre>
   * -I &lt;number of trees&gt;
   *  Number of trees to build.
   * </pre>
   * 
   * <pre>
   * -K &lt;number of features&gt;
   *  Number of features to consider (&lt;1=int(logM+1)).
   * </pre>
   * 
   * <pre>
   * -S
   *  Seed for random number generator.
   *  (default 1)
   * </pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('I', options);
    if (tmpStr.length() != 0) {
      m_numClassifiers = Integer.parseInt(tmpStr);
    } else {
      m_numClassifiers = 10;
    }

    tmpStr = Utils.getOption('K', options);
    if (tmpStr.length() != 0) {
      m_numFeatures = Integer.parseInt(tmpStr);
    } else {
      m_numFeatures = 0;
    }

    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0) {
      setSeed(Integer.parseInt(tmpStr));
    } else {
      setSeed(1);
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }


}


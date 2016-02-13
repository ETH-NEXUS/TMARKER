/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cytoplasmstaining;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.ImageIcon;
import tmarker.tmarker;

/**
 * This is the class that classifies all samples hierarchically.
 * @author Peter Schueffler
 */
public class HierarchicalClassifier {

    /** Hierarchical clustering with single linkage */
    final public static int SINGLE_LINKAGE = 0;
    
    /** Hierarchical clustering with average linkage */
    final public static int AVERAGE_LINKAGE = 1; 
    
    /** Hierarchical clustering with complete linkage */
    final public static int COMPLETE_LINKAGE = 2;
    
    private double[][] d; // distance matrix
    private List<ClusterTree> clusterTrees = new LinkedList<>();
    private List<Sample> samples;
    
    /**
     * Create a new hierarchical classifier.
     */ 
    public HierarchicalClassifier(List<Sample> samples) {
        this.samples = samples;
    }
    
    /**
     * Clusters the samples loaded in MethMarker with hierarchical 
     * clustering. If done the first time, it also opens a window
     * with the displayed cluster tree, which is a ClassifierResultFrame.
     * @param mode One of AVERAGE_LINKAGE, SINGLE_LINKAGE or COMPLETE_LINKAGE.
     * @param withResultDialog If True, a new ResultDialog is created.
     */
    public void doClustering(int mode, boolean withResultDialog) {
        // initialisiere single clusters
        initializeClusters();
        
        // initialisiere distance matrix
        initializeDistanceMatrix(mode);
        
        // while there are more clusters than one...
        while (clusterTrees.size()>1) {
            // ... find the clusterpair with the smallest distance
            int i;
            int j;
            int index_i = 0;
            int index_j = 1;
            for (i=0; i<clusterTrees.size(); i++) {
                for (j=i+1; j<clusterTrees.size(); j++) {
                    if (d[i][j] < d[index_i][index_j]) {
                        index_i = i;
                        index_j = j;
                    }
                }
            }
            
            // ... remove merge the two clusters
            ClusterTree ct1 = clusterTrees.remove(Math.max(index_i, index_j));
            ClusterTree ct2 = clusterTrees.remove(Math.min(index_i, index_j));
            List<ClusterTree> newClusterTree = new LinkedList<>();
            newClusterTree.add(ct1);
            newClusterTree.add(ct2);
            clusterTrees.add(new ClusterTree(newClusterTree));
            
            // ... recalculate distance matrix
            initializeDistanceMatrix(mode);
        }
        
        // cluster the samples according the tree
        if (!clusterTrees.isEmpty()) { 
            List<Sample> positives = new ArrayList<>();
            List<Sample> negatives = new ArrayList<>();
            
            List<ClusterTree> children = clusterTrees.get(0).getChildren();
            
            if (children.size()>2) {
                System.err.println("HierarchicalClassifier: More than two children for the first dividing discovered. Please review the clustering!");
            }
            
            if (children.size()>1) {
                positives = children.get(0).getSamplesOfSubtree();
                negatives = children.get(1).getSamplesOfSubtree();
            }
            else if (children.size()>0) {
                negatives = children.get(1).getSamplesOfSubtree();
            }
            
            /*for (int i=0, n=negatives.size(); i<n; i++) {
                    negatives.get(i).setClassC(AbstractSample.NEGATIVE);
            }
            for (int i=0, n=positives.size(); i<n; i++) {
                    positives.get(i).setClassC(AbstractSample.POSITIVE);
            }*/
        }
        if (withResultDialog) {
            ClassifierResultDialog crd = new ClassifierResultDialog(this, null, true);
        }
    }
    
    /**
     * Returns the Image of the created cluster tree. If no clustering is
     * performed, null is returned.
     * @param scale The scale of the image size (default 1).
     * @param mode One of AVERAGE_LINKAGE, SINGLE_LINKAGE or COMPLETE_LINKAGE.
     * Used for title in the picture (not for clustering).
     * @param colored If true, samples are colored according to their class.
     * @return The clustering tree as image icon.
     */
    public ImageIcon getClassifierImage(double scale, int mode, boolean colored) {
        ImageIcon ii = null;
        if (!clusterTrees.isEmpty()) { 
            int rowHeight = (int) (scale * 20);
            int lineWidth = (int) (scale * 12);
            int maxDepth = clusterTrees.get(0).getDepth();
            int imageWidth = maxDepth*(lineWidth+5) + 500;
            int imageHeight = clusterTrees.get(0).getNumberLeaves() * rowHeight + 50 + 25;
            if (tmarker.DEBUG>0) System.out.println("Number leaves: " + clusterTrees.get(0).getNumberLeaves());
            
            BufferedImage bi = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            ii = new ImageIcon(bi);
            Graphics2D gr = (Graphics2D) ii.getImage().getGraphics();
            gr.setColor(Color.BLACK);
            RenderingHints rh = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
            gr.setRenderingHints(rh);

            gr.setPaint(Color.WHITE);
            gr.fillRect(0, 0, imageWidth, imageHeight);
            gr.setPaint(Color.BLACK);
            
            // draw headline and legend
            gr.setFont(new Font("Arial", Font.PLAIN, 10));
            int y = (int) (0.3 * rowHeight) + 4;
            String title = "Dendrogram for hierarchical clustering"; 
            switch (mode) {
                case HierarchicalClassifier.AVERAGE_LINKAGE : title += " - average linkage"; break;
                case HierarchicalClassifier.COMPLETE_LINKAGE : title += " - complete linkage"; break;
                case HierarchicalClassifier.SINGLE_LINKAGE : title += " - single linkage"; break;
                default : ;
            }
            gr.drawString(title, 5, y);
            gr.drawString(" Image                             Sample", maxDepth * (lineWidth+1), y+((int)(scale*20)));
            
            // draw tree
            gr.setFont(new Font("Arial", Font.PLAIN, 12));
            gr.setStroke(new BasicStroke((float) scale));
            ((Graphics2D)gr).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            ((Graphics2D)gr).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            clusterTrees.get(0).drawTree(gr, 0, maxDepth, (imageHeight+50)/2, lineWidth, rowHeight, colored);
        }
        return ii;
    }
    
    /**
     * Initializes each sample loaded in the Center as a simple cluster. Stores
     * these clusters in clusterTrees.
     */
    private void initializeClusters() {
        clusterTrees.clear();
        for (int i=0, n=samples.size(); i<n; i++) {
            List<Sample> sample = new LinkedList<>();
            sample.add(samples.get(i));
            clusterTrees.add(new ClusterTree(samples.get(i)));
        }
    }
    
    private void initializeDistanceMatrix(int mode) {
        d = new double[clusterTrees.size()][clusterTrees.size()];
        for (int i=0, n=clusterTrees.size(); i<n; i++) {
            for (int j=i, m=clusterTrees.size(); j<m; j++) {
                d[i][j] = calculateDistance(clusterTrees.get(i).getSamplesOfSubtree(), clusterTrees.get(j).getSamplesOfSubtree(), mode);
            }
        }
    }
    
    /**
     * Calculates the distance of two sample lists. The methylation grade is used for
     * distance calculation.
     * @param sl1 Sample list 1.
     * @param sl2 Sample list 2.
     * @param mode One of AVERAGE_LINKAGE, SINGLE_LINKAGE or COMPLETE_LINKAGE.
     * Distance is calculated according to this in hierarchical clustering.
     * @return The distance measure of the two sample lists.
     */
    public static double calculateDistance(List<Sample> sl1, List<Sample> sl2, int mode) {
        if (sl1 == sl2) return 0;
        
        if (mode == SINGLE_LINKAGE) return calculateDistance_Single(sl1, sl2);
        if (mode == COMPLETE_LINKAGE) return calculateDistance_Complete(sl1, sl2);
        return calculateDistance_Average(sl1, sl2);
    }
    
    /**
     * The single linkage distance between two sample lists is the minimum distance
     * between two arbitrary samples of the two sets. Distance means methylation grade
     * difference.
     * @param sl1 The sample set 1.
     * @param sl2 The sample set 2.
     * @return The distance of the two sample sets. Double.MAX_VALUE, if one set is empty.
     */
    private static double calculateDistance_Single(List<Sample> sl1, List<Sample> sl2) {
        double distance = Double.MAX_VALUE;
        for (int i=0, n=sl1.size(); i<n; i++) {
            for (int j=0, m=sl2.size(); j<m; j++) {
                distance = Math.min(distance, euclideanDist(sl1.get(i).feature_vector, sl2.get(j).feature_vector));
            }
        }
        return distance;
    }
    
    /**
     * The complete linkage distance between two sample lists is the maximum distance
     * between two arbitrary samples of the two sets. Distance means methylation grade
     * difference.
     * @param sl1 The sample set 1.
     * @param sl2 The sample set 2.
     * @return The distance of the two sample sets. Double.MIN_VALUE, if one set is empty.
     */
    private static double calculateDistance_Complete(List<Sample> sl1, List<Sample> sl2) {
        double distance = Double.MIN_VALUE;
        for (int i=0, n=sl1.size(); i<n; i++) {
            for (int j=0, m=sl2.size(); j<m; j++) {
                distance = Math.max(distance, euclideanDist(sl1.get(i).feature_vector, sl2.get(j).feature_vector));
            }
        }
        return distance;
    }
    
    /**
     * The average linkage distance between two sample lists is the average distance
     * between every two arbitrary samples of the two sets. Distance means methylation grade
     * difference.
     * @param sl1 The sample set 1.
     * @param sl2 The sample set 2.
     * @return The distance of the two sample sets.
     */
    private static double calculateDistance_Average(List<Sample> sl1, List<Sample> sl2) {
        double distance = 0;
        for (int i=0, n=sl1.size(); i<n; i++) {
            for (int j=0, m=sl2.size(); j<m; j++) {
                distance += euclideanDist(sl1.get(i).feature_vector, sl2.get(j).feature_vector);
            }
        }
        return distance / (sl1.size()*sl2.size());
    }
    
    /**
     * Returns the Euclidean distance of two vectors.
     * @param a Vector a, has to be same size as b.
     * @param b Vector b, has to be same size as a.
     * @return Euclidean dist of a and b.
     */
    private static double euclideanDist(double[] a, double[] b) {
        double s = 0;
        for (int i=0; i<a.length; i++) {
            s += Math.pow(a[i]-b[i], 2);
        }
        return Math.sqrt(s);
    }
}

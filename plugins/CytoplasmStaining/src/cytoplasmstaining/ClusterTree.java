/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cytoplasmstaining;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * This ClusterTree is needed for generating the cluster tree for hierarchical clustering. Since this tree is binary, the ClusterTree
 * consists of a clusterLevel and either a value (leaf, sample) or two other ClusterTree (children). Nevertheless, a ClusterTree is
 * implemented as it could have arbitrary number of children (not necessary binary).
 * @author Peter Schueffler
 */
public class ClusterTree {
    
    private Sample sample = null;
    private List<ClusterTree> children = null;
    
    /**
     * Generates a new ClusterTree as Leave (no children).
     * @param sample The leave.
     */
    public ClusterTree(Sample sample) {
        this.sample = sample;
    }
    
    /**
     * Generates a new ClusterTree as node (not a leave).
     * @param children The children of this node.
     */
    public ClusterTree(List<ClusterTree> children) {
        this.children = children;
    }
    
    /**
     * Returns the children of the node.
     * @return The children of the node. Null, if this is a leave.
     */
    public List<ClusterTree> getChildren() {
        return children;
    }
    
    /**
     * Return the sample of this node.
     * @return The sample of this node. Null, if this is not a leave.
     */
    public Sample getSample() {
        return sample;
    }
    
    /**
     * Returns whether or not this is a leave.
     * @return True, if this is a leave, otherwise false.
     */
    public boolean isLeaf() {
        return sample != null;
    }
    
    /**
     * Returns a list with all samples that can be reached by this node.
     * @return A list with all samples that can be reached by this node. Empty if there
     * are no leaves.
     */
    public List<Sample> getSamplesOfSubtree() {
        List<Sample> samples = new ArrayList<>();
        if (isLeaf()) {
            samples.add(getSample());
        } 
        else {
            for (int i=0, n=getChildren().size(); i<n; i++) {
                samples.addAll(getChildren().get(i).getSamplesOfSubtree());
            }
        }
        return samples;
    }
    
    /**
     * Returns the number of leaves that can be reached by this node.
     * @return The number of leaves that can be reached by this node.
     */
    public int getNumberLeaves() {
        return getSamplesOfSubtree().size();
    }
    
    /**
     * Draws the tree with this node as root on a graphics. Some parameters are needed
     * to determine coordinated where lines and labels should be drawn.
     * @param gr The graphics to be drawn on.
     * @param level The recent depth of this node. Should be initialized with 0.
     * @param maxDepth The maximum Depth of the overall tree. Can be determined with getDepth().
     * @param y The y-coordinate to draw lines and labels. Starts with the y-coordinate 
     * of the middle of the tree.
     * @param lineLength The length of the horizontal lines in the tree.
     * @param rowHeight The length of the vertical lines of the tree.
     * @param colored If true, labels are colored according to their class.
     * @return The y-coordinate of the horizontal line of the tree root.
     * This is only important for the recursive calling of this function and is not needed
     * as result.
     */
    public int drawTree(Graphics2D gr, int level, int maxDepth, int y, int lineLength, int rowHeight, boolean colored) {
        if (isLeaf()) {
            int offset = 0;
            if (((y-50-rowHeight/2)/rowHeight)%2!=0) {
                offset = 5*lineLength;
            }
            gr.drawLine(lineLength*level, y, lineLength*(maxDepth), y);
            /*String methGrade = Double.toString(Math.round(getSample().getIntensity()*100));
            methGrade = methGrade.substring(0, methGrade.length()-2);
            if (methGrade.length() == 1) methGrade = "    " + methGrade;
            if (methGrade.length() == 2) methGrade =  "  " + methGrade;
            gr.drawString(methGrade +" %     "+ getSample().ts.getName(), lineLength*(maxDepth) + lineLength, y+(lineLength/2));*/
            gr.drawString(getSample().ts.getName(), lineLength*(maxDepth) + 11*lineLength, y+(lineLength/2));
            gr.drawImage(getSample().ts.getThumbnailImage(4*lineLength, 3*lineLength, null), lineLength*(1+maxDepth) + offset, (int)(y-1.5*lineLength), null);
            gr.setColor(Color.BLACK);
            return y;
        }
        else {
            int number_children = getChildren().size();
            int number_leaves = getNumberLeaves();
            int upperst_y = y - (rowHeight * number_leaves)/2;
            List<Integer> y_s = new ArrayList<>();
            int y_up = Integer.MIN_VALUE; 
            int y_low = Integer.MAX_VALUE;
            
            for (int i=0; i<number_children; i++) {
                int number_leavesOfChild = getChildren().get(i).isLeaf() ? 1 : getChildren().get(i).getNumberLeaves();
                int yForThisChild = upperst_y + (number_leavesOfChild * rowHeight)/2;
                //gr.drawLine(lineLength*(level+1), y, lineLength*(level+1), yForThisChild);
                y_s.add(getChildren().get(i).drawTree(gr, level+1, maxDepth, yForThisChild, lineLength, rowHeight, colored));
                y_up = Math.max(y_up, y_s.get(i));
                y_low = Math.min(y_low, y_s.get(i));
                upperst_y = upperst_y + (number_leavesOfChild * rowHeight);
            }
            int mid_y = y;
            if (y_s.size()>1) {
                mid_y = (y_s.get(0) + y_s.get(y_s.size()-1) ) / 2;
            }
            gr.drawLine(lineLength*(level+1), y_up, lineLength*(level+1), y_low);
            gr.drawLine(lineLength*level, mid_y, lineLength*(level+1), mid_y);
            return mid_y;
        }
    }
    
    /**
     * Returns the maximum depth of the tree with this node as root.
     * @return The depth of the tree with this node as root. 
     */
    public int getDepth() {
        if (isLeaf()) return 1;
        else {
            int d_ = 0;
            for (int i=0, n=getChildren().size(); i<n; i++) {
                d_ = Math.max(d_, 1+getChildren().get(i).getDepth());
            }
            return d_;
        }
    }
}

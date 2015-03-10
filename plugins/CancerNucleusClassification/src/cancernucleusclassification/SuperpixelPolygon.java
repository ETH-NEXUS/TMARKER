package cancernucleusclassification;
import signalprocesser.voronoi.representation.*;
import signalprocesser.voronoi.representation.triangulation.TriangulationRepresentation;
import signalprocesser.voronoi.statusstructure.VLinkedNode;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import signalprocesser.voronoi.VPoint;
import signalprocesser.voronoi.VoronoiAlgorithm;

public class SuperpixelPolygon extends Polygon {
    
    private ArrayList<VPoint> points = new ArrayList<>();
    private final TestRepresentationWrapper representationwrapper = new TestRepresentationWrapper();
    
    private AbstractRepresentation representation;
    
    private final javax.swing.JRadioButton optApplyAboveMSTAndSmallestTEdgeInProportion = new JRadioButton();
    private final javax.swing.JRadioButton optEdgeRemoval = new JRadioButton();
    private final javax.swing.JRadioButton optMaxEdgeOfMinSpanningTree = new JRadioButton();
    private final javax.swing.JRadioButton optMaxEdgeOfSmallestTEdge = new JRadioButton();
    private final javax.swing.JRadioButton optNoLengthRestriction = new JRadioButton();
    private final javax.swing.JRadioButton optNormalisedLengthRestriction = new JRadioButton();
    private final javax.swing.JRadioButton optUserLengthRestriction = new JRadioButton();
    private final double sliderApplyInProportion = 0;
    private int sliderLengthRestriction = 0;
    // this parameter influences the "convexity" of the shape. 0: non convex; 100: convex hull
    private double sliderNormalisedLengthRestriction = 0;
    
        
    /** Creates new form MainFrame */
    public SuperpixelPolygon() {
        // Sync the state with what is being visually shown
        TriangulationRepresentation.MAX_EDGES_TO_REMOVE = -1;
        
        // Set the default generation to use
        //optCountryGenerationActionPerformed(null);
        
        // Set the representation
        optEdgeRemoval.setSelected(true);
        optEdgeRemovalActionPerformed(null);
        optNormalisedLengthRestriction.setSelected(true);
    }
    
        public void addAreaPoint(int x, int y) {
            // Add to vector
            if ( representation==null ) {
                points.add( new VPoint(x, y) );
            } else {
                points.add( representation.createPoint(x, y) );
            }

            // Update controls (post repaint)
            updateControls();            
        }
        
        public void calculateBorders() {
            // Run algorithm
            // ( being very careful to catch errors and show them to the user )
            try {
                representationwrapper.innerrepresentation = representation;
                if ( points!=null ) {
                    VoronoiAlgorithm.generateVoronoi(representationwrapper, points);
                }
            } catch ( Error e ) {
                points.clear();
                throw e;
            } catch ( RuntimeException e ) {
                points.clear();
                throw e;
            }
            
            // generate polygon
            ArrayList<VPoint> vps = ((TriangulationRepresentation)representation).getPointsFormingOutterBoundary();
            if (vps!=null) {
                for (VPoint vp: vps) {
                    this.addPoint(vp.x, vp.y);
                }
            }
        }
        
        
    private void updateControls() {
        if ( optEdgeRemoval.isSelected() ) {
            TriangulationRepresentation trianglarrep = (TriangulationRepresentation) representation;
            
            // Set slider lengths
            if ( trianglarrep.getMinLength()>0 &&trianglarrep.getMaxLength()>0 ) {
                sliderLengthRestriction = Math.max(trianglarrep.getMinLength()-1, sliderLengthRestriction);
                sliderLengthRestriction = Math.min((int)(trianglarrep.getMaxLength()*1.25), sliderLengthRestriction);
            }
            
            // Enable/disable slider, select new cutoff value
            TriangulationRepresentation.CalcCutOff calccutoff;
            if ( optNoLengthRestriction.isSelected() ) {
                calccutoff = new TriangulationRepresentation.CalcCutOff() {
                    public int calculateCutOff(TriangulationRepresentation rep) {
                        int val = 0;
                        updateLengthSlider(rep, val);
                        updateNormalisedLengthSlider(rep, val);
                        return val;
                    }
                };
            } else if ( optUserLengthRestriction.isSelected() ) {
                calccutoff = new TriangulationRepresentation.CalcCutOff() {
                    public int calculateCutOff(TriangulationRepresentation rep) {
                        // Update sliders
                        if ( rep.getMinLength()>0 && rep.getMaxLength()>0 ) {
                            sliderLengthRestriction = Math.max(rep.getMinLength()-1, sliderLengthRestriction);
                            sliderLengthRestriction = Math.min((int)(rep.getMaxLength()*1.25), sliderLengthRestriction);
                        }
                        
                        // Calculate value
                        int val = sliderLengthRestriction;
                        //updateLengthSlider(rep, val);
                        updateNormalisedLengthSlider(rep, val);
                        return val;
                    }
                };
            } else if ( optNormalisedLengthRestriction.isSelected() ) {
                calccutoff = new TriangulationRepresentation.CalcCutOff() {
                    public int calculateCutOff(TriangulationRepresentation rep) {
                        // Get variables
                        double percentage = (double)sliderNormalisedLengthRestriction / 100.0;
                        double min = rep.getMinLength();
                        double max = rep.getMaxLength();
                        
                        // Calculate normalised length based off percentage
                        int val = (int)( percentage * (max-min) + min );
                        
                        // Return value
                        updateLengthSlider(rep, val);
                        //updateNormalisedLengthSlider(rep, val);
                        return val;
                    }
                };
            } else if ( optMaxEdgeOfMinSpanningTree.isSelected() ) {
                calccutoff = new TriangulationRepresentation.CalcCutOff() {
                    public int calculateCutOff(TriangulationRepresentation rep) {
                        int val = rep.getMaxLengthOfMinimumSpanningTree();
                        updateLengthSlider(rep, val);
                        updateNormalisedLengthSlider(rep, val);
                        return val;
                    }
                };
            } else if ( optMaxEdgeOfSmallestTEdge.isSelected() ) {
                calccutoff = new TriangulationRepresentation.CalcCutOff() {
                    public int calculateCutOff(TriangulationRepresentation rep) {
                        int val = rep.getMaxLengthOfSmallestTriangleEdge();
                        updateLengthSlider(rep, val);
                        updateNormalisedLengthSlider(rep, val);
                        return val;
                    }
                };
            } else if ( optApplyAboveMSTAndSmallestTEdgeInProportion.isSelected() ) {
                calccutoff = new TriangulationRepresentation.CalcCutOff() {
                    public int calculateCutOff(TriangulationRepresentation rep) {
                        double proportion = (double)sliderApplyInProportion/100.0;
                        int val = (int)(
                                (double)rep.getMaxLengthOfMinimumSpanningTree()*(1-proportion) +
                                (double)rep.getMaxLengthOfSmallestTriangleEdge()*proportion);
                        updateLengthSlider(rep, val);
                        updateNormalisedLengthSlider(rep, val);
                        return val;
                    }
                };
            } else {
                //Error
                return;
            }
            
            // Set the appropriate cutoff calculator
            trianglarrep.setCalcCutOff(calccutoff);
            
        } else {
            return;
        }
    }
    
    private void updateLengthSlider(TriangulationRepresentation rep, int cutoff) {
        // Update sliders
        int min = rep.getMinLength();
        int max = rep.getMaxLength();
        if ( min>0 && max>0 ) {
            sliderLengthRestriction = Math.max(min-1, sliderLengthRestriction);
            sliderLengthRestriction = Math.min((int)(max*1.25), sliderLengthRestriction);
        }
        
        // Set value
        sliderLengthRestriction = cutoff;
    }
    
    private void updateNormalisedLengthSlider(TriangulationRepresentation rep, int cutoff) {
        // Get variables
        int min = rep.getMinLength();
        int max = rep.getMaxLength();
        if ( min<=0 && max<=0 ) return;
        
        // Set slider position
        int percentage = (int)( (double)(cutoff - min) / (double)(max - min) * 100.0);
        sliderNormalisedLengthRestriction =  percentage ;
    }
    

    private void optEdgeRemovalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optEdgeRemovalActionPerformed
        // Set up new points/representation
        points = RepresentationFactory.convertPointsToTriangulationPoints(points);
        representation = RepresentationFactory.createTriangulationRepresentation();
        
        //optNoLengthRestriction.setSelected(true);
        //optNoLengthRestrictionActionPerformed(null);
        optApplyAboveMSTAndSmallestTEdgeInProportion.setSelected(true);
        
    }
          
    
    
    public class TestRepresentationWrapper implements RepresentationInterface {
        
        /* ***************************************************** */
        // Variables
        
        private final ArrayList<VPoint> circleevents = new ArrayList<VPoint>();
        
        private RepresentationInterface innerrepresentation = null;
        
        /* ***************************************************** */
        // Data/Representation Interface Method
        
        // Executed before the algorithm begins to process (can be used to
        //   initialise any data structures required)
        public void beginAlgorithm(Collection<VPoint> points) {
            // Reset the triangle array list
            circleevents.clear();
            
            // Call the inner representation
            if ( innerrepresentation!=null ) {
                innerrepresentation.beginAlgorithm(points);
            }
        }
        
        // Called to record that a vertex has been found
        public void siteEvent( VLinkedNode n1 , VLinkedNode n2 , VLinkedNode n3 ) {
            // Call the inner representation
            if ( innerrepresentation!=null ) {
                innerrepresentation.siteEvent(n1, n2, n3);
            }
        }
        public void circleEvent( VLinkedNode n1 , VLinkedNode n2 , VLinkedNode n3 , int circle_x , int circle_y ) {
            // Add the circle event
            circleevents.add( new VPoint(circle_x, circle_y) );
            
            // Call the inner representation
            if ( innerrepresentation!=null ) {
                innerrepresentation.circleEvent(n1, n2, n3, circle_x, circle_y);
            }
        }
        
        // Called when the algorithm has finished processing
        public void endAlgorithm(Collection<VPoint> points, int lastsweeplineposition, VLinkedNode headnode) {
            // Call the inner representation
            if ( innerrepresentation!=null ) {
                innerrepresentation.endAlgorithm(points, lastsweeplineposition, headnode);
            }
        }
        
        /* ***************************************************** */
    }
}

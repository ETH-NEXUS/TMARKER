/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package superpixels;

import cancernucleussegmentation.CancerNucleusSegmentation;
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
    CancerNucleusSegmentation CNS;
    
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package superpixels;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.Histogram;
import javax.media.jai.ROI;
import tmarker.TMAspot.TMALabel;
import tmarker.TMAspot.TMApoint;
import tmarker.delaunay.ArraySet;
import tmarker.misc.Misc;
import tmarker.tmarker;

/**
 *
 * @author Peter J. Schueffler
 */
public class Superpixel implements Comparable {

    private final static int[] lbp_rotinv = {0, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 37, 39, 43, 45, 47, 51, 53, 55, 59, 61, 63, 85, 87, 91, 95, 111, 119, 127, 255};

    private final String id;
    private byte label;
    private boolean stained = false;
    private double probability = 0.5;
    private double entropy = -1;
    private double similarityToUnlabeledPoints_detection = Double.NaN;
    private double similarityToUnlabeledPoints_classification = Double.NaN;
    private double[] fv = null;
    
    private final SLICsuperpixels s;
    
    private int[] indices_inside;
    private int[] indices_border;
    
    SuperpixelPolygon sppoly = null;
            
    private int ins_i = 0;
    private int bor_i = 0;
    
    ArraySet<Superpixel> neighbours = new ArraySet<>();
    
    Superpixel(String id, byte label, SLICsuperpixels s, int innersize, int outersize) {
        this.id = id;
        this.label = label;
        this.s = s;
        indices_inside = new int[innersize];
        indices_border = new int[outersize];
    }
    
    public byte getLabel() {
        return label;
    }
    
    public boolean isStained() {
        return stained;
    } 
    
    public void setStained(boolean stained) {
        this.stained = stained;
    }
    
    boolean containsOnBorder(int j) {
        for (int i=0; i<indices_border.length; i++) {
            if (indices_border[i]==j) {
                return true;
            }
        }
        return false;
    }
    
    public boolean contains(int j) {
        for (int i=0; i<indices_border.length; i++) {
            if (indices_border[i]==j) {
                return true;
            }
        }
        for (int i=0; i<indices_inside.length; i++) {
            if (indices_inside[i]==j) {
                return true;
            }
        }
        return false;
    }

    public void setLabel(byte new_value) {
        label = new_value;
    }
    
    public void addBorderPoint(int i) {
        indices_border[bor_i++]=i;
        
        if (sppoly!=null) {
            int[] xy = new int[2];
            Misc.Ind2sub(xy, i, s.width);
            sppoly.addAreaPoint(xy[0], xy[1]);
        }
    }
    
    public void addInsidePoint(int i) {
        indices_inside[ins_i++]=i;
        
        if (sppoly!=null) {
            int[] xy = new int[2];
            Misc.Ind2sub(xy, i, s.width);
            sppoly.addAreaPoint(xy[0], xy[1]);
        }
    }
    
    public void addBorderPoints(int[] i) {
        indices_border = Misc.concat(indices_border, i);
        bor_i+=i.length;
        
        if (sppoly!=null) {
            int[] xy = new int[2];
            for (int j:i) {
                Misc.Ind2sub(xy, j, s.width);
                sppoly.addAreaPoint(xy[0], xy[1]);
            }
        }
    }
    
    public void addInsidePoints(int[] i) {
        indices_inside = Misc.concat(indices_inside, i);
        ins_i+=i.length;
        
        if (sppoly!=null) {
            int[] xy = new int[2];
            for (int j:i) {
                Misc.Ind2sub(xy, j, s.width);
                sppoly.addAreaPoint(xy[0], xy[1]);
            }
        }
    }
    
    /**
     * BorderPoints are border points of this superpixel. They are painted blueish
     * in the TMA image. However, only the "left" borderline points are included,
     * to make the bluish lines as thin as possible.
     * @return The borderline points of this superpixel which can be drawn blueish
     * in the TMA image. The coordinates refer to the whole image (not only the
     * superpixel area).
     */
    public int[] getBorderPoints() {
        return indices_border;
    }
    
    public int[] getInsidePoints() {
        return indices_inside;
    }
    
    /**
     * Returns the cicumference of this SP. It has been calculated during superpixel object creation.
     * @return The circumference of this superpixel in pixels.
     */
    public int getCircumference() {
        return indices_border.length;
    }
    
    /**
     * Returns a list of the neighbours of this superpixel.
     * @return A list of the neighbours of this superpixel.
     */
    public Set<Superpixel> getNeighbours() {
        return neighbours;
    }
    
    /**
     * Adds a superpixel to the neighbours list.
     */
    public void addNeighbour(Superpixel sp) {
        this.neighbours.add(sp);
    }
    
    /**
     * Sets the probability of this superpixel. Note that 0.5 is "neutral", 
     * 1 is perfect the one class and 0 is perfect the other class. If you want
     * to set a "normal" probability between [0;1], than use @see #setScaledPobability(double probability)
     * @param probability A probability in [0;1]. 0.5 is neutral,
     * 0 is class "negative", 1 is class "positive".
     */
    public void setProbability(double probability) {
        this.probability = probability;
    }
    
    /**
     * Returns the Classification certainty of this superpixel. 1 means class is certain,
     * 0 means class is unsure.
     * @return Value between 0 and 1.
     */
    public double getCertainty() {
        /*
        double c = 2.0*Math.abs(probability-0.5);
        /*/
        double c = 1.0-getClassEntropy();
        //*/
        return c;
    }
    
    /**
     * Returns the Classification entropy of this superpixel, normalized by
     * maximum entropy for two classes (log(2)). 1 means high entropy,
     * 0 means low entropy.
     * @return Value between 0 and 1.
     */
    public double getClassEntropy() {
        return probability == 1.0 ? 0 : probability == 0.0 ? 0 : -(probability*Math.log(probability)+(1.0-probability)*Math.log(1.0-probability)) / (Math.log(2.0));
    }
    
    public double getInformativeness(boolean classification) {
        double info = 1.0-getCertainty();
        if (getSimilarityToNeighbours(classification)!=Double.NaN) info+=getSimilarityToNeighbours(classification);
        return info;
    }
    
    public double getSimilarityToNeighbours(boolean classification) {
        return (classification ? similarityToUnlabeledPoints_classification : similarityToUnlabeledPoints_detection);
    }
    
    public void setSimilarityToNeighbours(double sim, boolean classification) {
        if (classification) {
            similarityToUnlabeledPoints_classification = sim;
        } else {
            similarityToUnlabeledPoints_detection = sim;
        }
    }
    
    public int getMinX() {
        int minx = s.getWidth();
        int[] txy = new int[2];
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(txy, getInsidePoints()[i], s.getWidth());
            minx = Math.min(minx, txy[0]);
        }
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(txy, getBorderPoints()[i], s.getWidth());
            minx = Math.min(minx, txy[0]);
        }
        return minx;
    }
    
    public int getMaxX() {
        int maxx = 0;
        int[] txy = new int[2];
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(txy, getInsidePoints()[i], s.getWidth());
            maxx = Math.max(maxx, txy[0]);
        }
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(txy, getBorderPoints()[i], s.getWidth());
            maxx = Math.max(maxx, txy[0]);
        }
        return maxx;
    }
    
    public int getMinY() {
        /*int[] txy = new int[2];
        int[] txy2 = new int[2];
        SLICsuperpixels.ind2sub(txy, getInsidePoints()[0], s.getWidth());
        SLICsuperpixels.ind2sub(txy2, getBorderPoints()[0], s.getWidth());
        return Math.min(txy2[1], txy[1]);*/
        int miny = s.getHeight();
        int[] txy = new int[2];
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(txy, getInsidePoints()[i], s.getWidth());
            miny = Math.min(miny, txy[1]);
        }
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(txy, getBorderPoints()[i], s.getWidth());
            miny = Math.min(miny, txy[1]);
        }
        return miny;
    }
    
    public int getMaxY() {
        /*int[] txy = new int[2];
        int[] txy2 = new int[2];
        SLICsuperpixels.ind2sub(txy, getInsidePoints()[getInsidePoints().length-1], s.getWidth());
        SLICsuperpixels.ind2sub(txy2, getBorderPoints()[getBorderPoints().length-1], s.getWidth());
        return Math.min(txy2[1], txy[1]);
        */
        int maxy = 0;
        int[] txy = new int[2];
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(txy, getInsidePoints()[i], s.getWidth());
            maxy = Math.max(maxy, txy[1]);
        }
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(txy, getBorderPoints()[i], s.getWidth());
            maxy = Math.max(maxy, txy[1]);
        }
        return maxy;
    }
    
    
    /**
     * Returns the area in pixels of a given Roi.
     * @param roi The Roi to be measured.
     * @return Area of this Roi (= number of points within or on border of ROI).
     * @author Peter Schüffler
     */
    public static double getArea(ROI roi) {
        double A = 0.0;
        Rectangle rect = roi.getBounds();
        for (int i=rect.x; i<rect.x+rect.width; i++) {
            for (int j=rect.y; j<rect.y+rect.height; j++) {
                if (roi.contains(i, j)) A++;
            }
        }
        return A;
    }
    
    public Point calculateMidPoint() {
        int x = (int) (getMinX() + (getMaxX()-getMinX())/2);
        int y = (int) (getMinY() + (getMaxY()-getMinY())/2);
        if (!contains(Misc.Sub2ind(x, y, s.getWidth()))) {
            int[] xy = new int[2];
            Misc.Ind2sub(xy, getInsidePoints()[(getInsidePoints().length/2)], s.getWidth());
            x = xy[0];
            y = xy[1];
        }
        return new Point(x,y);
    }
    
    public TMApoint mergeToTMAPoint(double confidence) {
        if (getCertainty() > confidence) {
            Point p = calculateMidPoint();
            TMApoint tp = new TMApoint(s.getTMAspot(), p.x, p.y, label);
            return tp;
        }
        return null;
    }

    boolean touchesRectangle(Rectangle r) {
        return touchesRectangle(r, 1);
    }
    
    boolean touchesRectangle(Rectangle r, double zoom) {
        return (int)(getMaxX()*zoom)>=r.x && (int)(getMinX()*zoom)<=r.x+r.width && (int)(getMaxY()*zoom)>=r.y && (int)(getMinY()*zoom)<=r.y+r.height;
    }

    public void flipLabel() {
        if (label==TMALabel.LABEL_POS) {
            label = TMALabel.LABEL_NEG;
        } else if (label==TMALabel.LABEL_NEG) {
            label = TMALabel.LABEL_UNK;
        } else if (label==TMALabel.LABEL_UNK) {
            label = TMALabel.LABEL_BG;
        } else if (label==TMALabel.LABEL_BG) {
            label = TMALabel.LABEL_POS;
        }
    }
    
    public boolean touchesRectangleBorder(Rectangle r) {
        return touchesRectangleBorder(r, 1);
    }
    
    public boolean touchesRectangleBorder(Rectangle r, double zoom) {
        int maxX = (int)(getMaxX()*zoom);
        int maxY = (int)(getMaxY()*zoom);
        int minX = (int)(getMinX()*zoom);
        int minY = (int)(getMinY()*zoom);
        return maxX>=r.x && minX<=r.x ||
               maxX>=r.x+r.width-1 && minX<=r.x+r.width-1 ||
               maxY>=r.y && minY<=r.y ||
               maxY>=r.y+r.height-1 && minY<=r.y+r.height-1;
    }


    @Override
    public int compareTo(Object o) {
        Superpixel tmp = (Superpixel) o;
        return (int) (Math.signum(this.getCertainty() - tmp.getCertainty()));
    }

    public static void normalize(double[] fv) {
        double sum = Misc.sum(fv);
        if (sum > 0) {
            for (int i=0; i<fv.length; i++) {
                fv[i] /= sum;
            }
        }
    }
    
    public static double[] image2Entropy(BufferedImage bi, ROI roi, Superpixel sp) {
        double[] fvl = new double[1];
        if (sp!=null && sp.entropy>=0) {
            fvl[0] = sp.entropy;
        } else {
            double[] hist = image2histogram(bi, roi, 32, 1);
            fvl[0] = Misc.entropy(hist);
            if (sp!=null) {
                fvl[0] = sp.entropy = fvl[0];
            }
        }
        return fvl;
    }
    
    public static double[] image2histogram(BufferedImage bi, ROI roi, int numBins, int numBands) {
        Histogram hist = new Histogram(numBins, 0.0D, 256.0D, bi.getData().getNumBands());
        hist.countPixels(bi.getRaster(), roi, 0, 0, 1, 1);
                
        /*ParameterBlock pb = new ParameterBlock();
        
        int[] bins = { numBins };
        double[] low = { 0.0D };
        double[] high = { 256.0D };
        
        pb.addSource(bi);
        pb.add(null);
        pb.add(1);
        pb.add(1);
        pb.add(bins);
        pb.add(low);
        pb.add(high);
        
        RenderedOp op = JAI.create("histogram", pb, null);
        Histogram hist = (Histogram) op.getProperty("histogram");
        
        int numBands = hist.getNumBands();
        */
        
        double[] fv = new double[(numBands)*numBins];
        for (int i=0; i<numBands; i++) {
            for (int j=0; j<hist.getNumBins(i); j++) {
                fv[(i*numBins+j)] = hist.getBins(i)[j];
            }
        }
        normalize(fv);
        return fv;
    }
    
    public double[] getIntensities(BufferedImage img) {
        double[] intensities = new double[getBorderPoints().length + getInsidePoints().length];
        int[] sub = new int[2];
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(sub, getBorderPoints()[i], s.getWidth());
            intensities[i] = img.getRGB(sub[0], sub[1]);
        }
        for (int i=getBorderPoints().length; i<getBorderPoints().length+getInsidePoints().length; i++) {
            Misc.Ind2sub(sub, getInsidePoints()[i-getBorderPoints().length], s.getWidth());
            intensities[i] = img.getRGB(sub[0], sub[1]);
        }
        return intensities;
    }
    
    public double getEntropy() {
        return entropy;
    }
    
    public void calculateEntropy(BufferedImage img) {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        
        List<Object> patches = getPatches(img);
        BufferedImage bi_gray = (BufferedImage) patches.get(1);
        ROI roi = (ROI) patches.get(2);
        
        image2Entropy(bi_gray, roi, this);
    }
    
    /**
     * Returns the numeric ID of this superpixel.
     * @return The superpixel unique ID.
     */
    public String getID() {
        return id;
    }
    
    /**
     * Returns the rectangular patch around the superpixel as color buffered image, 
     * gray buffered image and the javax.​media.​jai.​ROI describing the superpixel.
     * @param img The whole TMA image as BufferedImage.
     * @return A list with three objects: bi_col (buffered image), bi_gray (buffered image)
     * and roi (javax.​media.​jai.​ROI)
     */
    public List<Object> getPatches (BufferedImage img) {
        List<Object> out = new ArrayList<Object>();
        
        int maxx = getMaxX();
        int maxy = getMaxY();
        int minx = getMinX();
        int miny = getMinY();
        BufferedImage bi_col = new BufferedImage(maxx-minx+1, maxy-miny+1, BufferedImage.TYPE_INT_ARGB);
        // initialize with -1
        for (int i=0; i<bi_col.getWidth(); i++) {
            for (int j=0; j<bi_col.getHeight(); j++) {
                bi_col.setRGB(i, j, 0);
            }
        }
        
        // fill in image info within the superpixel shape
        int[] sub = new int[2];
        for (int i=0; i<getBorderPoints().length; i++) {
            Misc.Ind2sub(sub, getBorderPoints()[i], s.getWidth());
            bi_col.setRGB(sub[0]-minx, sub[1]-miny, img.getRGB(sub[0], sub[1]));
        }
        for (int i=0; i<getInsidePoints().length; i++) {
            Misc.Ind2sub(sub, getInsidePoints()[i], s.getWidth());
            bi_col.setRGB(sub[0]-minx, sub[1]-miny, img.getRGB(sub[0], sub[1]));
        }
        
        // ROI is the shape of the superpixel
        BufferedImage bi_gray = new BufferedImage(bi_col.getWidth(), bi_col.getHeight(), BufferedImage.TYPE_BYTE_GRAY);  
        Graphics g = bi_gray.getGraphics();  
        g.drawImage(bi_col, 0, 0, null);  
        g.dispose(); 
        if (tmarker.DEBUG>5) {
            Misc.writeImage(bi_col, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_col1.png");
            Misc.writeImage(bi_gray, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_gray1.png");
        }        
        ROI roi = new ROI(bi_gray, 1);
        // run this otherwise roi.contains() returns true always.
        for (int i=0; i<bi_gray.getWidth(); i++) {
            for (int j=0; j<bi_gray.getHeight(); j++) {
                if (roi.contains(i,j)) {
                }
            }            
        }
        // END run this
        
        if (tmarker.DEBUG>5) {
            for (int i=0; i<bi_gray.getWidth(); i++) {
                for (int j=0; j<bi_gray.getHeight(); j++) {
                    System.out.print(bi_gray.getData().getSample(i, j, bi_gray.getData().getNumBands()-1)); System.out.print(" ");
                }
                System.out.println("num Bands: " + Integer.toString(bi_gray.getData().getNumBands()));
            }
        }
        if (tmarker.DEBUG>5) {
            for (int i=0; i<bi_gray.getWidth(); i++) {
                for (int j=0; j<bi_gray.getHeight(); j++) {
                    if (roi.contains(i,j)) {
                            System.out.print("1");
                        } else {
                            System.out.print(".");
                        }
                }
                System.out.println("num Bands: " + Integer.toString(bi_gray.getData().getNumBands()));
            }
        }
        
        // complete the bi_col and bi_gray
        for (int i=0; i<bi_col.getWidth(); i++) {
            for (int j=0; j<bi_col.getHeight(); j++) {
                if (bi_col.getRGB(i, j)==0) {
                    bi_col.setRGB (i, j, img.getRGB(i+minx, j+miny));
                }
            }
        }
        g = bi_gray.getGraphics();  
        g.drawImage(bi_col, 0, 0, null);  
        g.dispose(); 
        if (tmarker.DEBUG>5) {
            Misc.writeImage(bi_col, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_col2.png");
            Misc.writeImage(bi_gray, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_gray2.png");
        }    
        
        if (tmarker.DEBUG>5) {
            for (int i=0; i<bi_gray.getWidth(); i++) {
                for (int j=0; j<bi_gray.getHeight(); j++) {
                    if (roi.contains(i,j)) {
                        bi_gray.setRGB(i, j, Color.WHITE.getRGB());
                    } else {
                        bi_gray.setRGB(i, j, Color.BLACK.getRGB());
                    }
                }
            }
            Misc.writeImage(bi_gray, s.getTMAspot().getTmpDir() + File.separator + Integer.toString(indices_inside[0]) + "sp_gray3.png");
        }    
        
        out.add(bi_col);
        out.add(bi_gray);
        out.add(roi);
        
        return out;
    }

    /**
     * Return the size (area) of this superpixel, including borderline points.
     * @return The size of this superpixel in pixels.
     */
    public int getSize() {
        return indices_border.length + indices_inside.length;
    }
    
    /**
     * Same as getSize().
     * @return getSize().
     */
    public int getArea() {
        return getSize();
    }
    
    /**
     * Get the width of the superpixel
     * @return The Width of the superpixel
     */
    public int getWidth() {
        return getMaxX() - getMinX();
    }

    /**
     * Get the height of the superpixel
     * @return The height of the superpixel
     */
    public int getHeight() {
        return getMaxY() - getMinY();
    }

    /**
     * Debug Function: Save the SP as PNG image.
     * @param name Filename with extension.
     */
    public void saveAsPNG(String filename) {
        try {
            BufferedImage b = new BufferedImage(getWidth()+1, getHeight()+1, BufferedImage.TYPE_INT_ARGB);
            int[] sub = new int[2];
            int minX = getMinX();
            int minY = getMinY();
//            System.out.println();
//            System.out.println("bw: " + b.getWidth());
//            System.out.println("bh: " + b.getHeight());
//            System.out.println("minx: " + minX);
//            System.out.println("miny: " + minY);
            for (int ind: getBorderPoints()) {
                Misc.Ind2sub(sub, ind, s.getWidth());
//                System.out.println("  Koordinates: " + sub[0] + " " + sub[1]);
//                System.out.println("  Koordinates corr: " + (sub[0]-minX) + " " + (sub[1]-minY));
                b.setRGB(sub[0]-minX, sub[1]-minY, 0xFFFFFFFF);
            }
            ImageIO.write(b, "PNG", new File(filename));
        } catch (IOException ex) {
            Logger.getLogger(SLICsuperpixels.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}

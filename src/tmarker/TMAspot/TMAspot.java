/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker.TMAspot;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JOptionPane;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.imgscalr.Scalr;
import org.openslide.OpenSlide;
import tmarker.delaunay.ArraySet;
import tmarker.misc.Misc;
import tmarker.misc.SortedProperties;
import tmarker.misc.saveImagesThread;
import tmarker.tmarker;

/**
 * A TMAspot is a TMA image instance. It contains nuclei represented as TMApoints. 
 * Also it inherits a TMAspot_list_panel which is displayed in the TMARKER main window
 * in the TMA List.
 * @author Peter J. Schueffler
 */
public class TMAspot {

    private String original_filename = "";
    private String bgCorrected_filename = "";
    private String tmp_dir = "";
    private SortedProperties prop = null;
    private tmarker tc = null;
    private int w = -1;
    private int h = -1;
    
    private final List<TMApoint> nuclei = new ArrayList<>();
    private final List<Polygon> includingAreas = new ArrayList<>();
    private final List<Polygon> excludingAreas = new ArrayList<>();
    public static final int POLYGON_NODE_WIDTH = 5;
    
    // for nucleus density
    List<Integer> cellDensity = null;
    int densityRadius = 1;
    int density_d_grid = 1;
    
    // for staining intensity
    private byte intensity = -1;
    double[] features = null;
    
    // for TMAspot_list_panel
    private TMAspot_list_panel tlp = null;
    boolean isSelected = false;
    
    // params stored in the TMARKER.conf
    private int param_t2 = -1;
    private int param_t1 = -1;
    private int param_t3 = -1;
    private int param_tolerance = -1;
    private double param_blur = -1;
    private int param_nsuperpx = 0;
    private int param_smoothness = -1;
    private boolean param_lab_space = true;
    private int param_bgx = -1;
    private int param_bgy = -1;
    private int param_bg_rgb = -1;
    
    // params for NDPI support
    OpenSlide os = null;
    
    /**
     * Returns whether this TMAspot has a NDPI (OpenSlide) instance.
     * @return True if the OpenSlide os is not null.
     */
    public boolean isNDPI() {
        return os!=null;
    }
    
    /**
     * Sets the OpenSlide instance.
     * @return The OpenSlide instance (for NDPI support).
     */
    public OpenSlide getNDPI() {
        return os;
    }
    
    /**
     * Returns the TMAspot_list_panel which represents this TMAspot.
     * @return The panel.
     */
    public TMAspot_list_panel getTLP() {
        return tlp;
    }
    
    /**
     * Sets this TMAspot as selected.
     * @param b True for selected, false for not selected.
     */
    public void setSelected(boolean b) {
        isSelected = b;
        this.getTLP().setSelected(b);
    }
    
    /**
     * Returns the selection status of this spot.
     * @return True for selected, false for not selected.
     */
    public boolean isSelected() {
        return isSelected;
    }
    
    /**
     * Returns the intensity of this TMAspot.
     * @return The intensity of this TMAspot. -1 for not yet calculated.
     */
    public byte getIntensity() {
        return intensity;
    }

    /**
     * Sets the intensity of this TMAspot.
     * @param intensity The intensity of this TMAspot (e.g. STAINING_0).
     */
    public void setIntensity(byte intensity) {
        this.intensity = intensity;
    }
    
    /**
     * Returns the parameter of how many superpixel should be calculated.
     * @return The number of superpixels on this spot.
     */
    public int getParam_nsuperpx() {
        return param_nsuperpx;
    }

    /**
     * Returns the background color of this TMAspot. 
     * @return The background RGB color of this TMAspot. -1 for not yet calculated.
     */
    public int getParam_bg_rgb() {
        return param_bg_rgb;
    }
    
    /**
     * Return nucleus radius.
     * @return Nucleus Radius in pixels.
     */
    public int getParam_r() {
        return tc.getLabelRadius();
    }

    /**
     * Returns the blurring parameter (for color deconvolution).
     * @return The blurring radius in pixels. -1 for not yet specified.
     */
    public double getParam_blur() {
        return param_blur;
    }

    /**
     * Sets the blurring parameter (for color deconvolution).
     * @param param_blur The blurring radius in pixels.
     */
    public void setParam_blur(double param_blur) {
        this.param_blur = param_blur;
    }
    
    /**
     * Returns the tolerance parameter for color deconvolution.
     * @return The tolerance parameter. -1 if not yet specified.
     */
    public int getParam_tolerance() {
        return param_tolerance;
    }

    /**
     * Sets the tolerance parameter for color deconvolution.
     * @param param_tolerance The tolerance parameter.
     */
    public void setParam_tolerance(int param_tolerance) {
        this.param_tolerance= param_tolerance;
    }

    /**
     * Returns the smoothness parameter for superpixeling.
     * @return The smoothness parameter. -1 if not yet specified.
     */
    public int getParam_smoothness() {
        return param_smoothness;
    }

    /**
     * Sets the smoothness parameter for superpixeling.
     * @param param_smoothness The smoothness parameter.
     */
    public void setParam_smoothness(int param_smoothness) {
        this.param_smoothness = param_smoothness;
    }
    
    /**
     * Returns the color space parameter for superpixeling.
     * @return The color space parameter. True: use LAB space, False: use RGB space.
     */
    public boolean getParam_lab_space() {
        return param_lab_space;
    }

    /**
     * Sets the color space parameter for superpixeling.
     * @param param_lab_space The color space parameter. True: use LAB space, False: use RGB space.
     */
    public void setParam_lab_space(boolean param_lab_space) {
        this.param_lab_space = param_lab_space;
    }

    /**
     * Returns the threshold t_hema for color deconvolution.
     * @return The threshold t_hema. -1 if not yet specified.
     */
    public int getParam_t_hema() {
        return param_t2;
    }

    /**
     * Sets the threshold t_hema for color deconvolution.
     * @param param_t_hema threshold t_hema.
     */
    public void setParam_t_hema(int param_t_hema) {
        this.param_t2 = param_t_hema;
    }

    /**
     * Returns the threshold t_dab for color deconvolution.
     * @return The threshold t_dab. -1 if not yet specified.
     */
    public int getParam_t_dab() {
        return param_t1;
    }

    /**
     * Sets the threshold t_dab for color deconvolution.
     * @param param_t_dab The threshold t_dab.
     */
    public void setParam_t_dab(int param_t_dab) {
        this.param_t1 = param_t_dab;
    }
    
    /**
     * Returns the threshold t_ch3 for color deconvolution.
     * @return The threshold t_ch3. -1 if not yet specified.
     */
    public int getParam_t_ch3() {
        return param_t3;
    }

    /**
     * Sets the threshold t_dab for color deconvolution.
     * @param param_t_ch3 The threshold t_ch3.
     */
    public void setParam_t_ch3(int param_t_ch3) {
        this.param_t3 = param_t_ch3;
    }
    
    /**
     * Saves the dynamic threshold maps on hard disk.
     * @param TM The threshold maps.
     * @param asParallelThread If true, the saving is done in a parallel thread.
     */
    public void saveThresholdMaps(List<ImagePlus> TM, boolean asParallelThread) {
        if (TM != null && !TM.isEmpty()) {
            String ext = Misc.FilePathStringtoExtension(getOriginalImagename());
            if (asParallelThread) {
                saveImagesThread sit = new saveImagesThread(TM, ext.toUpperCase(), getTmpDir() + File.separator, null, "_thresholdmap");
                sit.start();
            } else {
                for (int i=0; i<TM.size(); i++) {
                    try {
                        File file = new File(getTmpDir() + File.separator + "channel" + Integer.toString(i+1) + "_thresholdmap" + "." + ext);
                        file.deleteOnExit();
                        ImageIO.write(TM.get(i).getBufferedImage(), ext.toUpperCase(), file);
                    } catch (IOException ex) {
                        Logger.getLogger(TMAspot.class.getName()).log(Level.SEVERE, null, ex);
                    }   
                }
            }
        }
    }
    
    /**
     * Saves the heatmap image on hard disk.
     * @param heatmap The heatmap image.
     */
    public void setHeatmapImage(ImagePlus heatmap) {
        if (heatmap != null) {
            List<ImagePlus> imps = new ArrayList<>();
            imps.add(heatmap);
            String ext = Misc.FilePathStringtoExtension(getOriginalImagename());
            saveImagesThread sit = new saveImagesThread(imps, ext.toUpperCase(), getTmpDir() + File.separator, "heatmap", "");
            sit.start();
        }
    }
    
    /**
     * Returns the including areas.
     * @return The including areas defined by the user.
     */
    public List<Polygon> getIncludingAreas() {
        return includingAreas;
    }
    
    /**
     * Returns the excluding areas.
     * @return The excluding areas defined by the user.
     */
    public List<Polygon> getExcludingAreas() {
        return excludingAreas;
    }
    
    
    /**
     * Creates a new TMAspot which belongs to the TMARKER session. The filename is
     * from its image (image file or NDPI file).
     * @param tc The TMARKER session.
     * @param filename The image file of the spot.
     */
    public TMAspot(tmarker tc, String filename) {
        this.tc = tc;
        setFilename(filename);
        if (filename.toLowerCase().endsWith("ndpi") || filename.toLowerCase().endsWith("svs")) {
            try {
                
                // load the native libraries necessary for openSlide.
                if (!System.getProperty("os.name").startsWith("Windows") && tc.getOptionDialog().getParam_OpenSlideLibraryPath() != null) {
                    System.load(tc.getOptionDialog().getParam_OpenSlideLibraryPath());
                }
                else {
                    System.loadLibrary("openslide-jni");
                }
        
                this.os = new OpenSlide(new File(filename));
                
                Map<String, String> props = os.getProperties();
                Set<String> keys = props.keySet();
                for (String key : keys) {
                    addProperty(key, props.get(key));
                }
            } catch (Exception | java.lang.UnsatisfiedLinkError ex) {
                tc.setCursor(Cursor.getDefaultCursor());
                Logger.getLogger(TMAspot.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(tc, "The file " + filename + " could not be parsed.\n\nPossible reason: OpenSlide is not found on the specified path (Tools -> Options).\n\n" + ex.getMessage(), "Error loading file", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        this.tlp = new TMAspot_list_panel(this);
        createThumbnail();
    }
		
    /**
     * Creates the thumbnail image in the TMAList panel.
     */
    void createThumbnail() {
        Thread thumbnailsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try { 
                    BufferedImage thumbnail;
                    if (isNDPI()) {
                        thumbnail = os.createThumbnailImage(TMAspot_list_panel.HEIGHT_UNEXPANDED - 2);
                    } else {
                        thumbnail = null;
                    }
                    getTLP().setThumbnailImage(thumbnail);
                } catch (OutOfMemoryError e) {
                   Logger.getLogger(TMAspot.class.getName()).log(java.util.logging.Level.INFO, "Not enough memory for thumbnail creation.");
                } catch (Exception e) {
                    if (tmarker.DEBUG>0) {
                         Logger.getLogger(TMAspot.class.getName()).log(Level.WARNING, e.getMessage(), e);
                    }
                }
            }
        });
        thumbnailsThread.start();
    }
    
    /**
     * Sets the file name of this TMAspot and creates a temporary work folder on hard disk.
     * @param original_filename The filename where this TMAspot originates.
     */
    public void setFilename(String original_filename) {
        this.original_filename = original_filename;
        String name = (new File(original_filename)).getName();
        int dotPos = name.lastIndexOf(".");
        tmp_dir = getCenter().getTmpDir() + name.substring(0, dotPos);
        java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, tmp_dir);
        File tmp = new File(tmp_dir);
        tmp.mkdir();
        tmp.deleteOnExit();
    }

    /**
     * Returns the original image name of this TMAspot (is always the filename of the loaded TMAspot image).
     * @return The image name of this TMAspot.
     */
    public String getOriginalImagename() {
        return original_filename;
    }
    
    /**
     * Returns the filename of this TMAspot (can be the original filename or the 
     * filename of the background corrected image, if white-balancing had been performed beforehand).
     * @return The current filename of this TMAspot.
     */
    public String getImagename() {
        String name;
        if (bgCorrected_filename.isEmpty()) {
            name = original_filename;
        } else {
            name = bgCorrected_filename;
        }
        return name;
    }
    
    /**
     * Returns if this TMAspot has a BG corrected version (White balance).
     * @return True, if this TMAspot has a BG corrected image.
     */
    public boolean hasBGCorrection() {
        return !bgCorrected_filename.isEmpty();
    }
    
    /**
     * Returns the filename of the original TMAspot image.
     * @return The filename of the original TMAspot image.
     */
    public String getName() {
        return Misc.FilePathStringtoFilename(original_filename);
    }
    
    /**
     * Returns whether or not this TMAspot has already a staining estimation.
     * @return True if this spot has already a staining estimation.
     */
    public boolean hasStainingEstimation() {
        return getStainingEstimation()>=0;
    }
    
    /**
     * Returns the number of cancerous nuclei.
     * @return The number of cancerous nuclei.
     */
    public int getNumPos_total() {
        return getPoints(TMALabel.LABEL_POS).size();
    }
    
    /**
     * Returns the number of benign nuclei.
     * @return The number of benign nuclei.
     */
    public int getNumNeg_total() {
        return getPoints(TMALabel.LABEL_NEG).size();
    }
    
    /**
     * Returns the number of cancerous gold-standard nuclei.
     * @return The number of cancerous gold-standard nuclei.
     */
    public int getNumPos_goldst() {
        return getPoints_GoldStandard(TMALabel.LABEL_POS).size();
    }
    
    /**
     * Returns the number of benign gold-standard nuclei.
     * @return The number of benign gold-standard nuclei.
     */
    public int getNumNeg_goldst() {
        return getPoints_GoldStandard(TMALabel.LABEL_NEG).size();
    }
    
    /**
     * Returns the number of gold-standard background points.
     * @return The number of gold-standard background points.
     */
    public int getNumBG_goldst() {
        return getPoints_GoldStandard(TMALabel.LABEL_BG).size();
    }
    
    /**
     * Returns the number of cancerous estimated nuclei.
     * @return The number of cancerous estimated nuclei.
     */
    public int getNumPos_estimated() {
        return getPoints_Estimated(TMALabel.LABEL_POS).size();
    }
    
    /**
     * Returns the number of benign estimated nuclei.
     * @return The number of benign estimated nuclei.
     */
    public int getNumNeg_estimated() {
        return getPoints_Estimated(TMALabel.LABEL_NEG).size();
    }
    
    /**
     * Returns the staining estimation of this TMAspot.
     * @return The staining estimation of this TMAspot (= relative number of stained nuclei among cancerous nuclei).
     */
    public int getStainingEstimation() {
        int se = getStainingEstimation(getPoints(TMALabel.LABEL_POS));
        
        // add the staining estimation as property        
        if (prop==null) {
            prop = new SortedProperties();
        }
        prop.setProperty("TMARKERStainingPercent", Integer.toString(se));
        return se;
    }
    
    /**
     * Returns the staining estimation of given nuclei.
     * @param tps TMApoints to be considered. They are not tested for beeing cancerous or not.
     * @return The staining estimation of these nuclei (= relative number of stained nuclei).
     */
    public static int getStainingEstimation(List<TMApoint> tps) {
        double num_stained = 0;
        double num_clear = 0;
        for (TMApoint tp: tps) {
            if (tp.isStained()) {
                num_stained++;
            } else {
                num_clear++;
            }
        }
        if (num_stained == 0 && num_clear == 0) {
            return 0;
        } else {
            return (int) (Math.round(10000.0*num_stained / (num_stained+num_clear))/100.0);
        }
    }
    
    /**
     * Returns the working directory of this TMAspot. Temporary files can be saved here.
     * The directory will be deleted after this TMARKER session.
     * @return The temporary directory of this TMAspot.
     */
    public String getTmpDir() {
        return tmp_dir;
    }
    
    /**
     * Return the parent TMARKER session.
     * @return The TMARKER session.
     */
    public tmarker getCenter() {
        return tc;
    }

    /**
     * Return all nuclei and background points.
     * @return All TMApoints on this TMAspot.
     */
    public List<TMApoint> getPoints() {
        return nuclei;
    }
    
    /**
     * Return all nuclei and background points within a ROI.
     * @param ROI A specified ROI.
     * @return A new list with all TMApoints on this TMAspot in this ROI.
     */
    public List<TMApoint> getPointsInROI(Polygon ROI) {
        List<TMApoint> tps = new ArrayList();
        tps.addAll(getPoints());
        if (ROI != null) {
            for (int i=tps.size()-1; i>=0; i--) {
                Polygon roi = getAreaOnPoint(tps.get(i).x, tps.get(i).y);
                if (roi == null || roi != ROI) {
                    tps.remove(i);
                }
            }
        }
        return tps;
    }
    
    /**
     * Returns all points with the proper label.
     * @param label One of TMAspot.LABEL_BG, TMAspot.LABEL_FG,
     * TMAspot.LABEL_UNK, TMAspot.LABEL_POS, TMAspot.LABEL_NEG.
     * @return All points with the correct label.
     */
    public List<TMApoint> getPoints(byte label) {
        return (getPoints(label, true));
    }
    
    /**
     * Returns all points with the proper tag. Tag can be the label byte or the staining byte.
     * @param tag If isLabel is true, one of TMAspot.LABEL_BG, TMAspot.LABEL_FG,
     * TMAspot.LABEL_UNK, TMAspot.LABEL_POS, TMAspot.LABEL_NEG. Otherwise, one of 
     * STAINING_0, STAINING_1, STAINING_2, STAINING_3.
     * @param isLabel If true, the tag is treated as label byte. Otherwise, as staining byte.
     * @return All points with the correct tag.
     */
    public List<TMApoint> getPoints(byte tag, boolean isLabel) {
        List<TMApoint> tps = new ArrayList<>();
        List<TMApoint> all_tps = getPoints();
        if (all_tps!=null) {
            for (int i=0; i<all_tps.size(); i++) {
                if (isLabel && all_tps.get(i).getLabel() == tag || !isLabel && all_tps.get(i).getStaining() == tag) {
                    tps.add(all_tps.get(i));
                }
            }
        } 
        return tps;
    }
    
    /**
     * Returns all TMApoints with specific staining and malignancy.
     * @param isGoldStandard If true, only gold-standard TMApoints are considered. Otherwise, only estimated TMApoints.
     * @param staining One of STAINING_0, STAINING_1, STAINING_2, STAINING_3.
     * @param malignancy One of TMAspot.LABEL_BG, TMAspot.LABEL_FG,
     * TMAspot.LABEL_UNK, TMAspot.LABEL_POS, TMAspot.LABEL_NEG.
     * @return All points with the indicated specifications.
     */
    public List<TMApoint> getPoints(boolean isGoldStandard, byte staining, byte malignancy) {
        List<TMApoint> tps = new ArrayList<>();
        List<TMApoint> all_tps = getPoints();
        if (all_tps!=null) {
            for (TMApoint tp: all_tps) {
                if ((isGoldStandard && tp.getGoldStandard() > 0 || !isGoldStandard && tp.getGoldStandard() == 0) && tp.getStaining() == staining && tp.getLabel() == malignancy) {
                    tps.add(tp);
                }
            }
        } 
        return tps;
    }
    
    /**
     * Returns the number of TMApoints with specific staining and malignancy.
     * @param isGoldStandard If true, only gold-standard TMApoints are considered. Otherwise, only estimated TMApoints.
     * @param staining One of STAINING_0, STAINING_1, STAINING_2, STAINING_3.
     * @param malignancy One of TMAspot.LABEL_BG, TMAspot.LABEL_FG,
     * TMAspot.LABEL_UNK, TMAspot.LABEL_POS, TMAspot.LABEL_NEG.
     * @return The number of TMApoints with the indicated specifications.
     */
    public int getNumPoints(boolean isGoldStandard, byte staining, byte malignancy) {
        return getPoints(isGoldStandard, staining, malignancy).size();
    }
    
    /**
     * Returns all TMApoints with specific staining and malignancy.
     * @param isGoldStandard If true, only gold-standard TMApoints are considered. Otherwise, only estimated TMApoints.
     * @param isStained If true, only nuclei with staining != STAINING_0 are considered. Otherwise only nuclei with staining == STAINING_0.
     * @param malignancy One of TMAspot.LABEL_BG, TMAspot.LABEL_FG,
     * TMAspot.LABEL_UNK, TMAspot.LABEL_POS, TMAspot.LABEL_NEG.
     * @return All TMApoints with the indicated specifications.
     */
    public List<TMApoint> getPoints(boolean isGoldStandard, boolean isStained, byte malignancy) {
        List<TMApoint> tps = new ArrayList<>();
        List<TMApoint> all_tps = getPoints();
        if (all_tps!=null) {
            for (TMApoint tp: all_tps) {
                //if (tp!=null) {
                    if ((isGoldStandard && tp.getGoldStandard() > 0 || 
                            !isGoldStandard && tp.getGoldStandard() == 0) && 
                            (isStained && tp.getStaining() > 0 || 
                            !isStained && tp.getStaining() == TMALabel.STAINING_0) && 
                            tp.getLabel() == malignancy) {
                        tps.add(tp);
                    }
                //} else {
                //
                //}
            }
        } 
        return tps;
    }
    
    /**
     * Returns the number of TMApoints with specific staining and malignancy.
     * @param isGoldStandard If true, only gold-standard TMApoints are considered. Otherwise, only estimated TMApoints.
     * @param isStained If true, only nuclei with staining != STAINING_0 are considered. Otherwise only nuclei with staining == STAINING_0.
     * @param malignancy One of TMAspot.LABEL_BG, TMAspot.LABEL_FG,
     * TMAspot.LABEL_UNK, TMAspot.LABEL_POS, TMAspot.LABEL_NEG.
     * @return The number of TMApoints with the indicated specifications.
     */
    public int getNumPoints(boolean isGoldStandard, boolean isStained, byte malignancy) {
        return getPoints(isGoldStandard, isStained, malignancy).size();
    }
    
    /**
     * Adds a point to the TMAspot.
     * @param p The TMApoint to be added. Can be nucleus or background point.
     */
    public void addPoint(TMApoint p) {
        nuclei.add(p);
    }
    
    /**
     * Adds a set of points to the TMAspot.
     * @param ps The set of TMApoint to be added. Can be nuclei or background points.
     */
    public void addPoints(List<TMApoint> ps) {
        nuclei.addAll(ps);
    }
    
    /**
     * Removes a point from the TMAspot.
     * @param p The TMApoint to be removed.
     */
    public void removePoint(TMApoint p) {
        nuclei.remove(p);
    }
    
    /**
     * Removes all estimated TMApoints.
     */
    public void deleteAllPoints_ES() {
        for (int i=nuclei.size()-1; i>=0; i--) {
            if (!nuclei.get(i).isGoldStandard()) {
                nuclei.remove(i);
            }
        }
    }
    
    /**
     * Returns the TMApoint which lies on position (x,y) or within a certain radius.
     * @param x The x-coord.
     * @param y The y-coord.
     * @param radius The radius in which the TMApoint should lie.
     * @param onlyVisible If true, only the currently visible TMApoints are searched. Otherwise, all TMApoints are searched.
     * @return The first TMApoint found at this location. Null if none is found.
     */
    public TMApoint getPointAt(int x, int y, int radius, boolean onlyVisible) {
        List<TMApoint> ps;
        if (onlyVisible) {
            ps = getVisiblePoints();
        } else {
            ps = getPoints();
        }
        for (TMApoint tp:ps) {
            if (Math.sqrt(Math.pow(tp.x-x,2)+Math.pow(tp.y-y,2))<=radius) {
                return (tp);
            }
        }
        return null;
    }
    
    /**
     * Returns all TMApoints which are currently visible on the screen.
     * @return All TMApoints which are currently visible on the screen.
     */
    public List<TMApoint> getVisiblePoints() {
        List<TMApoint> allpts = getPoints();
        List<TMApoint> viewpts = new ArrayList<>();
        tmarker t = getCenter();
        for (TMApoint tp: allpts) {
            if ((t.isShowingPosLabels() && tp.getLabel() == TMALabel.LABEL_POS || 
                t.isShowingNegLabels() && tp.getLabel() == TMALabel.LABEL_NEG ||
                t.isShowingUnkLabels() && tp.getLabel() == TMALabel.LABEL_UNK ||
                tp.getLabel() == TMALabel.LABEL_BG) &&
               (t.isShowingStainedLabels() && tp.isStained() ||
                t.isShowingUnstainedLabels() && !tp.isStained()) &&
               (t.isShowingEstimatedLabels() && !tp.isGoldStandard() ||
                t.isShowingGoldStandardLabels() && tp.isGoldStandard()) && 
               (t.getGSNumberForViewing() < 0 || t.getGSNumberForViewing()==tp.getGoldStandard())) {
                viewpts.add(tp);
            }
        }
        return viewpts;
    }
    
    /**
     * Returns the highest number of a labeler (pathologist). Each gold-standard point
     * is labelled by a patholgist indicated by a number.
     * @return The highest number of labelers of this TMAspot's TMApoints.
     */
    public byte getMaxGSNumber() {
        byte maxGSNumber = 0;
        for (TMApoint tp: getPoints()) {
            if (tp.getGoldStandard()!=TMApoint.CONSENSUS) {
                maxGSNumber = (byte) Math.max(maxGSNumber, tp.getGoldStandard());
            }
        }
        return maxGSNumber;
    }
    
    /**
     * Displays the staining info (TMA List View in TMARKER).
     */
    public void dispStainingInfo() {
        //tc.setState_ImagesLoaded();
        tc.getTSD().updateSummary(this);
        getCenter().updateTMATable(this);
    }

    /**
     * Adopts all parameters from another TMAspot.
     * @param other_spot The source TMAspot.
     */
    public void adoptParams(TMAspot other_spot, boolean convertPoints) {
        param_blur = other_spot.param_blur;
        param_tolerance = other_spot.param_tolerance;
        param_t2 = other_spot.param_t2;
        param_t1 = other_spot.param_t1;
        param_nsuperpx = other_spot.param_nsuperpx;
        param_smoothness = other_spot.param_smoothness;
        param_bgx = other_spot.param_bgx;
        param_bgy = other_spot.param_bgy;
        param_bg_rgb = other_spot.param_bg_rgb;
        
        prop = other_spot.prop;
        
        // add other spots TMApoints which are not already in here.
        List<TMApoint> old_points = getPoints();
        for (TMApoint tp_new: other_spot.getPoints()) {
            boolean found = false;
            TMApoint tp_old = null;
            for (int i=0; i<old_points.size(); i++) {
                tp_old = old_points.get(i);
                if (tp_old.x == tp_new.x && tp_old.y == tp_new.y) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (convertPoints) {
                    tp_new.setGoldStandard((byte) (getMaxGSNumber() + 1));
                }
                tp_new.setTMAspot(this);
                this.addPoint(tp_new);
            } else {
                if (convertPoints) {
                    tp_old.setGoldStandard((byte) (getMaxGSNumber() + 1));
                }
            }
        }
        
        // add other spots including areas which are not already in here.
        if (other_spot.includingAreas != null && !other_spot.includingAreas.isEmpty()) {
            List<Polygon> old_areas = getIncludingAreas();
            for (Polygon p_new: other_spot.includingAreas) {
                boolean found = false;
                for (Polygon p_old: old_areas) {
                    if (p_old.npoints == p_new.npoints) { 
                        boolean allPointsEqual = true;
                        for (int i=0; i<p_old.npoints; i++) {
                            allPointsEqual &= p_old.xpoints[i] == p_new.xpoints[i] && p_old.ypoints[i] == p_new.ypoints[i];
                        }
                        if (allPointsEqual) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    includingAreas.add(p_new);
                }
            }
        }
        
        // add other spots including areas which are not already in here.
        if (other_spot.excludingAreas != null && !other_spot.excludingAreas.isEmpty()) {
            List<Polygon> old_areas = getExcludingAreas();
            for (Polygon p_new: other_spot.excludingAreas) {
                boolean found = false;
                for (Polygon p_old: old_areas) {
                    if (p_old.npoints == p_new.npoints) { 
                        boolean allPointsEqual = true;
                        for (int i=0; i<p_old.npoints; i++) {
                            allPointsEqual &= p_old.xpoints[i] == p_new.xpoints[i] && p_old.ypoints[i] == p_new.ypoints[i];
                        }
                        if (allPointsEqual) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    excludingAreas.add(p_new);
                }
            }
        }
    }
    
    /**
     * Deletes all gold-standard points.
     */
    public void deleteAllPoints_GS() {
        for (int i=nuclei.size()-1; i>=0; i--) {
            if (nuclei.get(i).isGoldStandard()) {
                nuclei.remove(i);
            }
        }
        getCenter().getTMAView().repaint();
    }
    
    /**
     * Specifically deletes TMALabels from this spot.
     * @param whichPoints The label of the points to be deleted (e.g. TMALabel.LABEL_POS).
     * @param goldStandard True, if only gold standard points should be considered. Otherwise, only estimated points are considered.
     */
    public void deleteAllPoints(byte whichPoints, boolean goldStandard) {
        for (int i=nuclei.size()-1; i>=0; i--) {
            if (nuclei.get(i).getLabel()==whichPoints && nuclei.get(i).isGoldStandard()==goldStandard) {
                nuclei.remove(i);
            }
        }
    }
    
    /**
     * Specifically deletes TMALabels from this spot.
     * @param PointLabel The label of the points to be deleted (e.g. TMALabel.LABEL_POS).
     * @param PointStaining The staining of the points to be deleted (e.g. TMALabel.STAINING_0).
     * @param goldStandard True, if only gold standard points should be considered. Otherwise, only estimated points are considered.
     */
    public void deleteAllPoints(byte PointLabel, byte PointStaining, boolean goldStandard) {
        for (int i=nuclei.size()-1; i>=0; i--) {
            if (nuclei.get(i).getLabel()==PointLabel && nuclei.get(i).getStaining()==PointStaining && nuclei.get(i).isGoldStandard()==goldStandard) {
                nuclei.remove(i);
            }
        }
    }
    
    /**
     * Performs the manual white balancing.
     */
    public void doBgCorrection() {
        if (param_bgx >= 0 && param_bgy >= 0) {
            doBgCorrection(param_bgx, param_bgy);
        } else if (param_bg_rgb != -1 && param_bg_rgb != 0) {
            doBgCorrection(param_bg_rgb);
        }
    }
    
    /**
     * Performs the manual white balancing with a specific background point x,y. This points color value is used for balancing.
     * @param x The x-coord.
     * @param y The y-coord.
     */
    public void doBgCorrection(int x, int y) {
        param_bgx = x;
        param_bgy = y;
        String text = getCenter().getStatusMessageLabel().getText();
        getCenter().setStatusMessageLabel("Correcting Background...");
        getCenter().setProgressbar(0);
        java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Correcting background manually on point x=" + x + ", y=" + y + ".");
        BufferedImage I = getBufferedImage();
        int cb = I.getRGB(x, y);
        doBgCorrection(cb, I);
        getCenter().setStatusMessageLabel(text);
        getCenter().setProgressbar(0);
    }
    
    /**
     * Performs the manual white balancing with a specific background color bg_rgb which is used for correction.
     * The original image is not changed.
     * @param bg_rgb The background color.
     */
    public void doBgCorrection(int bg_rgb) {
        // getBufferedImage() deliveres a new BufferedImage.
        doBgCorrection(bg_rgb, getBufferedImage());
    }
    
    /**
     * Performs the manual white balancing on a given image I with a specific background color bg_rgb which is used for correction.
     * The image is altered and stored as a temporary image file on the hard drive (deleted on TMARKER exit).
     * @param bg_rgb The background color.
     * @param I The image which should be corrected.
     */
    public void doBgCorrection(int bg_rgb, BufferedImage I) {
        java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Correcting background manually with color " + bg_rgb + ".");
        param_bg_rgb = bg_rgb;
        String text = getCenter().getStatusMessageLabel().getText();
        getCenter().setStatusMessageLabel(this.getName() + " Correcting Background...");
        getCenter().setProgressbar(0);
        try {
            int rb = (bg_rgb & 0x00ff0000);
            int gb = (bg_rgb & 0x0000ff00);
            int bb =  bg_rgb & 0x000000ff;
            if (rb==16711680 && gb==65280 && bb==255) {
                getCenter().setStatusMessageLabel(this.getName() + " Background Already White.");
            } else {
                double scale_r = 16711680.0/rb;
                double scale_g = 65280.0/gb;
                double scale_b = 255.0/bb;

                int c, a, r, g, b;
                int w = I.getWidth();
                int h = I.getHeight();
                for (int i=0; i<w; i++) {
                    if (i % 100 ==0) {
                        getCenter().setProgressbar((int) 100*i/w);
                    }
                    for (int j=0; j<h; j++) {
                        c = I.getRGB(i, j);
                        a = (c & 0xff000000);// >> 24;
                        r = (c & 0x00ff0000);// >> 16;
                        g = (c & 0x0000ff00);// >> 8;
                        b =  c & 0x000000ff;
                        
                        r = Math.min(16711680, (int) (r*scale_r));
                        g = Math.min(65280,    (int) (g*scale_g));
                        b = Math.min(255,      (int) (b*scale_b));
                        
                        r = (r & 0x00ff0000);
                        g = (g & 0x0000ff00);
                        
                        I.setRGB(i,j, a | r | g | b);
                    }
                }
                
                String ftype = Misc.FilePathStringtoExtension(getOriginalImagename());
                String fname = getTmpDir()+File.separator + Misc.FilePathStringtoFilename(getOriginalImagename());
                File out = new File(fname);
                out.deleteOnExit();
                ImageIO.write(I, ftype, out);
                this.bgCorrected_filename = fname;
                if (getCenter().getVisibleTMAspot() == this) {
                    getCenter().getTMAView().showTMAspot(this, true);
                    getCenter().showTMAspotPreview();
                    //getCenter().showTMAspot(this);
                }
            }
            
            getCenter().setStatusMessageLabel(text);
        } catch (IOException ex) { 
            getCenter().setStatusMessageLabel("An error occurred while correcting background. " + ex.getMessage());
        } finally {
            getCenter().setProgressbar(0);
        }
    }
    
    /**
     * Performs the automatic white balancing. First, a good background color is searched: 
     * A sliding window with the radius of the nuclei runs over the whole image.
     * In each window, the average color is calculated and the entropy of the intensities are calculated.
     * The average color which is larger than 180 in red channel in the window with the smallest entropy defines the background.
     * Then the background is corrected with this color.
     */
    public void doBgCorrectionAutomatic() {
        String text = getCenter().getStatusMessageLabel().getText();
        getCenter().setStatusMessageLabel(this.getName() + " Correcting Background: Search for Background...");
        getCenter().setProgressbar(1);
        int r = getParam_r();
        BufferedImage I = getBufferedImage();
        ImagePlus ip = new ImagePlus("", I);
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(ip.getProcessor(), r/4, r/4, 0.02);
        BufferedImage I_gray = Misc.convertToGrayscale(ip.getBufferedImage());
        
        double entropy = Double.MAX_VALUE;
        double entropy_current;
        Color bg = Color.BLACK;
        Color bg_current;
        double[] hist;
        for (int i=r; i<I.getWidth()-r; i+=r) {
            getCenter().setProgressbar((int) 100*i/(I.getWidth()));
            if (entropy > 0) {
                for (int j=r; j<I.getHeight()-r; j+=r) {
                    if (entropy > 0) {
                        bg_current = getAverageColorAtPoint(I, i, j, r, false);
                        if (bg_current.getRed()>180) { //minimum intensity of background
                            hist = Misc.image2histogram(I_gray.getSubimage(i-r, j-r, 2*r, 2*r), null, 32);
                            entropy_current = Misc.entropy(hist);
                            if (entropy_current<entropy) {
                                if (tmarker.DEBUG>0) {
                                    java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Smaller Entropy found on x=" + i + " y=" + j + " (e=" + entropy_current + ")");
                                }
                                entropy = entropy_current;
                                bg = bg_current;
                                if (tmarker.DEBUG>0) {
                                    java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "  Color on this point (R/B/G): " + bg.getRed() + "/" + bg.getGreen() + "/" + bg.getBlue());
                                }
                            }
                        }  
                    }
                }
            }
        }
        if (bg.getRed()>180) {
            doBgCorrection(bg.getRGB(), I);
        }
        getCenter().setStatusMessageLabel(text);
        getCenter().setProgressbar(0);
    }
    
    /**
     * Returns the color on a given image on a specific point.
     * @param I The image.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The color on (x,y).
     */
    public static Color ColorAtPoint(BufferedImage I, int x, int y) {
        int c = I.getRGB(x,y);
        int r = (c & 0x00ff0000) >> 16;
        int g = (c & 0x0000ff00) >> 8;
        int b = c & 0x000000ff;
                        
        return new Color(r, g, b);
    }
    
    /**
     * Returns the average color on a given image in a specific area defined by a center point and radius.
     * @param I The image.
     * @param x The x-coordinate of the center point.
     * @param y The y-coordinate of the center point.
     * @param radius The radius of the round area.
     * @param median If true, the median color is returned, otherwise the mean.
     * @return The mean color which is a new color with mean red, mean green and mean blue of the values in the originial image.
     */
    public static Color getAverageColorAtPoint(BufferedImage I, int x, int y, int radius, boolean median) {
        List<double[]> rgb = getImagepointsAroundPoint(I, x, y, radius);
        double[] r = rgb.get(0);
        double[] g = rgb.get(1);
        double[] b = rgb.get(2);
        if (median) {
            Median m = new Median();
            return new Color((int)m.evaluate(r), (int)m.evaluate(g), (int)m.evaluate(b));  
        } else {
            Mean m = new Mean();
            return new Color((int)m.evaluate(r), (int)m.evaluate(g), (int)m.evaluate(b));  
        }
    }
    
    /**
     * Returns the color values of all points around a given point x,y in image I
     * within radius r. The radius is devided by sqrt(2) first (such that this
     * algorithm runs over the inner rectangle of the circle with radius r).
     * Pixels which lie outside the image are ignored.
     * @param I The image under consideration.
     * @param x x-coordinate of the center pixel.
     * @param y y-coordinate of the center pixel.
     * @param r Radius around (x,y) within which the r|g|b values are collected.
     * @return A list of three double arrays (red_array, green_array, blue_array).
     * All three arrays have same length and each entry in the array corresponds
     * to the color value of one pixel around (x|y). The point (x|y) is included
     * in the list.
     */
    public static List<double[]> getImagepointsAroundPoint(BufferedImage I, int x, int y, int r) {
        int sl = (int) Math.floor(r/Math.sqrt(2));
        Color c;
        double[] red = new double[(int)Math.pow(2*sl+1,2)];
        double[] gre = new double[(int)Math.pow(2*sl+1,2)];
        double[] blu = new double[(int)Math.pow(2*sl+1,2)];
        int i=0;
        for (int u=x-sl; u<=x+sl; u++) {
            if (u>=0 && u<I.getWidth()) {
                for (int v=y-sl; v<=y+sl; v++) {
                    if (v>=0 && v<I.getHeight()) {
                        c = ColorAtPoint(I,u,v);
                        red[i] = c.getRed();
                        gre[i] = c.getGreen();
                        blu[i] = c.getBlue();
                        //cm.g.fillOval((int)((ps.get(i).getX()))-r/2, (int)((ps.get(i).getY()))-r/2, r, r);
                        i++;
                    }
                }
            }
        } 
        List<double[]> rgb = new ArrayList<>();
        rgb.add(red);
        rgb.add(gre);
        rgb.add(blu);
        return rgb;        
    }

    /**
     * Returns the average color of all nuclei with the label label.
     * @param label The label of the nuclei to be considered (e.g. only TMAspot.LABEL_POS nuclei).
     * @param median If true, the median color is returned, otherwise the mean.
     * @return The average color of the indicated nuclei.
     */
    public Color getAverageColor(byte label, boolean median) {
        return (getAverageColor(getPoints(label, true), median));
    }
    
    /**
     * Returns the average color of all nuclei with the label tag or staining tag.
     * @param tag If isLabel is true, one of TMAspot.LABEL_BG, TMAspot.LABEL_FG,
     * TMAspot.LABEL_UNK, TMAspot.LABEL_POS, TMAspot.LABEL_NEG. Otherwise, one of 
     * STAINING_0, STAINING_1, STAINING_2, STAINING_3.
     * @param isLabel If true, the tag is treated as label byte. Otherwise, as staining byte.
     * @param median If true, the median color is returned, otherwise the mean.
     * @return The average color of the indicated nuclei.
     */
    public Color getAverageColor(byte tag, boolean isLabel, boolean median) {
        return (getAverageColor(getPoints(tag, isLabel), median));
    }
    
    /**
     * Returns the average color of given nuclei.
     * @param ps The TMApoints.
     * @param median If true, the median color is returned, otherwise the mean.
     * @return The average color of the given nuclei.
     */
    public Color getAverageColor(List<TMApoint> ps, boolean median) {
        if (ps.isEmpty()) {
            return Color.WHITE;
        }
        
        double[] r = new double[ps.size()];
        double[] g = new double[ps.size()];
        double[] b = new double[ps.size()];
        //try {
            BufferedImage I = getBufferedImage();
            int rd = getParam_r();
            Color c;
            for (int i=0; i<ps.size(); i++) {
                c = getAverageColorAtPoint(I, ps.get(i).x, ps.get(i).y, rd, median);
                r[i] = c.getRed();
                g[i] = c.getGreen();
                b[i] = c.getBlue();
            }
        //} catch (Exception ex) {
        //    JOptionPane.showMessageDialog(getCenter(), "An error occurred while opening " + Misc.FilePathStringtoFilename(getOriginalImagename(SHOW_ORIGINAL_IMAGE)), "Error opening image", JOptionPane.ERROR_MESSAGE);
        //}
        if (median) {
            Median m = new Median();
            return new Color((int)m.evaluate(r), (int)m.evaluate(g), (int)m.evaluate(b));  
        } else {
            Mean m = new Mean();
            return new Color((int)m.evaluate(r), (int)m.evaluate(g), (int)m.evaluate(b));  
        }
    }
    
    /**
     * Returns the intensities of the nuclei with the label label.
     * @param label
     * @param median If true, the median intensities are returned, otherwise the mean.
     * @return A new double[4][n] with n equals the number of nuclei. The 4 values per nucleus are: R, G, B and GRAY intensity.
     */
    public double[][] getIntensities(byte label, boolean median) {
        List<TMApoint> ps = getPoints(label);
        
        double[][] value = new double[4][ps.size()];
        double[] r = new double[ps.size()];
        double[] g = new double[ps.size()];
        double[] b = new double[ps.size()];
        double[] gray = new double[ps.size()];
        if (!ps.isEmpty()) {
            BufferedImage I = getBufferedImage();
            
            int rd = getParam_r();
            Color c;
            for (int i=0; i<ps.size(); i++) {
                c = getAverageColorAtPoint(I, ps.get(i).x, ps.get(i).y, rd, median);
                r[i] = c.getRed();
                g[i] = c.getGreen();
                b[i] = c.getBlue();
                gray[i] = (int) (Misc.RGBToGray(r[i], g[i], b[i]));
            }
        }
        value[0] = r;
        value[1] = g;
        value[2] = b;
        value[3] = gray;
        
        return value;  
    }
    
    /**
     * Returns the intensities of all pixels in the TMAspot image.
     * @return A new double[4][n] with n equals the number of pixels. The 4 values per pixel are: R, G, B and GRAY intensity.
     */
    public double[][] getIntensities() {
        BufferedImage I = getBufferedImage();
        
        if (I != null) {
            int n = I.getWidth()*I.getHeight();
            double[][] value = new double[4][n];
            double[] r = new double[n];
            double[] g = new double[n];
            double[] b = new double[n];
            double[] gray = new double[n];
        
            int c;
            n=0;
            for (int i=0; i<I.getWidth(); i++) {
                for (int j=0; j<I.getHeight(); j++) {
                    c = I.getRGB(i, j);
                    r[n] = (0x00FF0000 & c) >> 16;
                    g[n] = (0x0000FF00 & c) >> 8;
                    b[n] = (0x000000FF & c);
                    gray[n] = (int) (Misc.RGBToGray(r[i], g[i], b[i]));
                    n++;
                }
            }
            
            value[0] = r;
            value[1] = g;
            value[2] = b;
            value[3] = gray;
            return value;
        } else {
            return new double[][]{};
        }        
    }
    
    /**
     * Returns 4 histograms (one for R, G, B, and Gray) with nbin bins each. Histograms are calculated over the whole TMAspot image (pixel wise).
     * @param nbins The number of bins per histogram.
     * @param relativeFrequency If true, the histograms are normalized by the number of pixels. Otherwise, the raw counts are returned.
     * @return A new double[4][nbins] where n is the number of bins per historgram.
     */
    public double[][] getHistograms(int nbins, boolean relativeFrequency) {
        double[][] value = new double[4][nbins];
        double[] r = new double[nbins];
        double[] g = new double[nbins];
        double[] b = new double[nbins];
        double[] gray = new double[nbins];

        for (int i=0; i<nbins; i++) {
            r[i]=0;
            g[i]=0;
            b[i]=0;
            gray[i]=0;
        }

        BufferedImage I = getBufferedImage();
        
        if (I != null) {
            int n = I.getWidth()*I.getHeight();
            int r_, g_, b_, c;
            for (int i=0; i<I.getWidth(); i++) {
                for (int j=0; j<I.getHeight(); j++) {
                    c = I.getRGB(i, j);
                    r_ = ((0x00FF0000 & c) >> 16);
                    g_ = ((0x0000FF00 & c) >> 8);
                    b_ = ((0x000000FF & c));
                    r[r_]++;
                    g[g_]++;
                    b[b_]++;
                    gray[(int) (Misc.RGBToGray(r_, g_, b_))]++; // standard gray scaling formula
                }
            }
            if (relativeFrequency) {
                for (int i=0; i<nbins; i++) {
                    r[i]/=n;
                    g[i]/=n;
                    b[i]/=n;
                    gray[i]/=n;
                }
            }
        }
        value[0] = r;
        value[1] = g;
        value[2] = b;
        value[3] = gray;
        return value;
    }

    /**
     * Flips the labels of all points.
     */
    public void flipAllPoints() {
        if (hasStainingEstimation()) {
            // recalculate HScore
            getHScore();
            
            List<TMApoint> ps = getPoints();
            for (int i=0; i<ps.size(); i++) {
                ps.get(i).flipLabel(true);
            }
            dispStainingInfo();
            if (this==getCenter().getVisibleTMAspot()) {
                getCenter().getTMAView().repaint();
            }
            
            
        }
    }
    
    /**
     * Returns all points which are manually drawn (gold-standard).
     * @return All points which are manually drawn (gold-standard).
     */
    public List<TMApoint> getPoints_GoldStandard() {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (ps.get(i).isGoldStandard()) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns all points which are manually drawn (gold-standard) and have a specific label.
     * @param label The label (e.g. TMAspot.POS for cancerous nuclei).
     * @return All points which are manually drawn (gold-standard) and have a specific label.
     */
    public List<TMApoint> getPoints_GoldStandard(byte label) {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (ps.get(i).isGoldStandard() && ps.get(i).getLabel()==label) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns all points which are manually drawn (gold-standard) by labeller number gs_number, and which have a specific label. Interesting for inter-pathologists errors.
     * @param gs_number The number of the radiologist who drew the nuclei.
     * @param label The label (e.g. TMAspot.POS for cancerous nuclei).
     * @return All points which are manually drawn (gold-standard) by labeller number gs_number, and which have a specific label.
     */
    public List<TMApoint> getPoints_GoldStandard(byte gs_number, byte label) {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (ps.get(i).getGoldStandard()==gs_number && ps.get(i).getLabel()==label) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns all points which are computationally guessed (estimated).
     * @return All points which are computationally guessed (estimated) (new ArrayList).
     */
    public List<TMApoint> getPoints_Estimated() {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (!ps.get(i).isGoldStandard()) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns all points which are computationally guessed (estimated) and have a specific label.
     * @param label The label (e.g. TMAspot.POS for cancerous nuclei).
     * @return All points which are computationally guessed (estimated) and have a specific label.
     */
    public List<TMApoint> getPoints_Estimated(byte label) {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (!ps.get(i).isGoldStandard() && ps.get(i).getLabel()==label) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns all points which are computationally guessed (estimated) and have a specific label.
     * @param label The label (e.g. TMAspot.POS for cancerous nuclei).
     * @param stained If true, only stained nuclei are returned (staining != TMAspot.STAINING_0). Otherwise, only clear nuclei are returned (staining == STAINING_0).
     * @return All points which are computationally guessed (estimated) and have a specific label.
     */
    public List<TMApoint> getPoints_Estimated(byte label, boolean stained) {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (!ps.get(i).isGoldStandard() && ps.get(i).getLabel()==label && ps.get(i).isStained()==stained) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns all points which are computationally guessed (estimated) and have a specific label and staining.
     * @param label The label (e.g. TMAspot.POS for cancerous nuclei).
     * @param staining The staining (e.g. TMAspot.STAINING_0).
     * @return All points which are computationally guessed (estimated) and have a specific label and staining.
     */
    public List<TMApoint> getPoints_Estimated(byte label, byte staining) {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (!ps.get(i).isGoldStandard() && ps.get(i).getLabel()==label && ps.get(i).getStaining()==staining) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns the points which are not used for training a classifier.
     * @param classification If true the classifier for nucleus classification is considered. Otherwise, the classifier for nucleus detection.
     * @return All points which are not used for training.
     */
    public List<TMApoint> getNonTrainingPoints(boolean classification) {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (!ps.get(i).isTrainingPoint(classification)) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns the points which are used for training a classifier.
     * @param classification If true the classifier for nucleus classification is considered. Otherwise, the classifier for nucleus detection.
     * @return All points which are used for training.
     */
    public List<TMApoint> getTrainingPoints(boolean classification) {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (int i=0; i<ps.size(); i++) {
            if (ps.get(i).isTrainingPoint(classification)) {
                gs.add(ps.get(i));
            }
        }
        return gs;
    }
    
    /**
     * Returns the consensus points which are used for training a classifier.
     * @param classification If true the classifier for nucleus classification is considered. Otherwise, the classifier for nucleus detection.
     * @return All points which are used for training and have the gold standard label TMApoint.CONSENSUS.
     */
    public List<TMApoint> getTrainingPoints_Consensus(boolean classification) {
        List<TMApoint> ps = getPoints();
        List<TMApoint> gs = new ArrayList<>();
        for (TMApoint tp: ps) {
            if (tp.getGoldStandard()==TMApoint.CONSENSUS && tp.isTrainingPoint(classification)) {
                gs.add(tp);
            }
        }
        return gs;
    }
    
    
    /**
     * Same as getHScore(null).
     * @return getHScore(null).
     */
    public double getHScore() {
        return getHScore(null);
    }
    
    /**
     * Calculates and returns the nucleus H-Score of this spot as 100*n_1 + 200*n_2 + 300*n_3, where
     * n_1 is the relative amount of 1+ nuclei, n_2 the relative amount of 2+ nuclei and n_3 the relative
     * amount of 3+ nuclei. Nuclei in excluding ROIs are ignored.
     * @param includingROI If null, the whole image is considered, otherwise, only points in this Polygon.
     * @return The nucleus H-Score of this spot.
     */
    public double getHScore(Polygon includingROI) {
        List<TMApoint> ps;
        if (includingROI == null) {
            ps = getPoints();
        } else {
            ps = getPointsInROI(includingROI);
        }
        double n_0 = 0;
        double n_1 = 0;
        double n_2 = 0;
        double n_3 = 0;
        for (TMApoint tp: ps) {
            if (getExcludingAreaOnPoint(tp.x, tp.y) == null) {
                switch (tp.getStaining()) {
                    case (TMALabel.STAINING_1): n_1++; break;
                    case (TMALabel.STAINING_2): n_2++; break;
                    case (TMALabel.STAINING_3): n_3++; break;
                    default: n_0++;        
                }
            }
        }
        double sum = n_0 + n_1 + n_2 + n_3;
        n_1 /= sum;
        n_2 /= sum;
        n_3 /= sum;
        
        double s = 100*n_1 + 200*n_2 + 300*n_3;
        
        // add the staining estimation as property        
        if (prop==null) {
            prop = new SortedProperties();
        }
        prop.setProperty("TMARKERNucleusHScore", Double.toString(s));
        return s;
    }

    
    /**
     * 
     * @return A vector with 14 doubles: acc, sens, spec, then confusion matrix entries 1/1, 1/2, 2/1, 2/2 (Cols actual class, Rows predicted class),
     * f-score, precision, recall, then confusion matrix entries 1/1, 1/2, 2/1, 2/2 (Cols actual class, Rows predicted class).
     * {-1, -1, -1, 0, 0, 0, 0, -1, -1, -1, 0, 0, 0, 0}, if there are no gold standard points or estimated points.
     */
    public double[] calculateMatchStatistics() {
        List<TMApoint> gs = getPoints_GoldStandard(TMALabel.LABEL_NEG);
        gs.addAll(getPoints_GoldStandard(TMALabel.LABEL_POS));
        gs.addAll(getPoints_GoldStandard(TMALabel.LABEL_UNK));
        List<TMApoint> es = getPoints_Estimated(TMALabel.LABEL_NEG);
        es.addAll(getPoints_Estimated(TMALabel.LABEL_POS));
        es.addAll(getPoints_Estimated(TMALabel.LABEL_UNK));
        // do respect the areas
        filter_centroids_on_Areas(gs, this);
        filter_centroids_on_Areas(es, this);
        
        // do not consider points on the border
        filter_centroids_on_border(gs, getWidth(), getHeight(), getCenter().getLabelRadius());
        filter_centroids_on_border(es, getWidth(), getHeight(), getCenter().getLabelRadius());
        
        return calculateMatchStatistics(gs, es, getParam_r());
    }
    
    /**
     * Removes TMApoints which would overlap according to the given radius.
     * @param blue_spots A set with nuclei. This set might be smaller after this filtering.
     * @param brown_spots Another set with nuclei. If this is the same set as blue_spots, it also changes the size. If not, this set will remain as is.
     * @param radius The radius of the nuclei.
     */
    public static void filter_centroids_on_distance(List<TMApoint> blue_spots, List<TMApoint> brown_spots, int radius) {
        if (blue_spots.equals(brown_spots)) {
            for (int i=blue_spots.size()-1; i>=0; i--) {
                for (int j=i-1; j>=0; j--) {
                    if (Math.sqrt(Math.pow(blue_spots.get(i).getX()-brown_spots.get(j).getX(),2) + Math.pow(blue_spots.get(i).getY()-brown_spots.get(j).getY(),2)) < 2*radius) {
                        blue_spots.remove(i);
                        break;
                    }
                }
            }
        } else {
            for (int i=blue_spots.size()-1; i>=0; i--) {
                for (int j=0; j<brown_spots.size(); j++) {
                    if (Math.sqrt(Math.pow(blue_spots.get(i).getX()-brown_spots.get(j).getX(),2) + Math.pow(blue_spots.get(i).getY()-brown_spots.get(j).getY(),2)) < 2*radius) {
                        blue_spots.remove(i);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Removes TMAspots which are close to the image border.
     * @param spots List of nuclei to be processed. Might be smaller after this filter.
     * @param width The image width.
     * @param height The image height.
     * @param border The border in pixels which should be free of nuclei.
     */
    public static void filter_centroids_on_border(List<TMApoint> spots, int width, int height, int border) {
        for (int i=spots.size()-1; i>=0; i--) {
            if (spots.get(i).x<border || spots.get(i).y<border || spots.get(i).x>=width-border || spots.get(i).y>=height-border) {
                spots.remove(i);
            }
        }
    }
    
    /**
     * Removes points outside of including areas or inside of excluding areas.
     * @param points The List of points to be processed. Might change the size.
     * @param ts The TMAspot which stores the including and excluding areas (as drawn by the user).
     */
    public static void filter_centroids_on_Areas(List<TMApoint> points, TMAspot ts) {
        // First: Retain only points in including areas.
        if (!ts.getIncludingAreas().isEmpty()) {
            boolean found;
            for (int i=points.size()-1; i>=0; i--) {
                found = false;
                for (Polygon p: ts.getIncludingAreas()) {
                    if (p.contains(points.get(i))) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    points.remove(i);
                }
            }
            //return;
        }
        // Second: Remove points in excluding areas.
        if (!ts.getExcludingAreas().isEmpty()) {
            for (Polygon p: ts.getExcludingAreas()) {
                for (int i=points.size()-1; i>=0; i--) {
                    if (p.contains(points.get(i))) {
                        points.remove(i);
                    }
                }
            }            
        }
    }
    
    /**
     * 
     * @return A vector with 14 doubles: acc, sens, spec, then confusion matrix entries 1/1, 1/2, 2/1, 2/2 (Cols actual class, Rows predicted class),
     * f-score, precision, recall, then confusion matrix entries 1/1, 1/2, 2/1, 2/2 (Cols actual class, Rows predicted class).
     * {-1, -1, -1, 0, 0, 0, 0, -1, -1, -1, 0, 0, 0, 0}, if there are no gold standard points or estimated points.
     */
    public double[] calculateMatchStatistics(List<TMApoint> gs, List<TMApoint> es, int radius) {
        // copy for indices
        List<TMApoint> gs_tmp_ = new ArrayList<>(); gs_tmp_.addAll(gs);
        List<TMApoint> es_tmp_ = new ArrayList<>(); es_tmp_.addAll(es);
        
        double[] stats = new double[]{-1, -1, -1, 0, 0, 0, 0, -1, -1, -1, 0, 0, 0, 0};
        
        if (tmarker.DEBUG>0) {
            String info = "CalculateMatchStatistics(): " + getName() + "\n";
            info += "  GS size:" + gs_tmp_.size() + "\n";
            info += "  ES size:" + es_tmp_.size() + "\n";
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, info);
        }
        
        if (gs_tmp_.isEmpty() || es_tmp_.isEmpty()) {
            return stats;
        }
        
        // copy for indices
        List<TMApoint> gs_tmp = new ArrayList<>(); gs_tmp.addAll(gs_tmp_);
        List<TMApoint> es_tmp = new ArrayList<>(); es_tmp.addAll(es_tmp_);
        
        // match the points
        boolean[] matched_gs = new boolean[gs_tmp_.size()];
        boolean[] matched_es = new boolean[es_tmp_.size()];
        Arrays.fill(matched_gs, false);
        Arrays.fill(matched_es, false);
        
        for (int i=gs_tmp.size()-1; i>=0; i--) {
            TMApoint gsp = gs_tmp.get(i);
            List<TMApoint> es_within_radius = getPointsWithinRadius(gsp.x, gsp.y, 2*radius, es_tmp);
            if (!es_within_radius.isEmpty()) {
                Collections.sort(es_within_radius, new TMApointComparator(gsp.x, gsp.y));
                TMApoint esp = null;
                for (TMApoint tp_es_:es_within_radius) {
                    //matched_es[es_tmp_.indexOf(tp_es_)] = true;
                    if (esp == null && (tp_es_.getLabel() == TMALabel.LABEL_UNK || gsp.getLabel() == tp_es_.getLabel())) {
                        esp = tp_es_;
                        //break;
                    }
                }
                if (esp == null) {
                    esp = es_within_radius.get(0);
                }
            
                //if (!matched_gs[i]) {
                    if      (gsp.getLabel() == TMALabel.LABEL_POS && esp.getLabel() == TMALabel.LABEL_POS) {
                        stats[3]++;
                    }
                    else if (gsp.getLabel() == TMALabel.LABEL_NEG && esp.getLabel() == TMALabel.LABEL_POS) {
                        stats[4]++;
                    }
                    else if (gsp.getLabel() == TMALabel.LABEL_POS && esp.getLabel() == TMALabel.LABEL_NEG) {
                        stats[5]++;
                    }
                    else if (gsp.getLabel() == TMALabel.LABEL_NEG && esp.getLabel() == TMALabel.LABEL_NEG) {
                        stats[6]++;
                    }
                    //matched++;
                    matched_gs[gs_tmp_.indexOf(gsp)] = true;
                    matched_es[es_tmp_.indexOf(esp)] = true;
                    gs_tmp.remove(gsp);
                    es_tmp.remove(esp);
                    //break;
                //}
                stats[3+7]++;
        
            } else {
                stats[5+7]++; 
            }
        }
             
        // for accuracy
        stats[0] = (stats[3] + stats[6]) / (stats[3] + stats[4] + stats[5] + stats[6]); // accuracy
        stats[1] = (stats[3]) / (stats[3] + stats[5]); // sensitivity
        stats[2] = (stats[6]) / (stats[6] + stats[4]); // specificity
        if (tmarker.DEBUG>0) {
            String info = "  Accuracy:\n";
            info += "  " + stats[3] + " ";
            info += "  " + stats[4] +"\n";
            info += "  " + stats[5] + " ";
            info += "  " + stats[6] + "\n";
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, info);
        }
        
        // for precision/recall/f-score
        //stats[3+7] = matched;
        stats[4+7] = countBooleanArray(matched_es, false); //es.size();
        //stats[5+7] = countBooleanArray(matched_gs, false); //gs.size();
        stats[6+7] = 0;
        stats[1+7] = stats[3+7] / (stats[3+7] + stats[4+7]);
        stats[2+7] = stats[3+7] / (stats[3+7] + stats[5+7]);
        stats[0+7] = 2 * (stats[1+7]*stats[2+7]) / (stats[1+7]+stats[2+7]);
        if (tmarker.DEBUG>0) {
            String info = "  F-Score:\n";
            info += "  " + stats[3+7] + " ";
            info += "  " + stats[4+7] + "\n";
            info += "  " + stats[5+7] + " ";
            info += "  " + stats[6+7] + "\n";
            if (tmarker.DEBUG>5) {
                for (TMApoint tp: es_tmp) {
                    tp.setTrainingsPoint(true, true);
                    info += "    Not matched: " + tp.getLabel() + " " + tp.x + " " + tp.y + "\n";
                }
                for (TMApoint tp: gs_tmp) {
                    tp.setTrainingsPoint(true, true);
                    info += "    Not matched: " + tp.getLabel() + " " + tp.x + " " + tp.y + "\n";
                }
            }
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, info);
        }
        
        return stats;
    }
    
    /**
     * Find all TMApoints in tps that lie within a radius r around point (x,y).
     * @param x The x coordinate of the center point.
     * @param y The y coordinate of the center point.
     * @param r The radius r, i.e. the maximum distance of valid TMApoints from (x,y).
     * @param tps The TMApoints in which valid points are searched.
     * @return A set of TMApoints (possibly empty), which lie within radius r around the point (x,y).
     */
    public static List<TMApoint> getPointsWithinRadius(int x, int y, int r, List<TMApoint> tps) {
        List<TMApoint> set = new ArrayList<>();
        for (int j=tps.size()-1; j>=0; j--) {
            if (Math.sqrt(Math.pow(x-tps.get(j).x, 2) + Math.pow(y-tps.get(j).y, 2)) <= r ) {
                set.add(tps.get(j));
            }
        }
        return set;
    }
    
    /**
     * Creates new nuclei based on the gold-standard nuclei and adds them to this TMAspot.
     * The new nuclei are consensus nuclei of the pathologists. If there is only one
     * labeller, all of them are consensus nuclei. If there are more labellers, 
     * consensus nuclei are those which have been detected by all labellers.
     * @param considerLabel If true, consensus nuclei are only those which have been detected and
     * classified equally by all labellers. If false, a consensus nucleus with ambiguous class
     * will get an LABEL_UNK label.
     */
    public void calculateConsensusPoints(boolean considerLabel) {
        deleteConsensusPoints();
        List<TMApoint> cps = new ArrayList<>();
        Set<Byte> labelers = getLabelers();
        int n = labelers.size();
        Byte[] labelers_ = labelers.toArray(new Byte[n]);
        ArrayList<ArrayList<TMApoint>> allLabelersPoints = new ArrayList<>(labelers.size());
        for (int i=0; i<n; i++) {
            allLabelersPoints.add(new ArrayList<TMApoint>());
            allLabelersPoints.get(i).addAll(getPoints_GoldStandard(labelers_[i], TMALabel.LABEL_NEG));
            allLabelersPoints.get(i).addAll(getPoints_GoldStandard(labelers_[i], TMALabel.LABEL_POS));
            allLabelersPoints.get(i).addAll(getPoints_GoldStandard(labelers_[i], TMALabel.LABEL_UNK));
            allLabelersPoints.get(i).addAll(getPoints_GoldStandard(labelers_[i], TMALabel.LABEL_BG));
        }
        
        if (tmarker.DEBUG>0) {
            String info = getName() + ": calculateConsensusPoints()\n";
            for (int i=0; i<allLabelersPoints.size(); i++) {
                info += "  Labeler " + labelers_[i] + " size:" + allLabelersPoints.get(i).size() + "\n";
            }
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, info);
        }
        
        if (allLabelersPoints.isEmpty()) {
            return;
        }
        // for the first labeler only... (all consensus points can also be found in the first labeler points)...
        int k=0; //for (int k=0; k<n; k++) {
        boolean foundInAllLabelers, foundInLabeler;
        // ... go through all his points...
        for (int i=allLabelersPoints.get(k).size()-1; i>=0; i--) {
            TMApoint tpa = (TMApoint) allLabelersPoints.get(k).get(i);
            TMApoint tp = tpa.clone();
            //  tp.setWeight(1.0/(double)n);
            tp.setGoldStandard(TMApoint.CONSENSUS);
            foundInAllLabelers = true;
            // ... and for each point, go through all other labelers
            for (int m=k+1; m<n; m++) {
                foundInLabeler = false;
                // ... and through their points ...
                for (int j=allLabelersPoints.get(m).size()-1; j>=0; j--) {
                    TMApoint tpb = (TMApoint) allLabelersPoints.get(m).get(j);
                    // ... and if a match is found (pairwise match) ...
                    if ( Math.sqrt(Math.pow(tpa.x-tpb.x, 2) + Math.pow(tpa.y-tpb.y, 2)) < 2*getParam_r()) {
                        if (!considerLabel || tpa.getLabel() == tpb.getLabel()) {
                       //     tp.setWeight(tp.getWeight() + 1.0/(double)n);
                            // ... do midpoint shifting
                            tp.x = (tp.x + tpb.x) / 2;
                            tp.y = (tp.y + tpb.y) / 2;
                            if (!considerLabel && tp.getLabel() != tpb.getLabel()) {
                                tp.setLabel(TMALabel.LABEL_UNK);
                            }
                            allLabelersPoints.get(m).remove(j);  
                            foundInLabeler = true;
                            break;
                        }                    
                    }
                }
                foundInAllLabelers = foundInAllLabelers && foundInLabeler;
            }
            if (foundInAllLabelers) cps.add(tp);
        }
        //}
        
        nuclei.addAll(cps);
    }
    
    /**
     * Deletes all nuclei with glod-standard TMApoint.CONSENSUS.
     */
    private void deleteConsensusPoints() {
        for (int i=getPoints().size()-1; i>=0; i--) {
            if (getPoints().get(i).getGoldStandard() == TMApoint.CONSENSUS) {
                getPoints().remove(i);
            }
        }
    }
    
    /**
     * Deletes all gold-standard nuclei with glod-standard other than TMApoint.CONSENSUS.
     */
    public void deleteNonConsensusGSPoints() {
        for (int i=getPoints().size()-1; i>=0; i--) {
            if (getPoints().get(i).isGoldStandard() && getPoints().get(i).getGoldStandard() != TMApoint.CONSENSUS) {
                getPoints().remove(i);
            }
        }
    }
    
    /**
     * Counts the number of entries in array which are "b". E.g. countBooleanArray([T, F, F], T) = 1 and
     * countBooleanArray([T, F, F], F) = 2.
     * @param array The boolean array.
     * @param b The boolean to count.
     * @return The number of "b" in array.
     */
    public static int countBooleanArray(boolean[] array, boolean b) {
        int i = 0;
        for (int j=0; j<array.length; j++) {
            if (array[j]==b) {
                i++;
            }
        }
        return i;
    }
    
    /**
     * Deletes all estimated points and duplicates all gold-standard points as
     * new estimated points with label LABEL_UNK.
     */
    public void copyGoldStandardAsUnknownEstimated() {
        deleteAllPoints_ES();
        List<TMApoint> tps = new ArrayList<>();
        for (TMApoint tp: getPoints()) {
            TMApoint tpnew = new TMApoint(this, tp.x, tp.y, TMALabel.LABEL_UNK);
            tps.add(tpnew);
        }
        addPoints(tps);
    }

    /**
     * Returns the image of this TMAspot. Note: for large image format (SVS, NDPI or other pyramid images), this returns a kind of thumbnail whose size
     * regards the currently available memory, and 
     * @return The image of this TMAspot as a new BufferedImage.
     */
    public BufferedImage getBufferedImage() {
        BufferedImage bi = null;
        
        if (isNDPI()) {
            int maxbytes = (int) (0.8 *Runtime.getRuntime().maxMemory());
            int levels = getNDPI().getLevelCount();
            for(int i = 0; i<levels; i++) {
                if (maxbytes > 4 * getNDPI().getLevelWidth(i) * getNDPI().getLevelHeight(i)) {
                    try {
                        bi = getNDPI().createThumbnailImage((int) Math.max(getNDPI().getLevelWidth(i), getNDPI().getLevelHeight(i)));
                        break;
                    } catch (IOException | Error ex) {
                        if (tmarker.DEBUG>2) {
                            Logger.getLogger(TMAspot.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
	} else {
            //if (!asNewInstance && getCenter().getVisibleTMAspot()==this && getCenter().getTMAView().getImage()!=null) {
            //    bi = (BufferedImage) getCenter().getTMAView().getImage();
            //} else {
                try {
                    //bi = ImageIO.read(new File(getOriginalImagename(whichImage)));
                    bi = Misc.loadImageFast(getImagename());
                } catch (Exception ex) {
                    if (tmarker.DEBUG>0) {
                        Logger.getLogger(TMAspot.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    JOptionPane.showMessageDialog(getCenter(), "An error occurred while opening " + getImagename() + "\n\n"
                        + "Maybe the thread that generates this image has not finished, yet\n"
                        + "(e.g. after color deconvolution).", "Error opening image", JOptionPane.ERROR_MESSAGE);
                }
            //}
        }
        return bi;
    }
    
    /**
     * Returns a subimage of this TMAspot.
     * @param x The top left x coordinate.
     * @param y The top left y coordinate.
     * @param w The width.
     * @param h The height.
     * @param maxsize The maximum edge length (any edge) of the image. Used for NDPI images (openSlide).
     * @param fullImage The full buffered image of the TMAspot, if this a normal image (not NDPI). If this is null, getBufferedImage() will be called, otherwise fullImage will be used (faster).
     * @return A new BufferedImage, subimage of this TMAspot.
     */
    public BufferedImage getSubimage(int x, int y, int w, int h, int maxsize, BufferedImage fullImage) {
        return getSubimage(x, y, w, h, maxsize, BufferedImage.TYPE_INT_ARGB, fullImage);
    }
    
    /**
     * Returns a subimage of this TMAspot.
     * @param x The top left x coordinate.
     * @param y The top left y coordinate.
     * @param w The width.
     * @param h The height.
     * @param maxsize The maximum edge length (any edge) of the image. Used for NDPI images (openSlide).
     * @param bufferedImageType The bufferedImageType (e.g. BufferedImage.TYPE_INT_ARGB).
     * @param fullImage The full buffered image of the TMAspot, if this a normal image (not NDPI). If this is null, getBufferedImage() will be called, otherwise fullImage will be used (faster).
     * @return A new BufferedImage, subimage of this TMAspot.
     */
    public BufferedImage getSubimage(int x, int y, int w, int h, int maxsize, int bufferedImageType, BufferedImage fullImage) {
        BufferedImage img;
        if (isNDPI()) {
            try {
                img = getNDPI().createThumbnailImage(x, y, w, h, maxsize, bufferedImageType);
            } catch (IOException ex) {
                img = null;
                if (tmarker.DEBUG>0) {
                    Logger.getLogger(TMAspot.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            if (fullImage != null) {
                img = fullImage.getSubimage(x, y, w, h);
            } else {
                img = getBufferedImage().getSubimage(x, y, w, h);
            }
            if (img.getType() != bufferedImageType) {
                BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), bufferedImageType);
                convertedImg.getGraphics().drawImage(img, 0, 0, null);
                img = convertedImg;
            }
        }
        return img;
    }

    /**
     * Removes the including and excluding areas which contain the given point.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    public void deletePolygonOnPoint(int x, int y) {
        for (Polygon p: getIncludingAreas()) {
            if (p.contains(x, y)) {
                getIncludingAreas().remove(p);
                return;
            }
        }
        for (Polygon p: getExcludingAreas()) {
            if (p.contains(x, y)) {
                getExcludingAreas().remove(p);
                return;
            }
        }
    }
    
    /**
     * Switches the including area on point x,y to excluding area and vice versa.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    public void switchPolygonOnPoint(int x, int y) {
        for (Polygon p: getIncludingAreas()) {
            if (p.contains(x, y)) {
                getIncludingAreas().remove(p);
                getExcludingAreas().add(p);
                return;
            }
        }
        for (Polygon p: getExcludingAreas()) {
            if (p.contains(x, y)) {
                getExcludingAreas().remove(p);
                getIncludingAreas().add(p);
                return;
            }
        }
    }

    /**
     * Sets the properties (parameters) of this TMAspot.
     * @param prop The properties (parameters) of this TMAspot.
     */
    public void setProperties(SortedProperties prop) {
        this.prop = prop;
    }
    
    /**
     * Adds properties (parameters) to this TMAspot.
     * @param prop The properties (parameters) to be added.
     */
    public void addProperties(SortedProperties prop) {
        if (this.prop==null) {
            this.prop = new SortedProperties();
        }
        this.prop.putAll(prop);
    }
    
    /**
     * Adds a property (parameter) to this TMAspot.
     * @param key The property name (parameter) to be added.
     * @param value The property value (parameter) to be added.
     */
    public void addProperty(String key, String value) {
        if (this.prop==null) {
            this.prop = new SortedProperties();
        }
        this.prop.put(key, value);
    }
    
    /**
     * Gets the properties (parameters) of this TMAspot.
     * @return The properties (parameters) of this TMAspot.
     */
    public SortedProperties getProperties() {
        if (prop==null) {
            prop = new SortedProperties();
        }
        return prop;
    }
    
    /**
     * Returns whether this TMAspot has any including or excluding area drawn by a user.
     * @return True if there is at least one including or excluding area.
     */
    public boolean hasArea() {
        return (!getIncludingAreas().isEmpty() || ! getExcludingAreas().isEmpty());
    }

    /**
     * Returns the width of this TMAspot.
     * @return The width of this TMAspot.
     */
    public int getWidth() {
        if (w < 0) {
            try {
                if (isNDPI()) {
                    if (os!=null) {
                        w = (int) os.getLevel0Width();
                    }
                } else {
                    ImageInputStream in = ImageIO.createImageInputStream(new File(getImagename()));
                    final Iterator readers = ImageIO.getImageReaders(in);
                    if (readers.hasNext()) {
                        ImageReader reader = (ImageReader) readers.next();
                        try {
                            reader.setInput(in);
                            w = reader.getWidth(0);
                        } finally {
                            reader.dispose();
                        }
                    }
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (Exception e) {
                if (tmarker.DEBUG>0) {
                    e.printStackTrace();
                }
            }
        }
        return w;
    }
    
    /**
     * Returns the height of this TMAspot.
     * @return The height of this TMAspot.
     */
    public int getHeight() {
        if (h < 0) {
            try {
                if (isNDPI()) {
                    if (os!=null) {
                        h = (int) os.getLevel0Height();
                    }
                } else {
                    ImageInputStream in = ImageIO.createImageInputStream(new File(getImagename()));
                    final Iterator readers = ImageIO.getImageReaders(in);
                    if (readers.hasNext()) {
                        ImageReader reader = (ImageReader) readers.next();
                        try {
                            reader.setInput(in);
                            h = reader.getHeight(0);
                        } finally {
                            reader.dispose();
                        }
                    }
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (Exception e) {
                if (tmarker.DEBUG>0) {
                    e.printStackTrace();
                }
            }
        }
        return h;
    }

    /**
     * Returns the set of different labeller numbers which have been annotated the image already.
     * The consensus labeling is not included.
     * @return A set of labeller numbers.
     */
    public Set<Byte> getLabelers() {
        Set<Byte> set = new ArraySet<>();
        for (TMApoint tp: getPoints_GoldStandard()) {
            if (tp.getGoldStandard()!=TMApoint.CONSENSUS) {
                set.add(tp.getGoldStandard());
            }
        }
        return set;
    }

    /**
     * Returns the feature vector of this TMAspot which is used for spot intensity clustering (experimental).
     * @return This spots' feature vector. If not existent, it is created.
     */
    public double[] getFeatureVector_IntensityClustering() {
        if (features==null) {
            getCenter().setStatusMessageLabel(getName() + ": Calculate Feature Vector ...");
            /*
            // POSSIBILITY 1: Feature vector is composed of nuclei intensities
            int nbins = 256;
            double[][] values = getIntensities(TMAspot.LABEL_POS, false);
            double[] vals = new double[7+4];
            int[] hist_r = Misc.histogram(values[0], 0, 255, nbins);
            int[] hist_g = Misc.histogram(values[1], 0, 255, nbins);
            int[] hist_b = Misc.histogram(values[2], 0, 255, nbins);
            double [] params;
            GaussianFitter gf1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter gf2 = new GaussianFitter(new GaussNewtonOptimizer(false));
            GaussianFitter gf3 = new GaussianFitter(new GaussNewtonOptimizer(true));
            for (int i=0; i<nbins; i++) {
                gf1.addObservedPoint(i, hist_r[i]);
                gf2.addObservedPoint(i, hist_r[i]);
                gf3.addObservedPoint(i, hist_r[i]);
            }
            try {
                params = gf1.fit();
            } catch (Exception e1) {
                try {
                    params = gf2.fit();
                } catch (Exception e2) {
                    params = gf3.fit();
                }
            }
            System.arraycopy(params, 1, vals, 1, 2);
            gf1.clearObservations(); gf2.clearObservations(); gf3.clearObservations();
            for (int i=0; i<nbins; i++) {
                gf1.addObservedPoint(i, hist_g[i]);
                gf2.addObservedPoint(i, hist_g[i]);
                gf3.addObservedPoint(i, hist_g[i]);
            }
            try {
                params = gf1.fit();
            } catch (Exception e1) {
                try {
                    params = gf2.fit();
                } catch (Exception e2) {
                    params = gf3.fit();
                }
            }
            System.arraycopy(params, 1, vals, 3, 2);
            gf1.clearObservations(); gf2.clearObservations(); gf3.clearObservations();
            for (int i=0; i<nbins; i++) {
                gf1.addObservedPoint(i, hist_b[i]);
                gf2.addObservedPoint(i, hist_b[i]);
                gf3.addObservedPoint(i, hist_b[i]);
            }
            try {
                params = gf1.fit();
            } catch (Exception e1) {
                try {
                    params = gf2.fit();
                } catch (Exception e2) {
                    params = gf3.fit();
                }
            }
            System.arraycopy(params, 1, vals, 5, 2);

            // feature 7-10 are the relative amount of stained nuclei
            int n = 0;
            int m = getPoints_Estimated(TMAspot.LABEL_POS, STAINING_0).size(); vals[7] = m; n+=m;
            m = getPoints_Estimated(TMAspot.LABEL_POS, STAINING_1).size(); vals[8] = m; n+=m;
            m = getPoints_Estimated(TMAspot.LABEL_POS, STAINING_2).size(); vals[9] = m; n+=m;
            m = getPoints_Estimated(TMAspot.LABEL_POS, STAINING_3).size(); vals[10] = m; n+=m;
            if (n>0) {
                vals[7]/=n;
                vals[8]/=n;
                vals[9]/=n;
                vals[10]/=n;
            }
            features = vals;
            /*/
            // POSSIBILITY 2: FEATURE VECTOR IS ONLY COMPOSED ON IMAGE COLOR INFORMATION
            int patchsize = 100;
            //List<Rectangle> patches = PatchizeImage(this, 100, 100);
            BufferedImage BI = getBufferedImage();
            BufferedImage I = Scalr.resize(BI, BI.getWidth()/patchsize);
            if (tmarker.DEBUG>4) {
                Misc.writeImage(I, this.getTmpDir() + File.separator + getName() + "_featurevector.png");
            }
            
            // Find out the entropies of the patches. Use only those patches with low entropy
            // (other are background)
            double entr_thres = 240;
            BufferedImage BI_gray = Misc.convertToGrayscale(BI);
            double[][] Entropies = new double[I.getWidth()][I.getHeight()];
            double[] entropy = new double[(patchsize)*(patchsize)];
            for (int i=0; i<I.getWidth(); i++) {
                for (int j=0; j<I.getHeight(); j++) {
                    Entropies[i][j] = Misc.entropy(BI_gray.getRaster().getSamples(patchsize*i, patchsize*j, patchsize, patchsize, 0, entropy));
                }
            }
            BI_gray.flush();
            double entr_max = Misc.max(Entropies);
            double entr_min = Misc.min(Entropies);
            for (int i=0; i<I.getWidth(); i++) {
                for (int j=0; j<I.getHeight(); j++) {
                    Entropies[i][j] = (Entropies[i][j]-entr_min)*255/(entr_max-entr_min);
                }
            }
            if (tmarker.DEBUG>4) {
                Misc.writeImage(Entropies, false, this.getTmpDir() + File.separator + getName() + "_entropies.png");
            }
            
            /*double[] grays = new double[I.getWidth()*I.getHeight()];
            double[] reds = new double[I.getWidth()*I.getHeight()];
            double[] greens = new double[I.getWidth()*I.getHeight()];
            double[] blues = new double[I.getWidth()*I.getHeight()];
            */
            List<Double> grays = new ArrayList();
            List<Double> reds = new ArrayList();
            List<Double> greens = new ArrayList();
            List<Double> blues = new ArrayList();
            double[] pixel = new double[4];
            String info = "";
            for (int i=0; i<I.getWidth(); i++) {
                if (tmarker.DEBUG>4) info += "\n";
                for (int j=0; j<I.getHeight(); j++) {
                    if (Entropies[i][j]<entr_thres) {
                        if (tmarker.DEBUG>4) info += "8";
                        I.getData().getPixel(i, j, pixel);
                        reds.add(pixel[0]);
                        greens.add(pixel[1]);
                        blues.add(pixel[2]);
                        grays.add(0.21*pixel[0] + 0.71*pixel[1] + 0.07*pixel[2]);
                    } else {
                        if (tmarker.DEBUG>4) info += ".";
                    }
                }
            }
            if (tmarker.DEBUG>4) java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, info);
            
            // Convert List<Double> to double[]
            double[] areds = new double[grays.size()];
            double[] agreens = new double[grays.size()];
            double[] ablues = new double[grays.size()];
            double[] agrays = new double[grays.size()];
            for(int i = 0; i < grays.size(); i++) {
                areds[i] = reds.get(i);
                agreens[i] = greens.get(i);
                ablues[i] = blues.get(i);
                agrays[i] = grays.get(i);
            }
            BI.flush();
            I.flush();
            Median median = new Median();
            features = new double[]{median.evaluate(areds), median.evaluate(agreens), median.evaluate(ablues)};//, median.evaluate(agrays)};
            //*/
        }
        return features;
    }
    
    /**
     * Deletes nuclei in a given area.
     * @param pol The area.
     */
    public void deletePointsInArea(Polygon pol) {
        List<TMApoint> tps = getPoints();
        for (int i=tps.size()-1; i>=0; i--) {
            if (pol.contains(tps.get(i).x, tps.get(i).y)) {
                tps.remove(i);
            }
        }
    }

    /**
     * Returns the area which contains the point (x, y). An including area is preferred
     * over an excluding area.
     * @param x The x-coord.
     * @param y The y-coord.
     * @return The area which contains (x,y) or null if there is none.
     */
    public Polygon getAreaOnPoint(int x, int y) {
        return getAreaOnPoint(x, y, 0);
    }
    
    /**
     * Returns the area which contains the point (x, y). An including area is preferred
     * over an excluding area.
     * @param x The x-coord.
     * @param y The y-coord.
     * @param tolerance If > 0, finds also polygons which lie beside the coordinates with offset tolerance.
     * @return The area which contains (x,y) or null if there is none.
     */
    public Polygon getAreaOnPoint(int x, int y, int tolerance) {
        Polygon pol = getIncludingAreaOnPoint(x, y, tolerance);
        if (pol==null) {
            return getExcludingAreaOnPoint(x, y, tolerance);
        } else {
            return pol;
        }
    }
    
    /**
     * Returns the including area which contains the point (x, y).
     * @param x The x-coord.
     * @param y The y-coord.
     * @return The area which contains (x,y) or null if there is none.
     */
    public Polygon getIncludingAreaOnPoint(int x, int y) {
        return getIncludingAreaOnPoint(x, y, 0);
    }
    
    /**
     * Returns the including area which contains the point (x, y).
     * @param x The x-coord.
     * @param y The y-coord.
     * @param tolerance If > 0, finds also polygons which lie beside the coordinates with offset tolerance.
     * @return The area which contains (x,y) or null if there is none.
     */
    public Polygon getIncludingAreaOnPoint(int x, int y, int tolerance) {
        List<Polygon> pols = getIncludingAreas();
        for (Polygon pol : pols) {
            if (pol.contains(x, y) || pol.contains(x+tolerance, y) || pol.contains(x, y+tolerance) || pol.contains(x+tolerance, y+tolerance)
                    || pol.contains(x-tolerance, y) || pol.contains(x, y-tolerance) || pol.contains(x-tolerance, y-tolerance)
                    || pol.contains(x-tolerance, y+tolerance) || pol.contains(x+tolerance, y-tolerance)) {
                return pol;
            }
        }
        return null;
    }
    
    /**
     * Returns the excluding area which contains the point (x, y).
     * @param x The x-coord.
     * @param y The y-coord.
     * @return The area which contains (x,y) or null if there is none.
     */
    public Polygon getExcludingAreaOnPoint(int x, int y) {
        return getExcludingAreaOnPoint(x, y, 0);
    }
    
    /**
     * Returns the excluding area which contains the point (x, y).
     * @param x The x-coord.
     * @param y The y-coord.
     * @param tolerance If > 0, finds also polygons which lie beside the coordinates with offset tolerance.
     * @return The area which contains (x,y) or null if there is none.
     */
    public Polygon getExcludingAreaOnPoint(int x, int y, int tolerance) {
        List<Polygon> pols = getExcludingAreas();
        for (Polygon pol : pols) {
            if (pol.contains(x, y) || pol.contains(x+tolerance, y) || pol.contains(x, y+tolerance) || pol.contains(x+tolerance, y+tolerance)
                    || pol.contains(x-tolerance, y) || pol.contains(x, y-tolerance) || pol.contains(x-tolerance, y-tolerance)
                    || pol.contains(x-tolerance, y+tolerance) || pol.contains(x+tolerance, y-tolerance)) {
                return pol;
            }
        }
        return null;
    }

    /**
     * Experimental: return the ID of this spot. Here the ID is created from the filename. 
     * If this does not work, it is the index of itself in the list of TMAspots in this TMARKER session.
     * Still, a unique ID which is unique also over serveral TMARKER sessions still has to be implemented here.
     * @return The (hopefully) unique ID of this TMAspot.
     */
    public int getID() {
        int id;
        try {
            id = Integer.parseInt(Misc.FilePathStringtoFilenameWOExtension(getName()).replaceAll("spots_", "").replaceAll("_top_left", "").replaceAll("prostate_cancer_mib_validation_", "").replaceAll("\\D+",""));
        } catch (Exception ex) {
            id = tc.getTMAspots().indexOf(this);
        }
        return id;
    }

    /**
     * Returns a thumbnail of this TMAspot of the given max width or max height.
     * @param w_max Max width of the thumbnail image.
     * @param h_max Max height of the thumbnail image.
     * @param factor If not null and not empty, the resize factor is written into factor[0]. Can be null.
     * @return A new BufferedImage as a thumbnail of this TMAspot.
     */
    public Image getThumbnailImage(int w_max, int h_max, double [] factor) {
        if (isNDPI()) {
            try {
                BufferedImage thumbnail = getNDPI().createThumbnailImage(Math.min(w_max, h_max));
                if (factor!=null && factor.length>0) {
                    factor[0] = 1.0*thumbnail.getWidth()/getNDPI().getLevel0Width();
                }
                return thumbnail;
            } catch (IOException ex) {
                return null;
            }
        } else {
            double[] f = new double [] {1.0};
            return Misc.getScaledImageWithMaxSize(getBufferedImage(), w_max, h_max, factor);
        }
    }

    /**
    * Comparator for the distance of two TMApoint o1 and o2 to a given point (x|y)
     */
    class TMApointComparator implements Comparator<TMApoint> {

        int x, y;
        
        /**
        * Comparator for the distance of two TMApoint o1 and o2 to a given point (x|y)
        */
        public TMApointComparator(int x, int y) {
            this.x=x;
            this.y=y;
        }
        
        /**
        * Comparator for the distance of two TMApoint o1 and o2 to a given point (x|y)
        */
        @Override
        public int compare(TMApoint o1, TMApoint o2) {
            return (int) Math.signum(Math.sqrt(Math.pow(o2.x-x, 2) + Math.pow(o2.y-y, 2)) - Math.sqrt(Math.pow(o1.x-x, 2) + Math.pow(o1.y-y, 2)));
        }
    }
        
}

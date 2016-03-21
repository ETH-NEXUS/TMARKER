/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package superpixels;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import tmarker.TMAspot.TMALabel;
import tmarker.misc.Misc;
import tmarker.tmarker;

/*
 * Adopted from
 * Radhakrishna Achanta, Appu Shaji, Kevin Smith, Aurelien Lucchi, Pascal Fua, and Sabine Süsstrunk
 * SLIC Superpixels Compared to State-of-the-art Superpixel Methods
 * IEEE Transactions on Pattern Analysis and Machine Intelligence, vol. 34, num. 11, p. 2274 - 2282, May 2012.
 * 
 * Radhakrishna Achanta, Appu Shaji, Kevin Smith, Aurelien Lucchi, Pascal Fua, and Sabine Süsstrunk
 * SLIC Superpixels
 * EPFL Technical Report no. 149300, June 2010.
 *
 * (http://ivrg.epfl.ch/research/superpixels)
 * 
 * Translated to Java code by Peter J. Schüffler and used in 
 * Peter J. Schüffler, Thomas J. Fuchs, Cheng Soon Ong, Peter J. Wild, Niels J. Rupp, Joachim M. Buhmann
 * TMARKER: A free software toolkit for histopathological cell counting and staining estimation.
 * J Pathol Inform 2013, 4:2, doi 10.4103/2153-3539.109804
 * (http://www.comp-path.inf.ethz.ch/)
 * 
 *
 */
public class SLIC {

    // For superpixels
    private int[] dx4 = {-1, 0, 1, 0};
    private int[] dy4 = {0, -1, 0, 1};
    //private int[] dx8 = {-1, -1,  0,  1, 1, 1, 0, -1};
    //private int[] dy8 = { 0, -1, -1, -1, 0, 1, 1,  1};
    private int m_width;
    private int m_height;
    private double[] m_lvec = null;
    private double[] m_avec = null;
    private double[] m_bvec = null;

    //==============================================================================
    ///	RGB2XYZ
    ///
    /// sRGB (D65 illuninant assumption) to XYZ conversion
    //==============================================================================
    /*void RGB2XYZ(int sR, int sG, int sB, double[] output) {
        double R = sR / 255.0;
        double G = sG / 255.0;
        double B = sB / 255.0;

        double r, g, b;

        if (R <= 0.04045) {
            r = R / 12.92;
        } else {
            r = Math.pow((R + 0.055) / 1.055, 2.4);
        }
        if (G <= 0.04045) {
            g = G / 12.92;
        } else {
            g = Math.pow((G + 0.055) / 1.055, 2.4);
        }
        if (B <= 0.04045) {
            b = B / 12.92;
        } else {
            b = Math.pow((B + 0.055) / 1.055, 2.4);
        }

        double X = r * 0.4124564 + g * 0.3575761 + b * 0.1804375;
        double Y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750;
        double Z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041;

        output[0] = X;
        output[1] = Y;
        output[2] = Z;
        //return (new double[]{X, Y, Z});
    }*/

    //===========================================================================
    ///	RGB2LAB
    //===========================================================================
    /*void RGB2LAB(int sR, int sG, int sB, double[] output, double[] XYZ) {
        //------------------------
        // sRGB to XYZ conversion
        //------------------------
        RGB2XYZ(sR, sG, sB, XYZ);
        double X = XYZ[0], Y = XYZ[1], Z = XYZ[2];
        //------------------------
        // XYZ to LAB conversion
        //------------------------
        double epsilon = 0.008856;	//actual CIE standard
        double kappa = 903.3;		//actual CIE standard

        double Xr = 0.950456;	//reference white
        double Yr = 1.0;	//reference white
        double Zr = 1.088754;	//reference white

        double xr = X / Xr;
        double yr = Y / Yr;
        double zr = Z / Zr;

        double fx, fy, fz;
        if (xr > epsilon) {
            fx = Math.pow(xr, 1.0 / 3.0);
        } else {
            fx = (kappa * xr + 16.0) / 116.0;
        }
        if (yr > epsilon) {
            fy = Math.pow(yr, 1.0 / 3.0);
        } else {
            fy = (kappa * yr + 16.0) / 116.0;
        }
        if (zr > epsilon) {
            fz = Math.pow(zr, 1.0 / 3.0);
        } else {
            fz = (kappa * zr + 16.0) / 116.0;
        }

        double lval = 116.0 * fy - 16.0;
        double aval = 500.0 * (fx - fy);
        double bval = 200.0 * (fy - fz);
        
        output[0] = lval;
        output[1] = aval;
        output[2] = bval;
    }*/
    
    //==============================================================================
    ///	RGB2XYZ
    ///
    /// sRGB (D65 illuninant assumption) to XYZ conversion
    //==============================================================================
    void RGB2XYZ(int sR, int sG, int sB, double[] output, double[] p) {
        // p are params
        // 0 1 2 3 4 5 6 7 8 9       10    11 12 13 14 15 16 17 18 19
        // R G B r g b X Y Z epsilon kappa Xr Yr Zr xr yr zr fx fy fz
       
        p[0] = sR / 255.0;
        p[1] = sG / 255.0;
        p[2] = sB / 255.0;

        if (p[0] <= 0.04045) {
            p[3] = p[0] / 12.92;
        } else {
            p[3] = Math.pow((p[0] + 0.055) / 1.055, 2.4);
        }
        if (p[1] <= 0.04045) {
            p[4] = p[1] / 12.92;
        } else {
            p[4] = Math.pow((p[1] + 0.055) / 1.055, 2.4);
        }
        if (p[2] <= 0.04045) {
            p[5] = p[2] / 12.92;
        } else {
            p[5] = Math.pow((p[2] + 0.055) / 1.055, 2.4);
        }

        output[0] = p[3] * 0.4124564 + p[4] * 0.3575761 + p[5] * 0.1804375;
        output[1] = p[3] * 0.2126729 + p[4] * 0.7151522 + p[5] * 0.0721750;
        output[2] = p[3] * 0.0193339 + p[4] * 0.1191920 + p[5] * 0.9503041;
        //return (new double[]{X, Y, Z});
    }

    //===========================================================================
    ///	RGB2LAB
    //===========================================================================
    void RGB2LAB(int sR, int sG, int sB, double[] output, double[] XYZ, double[] p) {
        // p are params
        // 0 1 2 3 4 5 6 7 8 9       10    11 12 13 14 15 16 17 18 19
        // R G B r g b X Y Z epsilon kappa Xr Yr Zr xr yr zr fx fy fz
       
        //------------------------
        // sRGB to XYZ conversion
        //------------------------
        RGB2XYZ(sR, sG, sB, XYZ, p);
        
        //------------------------
        // XYZ to LAB conversion
        //------------------------
        p[14] = XYZ[0] / p[11];
        p[15] = XYZ[1] / p[12];
        p[16] = XYZ[2] / p[13];

        if (p[14] > p[9]) {
            p[17] = Math.pow(p[14], 1.0 / 3.0);
        } else {
            p[17] = (p[10] * p[14] + 16.0) / 116.0;
        }
        if (p[15] > p[9]) {
            p[18] = Math.pow(p[15], 1.0 / 3.0);
        } else {
            p[18] = (p[10] * p[15] + 16.0) / 116.0;
        }
        if (p[16] > p[9]) {
            p[19] = Math.pow(p[16], 1.0 / 3.0);
        } else {
            p[19] = (p[10] * p[16] + 16.0) / 116.0;
        }

        output[0] = 116.0 * p[18] - 16.0;
        output[1] = 500.0 * (p[17] - p[18]);
        output[2] = 200.0 * (p[18] - p[19]);
    }

    //===========================================================================
    ///	DoRGBtoLABConversion
    ///
    ///	For whole image: overlaoded floating point version
    //===========================================================================
    void DoRGBtoLABConversion(SLICsuperpixels slicsp, int[] ubuff, double[] lvec, double[] avec, double[] bvec) {
        int sz = m_width * m_height;

        double[] LAB = new double[3];
        double[] container = new double[3];
        double[] p = new double[20];
        // 0 1 2 3 4 5 6 7 8 9       10    11 12 13 14 15 16 17 18 19
        // R G B r g b X Y Z epsilon kappa Xr Yr Zr xr yr zr fx fy fz
        p[9] =  0.008856;	//actual CIE standard
        p[10] = 903.3;		//actual CIE standard
        p[11] = 0.950456;	//reference white
        p[12] = 1.0;            //reference white
        p[13] = 1.088754;	//reference white

        int i = 0, r, g, b;
        for (int j = 0; j < sz; j++) {
            if (10*j/sz > i) {
                if (slicsp.verbose) {slicsp.getTMAspot().getCenter().setProgressbar((int) (30 + 10*j/sz));}
                i++;
            }
            r = (ubuff[j] >> 16) & 0xFF;
            g = (ubuff[j] >>  8) & 0xFF;
            b = (ubuff[j]      ) & 0xFF;

            RGB2LAB(r, g, b, LAB, container, p);
            lvec[j] = LAB[0];
            avec[j] = LAB[1];
            bvec[j] = LAB[2];
        }
    }

//=================================================================================
/// DrawContoursAroundSegments
///
/// Internal contour drawing option exists. One only needs to comment the if
/// statement inside the loop that looks at neighbourhood.
//=================================================================================
    void DrawContoursAroundSegments(int[] ubuff, int[] labels, int width, int height, int color) {
        int[] dx8 = {-1, -1, 0, 1, 1, 1, 0, -1};
        int[] dy8 = {0, -1, -1, -1, 0, 1, 1, 1};
        
        int[] col_tmp = new int[8];

        int sz = width * height;

        boolean[] istaken = new boolean[sz];
        for (int i = 0; i < sz; i++) {
            istaken[i] = false;
        }

        int mainindex = 0;
        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                if (labels[mainindex] < 0) {
                    ubuff[mainindex] = CalculateNewColor(ubuff[mainindex], color, 1, col_tmp);
                    //istaken[mainindex] = true;
                } /*else*/ {
                    int np = 0;
                    for (int i = 0; i < 8; i++) {
                        int x = k + dx8[i];
                        int y = j + dy8[i];

                        if ((x >= 0 && x < width) && (y >= 0 && y < height)) {
                            int index = y * width + x;

                            if (false == istaken[index])//comment this to obtain internal contours
                            {
                                if (labels[mainindex] != labels[index]) {
                                    np++;
                                }
                            }
                        }
                    }
                    if (np > 1)//change to 2 or 3 for thinner lines
                    {
                        ubuff[mainindex] = CalculateNewColor(ubuff[mainindex], color, 1, col_tmp);
                        istaken[mainindex] = true;
                    }
                }
                mainindex++;
            }
        }
    }
    
    public static void DrawContoursAroundSegments(Graphics g, BufferedImage ubuff, int[] labels, int width, int height, int color_grid, int color_pos, int color_neg, double zoom) {
        
        int[] dx8 = {-1, -1, 0, 1, 1, 1, 0, -1};
        int[] dy8 = {0, -1, -1, -1, 0, 1, 1, 1};
        
        int[] col_tmp = new int[8];

        int sz = width * height;
        
        boolean[] istaken = new boolean[sz];
        for (int i = 0; i < sz; i++) {
            istaken[i] = false;
        }

        int mainindex = 0;
        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                if ((labels[mainindex] & 0xFF000000) == TMALabel.LABEL_NEG) { // clicked superpixels (they have negative label).
                    g.setColor(new Color(CalculateNewColor(ubuff.getRGB(k, j), color_neg, 1, col_tmp), true));
                    g.fillRect((int)(k*zoom), (int)(j*zoom), (int)Math.max(1, zoom), (int)Math.max(1, zoom));
                    //istaken[mainindex] = true;
                } if ((labels[mainindex] & 0xFF000000) == TMALabel.LABEL_POS) { // clicked superpixels (they have positive label).
                    g.setColor(new Color(CalculateNewColor(ubuff.getRGB(k, j), color_pos, 1, col_tmp), true));
                    g.fillRect((int)(k*zoom), (int)(j*zoom), (int)Math.max(1, zoom), (int)Math.max(1, zoom));
                    //istaken[mainindex] = true;
                } /*else*/ { // border lines (the points whose neighbors have different labels).
                    int np = 0;
                    for (int i = 0; i < 8; i++) {
                        int x = k + dx8[i];
                        int y = j + dy8[i];

                        if ((x >= 0 && x < width) && (y >= 0 && y < height)) {
                            int index = y * width + x;

                            if (false == istaken[index])//comment this to obtain internal contours
                            {
                                if (labels[mainindex] != labels[index]) {
                                    np++;
                                }
                            }
                        }
                    }
                    if (np > 1)//change to 2 or 3 for thinner lines
                    {
                        //ubuff[mainindex] = CalculateNewColor(ubuff[mainindex], color);
                        g.setColor(new Color(CalculateNewColor(ubuff.getRGB(k, j), color_grid, 1, col_tmp), true));
                        g.fillRect((int)(k*zoom), (int)(j*zoom), (int)Math.max(1, zoom), (int)Math.max(1, zoom));//ubuff.setRGB(j, k, CalculateNewColor(ubuff.getRGB(j, k), color));
                        istaken[mainindex] = true;
                    }
                }
                mainindex++;
            }
        }
    }

    
    public static void DrawContoursAroundSegments(Graphics g, BufferedImage ubuff, List<Superpixel> sps,  boolean draw_grid, Color c_grid, boolean draw_pos, Color c_pos, boolean draw_neg, Color c_neg, boolean draw_unk, Color c_unk, double zoom, Rectangle rect) {
        
        int x, y;
        int[] sub = new int[2];
        byte spl;
        int rect_w = (int) Math.max(1, zoom);
        int size;
        int[] pixels;
        Color c_trans;
        for (Superpixel sp: sps) {
            if (sp.touchesRectangle(rect, zoom)) {
                spl = sp.getLabel();
                // draw BorderPoints
                if (draw_grid) {    
                    g.setColor(c_grid);
                    /*
                    g.drawPolygon(Misc.ElemProduct(sp.vt.xpoints, zoom), Misc.ElemProduct(sp.vt.ypoints, zoom), sp.vt.npoints);
                    /*/
                    pixels = sp.getBorderPoints();
                    size = pixels.length;
                    for (int j=0; j<size; j++) {
                        Misc.Ind2sub(sub, pixels[j], ubuff.getWidth());
                        x = sub[0];
                        y = sub[1];
                        //g.setColor(new Color(CalculateNewColor(ubuff.getRGB(x, y), color_grid, 1, col_tmp), true)); //using colors instead of CalculateNewColor takes much more time.
                        g.fillRect((int)(x*zoom), (int)(y*zoom), rect_w, rect_w);
                    }
                    //*/
                }
                // draw InsidePoints
                if (draw_unk && spl == TMALabel.LABEL_UNK || draw_pos && spl == TMALabel.LABEL_POS || draw_neg && spl == TMALabel.LABEL_NEG) {
                    c_trans = spl == TMALabel.LABEL_POS ? c_pos : (spl == TMALabel.LABEL_NEG ? c_neg : c_unk);
                    g.setColor(CalculateNewAlpha(c_trans, sp.getCertainty()));
                    /*
                    g.fillPolygon(Misc.ElemProduct(sp.vt.xpoints, zoom), Misc.ElemProduct(sp.vt.ypoints, zoom), sp.vt.npoints);
                    /*/
                    if (!draw_grid) {
                        pixels = Misc.concat(sp.getInsidePoints(), sp.getBorderPoints());
                    } else {
                        pixels = sp.getInsidePoints();
                    }
                    size = pixels.length;
                    for (int j=0; j<size; j++) {
                        Misc.Ind2sub(sub, pixels[j], ubuff.getWidth());
                        x = sub[0];
                        y = sub[1];
                        //g.setColor(new Color(CalculateNewColor(ubuff.getRGB(x, y), color_trans, comb_fact, col_tmp), true));
                        g.fillRect((int)(x*zoom), (int)(y*zoom), rect_w, rect_w);
                    }
                    //*/
                }
            }
        }
    }
    
    public static void DrawContoursAroundSegmentsSLICs(Graphics g, BufferedImage ubuff, List<SLICsuperpixels> slics, boolean draw_grid, Color color_grid, boolean draw_pos, Color color_pos, boolean draw_neg, Color color_neg, boolean draw_unk, Color color_unk, double zoom, Rectangle rect) {
        
        int x, y;
        int[] sub = new int[2];
        byte spl;
        Color color_trans;
        int rect_w = (int)Math.max(1, zoom);
        int size;
        int[] pixels;
        int k = slics.size();
        for (int i=0; i<k; i++) {
            List<Superpixel> sps = slics.get(i).getSuperpixels();
            for (Superpixel sp: sps) {
                if (sp.touchesRectangle(rect, zoom)) {
                    spl = sp.getLabel();
                    // draw BorderPoints
                    if (draw_grid) {    
                        pixels = sp.getBorderPoints();
                        size = pixels.length;
                        g.setColor(color_grid);
                        for (int j=0; j<size; j++) {
                            Misc.Ind2sub(sub, pixels[j], slics.get(i).getWidth());
                            x = sub[0];
                            y = sub[1];
                            //g.setColor(new Color(CalculateNewColor(ubuff.getRGB(x, y), color_grid, 1, col_tmp), true)); //using colors instead of CalculateNewColor takes much more time.
                            g.fillRect((int)(x*zoom), (int)(y*zoom), rect_w, rect_w);
                        }
                    }
                    if (draw_unk && spl == TMALabel.LABEL_UNK || draw_pos && spl == TMALabel.LABEL_POS || draw_neg && spl == TMALabel.LABEL_NEG) {
                        color_trans = spl == TMALabel.LABEL_POS ? color_pos : (spl == TMALabel.LABEL_NEG ? color_neg : color_unk);
                        pixels = sp.getInsidePoints();
                        size = pixels.length;
                        g.setColor(CalculateNewAlpha(color_trans, sp.getCertainty()));
                        for (int j=0; j<size; j++) {
                            Misc.Ind2sub(sub, pixels[j], slics.get(i).getWidth());
                            x = sub[0];
                            y = sub[1];
                            //g.setColor(new Color(CalculateNewColor(ubuff.getRGB(x, y), color_trans, comb_fact, col_tmp), true));
                            g.fillRect((int)(x*zoom), (int)(y*zoom), rect_w, rect_w);
                        }
                    }
                }
            }
        }
    }


//=================================================================================
/// CalculateNewColor
///
/// Adds the new color value to the old color value (coloured conturs in the
/// segmented image).
//=================================================================================
    private static int CalculateNewColor(
            int oldcolor,
            int newcolor,
            double comb_fact,
            int[] tmp) {
        // old color separation in chanels
        tmp[0] = ((oldcolor >> 24) & 0xFF);
        tmp[1] = ((oldcolor >> 16) & 0xFF);
        tmp[2] = ((oldcolor >>  8) & 0xFF);
        tmp[3] = ((oldcolor)       & 0xFF);

        tmp[4] = ((newcolor >> 24) & 0xFF);
        tmp[5] = ((newcolor >> 16) & 0xFF);
        tmp[6] = ((newcolor >>  8) & 0xFF);
        tmp[7] = ((newcolor)       & 0xFF);

        //combination of both colors
        // mean
        tmp[0] = (int) (((2-comb_fact)*(double)tmp[0] + comb_fact*(double)tmp[4]) / 2);
        tmp[1] = (int) (((2-comb_fact)*(double)tmp[1] + comb_fact*(double)tmp[5]) / 2);
        tmp[2] = (int) (((2-comb_fact)*(double)tmp[2] + comb_fact*(double)tmp[6]) / 2);
        tmp[3] = (int) (((2-comb_fact)*(double)tmp[3] + comb_fact*(double)tmp[7]) / 2);
        
        //tmp[0] = (tmp[0] + tmp[4]) / 2;
        //tmp[1] = (tmp[1] + tmp[5]) / 2;
        //tmp[2] = (tmp[2] + tmp[6]) / 2;
        //tmp[3] = (tmp[3] + tmp[7]) / 2;
        
        tmp[0] = (tmp[0]) << 24;
        tmp[1] = (tmp[1]) << 16;
        tmp[2] = (tmp[2]) << 8;
        tmp[3] = (tmp[3]);

        return (tmp[0] | tmp[1] | tmp[2] | tmp[3]);
    }
    
    //=================================================================================
/// CalculateNewAlpha
///
/// Returns a new Color with weighted alpha (weighted with weight w)
//=================================================================================
    private static Color CalculateNewAlpha(
            Color oldcolor,
            double w) {
        return new Color(oldcolor.getRed(), oldcolor.getGreen(), oldcolor.getBlue(), (int) (w*oldcolor.getAlpha()));
    }

//==============================================================================
///	DetectLabEdges
//==============================================================================
    void DetectLabEdges(
            double[] lvec,
            double[] avec,
            double[] bvec,
            int width,
            int height,
            double[] edges) {
        int sz = width * height;

        for (int j = 1; j < height - 1; j++) {
            for (int k = 1; k < width - 1; k++) {
                int i = j * width + k;

                double dx = (lvec[i - 1] - lvec[i + 1]) * (lvec[i - 1] - lvec[i + 1])
                          + (avec[i - 1] - avec[i + 1]) * (avec[i - 1] - avec[i + 1])
                          + (bvec[i - 1] - bvec[i + 1]) * (bvec[i - 1] - bvec[i + 1]);

                double dy = (lvec[i - width] - lvec[i + width]) * (lvec[i - width] - lvec[i + width])
                          + (avec[i - width] - avec[i + width]) * (avec[i - width] - avec[i + width])
                          + (bvec[i - width] - bvec[i + width]) * (bvec[i - width] - bvec[i + width]);

                //edges[i] = (sqrt(dx) + sqrt(dy));
                edges[i] = (dx + dy);
            }
        }
    }

//===========================================================================
///	PerturbSeeds
//===========================================================================
    void PerturbSeeds(
            double[] kseedsl,
            double[] kseedsa,
            double[] kseedsb,
            double[] kseedsx,
            double[] kseedsy,
            double[] edges) {
        int[] dx8 = {-1, -1, 0, 1, 1, 1, 0, -1};
        int[] dy8 = {0, -1, -1, -1, 0, 1, 1, 1};

        int numseeds = kseedsl.length;

        for (int n = 0; n < numseeds; n++) {
            int ox = (int) kseedsx[n];//original x
            int oy = (int) kseedsy[n];//original y
            int oind = oy * m_width + ox;

            int storeind = oind;
            for (int i = 0; i < 8; i++) {
                int nx = ox + dx8[i];//new x
                int ny = oy + dy8[i];//new y

                if (nx >= 0 && nx < m_width && ny >= 0 && ny < m_height) {
                    int nind = ny * m_width + nx;
                    if (edges[nind] < edges[storeind]) {
                        storeind = nind;
                    }
                }
            }
            if (storeind != oind) {
                kseedsx[n] = storeind % m_width;
                kseedsy[n] = storeind / m_width;
                kseedsl[n] = m_lvec[storeind];
                kseedsa[n] = m_avec[storeind];
                kseedsb[n] = m_bvec[storeind];
            }
        }
    }

//===========================================================================
///	GetLABXYSeeds_ForGivenStepSize
///
/// The k seed values are taken as uniform spatial pixel samples.
//===========================================================================
    void GetLABXYSeeds_ForGivenStepSize(
            double[] kseedsl,
            double[] kseedsa,
            double[] kseedsb,
            double[] kseedsx,
            double[] kseedsy,
            int STEP,
            boolean perturbseeds,
            double[] edgemag) {
        int numseeds = 0;
        int n = 0;

        //int xstrips = m_width/STEP;
        //int ystrips = m_height/STEP;
        int xstrips = (int) (0.5 + (double) m_width / (double) STEP);
        int ystrips = (int) (0.5 + (double) m_height / (double) STEP);

        int xerr = m_width - STEP * xstrips;
        int yerr = m_height - STEP * ystrips;

        double xerrperstrip = (double) (xerr) / (double) (xstrips);
        double yerrperstrip = (double) (yerr) / (double) (ystrips);

        int xoff = STEP / 2;
        int yoff = STEP / 2;
        //-------------------------
        numseeds = xstrips * ystrips;
        //-------------------------
        kseedsl = (double[]) Misc.resizeArray(kseedsl, numseeds);
        kseedsa = (double[]) Misc.resizeArray(kseedsa, numseeds);
        kseedsb = (double[]) Misc.resizeArray(kseedsb, numseeds);
        kseedsx = (double[]) Misc.resizeArray(kseedsx, numseeds);
        kseedsy = (double[]) Misc.resizeArray(kseedsy, numseeds);

        for (int y = 0; y < ystrips; y++) {
            int ye = (int) (y * yerrperstrip);
            for (int x = 0; x < xstrips; x++) {
                int xe = (int) (x * xerrperstrip);
                int i = (y * STEP + yoff + ye) * m_width + (x * STEP + xoff + xe);

                kseedsl[n] = m_lvec[i];
                kseedsa[n] = m_avec[i];
                kseedsb[n] = m_bvec[i];
                kseedsx[n] = (x * STEP + xoff + xe);
                kseedsy[n] = (y * STEP + yoff + ye);
                n++;
            }
        }


        if (perturbseeds) {
            PerturbSeeds(kseedsl, kseedsa, kseedsb, kseedsx, kseedsy, edgemag);
        }
    }

    int CountkseedsSize(int K) {
        int sz = m_width * m_height;
        double step = Math.sqrt((double) sz / (double) K);
        int T = (int) step;
        int xoff = (int) (step / 2);
        int yoff = (int) (step / 2);

        int n = 0;
        for (int y = 0; y < m_height; y++) {
            int Y = (int) (y * step + yoff);
            if (Y > m_height - 1) {
                break;
            }

            for (int x = 0; x < m_width; x++) {
                int X = (int) (x * step + xoff);
                if (X > m_width - 1) {
                    break;
                }
                n++;
            }
        }
        return (n);

    }

//===========================================================================
///	GetLABXYSeeds_ForGivenK
///
/// The k seed values are taken as uniform spatial pixel samples.
//===========================================================================
    void GetLABXYSeeds_ForGivenK(
            double[] kseedsl,
            double[] kseedsa,
            double[] kseedsb,
            double[] kseedsx,
            double[] kseedsy,
            int K,
            boolean perturbseeds,
            double[] edgemag) {
        int sz = m_width * m_height;
        double step = Math.sqrt((double) sz / (double) K);
        int T = (int) step;
        int xoff = (int) (step / 2);
        int yoff = (int) (step / 2);
        int xevenoff = (int) (step / 2);
        int xoddoff = 0;
        

        int n = 0;
        for (int y = 0; y < m_height; y++) {
            int Y = (int) (y * step + yoff);
            if (Y > m_height - 1) {
                break;
            }

            for (int x = 0; x < m_width; x++) {
                int X = (int) (x * step + xoff);
                if (y%2==0) {
                    X += xevenoff;
                } else {
                    X += xoddoff;
                }
                if (X > m_width - 1) {
                    break;
                }

                int i = Y * m_width + X;

                //_ASSERT(n < K);

                kseedsl[n] = m_lvec[i];
                kseedsa[n] = m_avec[i];
                kseedsb[n] = m_bvec[i];
                kseedsx[n] = X;
                kseedsy[n] = Y;
                n++;
            }
        }


        if (perturbseeds) {
            PerturbSeeds(kseedsl, kseedsa, kseedsb, kseedsx, kseedsy, edgemag);
        }
    }

//===========================================================================
///	PerformSuperpixelSLIC
///
///	Performs k mean segmentation. It is fast because it looks locally, not
/// over the entire image.
//===========================================================================
    void PerformSuperpixelSLIC(
            double[] kseedsl,
            double[] kseedsa,
            double[] kseedsb,
            double[] kseedsx,
            double[] kseedsy,
            int[] klabels,
            int STEP,
            double[] edgemag,
            double M) {
        int sz = m_width * m_height;
        int numk = kseedsl.length;
        //----------------
        int offset = STEP;
        //----------------

        double[] clustersize = new double[numk];
        double[] inv = new double[numk];//to store 1/clustersize[k] values

        double[] sigmal = new double[numk];
        double[] sigmaa = new double[numk];
        double[] sigmab = new double[numk];
        double[] sigmax = new double[numk];
        double[] sigmay = new double[numk];
        double[] distvec = new double[sz];

        for (int i_ = 0; i_ < numk; i_++) {
            clustersize[i_] = 0;
            inv[i_] = 0;
            sigmal[i_] = 0;
            sigmaa[i_] = 0;
            sigmab[i_] = 0;
            sigmax[i_] = 0;
            sigmay[i_] = 0;

        }
        for (int i_ = 0; i_ < distvec.length; i_++) {
            distvec[i_] = Double.MAX_VALUE;
        }

        double invwt = 1.0 / ((STEP / M) * (STEP / M));

        int x1, y1, x2, y2;
        double l, a, b;
        double dist;
        double distxy;
        for (int itr = 0; itr < 10; itr++) {
            //distvec.assign(sz, DBL_MAX);
            for (int i_ = 0; i_ < distvec.length; i_++) {
                distvec[i_] = Double.MAX_VALUE;
            }
            for (int n = 0; n < numk; n++) {
                y1 = (int) Math.max(0, kseedsy[n] - offset);
                y2 = (int) Math.min(m_height, kseedsy[n] + offset);
                x1 = (int) Math.max(0, kseedsx[n] - offset);
                x2 = (int) Math.min(m_width, kseedsx[n] + offset);

                for (int y = y1; y < y2; y++) {
                    for (int x = x1; x < x2; x++) {                
                        int i = y * m_width + x;
                        
                        l = m_lvec[i];
                        a = m_avec[i];
                        b = m_bvec[i];

                        dist =    (l - kseedsl[n]) * (l - kseedsl[n])
                                + (a - kseedsa[n]) * (a - kseedsa[n])
                                + (b - kseedsb[n]) * (b - kseedsb[n]);

                        distxy =  (x - kseedsx[n]) * (x - kseedsx[n])
                                + (y - kseedsy[n]) * (y - kseedsy[n]);

                        //------------------------------------------------------------------------
                        dist += distxy * invwt;//dist = sqrt(dist) + sqrt(distxy*invwt);//this is more exact
                        //------------------------------------------------------------------------

                        if (dist < distvec[i]) {
                            distvec[i] = dist;
                            klabels[i] = n;
                        }
                    }
                }
            }
            //-----------------------------------------------------------------
            // Recalculate the centroid and store in the seed values
            //-----------------------------------------------------------------
            //instead of reassigning memory on each iteration, just reset.

            for (int i_ = 0; i_ < numk; i_++) {
                clustersize[i_] = 0;
                sigmal[i_] = 0;
                sigmaa[i_] = 0;
                sigmab[i_] = 0;
                sigmax[i_] = 0;
                sigmay[i_] = 0;
            }

            //------------------------------------
            //edgesum.assign(numk, 0);
            //------------------------------------
            
            int ind = 0;
            for (int r = 0; r < m_height; r++) {
                for (int c = 0; c < m_width; c++) {
                    sigmal[klabels[ind]] += m_lvec[ind];
                    sigmaa[klabels[ind]] += m_avec[ind];
                    sigmab[klabels[ind]] += m_bvec[ind];
                    sigmax[klabels[ind]] += c;
                    sigmay[klabels[ind]] += r;
                    //------------------------------------
                    //edgesum[klabels[ind]] += edgemag[ind];
                    //------------------------------------
                    clustersize[klabels[ind]] += 1.0;
                    ind++;
                }
            }            

            for (int k = 0; k < numk; k++) {
                if (clustersize[k] <= 0) {
                    clustersize[k] = 1;
                }
                inv[k] = 1.0 / clustersize[k];//computing inverse now to multiply, than divide later
            }
            
            for (int k = 0; k < numk; k++) {
                kseedsl[k] = sigmal[k] * inv[k];
                kseedsa[k] = sigmaa[k] * inv[k];
                kseedsb[k] = sigmab[k] * inv[k];
                kseedsx[k] = sigmax[k] * inv[k];
                kseedsy[k] = sigmay[k] * inv[k];
                //------------------------------------
                //edgesum[k] *= inv[k];
                //------------------------------------
            }            
        }
    }

//===========================================================================
///	SaveSuperpixelLabels
///
///	Save labels in raster scan order.
//===========================================================================
/*void SaveSuperpixelLabels(
    int[]					labels,
    int					width,
    int					height,
    String				filename,
    String				path) 
    {
    int sz = width*height;
    
    char fname[_MAX_FNAME];
    char extn[_MAX_FNAME];
    _splitpath(filename.c_str(), NULL, NULL, fname, extn);
    string temp = fname;
    
    ofstream outfile;
    string finalpath = path + temp + string(".dat");
    outfile.open(finalpath.c_str(), ios::binary);
    for( int i = 0; i < sz; i++ )
    {
    outfile.write((char*)&labels[i], sizeof(int));
    }
    outfile.close();
    }*/
    
//===========================================================================
///	FindNext
///
///	Helper function for EnforceLabelConnectivity. Called recursively.
//===========================================================================
    private void FindNext(
            int[] labels,
            int[] nlabels,
            int height,
            int width,
            int h,
            int w,
            int lab,
            int[] xvec,
            int[] yvec,
            int[] count) {
        int x, y, ind;
        for (int i = 0; i < 4; i++) {
            y = h + dy4[i];
            x = w + dx4[i];
            if ((y < height && y >= 0) && (x < width && x >= 0)) {
                ind = y * width + x;
                if (nlabels[ind] < 0 && labels[ind] == labels[h * width + w]) {
                    xvec[count[0]] = x;
                    yvec[count[0]] = y;
                    count[0]++;
                    nlabels[ind] = lab;
                    FindNext(labels, nlabels, height, width, y, x, lab, xvec, yvec, count);
                }
            }
        }
    }
    
//===========================================================================
///	FindNext (not recursive)
///
///	Helper function for EnforceLabelConnectivity. Called iteratively.
//===========================================================================
    private void FindNext_it(
            int[] labels,
            int[] nlabels,
            int height,
            int width,
            List<Integer> h_stack,
            List<Integer> w_stack,
            int lab,
            int[] xvec,
            int[] yvec,
            int[] count) {
        int s, x, y, h, w, ind;
        while(!h_stack.isEmpty()) {
            s = h_stack.size()-1;
            h = h_stack.remove(s);
            w = w_stack.remove(s);
            for (int i=0; i<4; i++) {
                y = h + dy4[i];
                x = w + dx4[i];
                if ((y < height && y >= 0) && (x < width && x >= 0)) {
                    ind = y * width + x;
                    if (nlabels[ind] < 0 && labels[ind] == labels[h * width + w]) {
                        xvec[count[0]] = x;
                        yvec[count[0]] = y;
                        count[0]++;
                        nlabels[ind] = lab;
                        h_stack.add(y);
                        w_stack.add(x);
                    }
                }
            }
        }
    }

//===========================================================================
///	EnforceLabelConnectivity
///
///	Some superpixels may be unconnected, Relabel them. Recursive algorithm
/// used here, can crash if stack overflows. This will only happen if the
/// superpixels are very large, otherwise safe.
///		STEPS:
///		1. finding an adjacent label for each new component at the start
///		2. if a certain component is too small, assigning the previously found
///		    adjacent label to this component, and not incrementing the label.
//===========================================================================
    private int EnforceLabelConnectivity(
            int[] labels,
            int width,
            int height,
            int[] nlabels,
            int numlabels,
            int K) {
        int sz = width * height;
        for (int i=0; i<sz; i++) {
           nlabels[i] = -1;
        }

        int SUPSZ = sz / K;
        //------------------
        // labeling
        //------------------
        int lab = 0;
        int i = 0;
        int adjlabel = 0;//adjacent label
        int[] xvec = new int[sz];//worst case size
        int[] yvec = new int[sz];//worst case size
        int[] count = new int[1];
        {
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    if (nlabels[i] < 0) {
                        nlabels[i] = lab;
                        //-------------------------------------------------------
                        // Quickly find an adjacent label for use later if needed
                        //-------------------------------------------------------
                        {
                            for (int n = 0; n < 4; n++) {
                                int x = w + dx4[n];
                                int y = h + dy4[n];
                                if ((x >= 0 && x < width) && (y >= 0 && y < height)) {
                                    int nindex = y * width + x;
                                    if (nlabels[nindex] >= 0) {
                                        adjlabel = nlabels[nindex];
                                    }
                                }
                            }
                        }
                        xvec[0] = w;
                        yvec[0] = h;

                        count[0] = 1;
                        // recursively, throws stack-overflow
                        //FindNext(labels, nlabels, height, width, h, w, lab, xvec, yvec, count);
                        
                        // iteratively, works without stack-overflow
                        List<Integer> hvec_stack = new ArrayList<Integer>();
                        List<Integer> wvec_stack = new ArrayList<Integer>();
                        hvec_stack.add(h);
                        wvec_stack.add(w);
                        FindNext_it(labels, nlabels, height, width, hvec_stack, wvec_stack, lab, xvec, yvec, count);

                        //-------------------------------------------------------
                        // If segment size is less then a limit, assign an
                        // adjacent label found before, and decrement label count.
                        //-------------------------------------------------------
                        if (count[0] <= (SUPSZ >> 2)) {
                            for (int c = 0; c < count[0]; c++) {
                                int ind = yvec[c] * width + xvec[c];
                                nlabels[ind] = adjlabel;
                            }
                            lab--;
                        }
                        lab++;
                    }
                    i++;
                }
            }
        }
        //------------------
        numlabels = lab;
        //------------------
        //if(xvec) delete [] xvec;
        //if(yvec) delete [] yvec;

        return (numlabels);
    }

/*//===========================================================================
///	DoSuperpixelSegmentation_ForGivenStepSize
///
/// There is option to save the labels if needed.
//===========================================================================
    int DoSuperpixelSegmentation_ForGivenStepSize(
            int[] ubuff,
            int width,
            int height,
            int[] klabels,
            int numlabels,
            int STEP,
            double m) {
        double[] kseedsl = new double[0];
        double[] kseedsa = new double[0];
        double[] kseedsb = new double[0];
        double[] kseedsx = new double[0];
        double[] kseedsy = new double[0];

        //--------------------------------------------------
        m_width = width;
        m_height = height;
        int sz = m_width * m_height;
        //klabels.resize( sz, -1 );
        //--------------------------------------------------
        klabels = new int[sz];
        for (int s = 0; s < sz; s++) {
            klabels[s] = -1;
        }
        //--------------------------------------------------
        DoRGBtoLABConversion(ubuff, m_lvec, m_avec, m_bvec);
        //--------------------------------------------------

        boolean perturbseeds = true;
        double[] edgemag = new double[sz];
        {
            for (int i = 0; i < sz; i++) {
                edgemag[i] = 0;
            }
        }
        if (perturbseeds) {
            DetectLabEdges(m_lvec, m_avec, m_bvec, m_width, m_height, edgemag);
        }
        GetLABXYSeeds_ForGivenStepSize(kseedsl, kseedsa, kseedsb, kseedsx, kseedsy, STEP, perturbseeds, edgemag);

        PerformSuperpixelSLIC(kseedsl, kseedsa, kseedsb, kseedsx, kseedsy, klabels, STEP, edgemag, m);

        numlabels = kseedsl.length;

        int[] nlabels = new int[sz];
        for (int s = 0; s < sz; s++) {
            nlabels[s] = -1;
        }
        EnforceLabelConnectivity(klabels, m_width, m_height, nlabels, numlabels, (int) ((double) (sz) / (double) (STEP * STEP)));

        {
            for (int i = 0; i < sz; i++) {
                klabels[i] = nlabels[i];
            }
        }
        //if(nlabels) delete [] nlabels;

        return (numlabels);
    }
*/
//===========================================================================
///	DoSuperpixelSegmentation_ForGivenStepSize
///
/// Originally called DoSegmentation_LABXY
/// There is option to save the labels if needed. However the filename and
/// path need to be provided.
//===========================================================================
    int DoSuperpixelSegmentation_ForGivenK(
            SLICsuperpixels slicsp,
            int[] ubuff,
            int width,
            int height,
            int[] klabels,
            int numlabels,
            int K,//required number of superpixels
            double m,//weight given to spatial distance
            boolean LAB_space)
    {

        tmarker tm = slicsp.getTMAspot().getCenter();
        //--------------------------------------------------
        m_width = width;
        m_height = height;
        int sz = m_width * m_height;
        //--------------------------------------------------
        //if(0 == klabels) klabels = new int[sz];
        for (int s = 0; s < sz; s++) {
            klabels[s] = -1;
        }
        //--------------------------------------------------
        
        if (slicsp.verbose) {tm.setStatusMessageLabel("Performing Superpixeling: color conversion ..."); tm.setProgressbar(30);}
        if (LAB_space)//LAB
        {
            m_lvec = new double[sz];
            m_avec = new double[sz];
            m_bvec = new double[sz];
            DoRGBtoLABConversion(slicsp, ubuff, m_lvec, m_avec, m_bvec);
        } else//RGB
        {
            m_lvec = new double[sz];
            m_avec = new double[sz];
            m_bvec = new double[sz];
            for (int i = 0; i < sz; i++) {
                m_lvec[i] = ubuff[i] >> 16 & 0xff;
                m_avec[i] = ubuff[i] >>  8 & 0xff;
                m_bvec[i] = ubuff[i]       & 0xff;
            }
        }
        //--------------------------------------------------

        if (slicsp.verbose) {tm.setStatusMessageLabel("Performing Superpixeling: discover seeds 1/2 ..."); tm.setProgressbar(40);}
        boolean perturbseeds = true;
        double[] edgemag = new double[sz];
        for (int i = 0; i < sz; i++) {
            edgemag[i] = 0;
        }
        if (perturbseeds) {
            DetectLabEdges(m_lvec, m_avec, m_bvec, m_width, m_height, edgemag);
        }

        int css = CountkseedsSize(K);
        double[] kseedsl = new double[css];
        double[] kseedsa = new double[css];
        double[] kseedsb = new double[css];
        double[] kseedsx = new double[css];
        double[] kseedsy = new double[css];

        if (slicsp.verbose) {tm.setStatusMessageLabel("Performing Superpixeling: discover seeds 2/2 ..."); tm.setProgressbar(50);}
        GetLABXYSeeds_ForGivenK(kseedsl, kseedsa, kseedsb, kseedsx, kseedsy, K, perturbseeds, edgemag);

        if (slicsp.verbose) {tm.setStatusMessageLabel("Performing Superpixeling: discover superpixels ..."); tm.setProgressbar(60);}
        int STEP = (int) (Math.sqrt((double) (sz) / (double) (K)) + 2.0);//adding a small value in the even the STEP size is too small.
        PerformSuperpixelSLIC(kseedsl, kseedsa, kseedsb, kseedsx, kseedsy, klabels, STEP, edgemag, m);
            
        numlabels = kseedsl.length;

        int[] nlabels = new int[sz];
        for (int s = 0; s < sz; s++) {
            nlabels[s] = -1;
        }
        if (slicsp.verbose) {tm.setStatusMessageLabel("Performing Superpixeling: enforce label connectivity ..."); tm.setProgressbar(80);}
        EnforceLabelConnectivity(klabels, m_width, m_height, nlabels, numlabels, K);
        
        for (int i = 0; i < sz; i++) {
            klabels[i] = nlabels[i];
        }
        
        //if(nlabels) delete [] nlabels;

        return (numlabels);
    }
}

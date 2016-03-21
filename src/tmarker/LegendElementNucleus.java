/*
 * Copyright (c) 2015, Peter J. Schueffler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package tmarker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import tmarker.TMAspot.TMALabel;
import tmarker.TMAspot.TMAspot;

/**
 *
 * @author Peter J. Schueffler
 */
public final class LegendElementNucleus extends LegendElement {
    
    int rad;
    byte nuc_label;
    byte staining;
    Color fg;
    
    /**
     * Creates new form LegendElement.
     * @param t The parent TMARKER program.
     * @param rad The radius of the nuclei.
     * @param nuc_label The label of the corresponding nuclei.
     * @param staining The staining of the corresponding nuclei.
     * @param fg The foreground color of the nucleus pictogram.
     */
    public LegendElementNucleus(tmarker t, int rad, byte nuc_label, byte staining, Color fg) {
        this.t = t;
        this.rad = rad;
        this.nuc_label = nuc_label;
        this.staining = staining;
        this.fg = fg;
        bg = getBackground();
        setAlignmentX((float) 0.5);
        generateToolTipText();
        setPreferredSize(new Dimension(2*rad+7, 2*rad+7));
        createNucIcon();
    }

    /**
     * Draws the nucleus pictogram on an new image.
     */
    void createNucIcon() {
        BufferedImage img;
        img = new BufferedImage(2*rad+7, 2*rad+7, BufferedImage.TYPE_INT_ARGB);
        setIcon(new ImageIcon(img));
        Graphics g = img.getGraphics();
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(fg);
        if (t.getOptionDialog().getLabelsShape_Gst() == tmarker.LABELS_SHAPE_CIRCLE) {
            g.fillOval(img.getWidth()/2-rad, img.getHeight()/2-rad, 2*rad, 2*rad);
            g.setColor(Color.YELLOW);
            g.drawOval(img.getWidth()/2-rad-1, img.getHeight()/2-rad-1, 2*rad+2, 2*rad+2);
        } else if (t.getOptionDialog().getLabelsShape_Gst() == tmarker.LABELS_SHAPE_CROSS || t.getOptionDialog().getLabelsShape_Gst() == tmarker.LABELS_SHAPE_CROSS_THICK) {
            int oldFontSize = g.getFont().getSize();
            g.setFont(g.getFont().deriveFont((float)(3.5*rad)));
            g.drawString("+", 0, getPreferredSize().height);
            g.setFont(g.getFont().deriveFont((float)(oldFontSize)));
        } else {
            double f=0.6;
            int r = rad;
            double z=1;
            int x = getPreferredSize().width/2;
            int y = getPreferredSize().height/2;
            g.drawLine((int)((x-r)*z), (int)((y-r)*z), (int)((x-r+f*r)*z), (int)((y-r)*z));
            g.drawLine((int)((x-r)*z), (int)((y-r)*z), (int)((x-r)*z), (int)((y-r+f*r)*z));
            g.drawLine((int)((x-r)*z), (int)((y+r)*z), (int)((x-r)*z), (int)((y+r-f*r)*z));
            g.drawLine((int)((x-r)*z), (int)((y+r)*z), (int)((x-r+f*r)*z), (int)((y+r)*z));
            g.drawLine((int)((x+r)*z), (int)((y+r)*z), (int)((x+r-f*r)*z), (int)((y+r)*z));
            g.drawLine((int)((x+r)*z), (int)((y+r)*z), (int)((x+r)*z), (int)((y+r-f*r)*z));
            g.drawLine((int)((x+r)*z), (int)((y-r)*z), (int)((x+r)*z), (int)((y-r+f*r)*z));
            g.drawLine((int)((x+r)*z), (int)((y-r)*z), (int)((x+r-f*r)*z), (int)((y-r)*z));
        }
        if (nuc_label!=TMALabel.LABEL_BG) {
            g.setColor(Color.BLACK);
            g.drawString(Integer.toString(staining), (img.getWidth())/2-g.getFontMetrics().stringWidth(Integer.toString(staining))/2, 
                    (img.getHeight())/2+g.getFontMetrics().getHeight()/4-((t.getOptionDialog().getLabelsShape_Gst() == tmarker.LABELS_SHAPE_CROSS || t.getOptionDialog().getLabelsShape_Gst() == tmarker.LABELS_SHAPE_CROSS_THICK)?4:-1));
        }
        repaint();
    }
    
    /**
     * Generates the tooltiptext for this LegendElementNucleus which explains the class and staining intensity of the nucleus.
     */
    private void generateToolTipText() {
        String text = "<html>Draw a ";
        text += nuc_label==TMALabel.LABEL_POS ? "malignant nucleus<br>" : (nuc_label==TMALabel.LABEL_NEG ? "benign nucleus<br>" : (nuc_label==TMALabel.LABEL_UNK ? "nucleus<br>" : "background location<br>"));
        if (nuc_label!=TMALabel.LABEL_BG) {
            text += "with staining intensity: " + staining + "</html>";
        }
        setToolTipText(text);
    }
    
    /**
     * On double click, select the label's color.
     */
    @Override
    void doubleClickAction() {
        Color c = JColorChooser.showDialog(t, "Choose Color", fg);
        if (c!=null) {
            setColor(c);
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, Integer.toString(c.getAlpha()));
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, Integer.toString(c.getTransparency()));
            TMAspot ts = t.getVisibleTMAspot();
            t.getTMAList().repaint();
            if (ts!=null &&  ts.hasStainingEstimation()) {
                t.getTSD().updateStainingEstimation(ts);
                t.getTMAView().repaint();
            }
        }
    }
    
    /**
     * Set the foreground color (Nucleus pictogram color).
     * @param fg The foreground color (Nucleus pictogram color).
     */
    void setColor(Color fg) {
        this.fg = fg;
        createNucIcon();
    }
    
    /**
     * Returns the foreground color (Nucleus pictogram color).
     * @return The foreground color (Nucleus pictogram color).
     */
    Color getColor() {
        return fg;
    }

    /**
     * Returns the nucleus' label.
     * @return One of TMAspot.LABEL_BG, LABEL_POS, LABEL_NEG or LABEL_UNK.
     */
    byte getNucLabel() {
        return nuc_label;
    }

    /**
     * Set the nucleus' label.
     * @param label One of TMAspot.LABEL_BG, LABEL_POS, LABEL_NEG or LABEL_UNK.
     */
    void setNucLabel(byte label) {
        this.nuc_label = label;
    }
    
    /**
     * Returs the nucleus' staining.
     * @return One of TMAspot.STAINING_0, STAINING_1, STAINING_2 or STAINING_3.
     */
    byte getStaining() {
        return staining;
    }

    /**
     * Sets the nucleus' staining.
     * @param staining One of TMAspot.STAINING_0, STAINING_1, STAINING_2 or STAINING_3.
     */
    void setStaining(byte staining) {
        this.staining = staining;
    }
    
}

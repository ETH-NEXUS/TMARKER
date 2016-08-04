/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker.TMAspot;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.SystemColor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.jdesktop.swingx.JXDialog;
import org.jdesktop.swingx.JXTable;
import tmarker.FileChooser;
import tmarker.misc.Misc;
import tmarker.misc.SortedProperties;
import tmarker.tmarker;

/**
 * The TMAspot_list_panel belongs to a TMAspot and is displayed in the TMA List of the TMARKER program.
 * It displays important information of the TMAspot such as the name of the TMAspot, a thumbnail and the number of cell nuclei.
 * @author Peter J. Schueffler
 */
public class TMAspot_list_panel extends javax.swing.JPanel {

    TMAspot ts;
    boolean isExpanded = false;
    private final Color bg_mouseOver = SystemColor.info;
    private final Color bg_even = Color.WHITE;
    private final Color bg_odd = Color.WHITE;
    private final Color bg_clicked_even = SystemColor.info.darker().darker().brighter().brighter();
    private final Color bg_clicked_odd = SystemColor.info.darker().darker().brighter().brighter();
    private final Dimension dim = new Dimension(100, 35);
    private boolean thumbnailSet = false;
    private boolean isShowingEstimated = true;
    private boolean isShowingGoldstandard = false;

    // Panel heights
    public final static int HEIGHT_UNEXPANDED = 85;
    public final static int HEIGHT_EXPANDED = 130;

    // for drawing
    final static Font FONT_PLAIN = new java.awt.Font("Arial", java.awt.Font.BOLD, 12);
    final static Font FONT_SMALL = new java.awt.Font("Arial", java.awt.Font.PLAIN, 11);
    final static Font FONT_BOLD = new java.awt.Font("Arial", java.awt.Font.BOLD, 12);
    String name;
    int offsetx = HEIGHT_UNEXPANDED+5;
    int offsety = 30;
    int lineheight = 15;
    int cellwidth = 50;

    /**
     * Number of malignant, stained, gold-standard nuclei.
     */
    int pos_gst_sta = 0;

    /**
     * Number of benign, stained gold-standard nuclei.
     */
    int neg_gst_sta = 0;

    /**
     * Number of unknown, stained gold-standard nuclei.
     */
    int unk_gst_sta = 0;

    /**
     * Number of malignant, stained estimated nuclei.
     */
    int pos_est_sta = 0;

    /**
     * Number of benign, stained estimated nuclei.
     */
    int neg_est_sta = 0;

    /**
     * Number of unknown, stained estimated nuclei.
     */
    int unk_est_sta = 0;

    /**
     * Number of malignant, stained estimated and gold-standard nuclei.
     */
    int pos_tot_sta = 0;

    /**
     * Number of benign, stained estimated and gold-standard nuclei.
     */
    int neg_tot_sta = 0;

    /**
     * Number of unkown, stained estimated and gold-standard nuclei.
     */
    int unk_tot_sta = 0;
    
    /**
     * Number of malignant, stained (1+), gold-standard nuclei.
     */
    int pos_gst_sta1 = 0;

    /**
     * Number of benign, stained (1+) gold-standard nuclei.
     */
    int neg_gst_sta1 = 0;

    /**
     * Number of unknown, stained (1+) gold-standard nuclei.
     */
    int unk_gst_sta1 = 0;

    /**
     * Number of malignant, stained (1+) estimated nuclei.
     */
    int pos_est_sta1 = 0;

    /**
     * Number of benign, stained (1+) estimated nuclei.
     */
    int neg_est_sta1 = 0;

    /**
     * Number of unknown, stained (1+) estimated nuclei.
     */
    int unk_est_sta1 = 0;

    /**
     * Number of malignant, stained (1+) estimated and gold-standard nuclei.
     */
    int pos_tot_sta1 = 0;

    /**
     * Number of benign, stained (1+) estimated and gold-standard nuclei.
     */
    int neg_tot_sta1 = 0;

    /**
     * Number of unkown, stained (1+) estimated and gold-standard nuclei.
     */
    int unk_tot_sta1 = 0;

    /**
     * Number of malignant, stained (2+), gold-standard nuclei.
     */
    int pos_gst_sta2 = 0;

    /**
     * Number of benign, stained (2+) gold-standard nuclei.
     */
    int neg_gst_sta2 = 0;

    /**
     * Number of unknown, stained (2+) gold-standard nuclei.
     */
    int unk_gst_sta2 = 0;

    /**
     * Number of malignant, stained (2+) estimated nuclei.
     */
    int pos_est_sta2 = 0;

    /**
     * Number of benign, stained (2+) estimated nuclei.
     */
    int neg_est_sta2 = 0;

    /**
     * Number of unknown, stained (2+) estimated nuclei.
     */
    int unk_est_sta2 = 0;

    /**
     * Number of malignant, stained (2+) estimated and gold-standard nuclei.
     */
    int pos_tot_sta2 = 0;

    /**
     * Number of benign, stained (2+) estimated and gold-standard nuclei.
     */
    int neg_tot_sta2 = 0;

    /**
     * Number of unkown, stained (2+) estimated and gold-standard nuclei.
     */
    int unk_tot_sta2 = 0;
    
    /**
     * Number of malignant, stained (3+), gold-standard nuclei.
     */
    int pos_gst_sta3 = 0;

    /**
     * Number of benign, stained (3+) gold-standard nuclei.
     */
    int neg_gst_sta3 = 0;

    /**
     * Number of unknown, stained (3+) gold-standard nuclei.
     */
    int unk_gst_sta3 = 0;

    /**
     * Number of malignant, stained (3+) estimated nuclei.
     */
    int pos_est_sta3 = 0;

    /**
     * Number of benign, stained (3+) estimated nuclei.
     */
    int neg_est_sta3 = 0;

    /**
     * Number of unknown, stained (3+) estimated nuclei.
     */
    int unk_est_sta3 = 0;

    /**
     * Number of malignant, stained (3+) estimated and gold-standard nuclei.
     */
    int pos_tot_sta3 = 0;

    /**
     * Number of benign, stained (3+) estimated and gold-standard nuclei.
     */
    int neg_tot_sta3 = 0;

    /**
     * Number of unkown, stained (3+) estimated and gold-standard nuclei.
     */
    int unk_tot_sta3 = 0;
    
    /**
     * Number of malignant, unstained, gold-standard nuclei.
     */
    int pos_gst_usta = 0;

    /**
     * Number of benign, unstained gold-standard nuclei.
     */
    int neg_gst_usta = 0;

    /**
     * Number of unknown, unstained gold-standard nuclei.
     */
    int unk_gst_usta = 0;

    /**
     * Number of malignant, unstained estimated nuclei.
     */
    int pos_est_usta = 0;

    /**
     * Number of benign, unstained estimated nuclei.
     */
    int neg_est_usta = 0;

    /**
     * Number of unkown, unstained estimated nuclei.
     */
    int unk_est_usta = 0;

    /**
     * Number of malignant, unstained estimated and gold-standard nuclei.
     */
    int pos_tot_usta = 0;

    /**
     * Number of benign, unstained estimated and gold-standard nuclei.
     */
    int neg_tot_usta = 0;

    /**
     * Number of unkown, unstained estimated and gold-standard nuclei.
     */
    int unk_tot_usta = 0;

    /**
     * Number of background gold-standard nuclei and points. As background, it has no
     * staining intensity.
     */
    int bg_gst = 0;


    /**
     * Creates new form TMAspot_list_panel
     * @param ts The TMAspot to which this panel belongs.
     */
    public TMAspot_list_panel(TMAspot ts) {
        this.ts = ts;
        initComponents();
        initComponents2();
    }

    /**
     * Initializes the name and size of this component.
     */
    private void initComponents2() {
        sizeComponent();
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        setSize(dim);
        name = ts.getName() + "   (" + ts.getWidth() + " x " + ts.getHeight() + ")";
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel6 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));
        setAlignmentX(0.0F);
        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        setInheritsPopupMenu(true);
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                formMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                formMouseExited(evt);
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                formMouseMoved(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                formKeyReleased(evt);
            }
        });
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 0, 0));

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/nopreview.png"))); // NOI18N
        add(jLabel6);
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        doMouseClick(evt);
    }//GEN-LAST:event_formMouseClicked

    private void formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseEntered
        setBackground(bg_mouseOver);
        setBorder(new javax.swing.border.LineBorder(bg_mouseOver.darker(), 1, false));
    }//GEN-LAST:event_formMouseEntered

    private void formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseExited
        boolean even = true;
        if (ts.isSelected()) {
            if (even) {
                this.setBackground(bg_clicked_even);
                this.setBorder(new javax.swing.border.LineBorder(bg_clicked_even, 1, true));
            } else {
                this.setBackground(bg_clicked_odd);
                this.setBorder(new javax.swing.border.LineBorder(bg_clicked_odd, 1, true));
            }
        }
        else {
            if (even) {
                this.setBackground(bg_even);
                this.setBorder(new javax.swing.border.LineBorder(bg_even, 1, true));
            } else {
                this.setBackground(bg_odd);
                this.setBorder(new javax.swing.border.LineBorder(bg_odd, 1, true));
            }
        }
    }//GEN-LAST:event_formMouseExited

    private void formKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyReleased
        List<TMAspot> tss = ts.getCenter().getSelectedTMAspots(false);
        if (!tss.isEmpty()) {
            if (evt.getKeyCode()==KeyEvent.VK_DELETE) {
                ts.getCenter().removeTMAspots(tss);
            } else {
                ts.getCenter().showTMAspot(tss.get(0));
            }
        }
    }//GEN-LAST:event_formKeyReleased

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        if (evt.isControlDown() && evt.getKeyCode()==java.awt.event.KeyEvent.VK_A) {
            ts.getCenter().selectAllTMAspots();
        }
    }//GEN-LAST:event_formKeyPressed

    private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
        String text = null;
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        if (isInExpandField(evt.getX(), evt.getY())) {
            if (!isExpanded) {
                text = "Expand for more details.";
            } else {
                text = "Collapse for less details.";
            }
        } else if (isInToggleShowGstEstField(evt.getX(), evt.getY())) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (isShowingEstimated() && !isShowingGoldstandard()) {
                text = "Now showing statistics of computer-estimated labels.";
            } else if (!isShowingEstimated() && isShowingGoldstandard()) {
                text = "Now showing statistics of manually drawn labels.";
            } else {
                text = "Now showing statistics of manually drawn and computer-estimated labels.";
            }
        } else if (isInCloseField(evt.getX(), evt.getY())) {
            text = "Close this image.";
        } else if (isInMalignantStainingField(evt.getX(), evt.getY())) {
            if (isShowingEstimated() && !isShowingGoldstandard()) {
                text = "<html>How many malignant nuclei are stained?<br>" + (pos_est_sta) + " / ( " + (pos_est_sta) + " + " + (pos_est_usta) + " ) = " + (int)(Math.round(10000.0*(pos_est_sta) / (pos_est_sta+pos_est_usta))/100.0) + " %</html>";
            } else if (!isShowingEstimated() && isShowingGoldstandard()) {
                text = "<html>How many malignant nuclei are stained?<br>" + (pos_gst_sta) + " / ( " + (pos_gst_sta) + " + " + (pos_gst_usta) + " ) = " + (int)(Math.round(10000.0*(pos_gst_sta) / (pos_gst_sta+pos_gst_usta))/100.0) + " %</html>";
            } else {
                text = "<html>How many malignant nuclei are stained?<br>" + (pos_est_sta+pos_gst_sta) + " / ( " + (pos_est_sta+pos_gst_sta) + " + " + (pos_est_usta+pos_gst_usta) + " ) = " + (int)(Math.round(10000.0*(pos_est_sta+pos_gst_sta) / (pos_est_sta+pos_est_usta+pos_gst_sta+pos_gst_usta))/100.0) + " %</html>";
            }
        } else if (isInStainingField(evt.getX(), evt.getY())) {
            if (isShowingEstimated() && !isShowingGoldstandard()) {
                text = "<html>How many nuclei are stained?<br>" + (unk_est_sta) + " / ( " + (unk_est_sta) + " + " + (unk_est_usta) + " ) = " + (int)(Math.round(10000.0*(unk_est_sta) / (unk_est_sta+unk_est_usta))/100.0) + " %</html>";
            } else if (!isShowingEstimated() && isShowingGoldstandard()) {
                text = "<html>How many nuclei are stained?<br>" + (unk_gst_sta) + " / ( " + (unk_gst_sta) + " + " + (unk_gst_usta) + " ) = " + (int)(Math.round(10000.0*(unk_gst_sta) / (unk_gst_sta+unk_gst_usta))/100.0) + " %</html>";
            } else {
                text = "<html>How many nuclei are stained?<br>" + (unk_est_sta+unk_gst_sta) + " / ( " + (unk_est_sta+unk_gst_sta) + " + " + (unk_est_usta+unk_gst_usta) + " ) = " + (int)(Math.round(10000.0*(unk_est_sta+unk_gst_sta) / (unk_est_sta+unk_est_usta+unk_gst_sta+unk_gst_usta))/100.0) + " %</html>";
            }
        } 
        setToolTipText(text);
    }//GEN-LAST:event_formMouseMoved


    /**
     * Returns the linked TMAspot of this panel.
     * @return The linked TMAspot of this panel.
     */
    public TMAspot getTMAspot() {
        return ts;
    }

    /**
     * Sets this TMAspot_list_panel selected, meaning the user clicked on it.
     * @param b True, if the user set this sample active.
     */
    public void setSelected(boolean b) {
        boolean even = true;
        if (b) {
            if (even) {
                this.setBackground(bg_clicked_even);
                this.setBorder(new javax.swing.border.LineBorder(bg_clicked_even, 1, true));
            } else {
                this.setBackground(bg_clicked_odd);
                this.setBorder(new javax.swing.border.LineBorder(bg_clicked_odd, 1, true));
            }
        }
        else {
            if (even) {
                this.setBackground(bg_even);
                this.setBorder(new javax.swing.border.LineBorder(bg_even, 1, true));
            } else {
                this.setBackground(bg_odd);
                this.setBorder(new javax.swing.border.LineBorder(bg_odd, 1, true));
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        sizeComponent();
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paintComponent(g);
        //Font tmp = g.getFont();
        // write name
        g.setFont(FONT_BOLD);
        g.drawString(name, offsetx, 12);
        g.setFont(FONT_PLAIN);

        //Draw the Close button (top right)
        if (getBackground() == bg_mouseOver) {
            g.setColor(Color.GRAY);
            g.drawRoundRect(getWidth()-13, 2, 10, 10, 3, 3);
            g.drawString("x", getWidth()-10, 11);
        }

        //Draw the view Estimated / View Goldstandard images
        /*if (isExpanded) {
            Image I = new ImageIcon(getClass().getResource("/tmarker/img/buttons/viewest.png")).getImage();
            g.setColor(SystemColor.textHighlight);
            if (isShowingEstimated) {
                g.fillRect(3, HEIGHT_UNEXPANDED+4, I.getWidth(null)+3, I.getWidth(null)+2);
            } else {
                g.fillRect(8+I.getWidth(null), HEIGHT_UNEXPANDED+4, I.getWidth(null)+3, I.getWidth(null)+2);
            }
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(3, HEIGHT_UNEXPANDED+4, I.getWidth(null)+3, I.getWidth(null)+2);
            g.drawRect(8+I.getWidth(null), HEIGHT_UNEXPANDED+4, I.getWidth(null)+3, I.getWidth(null)+2);
            g.drawImage(I, 5, HEIGHT_UNEXPANDED+5, null);
            I = new ImageIcon(getClass().getResource("/tmarker/img/buttons/viewgstd.png")).getImage();
            g.drawImage(I, 10+I.getWidth(null), HEIGHT_UNEXPANDED+5, null);
        }*/

        // Draw Nuclei Counts
        drawNucleiCounts(g, isShowingEstimated(), isShowingGoldstandard());
    }

    /**
     * Draw the numbers of nuclei and staining estimation on this component.
     * @param drawEstimated If True, the computed points are reported.
     * @param drawGoldstandard If True, the manually edited points are reported. If drawEstimated and drawGoldstandard are true, 
     * the total (sum) of both are reported.
     * @param g The graphics of this component to draw on.
     */
    public void drawNucleiCounts(Graphics g, boolean drawEstimated, boolean drawGoldstandard) {
        try {
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            String pos_gst_sta_str = Int2FormatedString(pos_gst_sta);
            String pos_gst_sta1_str = Int2FormatedString(pos_gst_sta1);
            String pos_gst_sta2_str = Int2FormatedString(pos_gst_sta2);
            String pos_gst_sta3_str = Int2FormatedString(pos_gst_sta3);
            String unk_gst_sta_str = Int2FormatedString(unk_gst_sta);
            String unk_gst_sta1_str = Int2FormatedString(unk_gst_sta1);
            String unk_gst_sta2_str = Int2FormatedString(unk_gst_sta2);
            String unk_gst_sta3_str = Int2FormatedString(unk_gst_sta3);
            String neg_gst_sta_str = Int2FormatedString(neg_gst_sta);
            String neg_gst_sta1_str = Int2FormatedString(neg_gst_sta1);
            String neg_gst_sta2_str = Int2FormatedString(neg_gst_sta2);
            String neg_gst_sta3_str = Int2FormatedString(neg_gst_sta3);
            String neg_gst_usta_str = Int2FormatedString(neg_gst_usta);
            String pos_gst_usta_str = Int2FormatedString(pos_gst_usta);
            String unk_gst_usta_str = Int2FormatedString(unk_gst_usta);
            String bg_gst_str = Int2FormatedString(bg_gst);
            
            String neg_est_sta_str = Int2FormatedString(neg_est_sta);
            String neg_est_sta1_str = Int2FormatedString(neg_est_sta1);
            String neg_est_sta2_str = Int2FormatedString(neg_est_sta2);
            String neg_est_sta3_str = Int2FormatedString(neg_est_sta3);
            //String neg_tot_sta_str = Int2FormatedString(neg_tot_sta);
            String pos_est_sta_str = Int2FormatedString(pos_est_sta);
            String pos_est_sta1_str = Int2FormatedString(pos_est_sta1);
            String pos_est_sta2_str = Int2FormatedString(pos_est_sta2);
            String pos_est_sta3_str = Int2FormatedString(pos_est_sta3);
            //String pos_tot_sta_str = Int2FormatedString(pos_tot_sta);
            String unk_est_sta_str = Int2FormatedString(unk_est_sta);
            String unk_est_sta1_str = Int2FormatedString(unk_est_sta1);
            String unk_est_sta2_str = Int2FormatedString(unk_est_sta2);
            String unk_est_sta3_str = Int2FormatedString(unk_est_sta3);
            //String unk_tot_sta_str = Int2FormatedString(unk_tot_sta);
            String neg_est_usta_str = Int2FormatedString(neg_est_usta);
            //String neg_tot_usta_str = Int2FormatedString(neg_tot_usta);
            String pos_est_usta_str = Int2FormatedString(pos_est_usta);
            //String pos_tot_usta_str = Int2FormatedString(pos_tot_usta);
            String unk_est_usta_str = Int2FormatedString(unk_est_usta);
            //String unk_tot_usta_str = Int2FormatedString(unk_tot_usta);
            
            String pos_tot_sta_str = Int2FormatedString(pos_gst_sta+pos_est_sta);
            String pos_tot_sta1_str = Int2FormatedString(pos_gst_sta1+pos_est_sta1);
            String pos_tot_sta2_str = Int2FormatedString(pos_gst_sta2+pos_est_sta2);
            String pos_tot_sta3_str = Int2FormatedString(pos_gst_sta3+pos_est_sta3);
            String unk_tot_sta_str = Int2FormatedString(unk_gst_sta+unk_est_sta);
            String unk_tot_sta1_str = Int2FormatedString(unk_gst_sta1+unk_est_sta1);
            String unk_tot_sta2_str = Int2FormatedString(unk_gst_sta2+unk_est_sta2);
            String unk_tot_sta3_str = Int2FormatedString(unk_gst_sta3+unk_est_sta3);
            String neg_tot_sta_str = Int2FormatedString(neg_gst_sta+neg_est_sta);
            String neg_tot_sta1_str = Int2FormatedString(neg_gst_sta1+neg_est_sta1);
            String neg_tot_sta2_str = Int2FormatedString(neg_gst_sta2+neg_est_sta2);
            String neg_tot_sta3_str = Int2FormatedString(neg_gst_sta3+neg_est_sta3);
            String neg_tot_usta_str = Int2FormatedString(neg_gst_usta+neg_est_usta);
            String pos_tot_usta_str = Int2FormatedString(pos_gst_usta+pos_est_usta);
            String unk_tot_usta_str = Int2FormatedString(unk_gst_usta+unk_est_usta);
            
            String tot_est_usta_str = Int2FormatedString(pos_est_usta+neg_est_usta+unk_est_usta);
            String tot_est_sta_str = Int2FormatedString(pos_est_sta+neg_est_sta+unk_est_sta);
            String tot_est_sta1_str = Int2FormatedString(pos_est_sta1+neg_est_sta1+unk_est_sta1);
            String tot_est_sta2_str = Int2FormatedString(pos_est_sta2+neg_est_sta2+unk_est_sta2);
            String tot_est_sta3_str = Int2FormatedString(pos_est_sta3+neg_est_sta3+unk_est_sta3);
            String tot_gst_usta_str = Int2FormatedString(pos_gst_usta+neg_gst_usta+unk_gst_usta);
            String tot_gst_sta_str = Int2FormatedString(pos_gst_sta+neg_gst_sta+unk_gst_sta);
            String tot_gst_sta1_str = Int2FormatedString(pos_gst_sta1+neg_gst_sta1+unk_gst_sta1);
            String tot_gst_sta2_str = Int2FormatedString(pos_gst_sta2+neg_gst_sta2+unk_gst_sta2);
            String tot_gst_sta3_str = Int2FormatedString(pos_gst_sta3+neg_gst_sta3+unk_gst_sta3);
            String pos_est_tot_str = Int2FormatedString(pos_est_usta+pos_est_sta);
            String pos_gst_tot_str = Int2FormatedString(pos_gst_usta+pos_gst_sta);
            String pos_tot_tot_str = Int2FormatedString(pos_est_usta+pos_est_sta+pos_gst_usta+pos_gst_sta);
            String neg_est_tot_str = Int2FormatedString(neg_est_usta+neg_est_sta);
            String neg_gst_tot_str = Int2FormatedString(neg_gst_usta+neg_gst_sta);
            String neg_tot_tot_str = Int2FormatedString(neg_est_usta+neg_est_sta+neg_gst_usta+neg_gst_sta);
            String unk_est_tot_str = Int2FormatedString(unk_est_usta+unk_est_sta);
            String unk_gst_tot_str = Int2FormatedString(unk_gst_usta+unk_gst_sta);
            String unk_tot_tot_str = Int2FormatedString(unk_est_usta+unk_est_sta+unk_gst_usta+unk_gst_sta);
            String est_tot_str = Int2FormatedString(pos_est_usta+pos_est_sta+neg_est_usta+neg_est_sta+unk_est_usta+unk_est_sta);
            String gst_tot_str = Int2FormatedString(pos_gst_usta+pos_gst_sta+neg_gst_usta+neg_gst_sta+unk_gst_usta+unk_gst_sta);
            
            
            if (g!=null) {
                int xx;
                int yy;
                
                // draw headlines
                xx = offsetx+cellwidth-2;
                yy = offsety;
                g.setFont(FONT_SMALL);
                g.setColor(Color.gray);
                g.drawString("Clear", xx, yy);
                xx += cellwidth;
                g.drawString("1+", xx, yy);
                xx += cellwidth;
                g.drawString("2+", xx, yy);
                xx += cellwidth;
                g.drawString("3+", xx, yy);
                xx += cellwidth+2;
                g.drawString("Total", xx, yy);
                xx += cellwidth;
                g.drawString("Stained", xx, yy);
                
                
                if (drawEstimated && !drawGoldstandard) {
                    // line 1: computer BEN
                    xx = offsetx;
                    yy += lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    BEN", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_0));
                    g.drawString(neg_est_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_1));
                    g.drawString(neg_est_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_2));
                    g.drawString(neg_est_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_3));
                    g.drawString(neg_est_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(neg_est_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained BEN nuclei
                    //g.setFont(FONT_BOLD);
                    //g.setColor(Color.BLACK);
                    //g.drawString((int)(Math.round(10000.0*(neg_est_sta) / (neg_est_sta+neg_est_usta))/100.0) + " %", xx, yy);                    


                    // line 2: computer MAL
                    xx = offsetx;
                    yy += lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    MAL", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_0));
                    g.drawString(pos_est_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_1));
                    g.drawString(pos_est_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_2));
                    g.drawString(pos_est_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_3));
                    g.drawString(pos_est_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(pos_est_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained MAL nuclei
                    g.setFont(FONT_BOLD);
                    g.setColor(Color.BLACK);
                    g.drawString((int)(Math.round(10000.0*(pos_est_sta) / (pos_est_sta+pos_est_usta))/100.0) + " %", xx, yy);


                    // line 3: computer UNK
                    xx = offsetx;
                    yy += lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    UNK", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_0));
                    g.drawString(unk_est_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_1));
                    g.drawString(unk_est_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_2));
                    g.drawString(unk_est_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_3));
                    g.drawString(unk_est_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(unk_est_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained UNK nuclei
                    g.setFont(FONT_BOLD);
                    g.setColor(Color.BLACK);
                    g.drawString((int)(Math.round(10000.0*(unk_est_sta) / (unk_est_sta+unk_est_usta))/100.0) + " %", xx, yy);

                    // show Staining Percentage: total stained nuclei
                    //xx += cellwidth;
                    //g.setFont(FONT_BOLD);
                    //g.setColor(Color.BLACK);
                    //g.drawString((int)(Math.round(10000.0*(neg_est_sta+pos_est_sta+unk_est_sta) / (neg_est_sta+pos_est_sta+unk_est_sta+neg_est_usta+pos_est_usta+unk_est_usta))/100.0) + " %", xx, yy);                    

                    // Draw PC label
                    g.setColor(Color.BLUE);
                    AffineTransform orig = ((Graphics2D)g).getTransform();
                    ((Graphics2D)g).rotate(-Math.PI/2, offsetx+5, yy-lineheight-2);
                    AttributedString as = new AttributedString("PC");
                    as.addAttribute(TextAttribute.FONT, FONT_SMALL);
                    as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    //as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
                    g.drawString(as.getIterator(), offsetx, yy-lineheight-2);
                    ((Graphics2D)g).setTransform(orig);
                
                } else if (!drawEstimated && drawGoldstandard) {

                    // line 1: Human BEN
                    xx = offsetx;
                    yy = offsety + lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    BEN", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_0));
                    g.drawString(neg_gst_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_1));
                    g.drawString(neg_gst_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_2));
                    g.drawString(neg_gst_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_3));
                    g.drawString(neg_gst_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(neg_gst_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained BEN nuclei
                    //g.setFont(FONT_BOLD);
                    //g.setColor(Color.BLACK);
                    //g.drawString((int)(Math.round(10000.0*(neg_gst_sta) / (neg_gst_sta+neg_gst_usta))/100.0) + " %", xx, yy);

                    // line 2: Human MAL
                    xx = offsetx;
                    yy += lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    MAL", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_0));
                    g.drawString(pos_gst_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_1));
                    g.drawString(pos_gst_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_2));
                    g.drawString(pos_gst_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_3));
                    g.drawString(pos_gst_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(pos_gst_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained MAL nuclei
                    g.setFont(FONT_BOLD);
                    g.setColor(Color.BLACK);
                    g.drawString((int)(Math.round(10000.0*(pos_gst_sta) / (pos_gst_sta+pos_gst_usta))/100.0) + " %", xx, yy);

                    // line 3: Human UNK
                    xx = offsetx;
                    yy += lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    UNK", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_0));
                    g.drawString(unk_gst_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_1));
                    g.drawString(unk_gst_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_2));
                    g.drawString(unk_gst_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_3));
                    g.drawString(unk_gst_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(unk_gst_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained UNK nuclei
                    g.setFont(FONT_BOLD);
                    g.setColor(Color.BLACK);
                    g.drawString((int)(Math.round(10000.0*(unk_gst_sta) / (unk_gst_sta+unk_gst_usta))/100.0) + " %", xx, yy);

                    // show Staining Percentage: total stained nuclei
                    //xx += cellwidth;
                    //g.setFont(FONT_BOLD);
                    //g.setColor(Color.BLACK);
                    //g.drawString((int)(Math.round(10000.0*(neg_gst_sta+pos_gst_sta+unk_gst_sta) / (neg_gst_sta+pos_gst_sta+unk_gst_sta+neg_gst_usta+pos_gst_usta+unk_gst_usta))/100.0) + " %", xx, yy);

                    // Draw PC label
                    g.setColor(Color.BLUE);
                    AffineTransform orig = ((Graphics2D)g).getTransform();
                    ((Graphics2D)g).rotate(-Math.PI/2, offsetx+5, yy-lineheight/2);
                    AttributedString as = new AttributedString("Human");
                    as.addAttribute(TextAttribute.FONT, FONT_SMALL);
                    as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    //as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
                    g.drawString(as.getIterator(), offsetx, yy-lineheight/2);
                    ((Graphics2D)g).setTransform(orig);
                
                    
                } else {

                    // line 1: Human BEN
                    xx = offsetx;
                    yy = offsety + lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    BEN", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_0));
                    g.drawString(neg_tot_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_1));
                    g.drawString(neg_tot_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_2));
                    g.drawString(neg_tot_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_NEG, TMALabel.STAINING_3));
                    g.drawString(neg_tot_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(neg_tot_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained BEN nuclei
                    //g.setFont(FONT_BOLD);
                    //g.setColor(Color.BLACK);
                    //g.drawString((int)(Math.round(10000.0*(neg_gst_sta+neg_est_sta) / (neg_gst_sta+neg_gst_usta+neg_est_sta+neg_est_usta))/100.0) + " %", xx, yy);

                    // line 2: Human MAL
                    xx = offsetx;
                    yy += lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    MAL", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_0));
                    g.drawString(pos_tot_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_1));
                    g.drawString(pos_tot_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_2));
                    g.drawString(pos_tot_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_POS, TMALabel.STAINING_3));
                    g.drawString(pos_tot_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(pos_tot_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained MAL nuclei
                    g.setFont(FONT_BOLD);
                    g.setColor(Color.BLACK);
                    g.drawString((int)(Math.round(10000.0*(pos_gst_sta+pos_est_sta) / (pos_gst_sta+pos_gst_usta+pos_est_sta+pos_est_usta))/100.0) + " %", xx, yy);

                    // line 3: Human UNK
                    xx = offsetx;
                    yy += lineheight;
                    g.setFont(FONT_SMALL);
                    g.setColor(Color.gray);
                    g.drawString("    UNK", xx, yy);
                    g.setFont(FONT_PLAIN);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_0));
                    g.drawString(unk_tot_usta_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_1));
                    g.drawString(unk_tot_sta1_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_2));
                    g.drawString(unk_tot_sta2_str, xx, yy);
                    xx += cellwidth;
                    g.setColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_3));
                    g.drawString(unk_tot_sta3_str, xx, yy);
                    xx += cellwidth+1;
                    g.setColor(Color.BLACK);
                    g.drawLine(xx-4, yy-lineheight+3, xx-4, getHeight()-10);
                    g.drawString(unk_tot_tot_str, xx, yy);
                    xx += cellwidth;
                    // show Staining Percentage: total stained UNK nuclei
                    g.setFont(FONT_BOLD);
                    g.setColor(Color.BLACK);
                    g.drawString((int)(Math.round(10000.0*(unk_gst_sta+unk_est_sta) / (unk_gst_sta+unk_gst_usta+unk_est_sta+unk_est_usta))/100.0) + " %", xx, yy);

                    // show Staining Percentage: total stained nuclei
                    //xx += cellwidth;
                    //g.setFont(FONT_BOLD);
                    //g.setColor(Color.BLACK);
                    //g.drawString((int)(Math.round(10000.0*(neg_gst_sta+pos_gst_sta+unk_gst_sta+neg_est_sta+pos_est_sta+unk_est_sta) / (neg_gst_sta+pos_gst_sta+unk_gst_sta+neg_gst_usta+pos_gst_usta+unk_gst_usta+neg_est_sta+pos_est_sta+unk_est_sta+neg_est_usta+pos_est_usta+unk_est_usta))/100.0) + " %", xx, yy);

                    // Draw PC+Hman label
                    g.setColor(Color.BLUE);
                    AffineTransform orig = ((Graphics2D)g).getTransform();
                    ((Graphics2D)g).rotate(-Math.PI/2, offsetx+5, yy);
                    AttributedString as = new AttributedString("PC+Human");
                    as.addAttribute(TextAttribute.FONT, FONT_SMALL);
                    as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    //as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
                    g.drawString(as.getIterator(), offsetx, yy);
                    ((Graphics2D)g).setTransform(orig);
                }
            }
            
        } catch (Exception ex) {
            if (tmarker.DEBUG>0) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Updated the nuclei numbers and redraws the components.
     * @param tlp The TMA_list_panel which should be updated. The new numbers are taken from its linked TMAspot.
     */
    public static void updateCellNumbers(TMAspot_list_panel tlp) {
        tlp.pos_gst_sta = tlp.ts.getNumPoints(true, true, TMALabel.LABEL_POS);
        tlp.neg_gst_sta = tlp.ts.getNumPoints(true, true, TMALabel.LABEL_NEG);
        tlp.unk_gst_sta = tlp.ts.getNumPoints(true, true, TMALabel.LABEL_UNK);
        tlp.pos_est_sta = tlp.ts.getNumPoints(false, true, TMALabel.LABEL_POS);
        tlp.neg_est_sta = tlp.ts.getNumPoints(false, true, TMALabel.LABEL_NEG);
        tlp.unk_est_sta = tlp.ts.getNumPoints(false, true, TMALabel.LABEL_UNK);
        
        tlp.pos_gst_sta1 = tlp.ts.getNumPoints(true, TMALabel.STAINING_1, TMALabel.LABEL_POS);
        tlp.neg_gst_sta1 = tlp.ts.getNumPoints(true, TMALabel.STAINING_1, TMALabel.LABEL_NEG);
        tlp.unk_gst_sta1 = tlp.ts.getNumPoints(true, TMALabel.STAINING_1, TMALabel.LABEL_UNK);
        tlp.pos_est_sta1 = tlp.ts.getNumPoints(false, TMALabel.STAINING_1, TMALabel.LABEL_POS);
        tlp.neg_est_sta1 = tlp.ts.getNumPoints(false, TMALabel.STAINING_1, TMALabel.LABEL_NEG);
        tlp.unk_est_sta1 = tlp.ts.getNumPoints(false, TMALabel.STAINING_1, TMALabel.LABEL_UNK);
        
        tlp.pos_gst_sta2 = tlp.ts.getNumPoints(true, TMALabel.STAINING_2, TMALabel.LABEL_POS);
        tlp.neg_gst_sta2 = tlp.ts.getNumPoints(true, TMALabel.STAINING_2, TMALabel.LABEL_NEG);
        tlp.unk_gst_sta2 = tlp.ts.getNumPoints(true, TMALabel.STAINING_2, TMALabel.LABEL_UNK);
        tlp.pos_est_sta2 = tlp.ts.getNumPoints(false, TMALabel.STAINING_2, TMALabel.LABEL_POS);
        tlp.neg_est_sta2 = tlp.ts.getNumPoints(false, TMALabel.STAINING_2, TMALabel.LABEL_NEG);
        tlp.unk_est_sta2 = tlp.ts.getNumPoints(false, TMALabel.STAINING_2, TMALabel.LABEL_UNK);
        
        tlp.pos_gst_sta3 = tlp.ts.getNumPoints(true, TMALabel.STAINING_3, TMALabel.LABEL_POS);
        tlp.neg_gst_sta3 = tlp.ts.getNumPoints(true, TMALabel.STAINING_3, TMALabel.LABEL_NEG);
        tlp.unk_gst_sta3 = tlp.ts.getNumPoints(true, TMALabel.STAINING_3, TMALabel.LABEL_UNK);
        tlp.pos_est_sta3 = tlp.ts.getNumPoints(false, TMALabel.STAINING_3, TMALabel.LABEL_POS);
        tlp.neg_est_sta3 = tlp.ts.getNumPoints(false, TMALabel.STAINING_3, TMALabel.LABEL_NEG);
        tlp.unk_est_sta3 = tlp.ts.getNumPoints(false, TMALabel.STAINING_3, TMALabel.LABEL_UNK);
        
        tlp.pos_gst_usta = tlp.ts.getNumPoints(true, false, TMALabel.LABEL_POS);
        tlp.neg_gst_usta = tlp.ts.getNumPoints(true, false, TMALabel.LABEL_NEG);
        tlp.unk_gst_usta = tlp.ts.getNumPoints(true, false, TMALabel.LABEL_UNK);
        tlp.pos_est_usta = tlp.ts.getNumPoints(false, false, TMALabel.LABEL_POS);
        tlp.neg_est_usta = tlp.ts.getNumPoints(false, false, TMALabel.LABEL_NEG);
        tlp.unk_est_usta = tlp.ts.getNumPoints(false, false, TMALabel.LABEL_UNK);
        
        tlp.bg_gst = tlp.ts.getNumBG_goldst();
        //tlp.drawNucleiCounts(tlp.getGraphics());
        
        //update H-Score
        tlp.ts.getHScore();
        
        tlp.repaint();
    }
    
    /**
     * Returns a string representation of the integer i. If i > 999, the string
     * uses the "k"(ilo) abbrevation (5 -> "5", 5123 -> "5.1k").
     * @param i The integer to be formated.
     * @return A formated string representation of i.
     */
    static String Int2FormatedString(int i) {
        return i > 999 ? Math.round(i / 100.0) / 10.0 + "k" : Integer.toString(i);
    }
            

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel6;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the current height of this component.
     * There are two heights: Expanded height and unexpanded height.
     * When this component is expanded by clicking on a cross (might be disabled for TMARKER Light),
     * more information can be displayed.
     * @return The current height of this component.
     */
    public int getListItemHeight() {
        if (isExpanded) {
            return HEIGHT_EXPANDED;
        } else {
            return HEIGHT_UNEXPANDED;
        }
    }
    
    /**
     * Returns the horizontal offset (= thumbnail width), after which the
     * cell counts are drawn.
     * @return The hoizontal offset.
     */
    public int getOffsetx() {
        return offsetx-5;
    }

    /**
     * Returns the vertical offset (= height of image name), after which the
     * cell counts are drawn.
     * @return The vertical offset.
     */
    public int getOffsety() {
        return offsety;
    }

    /**
     * Resizes the component and fits it to the width of the containing JScrollPane.
     */
    private void sizeComponent() {
        if (ts!=null && ts.getCenter()!=null && ts.getCenter().getTMAListScrollPane()!=null) {
            dim.width = Math.max(100, ts.getCenter().getTMAListScrollPane().getWidth());
        }
        if (isExpanded) {
            dim.height = HEIGHT_EXPANDED;
        } else {
            dim.height = HEIGHT_UNEXPANDED;
        }
        setMinimumSize(dim);
        setMaximumSize(dim);
    }

    /**
     * Whether or not the position (x,y) is in the area showing the overall staining.
     * @param x The x-coord.
     * @param y The y-coord.
     * @return True, if the position (x,y) is in the area showing the overall staining.
     */
    private boolean isInStainingField(int x, int y) {
        return x >= offsetx + 6*cellwidth && x < offsetx + 7*cellwidth && y >= offsety+2*lineheight && y < offsety + 3*lineheight;
//                x >= offsetx + 5*cellwidth && x < offsetx + 6*cellwidth && y >= offsety && y < offsety+3*lineheight;
    }
    
    /**
     * Whether or not the position (x,y) is in the area showing the staining estimation.
     * @param x The x-coord.
     * @param y The y-coord.
     * @return True, if the position (x,y) is in the area showing the staining estimation.
     */
    private boolean isInMalignantStainingField(int x, int y) {
        return x >= offsetx + 6*cellwidth && x < offsetx + 7*cellwidth && y >= offsety+lineheight && y < offsety+2*lineheight;
    }
    
    /**
     * Whether or not the position (x,y) is in the area for showing the gold-standard or estimated nuclei.
     * @param x The x-coord.
     * @param y The y-coord.
     * @return True, if the position (x,y) is in the area for showing the gold-standard or estimated nuclei.
     */
    private boolean isInToggleShowGstEstField(int x, int y) {
        return x >= offsetx-4 && x < offsetx+10 && y >= offsety+6 && y < HEIGHT_UNEXPANDED-2;
    }
    
    /**
     * Whether or not the position (x,y) is in the area showing the close button field.
     * @param x The x-coord.
     * @param y The y-coord.
     * @return True, if the position (x,y) is in the area showing the close button field.
     */
    private boolean isInCloseField(int x, int y) {
        return x >= getWidth()-13 && x < getWidth()-2 && y >= 2 && y < 12;
    }
    
    /**
     * Whether or not the position (x,y) is in the area showing the expand field.
     * @param x The x-coord.
     * @param y The y-coord.
     * @return True, if the position (x,y) is in the area showing the expand field.
     */
    private boolean isInExpandField(int x, int y) {
        return false; //x >= HEIGHT_UNEXPANDED+5 && x < HEIGHT_UNEXPANDED+15 && y >= 2 && y < 12;
    }

    /**
     * Whether or not this component is expanded.
     * @return True, if this component is expanded.
     */
    public boolean isExpanded() {
        return false; // isExpanded;
    }

    /**
     * Switches between isExpanded and !isExpanded.
     */
    public void toggleExpansion() {
        isExpanded = !isExpanded;
        sizeComponent();
    }
    
    /**
     * Switches between isShowingEstimated, isShowingGoldstandard and both.
     */
    public void toggleShowGstOrEst() {
        if (isShowingEstimated && !isShowingGoldstandard) {
            isShowingEstimated = false;
            isShowingGoldstandard = true;
        } else if (!isShowingEstimated && isShowingGoldstandard) {
            isShowingEstimated = true;
        } else {
            isShowingEstimated = true;
            isShowingGoldstandard = false;
        }
        repaint();
    }
    
    /**
     * Returns whether or not the esitimated information should be shown (or the gold-standard information or both).
     * @return True, if the esitimated information should be shown.
     */
    public boolean isShowingEstimated() {
        return isShowingEstimated;
    }
    
    /**
     * Returns whether or not the human gold standard information should be shown (or the estimated PC information or both).
     * @return True, if the gold standard information should be shown.
     */
    public boolean isShowingGoldstandard() {
        return isShowingGoldstandard;
    }

    /**
     * What to do on a mouse click. Location dependent actions are performed such as
     * removal of the TMAspot or expansion of the component.
     * @param evt The mouse click event.
     */
    private void doMouseClick(MouseEvent evt) {
        // Mark samples according to Win LAF (Ctr+click, shift+click)
        if (evt.isControlDown() && !evt.isShiftDown()) {
            ts.setSelected(!ts.isSelected());
            this.requestFocus();
        }

        // Mark samples according to Win LAF (Ctr+click, shift+click)
        // shift+click
        else if (evt.isShiftDown()) {
            int i_this = ts.getCenter().getTMAspots().indexOf(ts);
            int i_last = -1;
            for (int i=0, n=ts.getCenter().getTMAspots().size(); i<n; i++) {
                if (ts.getCenter().getTMAspots().get(i).getTLP().hasFocus()) {
                    i_last = i;
                    break;
                }
            }
            i_last = Math.max(i_last, 0);

            // shift and click on the one way
            if (i_last<i_this) {
                if (!evt.isControlDown()) {
                    for (int i=0; i<i_last; i++) {
                        ts.getCenter().getTMAspots().get(i).setSelected(false);
                    }
                }
                for (int i=i_last; i<=i_this; i++) {
                    ts.getCenter().getTMAspots().get(i).setSelected(true);
                }
                if (!evt.isControlDown()) {
                    for (int i=i_this+1; i<ts.getCenter().getTMAspots().size(); i++) {
                        ts.getCenter().getTMAspots().get(i).setSelected(false);
                    }
                }
            }

            // shift + click on the other way
            else {
                if (!evt.isControlDown()) {
                    for (int i=ts.getCenter().getTMAspots().size()-1; i>i_last; i--) {
                        ts.getCenter().getTMAspots().get(i).setSelected(false);
                    }
                }
                for (int i=i_last; i>=i_this; i--) {
                    ts.getCenter().getTMAspots().get(i).setSelected(true);
                }
                if (!evt.isControlDown()) {
                    for (int i=i_this-1; i>=0; i--) {
                        ts.getCenter().getTMAspots().get(i).setSelected(false);
                    }
                }
            }

        }

        // no shift and not ctr key is pressed and double click
        else if (evt.getClickCount()==2) {
        
            final SortedProperties props = ts.getProperties();
            //if (!props.isEmpty()) {
                String[] columnNames = {"Name", "Value"};
                String[][] rowData = new String[props.size()][2];
                Enumeration<Object> keys = props.keys();
                int i = 0;
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    rowData[i][0] = key;
                    rowData[i][1] = props.getProperty(key);
                    i++;
                }
                JXTable propTable = new JXTable(rowData, columnNames);
                JScrollPane scrollpane = new JScrollPane(propTable);
                final JXDialog dialog = new JXDialog(ts.getCenter(), scrollpane);
                dialog.getContentPane().setLayout(new javax.swing.BoxLayout(dialog.getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));
                if (ts.isNDPI()) {
                    JPanel saveLevelsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 15, 15));
                    String[] levelData = new String[ts.getNDPI().getLevelCount()];
                    for (int k = 0; k < ts.getNDPI().getLevelCount(); k++) {
                        levelData[k] = "Level " + k + " (" + props.getProperty("openslide.level[" + k + "].width") + " x " + props.getProperty("openslide.level[" + k + "].height") + ", " + String.format("%.2f", (Double.valueOf(props.getProperty("openslide.objective-power"))/Double.valueOf(props.getProperty("openslide.level[" + k + "].downsample")))) + "x)";
                    }
                    final JComboBox levels = new JComboBox(levelData);
                    saveLevelsPanel.add(levels);
                    final JButton saveAsJPG = new JButton("Save as JPG...");
                    saveAsJPG.setToolTipText("Only images smaller than " + Integer.MAX_VALUE + "px are supported.");
                    levels.addItemListener(new java.awt.event.ItemListener() {
                        @Override
                        public void itemStateChanged(java.awt.event.ItemEvent evt) {
                            if (evt.getStateChange()==java.awt.event.ItemEvent.SELECTED) {
                                int k = levels.getSelectedIndex();
                                int w = Integer.parseInt(props.getProperty("openslide.level[" + k + "].width"));
                                int h = Integer.parseInt(props.getProperty("openslide.level[" + k + "].height"));
                                saveAsJPG.setEnabled(h<Integer.MAX_VALUE/w);
                            }
                        }
                    });
                    levels.setSelectedIndex(1);
                    levels.setSelectedIndex(0);
                    saveAsJPG.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            int k = levels.getSelectedIndex();
                            try {
                                List<String> ext = new ArrayList();
                                List<String> expl = new ArrayList();
                                ext.add("jpg");
                                expl.add("JPEG Image");
                                File file = FileChooser.chooseSavingFile(ts.getCenter(), ts.getCenter().getCurrentDir(), Misc.FilePathStringtoFilenameWOExtension(ts.getName()) + "_level" + Integer.toString(k) + ".jpg", ext, expl);
                                if (file != null) {
                                    dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                    ts.getCenter().setCurrentDir(file.getParent());
                                    BufferedImage bi = ts.getNDPI().createThumbnailImage(Math.max(Integer.parseInt(props.getProperty("openslide.level[" + k + "].width")), Integer.parseInt(props.getProperty("openslide.level[" + k + "].height"))));
                                    Misc.writeImage(bi, file.getAbsolutePath());
                                    dialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(TMAspot_list_panel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    saveLevelsPanel.add(saveAsJPG);
                    dialog.add(saveLevelsPanel, 1);
                }
                dialog.setTitle(ts.getName() + " - Properties");
                dialog.revalidate();
                dialog.pack();
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);
            //}
            
        // no shift and not ctr key is pressed and single click
        } else if (evt.getClickCount()==1) {
            
            int x = evt.getX();
            int y = evt.getY();

            // for debugging
            if (tmarker.DEBUG>4) {
                String info = "TMAspot_list_panel: Mouse clicked (X,Y) = ";
                info += evt.getX() + "\n";
                info += evt.getY() + "\n";
                info += evt.getSource() + "\n";
                info += evt.getSource().getClass() + "\n";
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, info);
            }
            
            // close this TMAspot
            if (isInCloseField(x, y)) {
                List<TMAspot> tss = new ArrayList<>();
                tss.add(ts);
                ts.getCenter().removeTMAspots(tss);
                return;
            }
            
            // Show Gold Standard Points Statistics
            else if (isInToggleShowGstEstField(x, y)) {
                toggleShowGstOrEst();
                //if (isShowingEstimated) {
                //    isShowingEstimated = false;
                //    this.repaint();
                //}
            }

            else {
                int k = 0;
                for (int i=0, n=ts.getCenter().getTMAspots().size(); i<n; i++) {
                    TMAspot s = ts.getCenter().getTMAspots().get(i);
                    if (s.isSelected()) k++;
                    if (s!=ts) s.setSelected(false);
                }
                ts.setSelected(k > 1 || !ts.isSelected());
            }
            this.requestFocus();
            List<TMAspot> tss = ts.getCenter().getSelectedTMAspots(false);
            if (!tss.isEmpty()) {
                ts.getCenter().showTMAspot(tss.get(0));
            } else {
                ts.getCenter().showTMAspot(null);
            }
            if (!ts.isSelected()) {
                setBackground(bg_mouseOver);
                setBorder(new javax.swing.border.LineBorder(bg_mouseOver.darker(), 1, true));
            }
        }
        //ts.getCenter().setButtonStates_SampleSelected(!ts.getCenter().getSelectedTMAspots().isEmpty());
    }

    /**
     * Sets the thumbnail image created from the original sized image I.
     * @param I The original (large) sized image I.
     */
    public void setThumbnailImage(BufferedImage I) {
        if (!thumbnailSet){
            if (I==null) {
                I = ts.getBufferedImage();
            }
            if (I==null) { // if I still is null, since the image could not be loaded..
                if (tmarker.DEBUG==0) {
                    return;
                }
            }
            BufferedImage T;
            T = Scalr.resize(I, Method.SPEED, HEIGHT_UNEXPANDED-2);
            jLabel6.setIcon(new ImageIcon(T));
            thumbnailSet = true;
            if (I!=null) {
                I.flush();
                I = null;
            }
            T.flush();
            T = null;
        }
    }

}

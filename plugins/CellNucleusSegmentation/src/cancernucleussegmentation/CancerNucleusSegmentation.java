/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cancernucleussegmentation;

import TMARKERPluginInterface.Pluggable;
import TMARKERPluginInterface.PluginManager;
import graphcut.ConnectComponent;
import graphcut.GraphCut;
import graphcut.Terminal;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.GaussianBlur;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import plugins.TMARKERPluginManager;
import stainingestimation.StainingEstimation;
import superpixels.SLIC;
import superpixels.SLICsuperpixels;
import superpixels.Superpixel;
import tmarker.TMAspot.TMALabel;
import tmarker.TMAspot.TMApoint;
import tmarker.TMAspot.TMAspot;
import tmarker.misc.Misc;
import tmarker.misc.StringToIntConverter;
import tmarker.misc.lROI;
import tmarker.tmarker;

/**
 *
 * @author Peter J. Schueffler
 */
public class CancerNucleusSegmentation extends javax.swing.JFrame implements Pluggable {

    // For Plugin handling
    PluginManager manager = null;
    private static final String PLUGINNAME = "Nucleus Segmentation";
    private static final String PLUGINVERSION = "1." + java.util.ResourceBundle.getBundle("cancernucleussegmentation/Bundle").getString("build");

    /**
     * Used for Binding a JSlider to a JTextField. The String text is converted to int which is synchronized with the slider.
     */
    private final StringToIntConverter stic = new StringToIntConverter();
    
    /**
     * Creates new form CancerNucleusSegmentation
     */
    public CancerNucleusSegmentation() {
        initComponents();
    }

    /**
     * Returns whether or not the segmentations should be drawn in TMARKER main
     * window.
     *
     * @return True, if the segmentations should be drawn in TMARKER main
     * window.
     */
    boolean getParam_ShowSegmentations() {
        return jCheckBox4.isSelected();
    }

    /**
     * Sets the parameter whether or not the segmentations should be drawn in
     * TMARKER main window.
     *
     * @param b True, if the segmentations should be drawn in TMARKER main
     * window.
     */
    void setParam_ShowSegmentations(boolean b) {
        jCheckBox4.setSelected(b);
    }

    /**
     * Returns whether or not including and excluding ROI should be considered
     * for segmentation.
     *
     * @return True, if including and excluding ROI should be considered for
     * segmentation.
     */
    boolean getParam_respectROI() {
        return jCheckBox6.isSelected();
    }

    /**
     * Sets whether or not including and excluding ROI should be considered for
     * segmentation.
     *
     * @param b True, if including and excluding ROI should be considered for
     * segmentation.
     */
    void setParam_respectROI(boolean b) {
        jCheckBox6.setSelected(b);
    }

    /**
     * Returns whether GraphCut algorithm should be used for segmentation or
     * Superpixels.
     *
     * @return True, if GraphCut algorithm should be used for segmentation.
     * Otherwise, use Superpixels.
     */
    boolean getParam_useGraphcut() {
        return jRadioButton13.isSelected();
    }

    /**
     * Sets whether GraphCut algorithm should be used for segmentation or
     * Superpixels.
     *
     * @param b True, if GraphCut algorithm should be used for segmentation.
     * Otherwise, use Superpixels.
     */
    void setParam_useGraphcut(boolean b) {
        jRadioButton13.setSelected(b);
        jRadioButton14.setSelected(!b);
    }

    /**
     * Returns the balance between 0 (circular shape) and 100 (arbitrary shape)
     * for the graphcut algorithm.
     *
     * @return The balance between 0 (circular shape) and 100 (arbitrary shape)
     * for the graphcut algorithm.
     */
    int getParam_GCShapeBalance() {
        return jSlider4.getValue();
    }

    /**
     * Sets the balance between 0 (circular shape) and 100 (arbitrary shape) for
     * the graphcut algorithm.
     *
     * @param b The balance between 0 (circular shape) and 100 (arbitrary shape)
     * for the graphcut algorithm.
     */
    void setParam_GCShapeBalance(int b) {
        jSlider4.setValue(b);
    }

    /**
     * Returns the image modality which should be used for segmentation.
     *
     * @return 0 for original image, 1 for channel 1 image (if any), 2 for
     * channel 2 image(if any).
     */
    int getParam_imageChannel() {
        if (jRadioButton1.isSelected()) {
            return 0;
        }
        if (jRadioButton2.isSelected()) {
            return 1;
        }
        return 2;
    }

    /**
     * Sets the image modality which should be used for segmentation.
     *
     * @param b 0 for original image, 1 for channel 1 image (if any), 2 for
     * channel 2 image(if any).
     */
    void setParam_imageChannel(int b) {
        jRadioButton1.setSelected(b == 0);
        jRadioButton2.setSelected(b == 1);
        jRadioButton3.setSelected(b == 2);
    }

    /**
     * Returns the blurring radius for smoothing the images before segmentation.
     *
     * @return The blurring radius for smoothing the images before segmentation.
     */
    double getParam_blur() {
        return jSlider2.getValue()/10.0;
    }

    /**
     * Sets the blurring radius for smoothing the images before segmentation.
     *
     * @param blur The blurring radius for smoothing the images before
     * segmentation.
     */
    void setParam_blur(double blur) {
        jSlider2.setValue((int)(10*blur));
    }

    /**
     * Returns whether or not the blurring should be drawn in TMARKER main
     * window. This might be slow if there are a lot of labels.
     *
     * @return True, if the blurring should be drawn in TMARKER main window.
     */
    boolean getParam_ShowBlur() {
        return jCheckBox3.isSelected();
    }

    /**
     * Sets the parameter whether or not the blurring should be drawn in TMARKER
     * main window. Showing blurring might be slow if there are a lot of labels.
     *
     * @param b True, if the blurring should be drawn in TMARKER main window.
     */
    void setParam_ShowBlur(boolean b) {
        jCheckBox3.setSelected(b);
    }

    /**
     * Returns the patch size of the nucleus patches for segmentation.
     *
     * @return The patch size of the nucleus patches for segmentation.
     */
    int getParam_patchsize() {
        return jSlider1.getValue();
    }

    /**
     * Sets the patch size of the nucleus patches for segmentation.
     *
     * @param s The patch size of the nucleus patches for segmentation.
     */
    void setParam_patchsize(int s) {
        jSlider1.setValue(s);
    }

    /**
     * Returns whether or not the patches should be drawn in TMARKER main
     * window.
     *
     * @return True, if the patches should be drawn in TMARKER main window.
     */
    boolean getParam_ShowPatches() {
        return jCheckBox2.isSelected();
    }

    /**
     * Sets the parameter whether or not the patches should be drawn in TMARKER
     * main window.
     *
     * @param b True, if the patches should be drawn in TMARKER main window.
     */
    void setParam_ShowPatches(boolean b) {
        jCheckBox2.setSelected(b);
    }
    
    /**
     * Returns the color of the segmentation outline as int.
     *
     * @return The color of the segmentation outline as int.
     */
    int getParam_segmentationColor() {
        return jXColorSelectionButton3.getBackground().getRGB();
    }

    /**
     * Sets the color of the segmentation outline.
     *
     * @param c The color of the segmentation outline as int.
     */
    void setParam_segmentationColor(int c) {
        jXColorSelectionButton3.setBackground(new Color(c));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jSlider4 = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        jRadioButton14 = new javax.swing.JRadioButton();
        jRadioButton13 = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        jCheckBox4 = new javax.swing.JCheckBox();
        jCheckBox6 = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jSlider1 = new javax.swing.JSlider();
        jSlider2 = new javax.swing.JSlider();
        jTextField2 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jXColorSelectionButton3 = new org.jdesktop.swingx.JXColorSelectionButton();
        jLabel8 = new javax.swing.JLabel();

        setTitle(PLUGINNAME + " v" + PLUGINVERSION);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Nucleus Segmentation"));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Circular Shape");
        jLabel3.setToolTipText("More weight on circular objects");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton13, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel3, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel3Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(11, 35, 0, 5);
        jPanel3.add(jLabel3, gridBagConstraints);

        jSlider4.setToolTipText(Integer.toString(jSlider4.getValue()));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton13, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jSlider4, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jSlider4Binding");
        bindingGroup.addBinding(binding);

        jSlider4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider4StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        jPanel3.add(jSlider4, gridBagConstraints);

        jLabel4.setText("Arbitrary Shape");
        jLabel4.setToolTipText("More weight on intensity information");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton13, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel4, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel4Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 5);
        jPanel3.add(jLabel4, gridBagConstraints);

        jLabel5.setText("<html><p>Nucleus segmentation depends on the global <b>nucleus radius</b>.</p></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        jPanel3.add(jLabel5, gridBagConstraints);

        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 15, 5));

        jButton5.setText("Segment Nuclei");
        jButton5.setToolTipText("Segment Nuclei");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton5);

        jButton7.setText("Save Segmentation...");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton7);

        jButton6.setText("Clear Segmentation");
        jButton6.setToolTipText("Delete nuclei segmentations.");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton6);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 5);
        jPanel3.add(jPanel4, gridBagConstraints);

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("original image");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        jPanel3.add(jRadioButton1, gridBagConstraints);

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText("channel 1 image (if any)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel3.add(jRadioButton2, gridBagConstraints);

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setText("channel 2 image (if any)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel3.add(jRadioButton3, gridBagConstraints);

        jLabel6.setText("The segmentation should be based on the:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        jPanel3.add(jLabel6, gridBagConstraints);

        buttonGroup2.add(jRadioButton14);
        jRadioButton14.setText("Superpixel Segmentation (slower)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 0);
        jPanel3.add(jRadioButton14, gridBagConstraints);

        buttonGroup2.add(jRadioButton13);
        jRadioButton13.setSelected(true);
        jRadioButton13.setText("Graphcut Segmentation (faster)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 0);
        jPanel3.add(jRadioButton13, gridBagConstraints);

        jLabel1.setText("Select Segmentation Algorithm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 0);
        jPanel3.add(jLabel1, gridBagConstraints);

        jSeparator5.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel3.add(jSeparator5, gridBagConstraints);

        jCheckBox4.setSelected(true);
        jCheckBox4.setToolTipText("View the segmentations in TMARKER (can be slow)");
        jCheckBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox4ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        jPanel3.add(jCheckBox4, gridBagConstraints);

        jCheckBox6.setText("<html>Respect 'including' or'excluding' ROI (if any)</html>");
        jCheckBox6.setToolTipText("<html>If selected, regions of interest are considered for nucleus segmentation.<br> If there are no ROIs, the whole image is considered.<br> ROI are defined in the TMARKER main window.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 2, 0);
        jPanel3.add(jCheckBox6, gridBagConstraints);

        jLabel2.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 5, 0);
        jPanel3.add(jLabel2, gridBagConstraints);

        jLabel17.setText("patch size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 5, 0, 0);
        jPanel3.add(jLabel17, gridBagConstraints);

        jLabel19.setText("blur");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 5, 0, 0);
        jPanel3.add(jLabel19, gridBagConstraints);

        jTextField1.setColumns(3);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField1.setText("50");
        jTextField1.setToolTipText("If no cell segmentation is possible, cells are approx. circles with this diameter.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 5, 0, 5);
        jPanel3.add(jTextField1, gridBagConstraints);

        jSlider1.setMinimum(1);
        jSlider1.setSnapToTicks(true);
        jSlider1.setToolTipText("If no cell segmentation is possible, cells are approx. circles with this diameter.");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, jTextField1, org.jdesktop.beansbinding.ELProperty.create("${text}"), jSlider1, org.jdesktop.beansbinding.BeanProperty.create("value"), "jSlider1Binding");
        binding.setSourceNullValue(5);
        binding.setSourceUnreadableValue(5);
        binding.setConverter(stic);
        bindingGroup.addBinding(binding);

        jSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider1StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        jPanel3.add(jSlider1, gridBagConstraints);

        jSlider2.setSnapToTicks(true);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, jTextField2, org.jdesktop.beansbinding.ELProperty.create("${text}"), jSlider2, org.jdesktop.beansbinding.BeanProperty.create("value"), "jSlider2Binding");
        binding.setSourceNullValue(0);
        binding.setSourceUnreadableValue(0);
        binding.setConverter(stic);
        bindingGroup.addBinding(binding);

        jSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider2StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        jPanel3.add(jSlider2, gridBagConstraints);

        jTextField2.setColumns(3);
        jTextField2.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField2.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 5, 0, 5);
        jPanel3.add(jTextField2, gridBagConstraints);

        jButton4.setText("Auto size");
        jButton4.setToolTipText("Cell size is set to 4 * Nucleus Radius");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        jPanel3.add(jButton4, gridBagConstraints);

        jCheckBox2.setSelected(true);
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 0);
        jPanel3.add(jCheckBox2, gridBagConstraints);

        jCheckBox3.setToolTipText("");
        jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox3ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 0);
        jPanel3.add(jCheckBox3, gridBagConstraints);

        jLabel7.setText("Preview");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel3.add(jLabel7, gridBagConstraints);

        jXColorSelectionButton3.setToolTipText("Segmentation color");
        jXColorSelectionButton3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jXColorSelectionButton3StateChanged(evt);
            }
        });
        jXColorSelectionButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jXColorSelectionButton3ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        jPanel3.add(jXColorSelectionButton3, gridBagConstraints);

        jLabel8.setText("color");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        jPanel3.add(jLabel8, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 183;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(jPanel3, gridBagConstraints);

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jSlider4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider4StateChanged
        jSlider4.setToolTipText(Integer.toString(jSlider4.getValue()));
    }//GEN-LAST:event_jSlider4StateChanged

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        performSegmentation();
        setParam_ShowSegmentations(true);
        if (manager != null && manager.getVisibleTMAspot() != null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        clearSegmentations();
        if (manager != null && manager.getVisibleTMAspot() != null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        saveSegmentationMask();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox4ActionPerformed
        if (manager != null && manager.getVisibleTMAspot() != null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jCheckBox4ActionPerformed

    private void jSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider1StateChanged
        if (manager != null && manager.getVisibleTMAspot() != null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jSlider1StateChanged

    private void jSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider2StateChanged
        if (manager != null && manager.getVisibleTMAspot() != null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jSlider2StateChanged

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        jSlider1.setValue(4 * manager.getLabelRadius());
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        if (manager != null && manager.getVisibleTMAspot() != null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox3ActionPerformed
        if (manager != null && manager.getVisibleTMAspot() != null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jCheckBox3ActionPerformed

    private void jXColorSelectionButton3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jXColorSelectionButton3StateChanged
        if (manager!=null && manager.getVisibleTMAspot()!=null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jXColorSelectionButton3StateChanged

    private void jXColorSelectionButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jXColorSelectionButton3ActionPerformed
        if (manager!=null && manager.getVisibleTMAspot()!=null) {
            manager.repaintVisibleTMAspot();
        }
    }//GEN-LAST:event_jXColorSelectionButton3ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CancerNucleusSegmentation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CancerNucleusSegmentation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CancerNucleusSegmentation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CancerNucleusSegmentation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CancerNucleusSegmentation().setVisible(true);
            }
        });
    }

    @Override
    public String getAuthor() {
        return "Peter J. Schüffler";
    }

    @Override
    public String getVersion() {
        return PLUGINVERSION;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        setVisible(false);
        return true;
    }

    @Override
    public void setPluginManager(PluginManager manager) {
        this.manager = manager;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getPluginName() {
        return PLUGINNAME;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        this.setVisible(true);
        updateOptionsToTMAspot(manager.getVisibleTMAspot(), manager.getSelectedTMAspots());
    }

    @Override
    public void setParameterDefaults() {
        setParam_patchsize(10);
        setParam_blur(0);
        setParam_ShowPatches(true);
        setParam_ShowBlur(false);
        setParam_ShowSegmentations(true);
        setParam_respectROI(true);
        setParam_useGraphcut(true);
        setParam_GCShapeBalance(50);
        setParam_imageChannel(0);
        setParam_segmentationColor(Color.RED.getRGB());
    }

    @Override
    public void setParameters(Properties parameters) {
        String value;
        value = parameters.getProperty("patchsize");
        if (value != null) {
            setParam_patchsize(Integer.parseInt(value));
        }
        value = parameters.getProperty("blur");
        if (value != null) {
            setParam_blur(Double.parseDouble(value));
        }
        value = parameters.getProperty("ShowPatches");
        if (value != null) {
            setParam_ShowPatches(Boolean.parseBoolean(value));
        }
        value = parameters.getProperty("ShowBlur");
        if (value != null) {
            setParam_ShowBlur(Boolean.parseBoolean(value));
        }
        value = parameters.getProperty("showSegmentations");
        if (value != null) {
            setParam_ShowSegmentations(Boolean.parseBoolean(value));
        }
        value = parameters.getProperty("respectROI");
        if (value != null) {
            setParam_respectROI(Boolean.parseBoolean(value));
        }
        value = parameters.getProperty("useGraphcut");
        if (value != null) {
            setParam_useGraphcut(Boolean.parseBoolean(value));
        }
        value = parameters.getProperty("GCShapeBalance");
        if (value != null) {
            setParam_GCShapeBalance(Integer.parseInt(value));
        }
        value = parameters.getProperty("imageChannel");
        if (value != null) {
            setParam_imageChannel(Integer.parseInt(value));
        }
        value = parameters.getProperty("segmentationColor");
        if (value != null) {
            setParam_segmentationColor(Integer.parseInt(value));
        }
    }

    @Override
    public Properties getParameters() {
        Properties parameters = new Properties();
        parameters.setProperty("patchsize", Integer.toString(getParam_patchsize()));
        parameters.setProperty("blur", Double.toString(getParam_blur()));
        parameters.setProperty("ShowPatches", Boolean.toString(getParam_ShowPatches()));
        parameters.setProperty("ShowBlur", Boolean.toString(getParam_ShowBlur()));
        parameters.setProperty("showSegmentations", Boolean.toString(getParam_ShowSegmentations()));
        parameters.setProperty("respectROI", Boolean.toString(getParam_respectROI()));
        parameters.setProperty("useGraphcut", Boolean.toString(getParam_useGraphcut()));
        parameters.setProperty("GCShapeBalance", Integer.toString(getParam_GCShapeBalance()));
        parameters.setProperty("imageChannel", Integer.toString(getParam_imageChannel()));
        parameters.setProperty("segmentationColor", Integer.toString(getParam_segmentationColor()));

        return parameters;
    }

    @Override
    public String getHTMLReport(String HTMLFolderName) {
        String output = "<html>";
        output += "Cell Nucleus Segmentation has been performed.";
        output += "</html>";
        return output;
    }

    @Override
    public void updateOptionsToTMAspot(TMAspot visible_TMAspot, List<TMAspot> selected_TMAspots) {
        if (manager != null && manager.getVisibleTMAspot() != null) {
            manager.updateTMAspot(manager.getVisibleTMAspot());
            manager.repaintVisibleTMAspot();
        }
    }

    @Override
    public void drawInformationPreNuclei(TMAspot ts, Graphics g, double z, int x_min, int y_min, int x_max, int y_max) {
        if (isVisible()) {
            
            BufferedImage I = null;
            GaussianBlur blur = new GaussianBlur();
            
            if (getParam_ShowBlur()) {
                if (!ts.isNDPI()) {
                    if (getParam_imageChannel() == 1) {
                        I = getChannelImage(ts, StainingEstimation.SHOW_CHANNEL1_IMAGE);
                    } else if (getParam_imageChannel() == 2) {
                        I = getChannelImage(ts, StainingEstimation.SHOW_CHANNEL2_IMAGE);
                    }
                    if (I == null) {
                        I  = ts.getBufferedImage();
                    }

                    //Blur the image for smoother features
                    //if (getParam_blur() > 0) {
                        ImagePlus ip = new ImagePlus(" ", I);
                        blur.blurGaussian(ip.getProcessor(), getParam_blur(), getParam_blur(), 0.02);
                        I = ip.getBufferedImage();
                    //}

                }
            }
            
            int[] progress_container = new int[]{1};
            DrawInformationPreNucleiFork fb = new DrawInformationPreNucleiFork((TMARKERPluginManager) manager, this, ts, ts.getPoints(), I, g, z, x_min, y_min, x_max, y_max, 0, ts.getPoints().size(), true, progress_container);
            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(fb);
            pool.shutdown();
        }
    }

    /**
     * Draws the segmentation of a given TMAspot and TMApoint on the graphics.
     * This function is static as it can be used for parallel processing.
     *
     * @param cns The living instance of CancerNucleusSegmentation.
     * @param ts The TMAspot to be drawn.
     * @param tp The TMApoint to be drawn.
     * @param g The graphics on which is drawn.
     * @param z The zoom factor of this graphics (TMARKER support zooming the
     * image).
     * @param x_min The field of view which is visible in TMARKER. We don't need
     * to draw something out of these bounds.
     * @param y_min The field of view which is visible in TMARKER. We don't need
     * to draw something out of these bounds.
     * @param x_max The field of view which is visible in TMARKER. We don't need
     * to draw something out of these bounds.
     * @param y_max The field of view which is visible in TMARKER. We don't need
     * to draw something out of these bounds.
     */
    public static void drawInformationPreNucleus(CancerNucleusSegmentation cns, TMAspot ts, TMApoint tp, BufferedImage I, Graphics g, double z, int x_min, int y_min, int x_max, int y_max) {
        if (cns.isVisible()) {
            
            int d = cns.getParam_patchsize();
            int r = d / 2;

            // blur the patch
            if (cns.getParam_ShowBlur()) {
                if (tp.x >= x_min && tp.y >= y_min && tp.x < x_max && tp.y < y_max) {
                    try {
                        BufferedImage patch;
                        if (ts.isNDPI() || I==null) {
                            
                            GaussianBlur blur = new GaussianBlur();
                            patch = ts.getSubimage((int) (tp.getX() - r), (int) (tp.getY() - r), d, d, d, BufferedImage.TYPE_INT_ARGB, I);

                            // get here the channel images of the Patch of an NDPI
                            if (cns.getParam_imageChannel() > 0) {
                                Pluggable p = cns.manager.getPlugin("Color Deconvolution");
                                if (p != null) {
                                     {
                                        List<ImagePlus> HE = StainingEstimation.deconvolveImage(patch, ts.getCenter(), (StainingEstimation) p, ts, true, ((StainingEstimation) p).getParam_ColorChannel(), ((StainingEstimation) p).getParam_substractChannels(), ((StainingEstimation) p).getParam_invertCH1(), ((StainingEstimation) p).getParam_invertCH2(), ((StainingEstimation) p).getParam_invertCH3(), false);
                                        if (cns.getParam_imageChannel() == 1 && HE != null && HE.size() > 0 && HE.get(0) !=null) {
                                            patch = HE.get(0).getBufferedImage();
                                        } else if (HE != null && HE.size() > 0 && HE.get(0) !=null) {
                                            patch = HE.get(1).getBufferedImage();
                                        }
                                    } 
                                }
                            }
                            //Blur the image for smoother features
                            if (cns.getParam_blur() > 0) {
                                ImagePlus ip = new ImagePlus(" ", patch);
                                blur.blurGaussian(ip.getProcessor(), cns.getParam_blur(), cns.getParam_blur(), 0.02);
                                patch = ip.getBufferedImage();
                            }

                        } else {
                            patch = I.getSubimage((int) (tp.getX() - r), (int) (tp.getY() - r), d, d);
                        }
                        
                        if ((int) (d * z) > 0 && (int) (d * z) > 0) {
                            g.drawImage(patch.getScaledInstance((int) (d * z), (int) (d * z), Image.SCALE_SMOOTH), (int) ((tp.getX() - r) * z), (int) ((tp.getY() - r) * z), null);
                        }
                    } catch (Exception e) {

                    }
                }
            }
            
            g.setColor(new Color(cns.getParam_segmentationColor()));

            // Draw the patches around the nuclei
            if (cns.getParam_ShowPatches()) {
                Stroke old_stroke = ((Graphics2D) g).getStroke();
                if (tp.x >= x_min && tp.y >= y_min && tp.x < x_max && tp.y < y_max) {
                    //due to parallel drawing on g, the stroke has to be set immediatly before and after writing.
                    BasicStroke bs = new BasicStroke(3);
                    ((Graphics2D) g).setStroke(bs);
                    g.drawRect((int) ((tp.getX() - r) * z), (int) ((tp.getY() - r) * z), (int) (2 * r * z), (int) (2 * r * z));
                    ((Graphics2D) g).setStroke(old_stroke);
                }
            }

            // draw the segmentations
            if (cns.getParam_ShowSegmentations() && tp.getRoi() != null) {
                BasicStroke bs = new BasicStroke(2);
                AffineTransform scaling = AffineTransform.getScaleInstance(z, z);
                if (tp.x >= x_min && tp.y >= y_min && tp.x < x_max && tp.y < y_max) {
                    // draw the actual segmentation
                    /*g.setColor(Color.YELLOW);
                    for (int i=lROI.tp.x-r; i<lROI.tp.x+r; i++) {
                        for (int j=lROI.tp.y-r; j<lROI.tp.y+r; j++) {
                            if (lROI.roi.contains(i-lroi.tp.x+r, j-lROI.tp.y+r)) {
                                ((Graphics2D)g).fillRect((int)(z*i), (int)(z*j), (int)Math.max(1,z), (int)Math.max(1, z));
                            }
                        }
                    }*/
                    // Draw the boundary
                    Polygon p = tp.getRoi().getPolygon();
                    p.translate(tp.x, tp.y);
                    Shape s = (Shape) scaling.createTransformedShape(p);
                    //due to parallel drawing on g, the stroke has to be set immediatly before and after writing.
                    Stroke old_stroke = ((Graphics2D) g).getStroke();
                    ((Graphics2D) g).setStroke(bs);
                    ((Graphics2D) g).draw(s);
                    ((Graphics2D) g).setStroke(old_stroke);
                }
            }
        }
    }

    @Override
    public void drawInformationPostNuclei(TMAspot ts, Graphics g, double z, int x_min, int y_min, int x_max, int y_max) {

    }

    @Override
    public BufferedImage showAlternativeImage(TMAspot ts) {
        return null;
    }

    @Override
    public void TMAspotMouseClicked(TMAspot ts, TMApoint tp, MouseEvent evt) {
        if (isVisible()) {
            if (ts != null && tp != null) {
                // add the segmentation if the point has not yet been segmented.
                segmentOneNucleus(ts, tp, null, null);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton13;
    private javax.swing.JRadioButton jRadioButton14;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JSlider jSlider2;
    private javax.swing.JSlider jSlider4;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private org.jdesktop.swingx.JXColorSelectionButton jXColorSelectionButton3;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    /**
     * Saves the black white mask of nucleus segmentation as PNG File. The file
     * can be chosen with the FileChooser (pops up).
     */
    private void saveSegmentationMask() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a Folder to Save the Masks");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String foldername = chooser.getSelectedFile().getPath() + File.separator;

            List<TMAspot> tss = manager.getSelectedTMAspots();
            for (TMAspot ts : tss) {
                BufferedImage bi = new BufferedImage(ts.getWidth(), ts.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bi.createGraphics();

                // draw the segmentations
                g.setColor(Color.RED);
                Stroke old_stroke = ((Graphics2D) g).getStroke();
                BasicStroke bs = new BasicStroke(2);
                ((Graphics2D) g).setStroke(bs);
                AffineTransform scaling = AffineTransform.getScaleInstance(1, 1);
                for (TMApoint tp : ts.getPoints()) {
                    Shape s = (Shape) scaling.createTransformedShape(tp.getRoi().getPolygon());
                    ((Graphics2D) g).draw(s);
                }
                ((Graphics2D) g).setStroke(old_stroke);

                // Write the image file
                try {
                    File outputfile = new File(foldername + ts.getName() + "_SegmentationMask.png");
                    ImageIO.write(bi, "PNG", outputfile);
                } catch (IOException e) {
                    Logger.getLogger(CancerNucleusSegmentation.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Deletes all segmentations of the nuclei in all selected TMAspots.
     * Essentially, it calls tp.setROI(null) and tp.setRoi(null) for all
     * TMApoints tp in selected TMAspots.
     */
    private void clearSegmentations() {
        List<TMAspot> tss = manager.getSelectedTMAspots();
        for (TMAspot ts : tss) {
            for (TMApoint tp : ts.getPoints()) {
                tp.setRoi(null);
                tp.setROI(null);
            }
        }
    }

    /**
     * Segments the cell nuclei on all selected TMAspots.
     */
    private void performSegmentation() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        List<TMAspot> tss = manager.getSelectedTMAspots();

        GaussianBlur blur = new GaussianBlur();
        ImagePlus ip;
        BufferedImage I_col = null;
        BufferedImage I_gray = null;

        for (TMAspot ts : tss) {
            List<TMApoint> tps = ts.getPoints();
            if (getParam_respectROI()) {
                TMAspot.filter_centroids_on_Areas(tps, ts);
            }

            if (!ts.isNDPI()) {
                if (getParam_imageChannel() == 1) {
                    I_gray = getChannelImage(ts, StainingEstimation.SHOW_CHANNEL1_IMAGE);
                } else if (getParam_imageChannel() == 2) {
                    I_gray = getChannelImage(ts, StainingEstimation.SHOW_CHANNEL2_IMAGE);
                }
                if (I_gray == null) {
                    
                    if (getParam_imageChannel() > 0) {
                        JOptionPane.showMessageDialog(this, "A channel image could not be found.\nMaybe it has not been created\nor the Color Deconvolution plugin is not installed. \nThe original image is used.", "No Channel Image Found", JOptionPane.WARNING_MESSAGE);
                    }
                    
                    // I_col
                    I_col = ts.getBufferedImage();
                    // I_gray
                    I_gray = new BufferedImage(I_col.getWidth(), I_col.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                    Graphics g = I_gray.getGraphics();
                    g.drawImage(I_col, 0, 0, null);
                    g.dispose();
                }

                //Blur the image for smoother features
                if (getParam_blur() > 0) {
                    ip = new ImagePlus(" ", I_gray);
                    blur.blurGaussian(ip.getProcessor(), getParam_blur(), getParam_blur(), 0.02);
                    I_gray = ip.getBufferedImage();
                }
                
                // A type conversion has to be done to INT ARGB, otherwise the segmentation wont work (the imagePlus would handle it differently and 
                // store the normalization differently.
                BufferedImage I_gray_conv = new BufferedImage(I_gray.getWidth(), I_gray.getHeight(), BufferedImage.TYPE_INT_ARGB);
                I_gray_conv.getGraphics().drawImage(I_gray, 0, 0, null);
                I_gray = I_gray_conv;
            }

            SegmentNucleiThread snt = new SegmentNucleiThread((TMARKERPluginManager) manager, this, ts, tps, I_col, I_gray);
            snt.start();
            /*try {
                snt.join();
                setProgressNumber_2(0, 0, 0);
            } catch (InterruptedException ex) {
                manager.repaintVisibleTMAspot();
            }*/
            //manager.repaintVisibleTMAspot();

        }
        
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Segments one nucleus on a given TMAspot. It will try to do that on the
     * channel 1 image of the ColorDeconvolution plugin, and on the gray scaled
     * original image as fall back.
     *
     * @param ts The TMAspot on which points are segmented (used for size and
     * border check).
     * @param tp The TMApoint to be segmented.
     * @param I_col The colored BufferedImage of this spot, used for faster segmentation on normal images.
     * @param I_gray The grayed BufferedImage of this spot, used for faster segmentation on normal images.
     * @return A matched TMApoint and its segmentation (which is a ROI).
     */
    void segmentOneNucleus(TMAspot ts, TMApoint tp, BufferedImage I_col, BufferedImage I_gray) {
        Rectangle rect = PatchRectangle(tp, getParam_patchsize());
        if (rect.x >= 0 && rect.y >= 0 && rect.x + rect.width < ts.getWidth() && rect.y + rect.height < ts.getHeight()) {
            BufferedImage bi_gray;

            if (ts.isNDPI() || I_gray == null) {
                GaussianBlur blur = new GaussianBlur();
                bi_gray = null;

                // get here the channel images of the Patch of an NDPI
                if (getParam_imageChannel() > 0) {
                    Pluggable p = manager.getPlugin("Color Deconvolution");
                    if (p != null) {
                         {
                            BufferedImage bi_col = ts.getSubimage(rect.x, rect.y, rect.width, rect.height, Math.max(rect.width, rect.height), BufferedImage.TYPE_INT_ARGB, I_col);
                            List<ImagePlus> HE = StainingEstimation.deconvolveImage(bi_col, ts.getCenter(), (StainingEstimation) p, ts, true, ((StainingEstimation) p).getParam_ColorChannel(), ((StainingEstimation) p).getParam_substractChannels(), ((StainingEstimation) p).getParam_invertCH1(), ((StainingEstimation) p).getParam_invertCH2(), ((StainingEstimation) p).getParam_invertCH3(), false);
                            if (getParam_imageChannel() == 1) {
                                bi_gray = HE.get(0).getBufferedImage();
                            } else {
                                bi_gray = HE.get(1).getBufferedImage();
                            }
                        } 
                    }
                }

                // if the patch is still null, then the original gray scaled image is used
                if (bi_gray == null) {
                    // gray patch
                    bi_gray = ts.getSubimage(rect.x, rect.y, rect.width, rect.height, Math.max(rect.width, rect.height), BufferedImage.TYPE_BYTE_GRAY, I_gray);
                }
                
                //Blur the image for smoother features
                if (getParam_blur() > 0) {
                    ImagePlus ip = new ImagePlus(" ", bi_gray);
                    blur.blurGaussian(ip.getProcessor(), getParam_blur(), getParam_blur(), 0.02);
                    bi_gray = ip.getBufferedImage();
                }
                
                // A type conversion has to be done to INT ARGB, otherwise the segmentation wont work (the imagePlus would handle it differently and 
                // store the normalization differently.
                BufferedImage bi_gray_conv = new BufferedImage(bi_gray.getWidth(), bi_gray.getHeight(), BufferedImage.TYPE_INT_ARGB);
                bi_gray_conv.getGraphics().drawImage(bi_gray, 0, 0, null);
                bi_gray = bi_gray_conv;
                
            } else {
                bi_gray = I_gray.getSubimage(rect.x, rect.y, rect.width, rect.height);
            }
            
            lROI roi = PatchToShape(bi_gray, ts, getParam_useGraphcut(), 0, ts.getParam_r(), (float) (getParam_GCShapeBalance() / 100.0));
            roi.setOffset(-rect.width/2, -rect.height/2);
            
            Roi roi2 = Misc.ROIToRoi(roi);
            if (roi2 != null && ((PolygonRoi)roi2).getNCoordinates()>0) {
                tp.setROI(roi);
                tp.setRoi(roi2);
            }
        }
    }

    /**
     * Returns one of the channel images of a given TMAspot after color deconvolution. The channel image has already 
     * to be created by the Color Deconvolution plugin. Otherwise, null is returned.
     *
     * @param ts The TMAspot to be searched for.
     * @param whichImage One of StainingEstimation.SHOW_CHANNEL1_IMAGE, SHOW_CHANNEL2_IMAGE or SHOW_CHANNEL3_IMAGE.
     * @return The channel image of ts. Null if the plugin "Color
     * Deconvolution" is not found or the channel image has not been created.
     */
    BufferedImage getChannelImage(TMAspot ts, int whichImage) {
        Pluggable p = manager.getPlugin("Color Deconvolution");
        if (p == null) {
            return null;
        } else {
            try {
                return ((StainingEstimation) p).getBufferedImage(ts, whichImage);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Segments the object in the middle of the patch and returns its shape.
     *
     * @param bi The image patch. The object (i.e. nucleus) is expected to be in
     * the middle.
     * @param ts The TMAspot to which the patch belongs (only used for
     * superpixel based segmentation and for accessing the temp directory if
     * DEBUG>5).
     * @param graphcut If true, a graphcut based segmentation is used, otherwise
     * a superpixel based.
     * @param blurring Blurring radius before processing for smoothing.
     * @return A ROI with same shape as the segmented shape. The ROI has the
     * same bounding box size as bi.
     */
    private static lROI PatchToShape(BufferedImage bi, TMAspot ts, boolean graphcut, int blurring, int nucleus_r, float circleweight) {
        try {
            //// CREATE BLACK WHITE SHAPE IMAGE
            BufferedImage BW = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            
            // SEGMENTATION WITH GRAPHCUT
            // grayed out are some debugging lines, e.g. for saving the intermediate steps as images.
            if (graphcut) {
                /*
                Maxflow mf = new Maxflow();
                mf.inputImageCapacity(bi, blurring);
                int[] foreground = mf.runMF();
                if (foreground!=null) {
                    int[] sub = new int[2];
                    for (int i=1; i<foreground.length-1; i++) {
                        Misc.Ind2sub(sub, foreground[i]-1, bi.getWidth());
                        if (tmarker.DEBUG > 5) { System.out.println(sub[0] + " " + Integer.toString(sub[1])); }
                        BW.setRGB(sub[0], sub[1], 0xFFFFFFFF);
                    }
                /*/
                // Grayscale the image
                BufferedImage bi_bw = Misc.convertToGrayscale(bi);
                ImagePlus ip = new ImagePlus(" ", bi_bw);

                // blur the image
                if (blurring > 0) {
                    GaussianBlur gb = new GaussianBlur();
                    gb.blurGaussian(ip.getProcessor(), blurring, blurring, 0.02);
                }

                // normalize
                ip.getProcessor().setMinAndMax(ip.getStatistics().min, ip.getStatistics().max);
                int w = bi_bw.getWidth();
                int h = bi_bw.getHeight();

                GraphCut gc = new GraphCut(w * h, (w - 1) * (h - 1) * 2 + w + h - 2);
                // set the terminal weights
                int i = 0;
                float factor; // The factor for roundish shape: the more in the middle of the patch, the higher the factor
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {

                        //E = (w)*P + (1-w)*r*P = P(w+(1-f)*(1-w))
                        factor = Math.min(1, (float) Math.pow(Math.sqrt(Math.pow(w / 2 - x, 2) + Math.pow(h / 2 - y, 2)) / nucleus_r, 2));
                        //gc.setTerminalWeights(i++, (1-factor)*(255-ip.getPixel(x, y)[0]), factor*ip.getPixel(x, y)[0]);
                        gc.setTerminalWeights(i++, (circleweight + (1 - factor) * (1 - circleweight)) * (255 - ip.getPixel(x, y)[0]), (circleweight + (factor) * (1 - circleweight)) * ip.getPixel(x, y)[0]);

                        //int rgb = 0x000000FF & (int) (255*(1-factor));
                        //rgb = rgb << 8;
                        //rgb += (int) (255*(1-factor));
                        //rgb = rgb << 8;
                        //rgb += (int) (255*(1-factor));
                        //rgb = rgb << 8;
                        //rgb += (int) (255*(1-factor));
                        //BW.setRGB(x, y, rgb);
                    }
                }
                //Misc.writeImage(ip.getBufferedImage(), ts.getTmpDir() + File.separator + "PatchROItmp.PNG");

                // set the internal weights (=differences of neighour pixels). Nodes are incrementally counted column by column.
                i = 0;
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        if (x < w - 1) {
                            gc.setEdgeWeight(i, i + h, Math.abs(ip.getPixel(x, y)[0] - ip.getPixel(x + 1, y)[0]));
                        }
                        if (y < h - 1) {
                            gc.setEdgeWeight(i, i + 1, Math.abs(ip.getPixel(x, y)[0] - ip.getPixel(x, y + 1)[0]));
                        }
                        i++;
                    }
                }

                gc.computeMaximumFlow(true, null);

                int[] sub = new int[2];
                for (i = 0; i < gc.getNumNodes(); i++) {
                    if (gc.getTerminal(i) == Terminal.FOREGROUND) {
                        Misc.Ind2sub(sub, i, bi.getHeight());
                        BW.setRGB(sub[1], sub[0], 0xFFFFFFFF);
                    }
                }
                //Misc.writeImage(BW, ts.getTmpDir() + File.separator + "PatchROI_graphcut.PNG");
                //*/

                ConnectComponent cc = new ConnectComponent();
                int[] img = new int[w * h];
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        img[Misc.Sub2ind(x, y, w)] = BW.getRGB(x, y);
                    }
                }
                int[] labels = cc.labeling(img, new Dimension(w, h), true);
                int label_center = labels[Misc.Sub2ind(w / 2, h / 2, w)];
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        if (labels[Misc.Sub2ind(x, y, w)] != label_center) {
                            BW.setRGB(x, y, 0x0);
                        }
                    }
                }
            } 
            // SEGMENTATION WITH SUPERPIXELS
            // Strategy: find nine superpixels on the patch, the nucleus is the middle one.
            else {
                SLICsuperpixels slicspx = new SLICsuperpixels();
                slicspx.verbose = false;
                slicspx.setTMASpot(ts);
                SLICsuperpixels.run(slicspx, bi, 9, 40, true, blurring);
                Rectangle rect = new Rectangle(bi.getWidth(), bi.getHeight());
                int mid = Misc.Sub2ind(bi.getWidth()/2, bi.getHeight()/2, bi.getWidth());
                for (Superpixel sp : slicspx.getSuperpixels()) {
                    if (!sp.touchesRectangleBorder(rect) || sp.contains(mid)) {
                        sp.setLabel(TMALabel.LABEL_POS);
                        sp.setProbability(1);
                    }
                }
                SLIC.DrawContoursAroundSegments(BW.getGraphics(), bi, slicspx.getSuperpixels(), false, Color.BLACK, true, Color.WHITE, false, Color.BLACK, false, Color.BLACK, 1, new Rectangle(0, 0, BW.getWidth(), BW.getHeight()));
            }

            // Black White Image to ROI
            lROI roi = new lROI(BW, 1, 0, 0);
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");
            
            // run this otherwise roi.contains() returns true always.
            for (int i = 0; i < BW.getWidth(); i++) {
                for (int j = 0; j < BW.getHeight(); j++) {
                    if (roi.contains(i, j)) {
                    }
                }
            }
            // END run this

            // if TMARKER is in very high debugging mode, write the segmentation to file into the tmp directory
            if (tmarker.DEBUG > 5) {
                for (int j = 0; j < bi.getHeight(); j++) {
                    System.out.println("");
                    for (int i = 0; i < bi.getWidth(); i++) {
                        if (roi.contains(i, j)) {
                            System.out.print("1");
                        } else {
                            System.out.print(".");
                        }
                    }
                }
                Misc.writeImage(BW, ts.getTmpDir() + File.separator + "PatchROI.PNG");
                Misc.writeImage(bi, ts.getTmpDir() + File.separator + "Patch.PNG");
            }

            return roi;
        } catch (Exception ex) {
            if (tmarker.DEBUG > 0) {
                Logger.getLogger(TMAspot.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }
    }

    /**
     * Returns a new Rectangle with center on the middle of tp and with and
     * height of patchsize.
     *
     * @param tp The nucleus over which a patch should be created.
     * @param patchsize The size of the patch.
     * @return A new quadratic Rectangle.
     */
    private static Rectangle PatchRectangle(TMALabel tp, int patchsize) {
        int p_half = patchsize / 2;
        return (new Rectangle(tp.x - p_half, tp.y - p_half, patchsize, patchsize));
    }

    /**
     * Writes the second progress numbers and estimated time according to total
     * number of patches, already processed number of instances and process
     * start time to a JLabel. If total is 0, " " is written (making the
     * progress information invisible). If startTimeMillis > 0, the estimated
     * time for the remaining instances is added.
     *
     * @param processed Processed number of instances.
     * @param total Total number of instances (if 0, " " will be written).
     * @param startTimeMillis The starting time of the process.
     */
    void setProgressNumber_2(int processed, int total, long startTimeMillis) {
        if (processed <= 0 || total <= 0) {
            jLabel2.setText(" ");
        } else {
            String text = "Processed  " + processed + "/" + total + "  Nuclei  (" + 100 * processed / total + " %)";
            if (startTimeMillis > 0) {
                long time = (total - processed) * (System.currentTimeMillis() - startTimeMillis) / processed;
                text += "    (est. " + String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes(time),
                        TimeUnit.MILLISECONDS.toSeconds(time)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))) + ")";
            }
            jLabel2.setText(text);
        }
    }

}

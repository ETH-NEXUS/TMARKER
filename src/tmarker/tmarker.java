/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * tmarker.java
 *
 * Created on 11.10.2011, 17:20:23
 * author:pschüffler
 */
package tmarker;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import org.jdesktop.swingx.JXStatusBar;
import org.xml.sax.SAXException;
import TMARKERPluginInterface.Pluggable;
import TMARKERPluginInterface.PluginManager;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import plugins.TMARKERPluginManager;
import plugins.PluginLoader;
import plugins.TMARKERSecurityManager;
import tmarker.TMAspot.TMALabel;
import tmarker.TMAspot.TMA_view_panel;
import tmarker.TMAspot.TMApoint;
import tmarker.TMAspot.TMAspot;
import tmarker.TMAspot.TMAspot_list_panel;
import tmarker.TMAspot.TMAspot_summary_Dialog;
import tmarker.delaunay.ArraySet;
import tmarker.delaunay.DelaunayAp;
import tmarker.delaunay.Pnt;
import tmarker.misc.FileDrop;
import tmarker.misc.Misc;
import tmarker.misc.SortedProperties;
import tmarker.misc.StringToIntConverter;
import tmarker.misc.ZoomableImagePanel;



/**
 *
 * @author Peter J. Schueffler
 */
public final class tmarker extends javax.swing.JFrame {

    /** Creates new form tmarker
     * @param tmp_dir The TMARKER temp directory which is deleted on exit. Temporary files can be written here.
     */
    public tmarker(String tmp_dir) {
        initComponents();
        setTmpDir(tmp_dir);
        initComponents2();
    }

    /** Revision number*/
    public static String REVISION = "2." + "$Revision: 21603 $".replaceAll("\\$", "").replaceAll("\\D", "");
    
    /** Unique Identifier*/
    public static UUID UID = UUID.randomUUID();
    
    /**
     * The logger of this class.
     */
    public static final Logger logger = Logger.getLogger(tmarker.class.getName());
    
    /*
     * The loaded plugins.
    */
    List<Pluggable> plugins = new ArrayList<>();
    
    private String currentDir = "";
    private String tmp_dir = "";
    private final List<String> path = new ArrayList<>();
    private final List<TMAspot> TMAspots = new ArrayList<>();
    private final Random random = new Random();
    private String infoText = "";
    
    //for Thumbnails
    Thread thumbnailsThread = null;
    
    /**
     * When clicked on the image, add cancerous nucleus.
     */
    public final static int CLICK_BEHAVIOUR_ADD_POS = 1;
    
    /**
     * When clicked on the image, add benign nucleus.
     */
    public final static int CLICK_BEHAVIOUR_ADD_NEG = 2;
    
    /**
     * When clicked on the image, add delete nucleus.
     */
    public final static int CLICK_BEHAVIOUR_DELETE = 3;
    
    /**
     * When clicked on the image, add flip nucleus class.
     */
    public final static int CLICK_BEHAVIOUR_FLIP = 4;
    
    /**
     * When clicked on the image, add nucleus without class.
     */
    public final static int CLICK_BEHAVIOUR_ADD_UNK = 5;
    
    /**
     * When clicked on the image, add background point.
     */
    public final static int CLICK_BEHAVIOUR_ADD_BG = 6;
    
    /**
     * When clicked on the image, do nothing.
     */
    public final static int CLICK_BEHAVIOUR_NONE = 7;
    
    /**
     * When clicked on the image, switch the nucleus' staining between STAINING_0, STAINING_1, STAINING_2 and STAINING_3.
     */
    public final static int CLICK_BEHAVIOUR_CORSTAIN_GRAD = 8;
    
    /**
     * When clicked on the image, switch the nucleus' staining between STAINING_0 and STAINING_3.
     */
    public final static int CLICK_BEHAVIOUR_CORSTAIN_BIN = 9;
    
    /**
     * The shape of the nuclei as drawn as circle.
     */
    public final static int LABELS_SHAPE_CIRCLE = 1;
    
    /**
     * The shape of the nuclei as drawn as cross.
     */
    public final static int LABELS_SHAPE_CROSS = 2;
    
    /**
     * The shape of the nuclei as drawn as Rectangle.
     */
    public final static int LABELS_SHAPE_RECTANGLE = 3;
    
    /**
     * DEBUG: 0 for little output information, 5 for much output information. You can start TMARKER with the option -d 0 or -d 5.
     */
    public static int DEBUG = 0;
    
    private JDialog aboutBox = null;
    private final OptionDialog od = new OptionDialog(this, false);
    private BgCorrectionDialog bcd = null;
    private final TMAspot_summary_Dialog tsd = new TMAspot_summary_Dialog(this, false);
    private final ZoomableImagePanel zip = new ZoomableImagePanel();
    private final TMA_view_panel tvp = new TMA_view_panel(this);
    private String targetProperty = null;
    
    //for the Whole Slice Annotation
    private boolean overwriteProperty = false;
    private boolean overwritePropertyQuestionAnswered = false;
    private final List<TMAspot> AnnotationAlreadySet = new ArrayList<>();
    private final List<TMAspot> AnnotationSkipped = new ArrayList<>();
    
    // For the zoom window
    private final ZoomableImagePanel zipl = new ZoomableImagePanel();
    private final double zoomfactor = 2;
    
    /**
     * The File separator (mostly slash or backslash).
     */
    public final static String fs = System.getProperty("file.separator");
    
    // For File handling (opening, saving)
    private boolean question_answered = false;
    private boolean convertPoints = false;  
    
    /**
     * Returns the property which is targeted for survival analysis (e.g. the staining property if the staining is indicative for survival).
     * @return The property which is indicative for survival, as defined by the user.
     */
    public String getTargetProperty() {
        return targetProperty;
    }

    /**
     * Sets the property which is targeted for survival analysis (e.g. the staining property if the staining is indicative for survival).
     * @param targetProperty The property which is indicative for survival, as defined by the user.
     */
    public void setTargetProperty(String targetProperty) {
        if (DEBUG>0) {
            logger.log(java.util.logging.Level.INFO, targetProperty + " as target property set.");
        }
        this.targetProperty = targetProperty;
        tsd.updateTargetProperty(getVisibleTMAspot());
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jMenuItem14 = new javax.swing.JMenuItem();
        jPopupMenu2 = new javax.swing.JPopupMenu();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItem15 = new javax.swing.JMenuItem();
        jMenuItem18 = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jSeparator15 = new javax.swing.JPopupMenu.Separator();
        jMenuItem23 = new javax.swing.JMenuItem();
        jMenuItem24 = new javax.swing.JMenuItem();
        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jXStatusBar1 = new org.jdesktop.swingx.JXStatusBar();
        jLabel4 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jPanel4 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jPanel20 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        jButton6 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        jPanel36 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel13 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        jToolBar3 = new javax.swing.JToolBar();
        jSlider1 = new javax.swing.JSlider();
        jTextField1 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5));
        jButton10 = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(30, 30), new java.awt.Dimension(30, 30), new java.awt.Dimension(30, 30));
        jToggleButton9 = new javax.swing.JToggleButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5));
        jToggleButton10 = new javax.swing.JToggleButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5));
        jToggleButton11 = new javax.swing.JToggleButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(30, 30), new java.awt.Dimension(30, 30), new java.awt.Dimension(30, 30));
        jToggleButton12 = new javax.swing.JToggleButton();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5));
        jToggleButton13 = new javax.swing.JToggleButton();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(30, 30), new java.awt.Dimension(30, 30), new java.awt.Dimension(30, 30));
        jToggleButton14 = new javax.swing.JToggleButton();
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5), new java.awt.Dimension(5, 5));
        jToggleButton15 = new javax.swing.JToggleButton();
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(30, 30), new java.awt.Dimension(30, 30), new java.awt.Dimension(30, 30));
        jToggleButton2 = new javax.swing.JToggleButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                super.paint(g2d);
                g2d.dispose();
            }
        };
        jToolBar1 = new javax.swing.JToolBar();
        jLabel1 = new javax.swing.JLabel();
        jSlider2 = new javax.swing.JSlider();
        jTextField2 = new javax.swing.JTextField();
        jToolBar2 = new javax.swing.JToolBar();
        jPanel32 = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        jPanel24 = new javax.swing.JPanel();
        jXTextField1 = new org.jdesktop.swingx.JXTextField();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jTextField6 = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jSlider3 = new javax.swing.JSlider();
        jPanel33 = new javax.swing.JPanel();
        jPanel29 = new javax.swing.JPanel();
        jXTextField2 = new org.jdesktop.swingx.JXTextField();
        jPanel25 = new javax.swing.JPanel();
        jRadioButton5 = new javax.swing.JRadioButton();
        jRadioButton6 = new javax.swing.JRadioButton();
        jRadioButton7 = new javax.swing.JRadioButton();
        jRadioButton8 = new javax.swing.JRadioButton();
        jPanel35 = new javax.swing.JPanel();
        jPanel31 = new javax.swing.JPanel();
        jPanel26 = new javax.swing.JPanel();
        jXTextField4 = new org.jdesktop.swingx.JXTextField();
        jRadioButton10 = new javax.swing.JRadioButton();
        jRadioButton11 = new javax.swing.JRadioButton();
        jRadioButton12 = new javax.swing.JRadioButton();
        jRadioButton13 = new javax.swing.JRadioButton();
        jPanel34 = new javax.swing.JPanel();
        jPanel30 = new javax.swing.JPanel();
        jXTextField3 = new org.jdesktop.swingx.JXTextField();
        jPanel27 = new javax.swing.JPanel();
        jTextField10 = new javax.swing.JTextField();
        jButton7 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jMenuItem21 = new javax.swing.JMenuItem();
        jMenuItem22 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem19 = new javax.swing.JMenuItem();
        jMenuItem20 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem16 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenu5 = new javax.swing.JMenu();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        jMenuItem25 = new javax.swing.JMenuItem();
        jSeparator14 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItem2 = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItem3 = new javax.swing.JCheckBoxMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem17 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();

        jMenuItem14.setText("Remove Selected Images");
        jMenuItem14.setToolTipText("");
        jMenuItem14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem14ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jMenuItem14);

        jMenuItem8.setText("Sort by Name (Ascending)");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem8);

        jMenuItem10.setText("Sort by Name (Descending)");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem10);
        jPopupMenu2.add(jSeparator1);

        jMenuItem15.setText("Sort by Number Nuclei (Ascending)");
        jMenuItem15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem15ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem15);

        jMenuItem18.setText("Sort by Number Nuclei (Descending)");
        jMenuItem18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem18ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem18);
        jPopupMenu2.add(jSeparator13);

        jMenuItem11.setText("Sort by Staining Percentage (Ascending)");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem11);

        jMenuItem12.setText("Sort by Staining Percentage (Descending)");
        jMenuItem12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem12ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem12);
        jPopupMenu2.add(jSeparator15);

        jMenuItem23.setText("Sort by Staining Percentage on Malignant Nuclei (Ascending)");
        jMenuItem23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem23ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem23);

        jMenuItem24.setText("Sort by Staining Percentage on Malignant Nuclei (Ascending)");
        jMenuItem24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem24ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem24);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("TMARKER v" + tmarker.REVISION);

        jXStatusBar1.add(jLabel4);
        jXStatusBar1.add(jProgressBar1);

        getContentPane().add(jXStatusBar1, java.awt.BorderLayout.PAGE_END);

        jPanel4.setLayout(new java.awt.BorderLayout());

        jSplitPane2.setDividerLocation(462);
        jSplitPane2.setOneTouchExpandable(true);

        jPanel1.setLayout(new java.awt.BorderLayout(0, 5));

        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel11.setOpaque(false);
        jPanel11.setPreferredSize(new java.awt.Dimension(231, 231));
        jPanel11.setLayout(new java.awt.BorderLayout());
        jPanel7.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel15.setOpaque(false);
        jPanel15.setPreferredSize(new java.awt.Dimension(230, 230));
        jPanel15.setLayout(new java.awt.BorderLayout());

        jScrollPane3.setBorder(null);
        jScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane3.setOpaque(false);
        jPanel15.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel7.add(jPanel15, java.awt.BorderLayout.EAST);

        jPanel1.add(jPanel7, java.awt.BorderLayout.PAGE_START);

        jPanel20.setBorder(javax.swing.BorderFactory.createTitledBorder("TMA List - 0 file(s)"));
        jPanel20.setLayout(new java.awt.BorderLayout());

        jPanel10.setLayout(new java.awt.BorderLayout(0, 5));

        jPanel18.setLayout(new java.awt.GridLayout(1, 0));

        jButton6.setText("Select All");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jPanel18.add(jButton6);

        jButton1.setText("Human/PC");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel18.add(jButton1);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/menu/move_up_16x16.png"))); // NOI18N
        jButton3.setText("Up");
        jButton3.setToolTipText("Move Up");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel18.add(jButton3);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/menu/move_down_16x16.png"))); // NOI18N
        jButton4.setText("Down");
        jButton4.setToolTipText("Move Down");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel18.add(jButton4);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/menu/sort_16x16.png"))); // NOI18N
        jButton5.setText("Sort...");
        jButton5.setToolTipText("Sort By....");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel18.add(jButton5);

        jPanel10.add(jPanel18, java.awt.BorderLayout.PAGE_START);

        jScrollPane7.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jPanel36.setBackground(new java.awt.Color(255, 255, 255));
        jPanel36.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                jPanel36ComponentMoved(evt);
            }
        });
        jPanel36.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jPanel36MouseDragged(evt);
            }
        });
        jPanel36.setLayout(new javax.swing.BoxLayout(jPanel36, javax.swing.BoxLayout.PAGE_AXIS));
        jScrollPane7.setViewportView(jPanel36);

        jPanel10.add(jScrollPane7, java.awt.BorderLayout.CENTER);

        jPanel20.add(jPanel10, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel20, java.awt.BorderLayout.CENTER);

        jSplitPane2.setLeftComponent(jPanel1);

        jSplitPane1.setBorder(null);
        jSplitPane1.setOneTouchExpandable(true);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel13.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(null);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        jScrollPane1.setViewportView(jPanel3);

        jPanel13.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("TMA View", jPanel13);

        jPanel2.add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        jToolBar3.setRollover(true);
        jToolBar3.setAlignmentX(0.0F);
        jToolBar3.setName("View Control"); // NOI18N

        jSlider1.setMajorTickSpacing(50);
        jSlider1.setMaximum(400);
        jSlider1.setMinimum(1);
        jSlider1.setMinorTickSpacing(10);
        jSlider1.setPaintTicks(true);
        jSlider1.setToolTipText("Zoom Slider");
        jSlider1.setValue(100);
        jToolBar3.add(jSlider1);

        jTextField1.setColumns(4);
        jTextField1.setToolTipText("Zoom Percentage");
        jTextField1.setMaximumSize(new java.awt.Dimension(40, 20));
        jTextField1.setMinimumSize(new java.awt.Dimension(40, 20));
        jTextField1.setPreferredSize(new java.awt.Dimension(40, 20));

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, jSlider1, org.jdesktop.beansbinding.ELProperty.create("${value}"), jTextField1, org.jdesktop.beansbinding.BeanProperty.create("text"), "jTextField1Binding");
        bindingGroup.addBinding(binding);

        jTextField1.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                jTextField1CaretUpdate(evt);
            }
        });
        jToolBar3.add(jTextField1);

        jLabel3.setText(" %");
        jLabel3.setAlignmentX(0.5F);
        jToolBar3.add(jLabel3);
        jToolBar3.add(filler1);

        jButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/fittowindow.png"))); // NOI18N
        jButton10.setToolTipText("Fit to Window / Original Size");
        jButton10.setAlignmentX(0.5F);
        jButton10.setFocusable(false);
        jButton10.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton10.setMaximumSize(new java.awt.Dimension(40, 25));
        jButton10.setMinimumSize(new java.awt.Dimension(40, 25));
        jButton10.setPreferredSize(new java.awt.Dimension(40, 25));
        jButton10.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jToolBar3.add(jButton10);
        jToolBar3.add(filler2);

        jToggleButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/dontviewpos.png"))); // NOI18N
        jToggleButton9.setSelected(true);
        jToggleButton9.setToolTipText("View Positive Labels");
        jToggleButton9.setAlignmentX(0.5F);
        jToggleButton9.setFocusable(false);
        jToggleButton9.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton9.setMaximumSize(new java.awt.Dimension(40, 25));
        jToggleButton9.setMinimumSize(new java.awt.Dimension(40, 25));
        jToggleButton9.setPreferredSize(new java.awt.Dimension(40, 25));
        jToggleButton9.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/viewpos.png"))); // NOI18N
        jToggleButton9.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton9ActionPerformed(evt);
            }
        });
        jToolBar3.add(jToggleButton9);
        jToolBar3.add(filler3);

        jToggleButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/dontviewneg.png"))); // NOI18N
        jToggleButton10.setSelected(true);
        jToggleButton10.setToolTipText("View Negative Labels");
        jToggleButton10.setActionCommand("jToggleButton10");
        jToggleButton10.setAlignmentX(0.5F);
        jToggleButton10.setFocusable(false);
        jToggleButton10.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton10.setMaximumSize(new java.awt.Dimension(40, 25));
        jToggleButton10.setMinimumSize(new java.awt.Dimension(40, 25));
        jToggleButton10.setPreferredSize(new java.awt.Dimension(40, 25));
        jToggleButton10.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/viewneg.png"))); // NOI18N
        jToggleButton10.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton10ActionPerformed(evt);
            }
        });
        jToolBar3.add(jToggleButton10);
        jToolBar3.add(filler4);

        jToggleButton11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/dontviewunk.png"))); // NOI18N
        jToggleButton11.setSelected(true);
        jToggleButton11.setToolTipText("View Unkown Labels");
        jToggleButton11.setAlignmentX(0.5F);
        jToggleButton11.setFocusable(false);
        jToggleButton11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton11.setMaximumSize(new java.awt.Dimension(40, 25));
        jToggleButton11.setMinimumSize(new java.awt.Dimension(40, 25));
        jToggleButton11.setPreferredSize(new java.awt.Dimension(40, 25));
        jToggleButton11.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/viewunk.png"))); // NOI18N
        jToggleButton11.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton11ActionPerformed(evt);
            }
        });
        jToolBar3.add(jToggleButton11);
        jToolBar3.add(filler5);

        jToggleButton12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/dontviewstained.png"))); // NOI18N
        jToggleButton12.setSelected(true);
        jToggleButton12.setToolTipText("View Stained Nuclei");
        jToggleButton12.setAlignmentX(0.5F);
        jToggleButton12.setFocusable(false);
        jToggleButton12.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton12.setMaximumSize(new java.awt.Dimension(40, 25));
        jToggleButton12.setMinimumSize(new java.awt.Dimension(40, 25));
        jToggleButton12.setPreferredSize(new java.awt.Dimension(40, 25));
        jToggleButton12.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/viewstained.png"))); // NOI18N
        jToggleButton12.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton12ActionPerformed(evt);
            }
        });
        jToolBar3.add(jToggleButton12);
        jToolBar3.add(filler6);

        jToggleButton13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/dontviewunstained.png"))); // NOI18N
        jToggleButton13.setSelected(true);
        jToggleButton13.setToolTipText("View Unstained Nuclei");
        jToggleButton13.setAlignmentX(0.5F);
        jToggleButton13.setFocusable(false);
        jToggleButton13.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton13.setMaximumSize(new java.awt.Dimension(40, 25));
        jToggleButton13.setMinimumSize(new java.awt.Dimension(40, 25));
        jToggleButton13.setPreferredSize(new java.awt.Dimension(40, 25));
        jToggleButton13.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/viewunstained.png"))); // NOI18N
        jToggleButton13.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton13ActionPerformed(evt);
            }
        });
        jToolBar3.add(jToggleButton13);
        jToolBar3.add(filler7);

        jToggleButton14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/viewgstd.png"))); // NOI18N
        jToggleButton14.setSelected(true);
        jToggleButton14.setToolTipText("View Goldstandard Labels");
        jToggleButton14.setAlignmentX(0.5F);
        jToggleButton14.setFocusable(false);
        jToggleButton14.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton14.setMaximumSize(new java.awt.Dimension(40, 25));
        jToggleButton14.setMinimumSize(new java.awt.Dimension(40, 25));
        jToggleButton14.setPreferredSize(new java.awt.Dimension(40, 25));
        jToggleButton14.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton14ActionPerformed(evt);
            }
        });
        jToolBar3.add(jToggleButton14);
        jToolBar3.add(filler8);

        jToggleButton15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/viewest.png"))); // NOI18N
        jToggleButton15.setSelected(true);
        jToggleButton15.setToolTipText("View Computer-Estimated Labels");
        jToggleButton15.setAlignmentX(0.5F);
        jToggleButton15.setFocusable(false);
        jToggleButton15.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton15.setMaximumSize(new java.awt.Dimension(40, 25));
        jToggleButton15.setMinimumSize(new java.awt.Dimension(40, 25));
        jToggleButton15.setPreferredSize(new java.awt.Dimension(40, 25));
        jToggleButton15.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton15ActionPerformed(evt);
            }
        });
        jToolBar3.add(jToggleButton15);
        jToolBar3.add(filler9);

        jToggleButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/dontviewdensity.png"))); // NOI18N
        jToggleButton2.setToolTipText("View Nucleus Density Heatmap");
        jToggleButton2.setAlignmentX(0.5F);
        jToggleButton2.setFocusable(false);
        jToggleButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton2.setMaximumSize(new java.awt.Dimension(40, 25));
        jToggleButton2.setMinimumSize(new java.awt.Dimension(40, 25));
        jToggleButton2.setPreferredSize(new java.awt.Dimension(40, 25));
        jToggleButton2.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/viewdensity.png"))); // NOI18N
        jToggleButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton2ActionPerformed(evt);
            }
        });
        jToolBar3.add(jToggleButton2);

        listenForOrientationChange(jToolBar3);
        jPanel2.add(jToolBar3, java.awt.BorderLayout.PAGE_END);

        jSplitPane1.setLeftComponent(jPanel2);

        jScrollPane4.setBorder(javax.swing.BorderFactory.createTitledBorder("Info View"));

        jTextPane1.setEditable(false);
        jTextPane1.setBackground(SystemColor.info);
        jTextPane1.setContentType("text/html"); // NOI18N
        jTextPane1.setToolTipText("");
        jTextPane1.setMargin(new java.awt.Insets(5, 5, 5, 5));
        jScrollPane4.setViewportView(jTextPane1);
        jTextPane1.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    try {
                        openWebsite(hle.getURL().toURI().toString());
                    } catch (Exception e) {

                    }   
                }
            }
        });

        jSplitPane1.setRightComponent(jScrollPane4);

        jSplitPane2.setRightComponent(jSplitPane1);

        jPanel4.add(jSplitPane2, java.awt.BorderLayout.CENTER);

        jToolBar1.setRollover(true);
        jToolBar1.setName("Nucleus Annotation"); // NOI18N

        jLabel1.setText("Nucleus Radius = ");
        jLabel1.setToolTipText("");
        jLabel1.setAlignmentX(0.5F);
        jToolBar1.add(jLabel1);

        jSlider2.setMajorTickSpacing(5);
        jSlider2.setMaximum(50);
        jSlider2.setMinorTickSpacing(1);
        jSlider2.setPaintTicks(true);
        jSlider2.setSnapToTicks(true);
        jSlider2.setToolTipText("Change the nuclei radius on this image");
        jSlider2.setMaximumSize(new java.awt.Dimension(200, 31));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, jTextField2, org.jdesktop.beansbinding.ELProperty.create("${text}"), jSlider2, org.jdesktop.beansbinding.BeanProperty.create("value"), "jSlider2Binding");
        binding.setSourceNullValue(1);
        binding.setSourceUnreadableValue(1);
        binding.setConverter(new StringToIntConverter());
        bindingGroup.addBinding(binding);

        jSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider2StateChanged(evt);
            }
        });
        jToolBar1.add(jSlider2);

        jTextField2.setColumns(2);
        jTextField2.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField2.setText("9");
        jTextField2.setToolTipText("Change the nuclei radius on this image");
        jTextField2.setMaximumSize(new java.awt.Dimension(40, 20));
        jToolBar1.add(jTextField2);

        buildToolBar1();
        listenForOrientationChange(jToolBar1);
        jPanel4.add(jToolBar1, java.awt.BorderLayout.PAGE_START);

        getContentPane().add(jPanel4, java.awt.BorderLayout.CENTER);

        jToolBar2.setRollover(true);
        jToolBar2.setAlignmentX(0.0F);
        jToolBar2.setName("Whole Image Annotation"); // NOI18N

        jPanel32.setBackground(new java.awt.Color(255, 255, 204));
        jPanel32.setLayout(new java.awt.BorderLayout());

        jPanel28.setOpaque(false);
        jPanel28.setLayout(new java.awt.GridBagLayout());

        jPanel24.setOpaque(false);
        jPanel24.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 1, 1));

        jXTextField1.setColumns(15);
        jXTextField1.setPrompt("My_Percentage");
        jXTextField1.setToolTipText("Insert here a name of the property to be annotated.");
        jPanel24.add(jXTextField1);

        buttonGroup3.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("NA");
        jRadioButton1.setToolTipText("This property is not readable or is not considered.");
        jRadioButton1.setOpaque(false);
        jPanel24.add(jRadioButton1);

        buttonGroup3.add(jRadioButton2);
        jRadioButton2.setText("value");
        jRadioButton2.setToolTipText("Please select a value for this property for the current image.");
        jRadioButton2.setOpaque(false);
        jPanel24.add(jRadioButton2);

        jTextField6.setColumns(3);
        jTextField6.setToolTipText("Please insert a value for this property for the current image.");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, jSlider3, org.jdesktop.beansbinding.ELProperty.create("${value}"), jTextField6, org.jdesktop.beansbinding.BeanProperty.create("text"), "jTextField6Binding");
        bindingGroup.addBinding(binding);

        jPanel24.add(jTextField6);

        jLabel11.setText("%");
        jPanel24.add(jLabel11);

        jSlider3.setMinorTickSpacing(5);
        jSlider3.setPaintLabels(true);
        jSlider3.setPaintTicks(true);
        jSlider3.setToolTipText("Please select a value for this property for the current image.");
        jSlider3.setOpaque(false);
        jSlider3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider3StateChanged(evt);
            }
        });
        jPanel24.add(jSlider3);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel28.add(jPanel24, gridBagConstraints);

        jPanel32.add(jPanel28, java.awt.BorderLayout.WEST);

        jToolBar2.add(jPanel32);

        jPanel33.setBackground(new java.awt.Color(204, 204, 255));
        jPanel33.setLayout(new java.awt.BorderLayout());

        jPanel29.setOpaque(false);
        jPanel29.setLayout(new java.awt.GridBagLayout());

        jXTextField2.setColumns(15);
        jXTextField2.setPrompt("My_Mitotic_Count");
        jXTextField2.setToolTipText("Insert here a name of the property to be annotated.");
        jPanel29.add(jXTextField2, new java.awt.GridBagConstraints());

        jPanel25.setOpaque(false);
        jPanel25.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 1, 1));

        buttonGroup1.add(jRadioButton5);
        jRadioButton5.setSelected(true);
        jRadioButton5.setText("NA");
        jRadioButton5.setToolTipText("This property is not readable or is not considered.");
        jRadioButton5.setOpaque(false);
        jPanel25.add(jRadioButton5);

        buttonGroup1.add(jRadioButton6);
        jRadioButton6.setText("1");
        jRadioButton6.setToolTipText("0-5 mitotic counts per 10 fields");
        jRadioButton6.setOpaque(false);
        jPanel25.add(jRadioButton6);

        buttonGroup1.add(jRadioButton7);
        jRadioButton7.setText("2");
        jRadioButton7.setToolTipText("6-10 mitotic counts per 10 fields");
        jRadioButton7.setOpaque(false);
        jPanel25.add(jRadioButton7);

        buttonGroup1.add(jRadioButton8);
        jRadioButton8.setText("3");
        jRadioButton8.setToolTipText("over 10 mitotic counts per 10 fields");
        jRadioButton8.setOpaque(false);
        jPanel25.add(jRadioButton8);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel29.add(jPanel25, gridBagConstraints);

        jPanel33.add(jPanel29, java.awt.BorderLayout.WEST);

        jToolBar2.add(jPanel33);

        jPanel35.setBackground(new java.awt.Color(255, 204, 204));
        jPanel35.setLayout(new java.awt.BorderLayout());

        jPanel31.setOpaque(false);
        jPanel31.setLayout(new java.awt.GridBagLayout());

        jPanel26.setOpaque(false);
        jPanel26.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 1, 1));

        jXTextField4.setColumns(15);
        jXTextField4.setPrompt("My_Pleomorphism");
        jXTextField4.setToolTipText("Insert here a name of the property to be annotated.");
        jPanel26.add(jXTextField4);

        buttonGroup2.add(jRadioButton10);
        jRadioButton10.setSelected(true);
        jRadioButton10.setText("NA");
        jRadioButton10.setToolTipText("This property is not readable or is not considered.");
        jRadioButton10.setOpaque(false);
        jPanel26.add(jRadioButton10);

        buttonGroup2.add(jRadioButton11);
        jRadioButton11.setText("1");
        jRadioButton11.setToolTipText("nuclei with minimal variation in size and shape");
        jRadioButton11.setOpaque(false);
        jPanel26.add(jRadioButton11);

        buttonGroup2.add(jRadioButton12);
        jRadioButton12.setText("2");
        jRadioButton12.setToolTipText("nuclei with moderate variation in size and shape");
        jRadioButton12.setOpaque(false);
        jPanel26.add(jRadioButton12);

        buttonGroup2.add(jRadioButton13);
        jRadioButton13.setText("3");
        jRadioButton13.setToolTipText("nuclei with marked variation in size and shape");
        jRadioButton13.setOpaque(false);
        jPanel26.add(jRadioButton13);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel31.add(jPanel26, gridBagConstraints);

        jPanel35.add(jPanel31, java.awt.BorderLayout.WEST);

        jToolBar2.add(jPanel35);

        jPanel34.setBackground(new java.awt.Color(204, 255, 204));
        jPanel34.setLayout(new java.awt.BorderLayout());

        jPanel30.setOpaque(false);
        jPanel30.setLayout(new java.awt.GridBagLayout());

        jXTextField3.setColumns(15);
        jXTextField3.setPrompt("My_Comment");
        jXTextField3.setToolTipText("Insert here a name of the property to be annotated.");
        jPanel30.add(jXTextField3, new java.awt.GridBagConstraints());

        jPanel27.setOpaque(false);
        jPanel27.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 1, 1));

        jTextField10.setColumns(25);
        jTextField10.setToolTipText("Please insert a value for this property for the current image or leave blank for no value.");
        jPanel27.add(jTextField10);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel30.add(jPanel27, gridBagConstraints);

        jPanel34.add(jPanel30, java.awt.BorderLayout.WEST);

        jToolBar2.add(jPanel34);

        jButton7.setText("Skip");
        jButton7.setToolTipText("Skip this image proceed with the next image.");
        jButton7.setAlignmentX(0.5F);
        jButton7.setFocusable(false);
        jButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton7.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jToolBar2.add(jButton7);

        jButton11.setText("Save and Next");
        jButton11.setToolTipText("Save the annotations to the current image and proceed with the next image.");
        jButton11.setAlignmentX(0.5F);
        jButton11.setFocusable(false);
        jButton11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton11.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jToolBar2.add(jButton11);

        jToolBar2.add(new JToolBar.Separator(), 4);
        jToolBar2.add(new JToolBar.Separator(), 3);
        jToolBar2.add(new JToolBar.Separator(), 2);
        jToolBar2.add(new JToolBar.Separator(), 1);
        jPanel32.setMaximumSize(jPanel32.getPreferredSize());
        jPanel33.setMaximumSize(new Dimension(jPanel33.getPreferredSize().width, jPanel32.getPreferredSize().height));
        jPanel34.setMaximumSize(new Dimension(jPanel34.getPreferredSize().width, jPanel32.getPreferredSize().height));
        jPanel35.setMaximumSize(new Dimension(jPanel35.getPreferredSize().width, jPanel32.getPreferredSize().height));
        listenForOrientationChange(jToolBar2);
        getContentPane().add(jToolBar2, java.awt.BorderLayout.PAGE_START);

        jMenu1.setText("File");

        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/toolbarButtonGraphics/general/Open16.gif"))); // NOI18N
        jMenuItem1.setText("Open...");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/toolbarButtonGraphics/general/Save16.gif"))); // NOI18N
        jMenuItem2.setText("Save As...");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setText("Quit");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        jMenu4.setText("TMA");

        jMenuItem6.setText("Delete all Estimated Points");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem6);

        jMenuItem13.setText("Delete all Gold Standard Points");
        jMenuItem13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem13ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem13);

        jMenuItem21.setText("Convert Estimated Labels to Gold Standard");
        jMenuItem21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem21ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem21);

        jMenuItem22.setText("Convert Gold Standard Labels to Estimated");
        jMenuItem22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem22ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem22);

        jMenuItem7.setText("White Balance...");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem7);

        jMenu6.setText("Background");

        jMenuItem19.setText("Create Background Labels by Voronoi Sampling");
        jMenuItem19.setToolTipText("Vonoi sampling around manual labels");
        jMenuItem19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem19ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem19);

        jMenuItem20.setText("Delete Background Labels");
        jMenuItem20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem20ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem20);

        jMenu4.add(jMenu6);

        jMenuItem9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/toolbarButtonGraphics/general/Delete16.gif"))); // NOI18N
        jMenuItem9.setText("Remove");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem9);

        jMenuItem16.setText("Add Metainformation to TMA study...");
        jMenuItem16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem16ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem16);

        jMenuBar1.add(jMenu4);

        jMenu3.setText("Tools");

        jMenu5.setText("Plugins");
        jMenu5.add(jSeparator16);

        jMenuItem25.setText("Get More Plugins...");
        jMenuItem25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem25ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem25);

        jMenu3.add(jMenu5);
        jMenu3.add(jSeparator14);

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("Nucleus Annotation Toolbar");
        jCheckBoxMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItem1ActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItem1);

        jCheckBoxMenuItem2.setSelected(true);
        jCheckBoxMenuItem2.setText("Whole Image Annotation Toolbar");
        jCheckBoxMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItem2ActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItem2);

        jCheckBoxMenuItem3.setSelected(true);
        jCheckBoxMenuItem3.setText("View Control Toolbar");
        jCheckBoxMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItem3ActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItem3);
        jMenu3.add(jSeparator17);

        jMenuItem5.setText("Options...");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem5);

        jMenuBar1.add(jMenu3);

        jMenu2.setText("Help");

        jMenuItem17.setText("Check for Updates...");
        jMenuItem17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem17ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem17);

        jMenuItem4.setText("About and Citation...");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem4);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        if (aboutBox == null) {
            aboutBox = new TMARKERAboutBox(this);
        }
            aboutBox.setLocationRelativeTo(this);
            aboutBox.setVisible(true);        
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        //if (od == null) od = new OptionDialog(this, false);
        od.setVisible(true);
        od.toFront();
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        int c = JOptionPane.showConfirmDialog(this, "Are you sure to delete all estimated labels from the selected images?", "Confirm Deleting Labels", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            List<TMAspot> tss = getSelectedTMAspots();
            for (int i=0; i<tss.size(); i++) {
                TMAspot ts = tss.get(i);
                ts.deleteAllPoints_ES();
                if (ts==getVisibleTMAspot()) {getTMAView().repaint();}
                ts.dispStainingInfo();
            }
        }
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        openBgCorrectionDialog();
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        if (!getSelectedTMAspots(false).isEmpty()) {
            removeTMAspots(getSelectedTMAspots());
        }
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jMenuItem13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem13ActionPerformed
        int c = JOptionPane.showConfirmDialog(this, "Are you sure to delete all gold standard labels from the selected images?", "Confirm Deleting Labels", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            List<TMAspot> tss = getSelectedTMAspots();
            for (int i=0; i<tss.size(); i++) {
                TMAspot ts = tss.get(i);
                ts.deleteAllPoints_GS();
                if (ts==getVisibleTMAspot()) {getTMAView().repaint();}
                ts.dispStainingInfo();
            }
        }
    }//GEN-LAST:event_jMenuItem13ActionPerformed

    private void jMenuItem14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem14ActionPerformed
        removeTMAspots(getSelectedTMAspots(false));
    }//GEN-LAST:event_jMenuItem14ActionPerformed

    private void jMenuItem16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem16ActionPerformed
        addMetainformation();
    }//GEN-LAST:event_jMenuItem16ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        selectAllTMAspots();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jMenuItem17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem17ActionPerformed
        checkForUpdates();
    }//GEN-LAST:event_jMenuItem17ActionPerformed

    private void jPanel36MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel36MouseDragged
        List<TMAspot> tss = getSelectedTMAspots(false);
        if (!tss.isEmpty()) {
            if (evt.getClickCount() > 1) {
                if (getVisibleTMAspot()!=null) {
                    tsd.setVisible(true);
                    tsd.updateSummary(tss.get(0));
                }
            } else {
                showTMAspot(tss.get(0));                
            }
        }
    }//GEN-LAST:event_jPanel36MouseDragged

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        doExit();
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        SaveFile(this, getCurrentDir());
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        LoadFilesWithChooser(this, getCurrentDir());
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jPanel36ComponentMoved(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanel36ComponentMoved
        setThumbnailsThread();
    }//GEN-LAST:event_jPanel36ComponentMoved

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        Point p1 = MouseInfo.getPointerInfo().getLocation();
        Point p2 = jButton5.getLocationOnScreen();
        jPopupMenu2.show(jButton5, p1.x-p2.x, p1.y-p2.y);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        moveTMAspotUp(getSelectedTMAspots(false));
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        moveTMAspotDown(getSelectedTMAspots(false));
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        sortTMAspotsByName(true);
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        sortTMAspotsByName(false);
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        sortTMAspotsByStaining(true);
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void jMenuItem12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem12ActionPerformed
        sortTMAspotsByStaining(false);
    }//GEN-LAST:event_jMenuItem12ActionPerformed

    private void jMenuItem15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem15ActionPerformed
        sortTMAspotsByNumberNuclei(true);
    }//GEN-LAST:event_jMenuItem15ActionPerformed

    private void jMenuItem18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem18ActionPerformed
        sortTMAspotsByNumberNuclei(false);
    }//GEN-LAST:event_jMenuItem18ActionPerformed

    private void jMenuItem19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem19ActionPerformed
        performVoronoiSampling();
    }//GEN-LAST:event_jMenuItem19ActionPerformed

    private void jMenuItem20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem20ActionPerformed
        deleteBackgroundPoints(getSelectedTMAspots(false));
    }//GEN-LAST:event_jMenuItem20ActionPerformed

    private void jMenuItem21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem21ActionPerformed
        List<TMAspot> tss = getSelectedTMAspots();
        for (TMAspot ts: tss) {
            for (TMApoint tp: ts.getPoints_Estimated()) {
                tp.setGoldStandard(getGSNumberForLabeling());
            }
            if (ts==getVisibleTMAspot()) {getTMAView().repaint();}
            ts.dispStainingInfo();
        }
    }//GEN-LAST:event_jMenuItem21ActionPerformed

    private void jMenuItem22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem22ActionPerformed
        List<TMAspot> tss = getSelectedTMAspots();
        for (TMAspot ts: tss) {
            for (TMApoint tp: ts.getPoints_GoldStandard()) {
                tp.setGoldStandard(TMApoint.ESTIMATED);
            }
            if (ts==getVisibleTMAspot()) {getTMAView().repaint();}
            ts.dispStainingInfo();
        }
    }//GEN-LAST:event_jMenuItem22ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        List<TMAspot> tss = getTMAspots();
        if (!tss.isEmpty()) {
            boolean isShowingEstimatedNow = tss.get(0).getTLP().isShowingEstimated();
            boolean isShowingGoldstandardNow = tss.get(0).getTLP().isShowingGoldstandard();
            for (TMAspot ts: tss) {
                if (ts.getTLP().isShowingEstimated()==isShowingEstimatedNow) {
                    ts.getTLP().toggleShowGstOrEst();
                }
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jMenuItem23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem23ActionPerformed
        sortTMAspotsByStainingMalignant(true);
    }//GEN-LAST:event_jMenuItem23ActionPerformed

    private void jMenuItem24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem24ActionPerformed
        sortTMAspotsByStainingMalignant(false);
    }//GEN-LAST:event_jMenuItem24ActionPerformed

    private void jMenuItem25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem25ActionPerformed
        openWebsite("http://www.comp-path.inf.ethz.ch");
    }//GEN-LAST:event_jMenuItem25ActionPerformed

    private void jCheckBoxMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItem1ActionPerformed
        jToolBar1.setVisible(jCheckBoxMenuItem1.isSelected());
    }//GEN-LAST:event_jCheckBoxMenuItem1ActionPerformed

    private void jCheckBoxMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItem2ActionPerformed
        jToolBar2.setVisible(jCheckBoxMenuItem2.isSelected());
    }//GEN-LAST:event_jCheckBoxMenuItem2ActionPerformed

    private void jTextField1CaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_jTextField1CaretUpdate
        if (getVisibleTMAspot() != null) {
            try {
                getTMAView().setZoom(Double.parseDouble(jTextField1.getText().trim())/100.0);
            } catch (Exception e) {

            }
        }
    }//GEN-LAST:event_jTextField1CaretUpdate

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        if (!jTextField1.getText().equals("100")) {
            jTextField1.setText("100");
        } else {
            TMAspot ts = getVisibleTMAspot();
            if (ts != null) {
                int s_w = (int) Math.round((100.0*getTMAViewContainer().getViewport().getWidth()/getTMAView().getImageWidth()));
                int s_h = (int) Math.round((100.0*getTMAViewContainer().getViewport().getHeight()/getTMAView().getImageHeight()));
                jTextField1.setText(Integer.toString(Math.min(s_h, s_w)));
            }
        }
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jToggleButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton9ActionPerformed
        getTMAView().repaint();
    }//GEN-LAST:event_jToggleButton9ActionPerformed

    private void jToggleButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton10ActionPerformed
        getTMAView().repaint();
    }//GEN-LAST:event_jToggleButton10ActionPerformed

    private void jToggleButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton11ActionPerformed
        getTMAView().repaint();
    }//GEN-LAST:event_jToggleButton11ActionPerformed

    private void jToggleButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton12ActionPerformed
        getTMAView().repaint();
    }//GEN-LAST:event_jToggleButton12ActionPerformed

    private void jToggleButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton13ActionPerformed
        getTMAView().repaint();
    }//GEN-LAST:event_jToggleButton13ActionPerformed

    private void jToggleButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton14ActionPerformed
        getTMAView().repaint();
    }//GEN-LAST:event_jToggleButton14ActionPerformed

    private void jToggleButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton15ActionPerformed
        getTMAView().repaint();
    }//GEN-LAST:event_jToggleButton15ActionPerformed

    private void jToggleButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton2ActionPerformed
        getTMAView().repaint();
    }//GEN-LAST:event_jToggleButton2ActionPerformed

    private void jSlider3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider3StateChanged
        jRadioButton2.setSelected(true);
    }//GEN-LAST:event_jSlider3StateChanged

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        doSkipAndShowNextSpot();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        doAnnotationAndShowNextSpot();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider2StateChanged
        getTMAView().repaint();
    }//GEN-LAST:event_jSlider2StateChanged

    private void jCheckBoxMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItem3ActionPerformed
        jToolBar3.setVisible(jCheckBoxMenuItem3.isSelected());
    }//GEN-LAST:event_jCheckBoxMenuItem3ActionPerformed

    /**
     * Starts the TMARKER session.
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        int debug = 5; // SET THIS TO 0 IF YOU COMPILE FOR PUBLIC DISTRIBUTION OTHERWISE 1-5 FOR LESS OR MORE DEBUG INFO
        if (args.length>0) {
            try {
                String helpOption = weka.core.Utils.getOption('h', args);
                if (helpOption.length() > 0) {
                    //System.out.println("Help " + helpOption);
                    printHelp();
                    System.exit(0);
                }  
            } catch (Exception ex) {
            }
            try {
                debug = Integer.valueOf(weka.core.Utils.getOption('d', args));
                debug = Math.min(6, debug);
                debug = Math.max(0, debug);
            } catch (Exception ex) {
                printHelp();
                System.exit(0);
            }
        }
        
        DEBUG = debug;
        logger.log(java.util.logging.Level.INFO, "Debug modus (0-5) = {0}", Integer.toString(DEBUG));
        
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
        */ 
        try {
            /*for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }*/
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(tmarker.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                tmarker frame = new tmarker(System.getProperty("user.home") + fs + "TMARKER_tmp");
                frame.setExtendedState(frame.getExtendedState()|JFrame.MAXIMIZED_BOTH);
                frame.setVisible(true);

                if (DEBUG>0) {
                    //... Add property list data to text area.
                    Properties pr = System.getProperties();
                    Collection<String> keys = pr.stringPropertyNames();
                    List<String> propKeys = new ArrayList<>(keys);
                    Collections.sort(propKeys);
                    String props = "";
                    for (Iterator<String> it = propKeys.iterator(); it.hasNext(); ) {
                        String key = it.next();
                        props += key + "=" + pr.get(key) + "\n";
                    }
                    logger.log(java.util.logging.Level.INFO, "System Properties:\n" + props);
                    
                    String[] its = ImageIO. getWriterFormatNames();
                    String imgformats = "";
                    for (int i=0; i<its.length; i++) {
                        imgformats += its[i] + "\n";
                    }
                    logger.log(java.util.logging.Level.INFO, "Accepted image file types:\n" + imgformats);
                    
                    logger.log(java.util.logging.Level.INFO, "Temp Directory: " + frame.getTmpDir());
                    
                    int processors = Runtime.getRuntime().availableProcessors();
                    logger.log(java.util.logging.Level.INFO, Integer.toString(processors) + " processor"
                            + (processors != 1 ? "s are " : " is ")
                            + "available");
                }
            }
        });
    }
    
    /** 
    * Prints help message for commands in standard output.
    */
    private static void printHelp() {
        System.out.println("\nTMARKER Usage java -jar TMARER.jar [options]\n\n" +
                "options:\n" + 
                "-h      this help\n" +
                "-d <n>  Debug modus n (0-5)" +
                "\n");
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.Box.Filler filler9;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem2;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
    private javax.swing.JMenuItem jMenuItem15;
    private javax.swing.JMenuItem jMenuItem16;
    private javax.swing.JMenuItem jMenuItem17;
    private javax.swing.JMenuItem jMenuItem18;
    private javax.swing.JMenuItem jMenuItem19;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem20;
    private javax.swing.JMenuItem jMenuItem21;
    private javax.swing.JMenuItem jMenuItem22;
    private javax.swing.JMenuItem jMenuItem23;
    private javax.swing.JMenuItem jMenuItem24;
    private javax.swing.JMenuItem jMenuItem25;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel35;
    private javax.swing.JPanel jPanel36;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JPopupMenu jPopupMenu2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton10;
    private javax.swing.JRadioButton jRadioButton11;
    private javax.swing.JRadioButton jRadioButton12;
    private javax.swing.JRadioButton jRadioButton13;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JRadioButton jRadioButton6;
    private javax.swing.JRadioButton jRadioButton7;
    private javax.swing.JRadioButton jRadioButton8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator14;
    private javax.swing.JPopupMenu.Separator jSeparator15;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JSlider jSlider2;
    private javax.swing.JSlider jSlider3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JToggleButton jToggleButton10;
    private javax.swing.JToggleButton jToggleButton11;
    private javax.swing.JToggleButton jToggleButton12;
    private javax.swing.JToggleButton jToggleButton13;
    private javax.swing.JToggleButton jToggleButton14;
    private javax.swing.JToggleButton jToggleButton15;
    private javax.swing.JToggleButton jToggleButton2;
    private javax.swing.JToggleButton jToggleButton9;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private org.jdesktop.swingx.JXStatusBar jXStatusBar1;
    private org.jdesktop.swingx.JXTextField jXTextField1;
    private org.jdesktop.swingx.JXTextField jXTextField2;
    private org.jdesktop.swingx.JXTextField jXTextField3;
    private org.jdesktop.swingx.JXTextField jXTextField4;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    public static void openWebsite(String url) {
        Logger.getLogger(tmarker.class.getName()).log(Level.INFO, "Trying to open " + url + ".");
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException ex) {
            System.err.println("Could not open " + url + ".");
            if (tmarker.DEBUG>0) {
                Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Stops all plugins and exits the program.
     */
    void doExit() {
        storeParameterValues();
        for (Pluggable p: plugins) {
            p.stop();
        }
        System.exit(0);
    }
    
    /**
     * Returns the random number generator.
     * @return The random number generator.
     */
    public Random getRandom() {
        return random;
    }
    
    /**
     * Adds a TMAspot to the TMAspot list. Does not update the TMA List in the TMARKER window. This is done by {@link #updateTMATable(TMAspot ts)}.
     * @param ts The TMAspot to be added.
     */
    public void addTMAspot(TMAspot ts) {
        TMAspots.add(ts);
    }
    
    /**
     * Updates the TMA List in the TMARKER window. If the TMAspot ts is not there yet, it is added. Otherwise the information of this TMAspot is updated.
     * @param ts The TMAspot to be updated.
     */
    public void updateTMATable(TMAspot ts) {
        updateTMATable(ts, false);
    }
    
    /**
     * Updates the TMA List in the TMARKER window. If the TMAspot ts is not there yet, it is added. Otherwise the information of this TMAspot is updated.
     * @param ts The TMAspot to be updated.
     * @param newspot If you already know this is a new spot (true), it is directly added to the table without search. If false, the table is searched for it.
     */
    public void updateTMATable(final TMAspot ts, boolean newspot) {
        int n = jPanel36.getComponentCount();
        int i;
        boolean found = false;
        if (!newspot) {
            for (i=0; i<n; i++) {
                if (((TMAspot_list_panel)jPanel36.getComponent(i)).getTMAspot() ==  ts) {
                    TMAspot_list_panel.updateCellNumbers(ts.getTLP());
                    found = true;
                    break;
                }
            }
        }
        // if not found, add a new row.
        if (!found) {
            TMAspot_list_panel.updateCellNumbers(ts.getTLP());
            jPanel36.add(ts.getTLP());
            n++;
        }
        
        //update the thumbnail image
        if (!found) {
            Thread thumbnail = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try { 
                            ts.getTLP().setThumbnailImage(null);
                        } catch (OutOfMemoryError e) {
                           logger.log(java.util.logging.Level.INFO, "Not enough memory for thumbnail creation.");
                        } catch (Exception e) {
                            if (tmarker.DEBUG>0) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            thumbnail.start();
        }
        
        
        //validateScrollPanel();
        ((TitledBorder) (jPanel20.getBorder())).setTitle("TMA List - " + n + " file(s)");
        jPanel20.repaint();
    }
    
    /**
     * Sets the User Info text.
     * @param text The User Info text.
     */
    public void setUserInfo(String text) {
        jTextPane1.setText(text);
        jTextPane1.setCaretPosition(0);
    }
    
    /**
     * validates the TMA List View
     */
    public void validateScrollPanel() {
        // TMA LIST VIEW
        //jScrollPane7.setPreferredSize(new Dimension(jScrollPane7.getWidth(),0));
        int k = 0;
        for (int i=0, n=jPanel36.getComponentCount(); i<n; i++) {
            k += ((TMAspot_list_panel)jPanel36.getComponent(i)).getListItemHeight() + 0;
        }
        jPanel36.setPreferredSize(new Dimension(0, k));
    }
    
    /**
     * Moves selected TMAspot up in the TMA List.
     * @param tss The TMAspots to be moved.
     */
    private void moveTMAspotUp(List<TMAspot> tss) {
        Component[] comps = jPanel36.getComponents();
        for (TMAspot ts: tss) {
            int ind = java.util.Arrays.asList(comps).indexOf(ts.getTLP());
            if (ind > 0+tss.indexOf(ts)) {
                jPanel36.add(ts.getTLP(), ind-1);
            }
        }
        jPanel36.revalidate();
    }
    
    /**
     * Moves selected TMAspot down in the TMA List.
     * @param tss The TMAspots to be moved.
     */
    private void moveTMAspotDown(List<TMAspot> tss) {
        Collections.reverse(tss);
        Component[] comps = jPanel36.getComponents();
        for (TMAspot ts: tss) {
            int ind = java.util.Arrays.asList(comps).indexOf(ts.getTLP());
            if (ind < jPanel36.getComponentCount()-1-tss.indexOf(ts)) {
                jPanel36.add(ts.getTLP(), ind+1);
            }
        }
        jPanel36.revalidate();
    }
    
    /**
     * Sorts the TMAspots in the TMAList by name.
     * @param ascending If true, the ascending order is used, otherwise descending.
     */
    private void sortTMAspotsByName(final boolean ascending) {
        List<TMAspot> tss = getTMAspots();
        Collections.sort(tss, new Comparator<TMAspot>() {

            @Override
            public int compare(TMAspot t1, TMAspot t2) {
                if (ascending) {
                    return (t1.getName().compareTo(t2.getName()));
                } else {
                    return (t2.getName().compareTo(t1.getName()));
                }
            }
        });
        for (TMAspot ts: tss) {
            jPanel36.add(ts.getTLP());
        }
        jPanel36.revalidate();
    }
    
    /**
     * Sorts the TMAspots in the TMAList by staining.
     * @param ascending If true, the ascending order is used, otherwise descending.
     */
    private void sortTMAspotsByStaining(final boolean ascending) {
        List<TMAspot> tss = getTMAspots();
        Collections.sort(tss, new Comparator<TMAspot>() {

            @Override
            public int compare(TMAspot t1, TMAspot t2) {
                if (t1.getTLP().isShowingEstimated()) {
                    if (ascending) {
                        return (TMAspot.getStainingEstimation(t1.getPoints_Estimated(TMALabel.LABEL_UNK)) - TMAspot.getStainingEstimation(t2.getPoints_Estimated(TMALabel.LABEL_UNK)));
                    } else {
                        return (TMAspot.getStainingEstimation(t2.getPoints_Estimated(TMALabel.LABEL_UNK)) - TMAspot.getStainingEstimation(t1.getPoints_Estimated(TMALabel.LABEL_UNK)));
                    }
                } else {
                    if (ascending) {
                        return (TMAspot.getStainingEstimation(t1.getPoints_GoldStandard(TMALabel.LABEL_UNK)) - TMAspot.getStainingEstimation(t2.getPoints_GoldStandard(TMALabel.LABEL_UNK)));
                    } else {
                        return (TMAspot.getStainingEstimation(t2.getPoints_GoldStandard(TMALabel.LABEL_UNK)) - TMAspot.getStainingEstimation(t1.getPoints_GoldStandard(TMALabel.LABEL_UNK)));
                    }
                }
            }
        });
        for (TMAspot ts: tss) {
            jPanel36.add(ts.getTLP());
        }
        jPanel36.revalidate();
    }
    
    /**
     * Sorts the TMAspots in the TMAList by pathological staining (only on malignant nuclei).
     * @param ascending If true, the ascending order is used, otherwise descending.
     */
    private void sortTMAspotsByStainingMalignant(final boolean ascending) {
        List<TMAspot> tss = getTMAspots();
        Collections.sort(tss, new Comparator<TMAspot>() {

            @Override
            public int compare(TMAspot t1, TMAspot t2) {
                if (t1.getTLP().isShowingEstimated()) {
                    if (ascending) {
                        return (TMAspot.getStainingEstimation(t1.getPoints_Estimated(TMALabel.LABEL_POS)) - TMAspot.getStainingEstimation(t2.getPoints_Estimated(TMALabel.LABEL_POS)));
                    } else {
                        return (TMAspot.getStainingEstimation(t2.getPoints_Estimated(TMALabel.LABEL_POS)) - TMAspot.getStainingEstimation(t1.getPoints_Estimated(TMALabel.LABEL_POS)));
                    }
                } else {
                    if (ascending) {
                        return (TMAspot.getStainingEstimation(t1.getPoints_GoldStandard(TMALabel.LABEL_POS)) - TMAspot.getStainingEstimation(t2.getPoints_GoldStandard(TMALabel.LABEL_POS)));
                    } else {
                        return (TMAspot.getStainingEstimation(t2.getPoints_GoldStandard(TMALabel.LABEL_POS)) - TMAspot.getStainingEstimation(t1.getPoints_GoldStandard(TMALabel.LABEL_POS)));
                    }
                }
            }
        });
        for (TMAspot ts: tss) {
            jPanel36.add(ts.getTLP());
        }
        jPanel36.revalidate();
    }
    
    /**
     * Sorts the TMAspots in the TMAList by their number of nuclei.
     * @param ascending If true, the ascending order is used, otherwise descending.
     */
    private void sortTMAspotsByNumberNuclei(final boolean ascending) {
        List<TMAspot> tss = getTMAspots();
        Collections.sort(tss, new Comparator<TMAspot>() {

            @Override
            public int compare(TMAspot t1, TMAspot t2) {
                if (ascending) {
                    return (t1.getPoints().size() - t2.getPoints().size());
                } else {
                    return (t2.getPoints().size() - t1.getPoints().size());
                }
            }
        });
        for (TMAspot ts: tss) {
            jPanel36.add(ts.getTLP());
        }
        jPanel36.revalidate();
    }
    
    /**
     * Returns the four whole image annotation Strings.
     * @return The four whole image annotation Strings. "NA" for no annotation made.
     */
    public List<String> getAnnotationValues() {
        List<String> values = new ArrayList<>();
        // Annotation 1
        if (jRadioButton2.isSelected()) {
            values.add(jTextField6.getText());
        } else {
            values.add(jRadioButton1.getText());
        }
        // Annotation 2
        values.add(Misc.getSelectedButtonText(buttonGroup1));
        // Annotation 3
        values.add(Misc.getSelectedButtonText(buttonGroup2));
        // Annotation 4
        if (jTextField10.getText().trim().isEmpty()) {
            values.add("NA");
        } else {
            values.add(jTextField10.getText());
        }
        return values;
    }
    
    /**
     * Sets the four whole image annotation Values.
     * @param values The four whole image annotation Strings. "" for no annotation made and value will not be changed, "NA" for value will be set to NA.
     */
    public void setAnnotationValues(List<String> values) {
        String value;
        // Annotation 1: Percentage
        if (values.size()>0 && !values.get(0).isEmpty()) {
            value = values.get(0);
            if (value.equals("NA")) {
                jRadioButton1.setSelected(true);
            } else {
                jRadioButton2.setSelected(true);
                jTextField6.setText(value);
            }
        }
        
        // Annotation 2: Mitotic Count
        if (values.size()>1 && !values.get(1).isEmpty()) {
            value = values.get(1);
            Misc.selectButtonWithText(buttonGroup1, value);
        }
        
        // Annotation 3: Pleomorphism
        if (values.size()>2 && !values.get(2).isEmpty()) {
            value = values.get(2);
            Misc.selectButtonWithText(buttonGroup2, value);
        }
        
        // Annotation 4: Comment
        if (values.size()>3 && !values.get(3).isEmpty()) {
            value = values.get(3);
            if (value.trim().equals("NA")) {
                value = "";
            }
            jTextField10.setText(value);
        }
    }
    
    
    /**
     * Returns the names of the four whole image annotations.
     * @return The names of the four whole image annotations.
     */
    public List<String> getAnnotationProperties() {
        List<String> props = new ArrayList<>();
        props.add((jXTextField1.getText().isEmpty()?jXTextField1.getPrompt():jXTextField1.getText()));
        props.add((jXTextField2.getText().isEmpty()?jXTextField2.getPrompt():jXTextField2.getText()));
        props.add((jXTextField4.getText().isEmpty()?jXTextField4.getPrompt():jXTextField4.getText()));
        props.add((jXTextField3.getText().isEmpty()?jXTextField3.getPrompt():jXTextField3.getText()));
        return(props);
    }
    
    /**
     * Sets the names of the four whole image annotations.
     * @param annotation_props The names of the four whole image annotations (should be 4 Strings).
     */
    public void setAnnotationProperties(List<String> annotation_props) {
        if (annotation_props.size()>0 && !annotation_props.get(0).equals("My_Percentage")) jXTextField1.setText(annotation_props.get(0));
        if (annotation_props.size()>1 && !annotation_props.get(1).equals("My_Mitotic_Count")) jXTextField2.setText(annotation_props.get(1));
        if (annotation_props.size()>2 && !annotation_props.get(2).equals("My_Pleomorphism")) jXTextField4.setText(annotation_props.get(2));
        if (annotation_props.size()>3 && !annotation_props.get(3).equals("My_Comment")) jXTextField3.setText(annotation_props.get(3));
    }
    
    /**
     * Saves the current whole image annotation to the current TMAspot properties and shows the nex TMAspot in the TMA List.
     */
    private void doAnnotationAndShowNextSpot() {
        TMAspot ts = getVisibleTMAspot();
        if (ts==null) {
            return;
        }
        List<String> props = getAnnotationProperties();
        boolean[] useThisProp = new boolean[props.size()];
        for (int i=0; i<props.size(); i++) {
            useThisProp[i] = !props.get(i).trim().equals("");
        }
        
        List<TMAspot> tss = getTMAspots();
        
        // test, if some Annotation Properties are already existing
        if (!overwritePropertyQuestionAnswered && !overwriteProperty) {
            List<String> alreadyExisting = new ArrayList<>();
            for (int i=0; i<props.size(); i++) {
                if (useThisProp[i] && tmarker.getProperties(tss).contains(props.get(i))) {
                    alreadyExisting.add(props.get(i));
                }
            }
            if (!alreadyExisting.isEmpty()) {
                String text = "";
                for (String prop: alreadyExisting) {
                    text += prop + " ";
                }
                int opt = JOptionPane.showConfirmDialog(this, "Following properities already exist in the images:\n"+text+"\nDo you want to overwrite them?\n\n"
                        + "YES: Overwrite existing values, if any.\n"
                        + "NO: Adopt existing values, if any.\n"
                        + "CANCEL: Choose new property names.", "Properties already exist", JOptionPane.YES_NO_CANCEL_OPTION);
                if (opt == JOptionPane.CANCEL_OPTION) {
                    return;
                } else overwriteProperty = opt != JOptionPane.NO_OPTION;
            } else {
                overwriteProperty = true;
            }
            overwritePropertyQuestionAnswered = true;
        }
        
        if (!overwriteProperty) {
            for (TMAspot ts_: tss) {
                boolean hasAlreadyAllProperties = true;
                for (int i=0; i<props.size(); i++) {
                    if (useThisProp[i]) { // the 4. property (String-Property) is not checked, since it might be empty, even if it is already set by the user.
                        hasAlreadyAllProperties &= !ts_.getProperties().getProperty(props.get(i), "").isEmpty();
                    }
                }
                if (hasAlreadyAllProperties && !AnnotationAlreadySet.contains(ts_)) {
                    AnnotationAlreadySet.add(ts_);
                }
            }
        }
        
        List<String> values = getAnnotationValues();
        for (int i=0; i<props.size(); i++) {
            if (useThisProp[i]) {
                ts.getProperties().setProperty(props.get(i), values.get(i));
            }
        }
        if (!AnnotationAlreadySet.contains(ts)) {
            AnnotationAlreadySet.add(ts);
        }
        
        // Set the next TMAspot visible...
        if (AnnotationAlreadySet.size() == tss.size()) {
            JOptionPane.showMessageDialog(this, "All images have been annotated. Please select all images and save them as XLS.\nOtherwise, the annotations will we lost after closing the program.", "All files annotated", JOptionPane.INFORMATION_MESSAGE);
            selectAllTMAspots();
            List<String> extensions = new ArrayList<>();
            List<String> descriptions = new ArrayList<>();
            extensions.add("csv");
            descriptions.add("CSV file for selected images with their property table");
            File file = FileChooser.chooseSavingFile(this, getCurrentDir(), Misc.FilePathStringtoFilenameWOExtension(getVisibleTMAspot().getName()), extensions, descriptions);
            tmarker.SaveAsCSV_Properties(this, file);
            AnnotationAlreadySet.clear();
            AnnotationSkipped.clear();
            return;
        }
        if (getTMAspots().size() == AnnotationAlreadySet.size() + AnnotationSkipped.size()) {
            AnnotationSkipped.clear();
            jButton7.setEnabled(false);
        }
        for (TMAspot ts_next: tss) {
            if (!AnnotationAlreadySet.contains(ts_next) && !AnnotationSkipped.contains(ts_next)) {
                ts.setSelected(false);
                showTMAspot(ts_next);
                break;
            }
        }
    }
    
    /**
     * Does not save the current whole image annotation to the current TMAspot properties and shows the nex TMAspot in the TMA List.
     */
    private void doSkipAndShowNextSpot() {
        TMAspot ts = getVisibleTMAspot();
        AnnotationSkipped.add(ts);
        if (getTMAspots().size() == AnnotationAlreadySet.size() + AnnotationSkipped.size()) {
            AnnotationSkipped.clear();
            jButton7.setEnabled(false);
        }
        for (TMAspot ts_next: getTMAspots()) {
            if (!AnnotationAlreadySet.contains(ts_next) && !AnnotationSkipped.contains(ts_next)) {
                showTMAspot(ts_next);
                break;
            }
        }
    }
    
    /**
     * Returns the TMAspots.
     * @return All TMAspots stored in this session.
     */
    public List<TMAspot> getTMAspots() {
        List<TMAspot> tss = new ArrayList<>();
        Component[] comps = jPanel36.getComponents();
        for (Component comp : comps) {
            tss.add(((TMAspot_list_panel) (comp)).getTMAspot());
        }
        // return TMAspots;
        return tss;
    }
    
    /**
     * Sets the current directory (e.g. for file saving and opening).
     * @param currentDir The current directory.
     */
    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
    }

    /**
     * Returns the current directory (e.g. for file saving and opening).
     * @return The current directory.
     */
    public String getCurrentDir() {
        return currentDir;
    }
     
    /**
     * Returns the current working directory (e.g. for saving temporary files).
     * @return The current working directory.
     */
    public String getWorkingDir() {
        return System.getProperty("user.dir");
    }
    
    /**
     * Returns the option dialog.
     * @return The option dialog.
     */
    public OptionDialog getOptionDialog() {
        return od;
    }
    
    /**
     * Loads an image as TMAspot. The file is a new TMAspot and added to TMARKER t.
     * @param t The TMARKER session.
     * @param file The (image) file which becomes a new TMAspot.
     */
    private static void LoadImage(tmarker t, File file) {
        String fname = file.getAbsolutePath();
        TMAspot ts = new TMAspot(t, fname);
        if (t.getTMAspotWithName(ts.getName()) == null) {
            t.addTMAspot(ts);
            t.updateTMATable(ts, true);
        }
    }
    
    /**
     * Loads a XML file which has been stored previously. Tries first to match
     * xml with opened images. If it fails, it tries to find the images on the
     * System. If it fails, it asks the user for a path for the images. If this
     * fails, the file can't be opened.
     * @param t The tmarker center
     * @param file The file to be loaded.
     */private static void LoadXML(tmarker t, File file) {
        t.setStatusMessageLabel("Reading " + file.getName() + " ...");
        XStream xstream = new XStream(new DomDriver());
        xstream.ignoreUnknownElements();
        xstream.alias("TMAspot", TMAspot.class);
        xstream.alias("TMALabel", TMALabel.class);
        xstream.alias("TMApoint", TMApoint.class);
        xstream.omitField(TMAspot.class, "tc");
        xstream.omitField(TMAspot.class, "tlp");
        
        List<TMAspot> newspots = new ArrayList<>();
        try {
            ObjectInputStream in = xstream.createObjectInputStream(new BufferedReader(new FileReader(file)));
            Object o;
            
            try {
                o = in.readObject();
            } catch (java.io.EOFException eof) {
                t.setStatusMessageLabel("");
                in.close();
                return;
            }
            
            // Read the Sesstion ID.
            try {
                UUID UID_old = (UUID) o;
                o = in.readObject();
            } catch (ClassCastException e) { }
            
            // Read the properties.
            try {
                Properties props = (Properties) o;
                t.restoreParameterValues(props, false);
                t.restoreParameterValues(props, true);
                o = in.readObject();                
            } catch (ClassCastException e) { }
            
            // Read the TMAspots
            while(true) {
                try {
                    // The object is a new TMAspot
                    TMAspot ts = (TMAspot) o;
                    t.setStatusMessageLabel("Reading " + ts.getName() + " ...");
                    newspots.add(ts);
                    o = in.readObject();
                } catch (java.io.EOFException eof) {
                    in.close();
                    break;
                }
            }
            
            // first: try to match the spots with already opened spots
            for (int i=newspots.size()-1; i>=0; i--) {
                TMAspot opened_spot = t.getTMAspotWithName(newspots.get(i).getName());
                if (opened_spot!=null) {
                    if (!t.question_answered && !opened_spot.getPoints().isEmpty()) {
                        int ok = JOptionPane.showOptionDialog(t, "TMARKER has recognized that you want to open a XML file\n"
                                + "for an already opened image with existing points.\n"
                                + "Do you want to add the XML file as new labeler?\n\n"
                                + "If YES, all labels from the XML are converted to\n"
                                + "gold standard with new Labeler's number.\n\n"
                                + "If NO, all labels from the XML are loaded as they are\n"
                                + "and added to the current image.", "How to open XML files?", JOptionPane.YES_NO_OPTION, 1, null, null, null);
                        t.convertPoints = ok==JOptionPane.YES_OPTION;
                        t.question_answered = true;
                    }
                    byte new_GS_number = (t.convertPoints?(byte)(opened_spot.getMaxGSNumber()+1):0);
                    //if (convertPoints) {
                    //    for (int z=0; z<opened_spot.getPoints().size(); z++) {
                    //       opened_spot.getPoints().get(z).setGoldStandard(t.getGSNumber());
                    //    }
                    //}
                    for (int j=0; j<newspots.get(i).getPoints().size(); j++) {
                        if (t.convertPoints) {
                            newspots.get(i).getPoints().get(j).setGoldStandard(new_GS_number);
                        }
                        newspots.get(i).getPoints().get(j).setTMAspot(opened_spot);
                        opened_spot.addPoint(newspots.get(i).getPoints().get(j));
                    }
                    opened_spot.adoptParams(newspots.get(i));
                    opened_spot.doBgCorrection();
                    t.updateTMATable(opened_spot);
                    newspots.remove(i);
                }
            }

            // second: try to find the spots that could not be mached on the location specified in the xml
            for (int i=newspots.size()-1; i>=0; i--) {
                if (new File(newspots.get(i).getOriginalImagename()).exists()) {
                    TMAspot ts = new TMAspot(t, newspots.get(i).getOriginalImagename());
                    for (int j=0; j<newspots.get(i).getPoints().size(); j++) {
                        newspots.get(i).getPoints().get(j).setTMAspot(ts);
                        ts.addPoint(newspots.get(i).getPoints().get(j));
                    }
                    ts.adoptParams(newspots.get(i));
                    ts.doBgCorrection();
                    t.addTMAspot(ts);
                    t.updateTMATable(ts, true);
                    newspots.remove(i);
                }
            }

            // third: try to find the images in the same folder like xml
            if (!newspots.isEmpty()) {
                for (int i=newspots.size()-1; i>=0; i--) {
                    logger.log(java.util.logging.Level.INFO, file.getParent() + File.separator + newspots.get(i).getName());
                    if (new File(file.getParent() + File.separator + newspots.get(i).getName()).exists()) {
                        TMAspot ts = new TMAspot(t, file.getParent() + File.separator + newspots.get(i).getName());
                        for (int j=0; j<newspots.get(i).getPoints().size(); j++) {
                            newspots.get(i).getPoints().get(j).setTMAspot(ts);
                            ts.addPoint(newspots.get(i).getPoints().get(j));
                        }
                        ts.adoptParams(newspots.get(i));
                        ts.doBgCorrection();
                        t.addTMAspot(ts);
                        t.updateTMATable(ts, true);
                        newspots.remove(i);
                    }
                }            
            }

            // fourth: try to find the images in path
            if (!newspots.isEmpty()) {
                for (int p=0; p<t.getPath().size(); p++) {
                    for (int i=newspots.size()-1; i>=0; i--) {
                        if (new File(t.getPath().get(p) + File.separator + newspots.get(i).getName()).exists()) {
                            TMAspot ts = new TMAspot(t, t.getPath().get(p) + File.separator + newspots.get(i).getName());
                            for (int j=0; j<newspots.get(i).getPoints().size(); j++) {
                                newspots.get(i).getPoints().get(j).setTMAspot(ts);
                                ts.addPoint(newspots.get(i).getPoints().get(j));
                            }
                            ts.adoptParams(newspots.get(i));
                            ts.doBgCorrection();
                            t.addTMAspot(ts);
                            t.updateTMATable(ts, true);
                            newspots.remove(i);
                        }
                    }
                }
            }


            // fifth: ask the user to select a path to the images
            if (!newspots.isEmpty()) {
                JOptionPane.showMessageDialog(t, "Some images specified in " + file.getName() + " could not be found.\n"
                        + "After this dialog, you will be asked to specify a path to the images.\n"
                        + "Note, that the images must have the same name as saved in " + file.getName() + ".\n"
                        + "e.g. " + newspots.get(0).getName(), "Images not found", JOptionPane.INFORMATION_MESSAGE);
                JFileChooser chooser = new JFileChooser(t.getCurrentDir());
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int approve = chooser.showOpenDialog(t);
                if (approve == JFileChooser.APPROVE_OPTION) {
                    for (int i=newspots.size()-1; i>=0; i--) {
                        if (new File(chooser.getSelectedFile().getAbsoluteFile() + File.separator + newspots.get(i).getName()).exists()) {
                            TMAspot ts = new TMAspot(t, chooser.getSelectedFile().getAbsoluteFile() + File.separator + newspots.get(i).getName());
                            for (int j=0; j<newspots.get(i).getPoints().size(); j++) {
                                newspots.get(i).getPoints().get(j).setTMAspot(ts);
                                ts.addPoint(newspots.get(i).getPoints().get(j));
                            }
                            ts.adoptParams(newspots.get(i));
                            ts.doBgCorrection();
                            t.addTMAspot(ts);
                            t.updateTMATable(ts, true);
                            newspots.remove(i);
                            t.addPath(chooser.getSelectedFile().getAbsoluteFile().toString());
                        }
                    }
                }
            }

            // sixth: report spots that could not be found
            if (!newspots.isEmpty()) {
                String message = "Following images could not be found:\n";
                int dmax = 10;
                for (int i=0; i<Math.min(dmax, newspots.size()); i++) {
                    message += "\t" + newspots.get(i).getName() + "\n";
                }
                if (dmax<newspots.size()) {
                    message += "and " + Integer.toString(newspots.size()-dmax) + " more.";
                }
                JOptionPane.showMessageDialog(t, message, "Images not found", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(t, "The file " + file.getName() + " could not be parsed.\n\n Maybe the XML file is from an old version of TMARKER.", "Wrong File Format", JOptionPane.ERROR_MESSAGE);                        
            if (tmarker.DEBUG>0) {
                Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, e);
            }
        } finally {
            t.setStatusMessageLabel("");
        }
    }
     
    /**
      * Loads a CSV file with metainformation for the TMAspots already loaded.
      * The CSV file is semicolon separated, has one row per TMAspot and one column
      * per metainformation (e.g. protein expression rates). One column must contain
      * the TMAspot name (filename of the TMA image).
      * @param t The tmarker instance.
      * @param file The CSV file. Must be semicolon separated and have headers.
      */
    private static void LoadCSV(tmarker t, File file) {
        String oldStatusText = t.getStatusMessageLabel().getText();
        t.setStatusMessageLabel("Reading File " + file.getName());
        try {
            BufferedReader bufFRdr  = new BufferedReader(new FileReader(file));
            String sep = ";";
            String line;
            int row = 0;
            List<String> headers = new ArrayList<>(); // keys for properties
            int imagename_ind = 0;
            boolean userSetIndex = false;
            boolean userWantsSearchOtherFiles = true;
            boolean userWantsSearchOtherPaths = true;
            int ln = 0;
            String txt = "";
            
            // pre-read the file to release it fast
            while((line = bufFRdr.readLine()) != null) {
                txt += line + "\n";
                ln++;
            }
            bufFRdr.close();
            BufferedReader bufRdr = new BufferedReader(new StringReader(txt));
            
            // read header line of text file
            line = bufRdr.readLine();
            if (line != null) {
                String[] keys = line.split(sep);
                for (String key : keys) {
                    //get next token and store it in the header list
                    headers.add(key.replaceAll("\"", ""));
                }
            }
            
            
            //read each line of text file
            while((line = bufRdr.readLine()) != null && userWantsSearchOtherFiles) {
                t.setProgressbar((int)(100.0 * row++/ln));
                String[] values = line.split(sep, headers.size());
                String value;
                SortedProperties prop = new SortedProperties();
                for (int i=0; i<headers.size(); i++) {
                    String key = headers.get(i);
                    if (!key.isEmpty()) {
                        value = "";
                        if (i<values.length) {
                            //get next token and store it in the properties
                            value = values[i].replaceAll("\"", "").trim();
                        }
                        prop.setProperty(headers.get(i), value);
                    }
                }
                
                if (!prop.isEmpty()) {
                    // first: try to match the spots with already opened spots
                    // find the TMA image with the right imagename
                    TMAspot ts = null;
                    String imgname = prop.getProperty(headers.get(imagename_ind));
                    if (imgname!=null && !imgname.equals("")) {
                        ts = t.getTMAspotWithName(imgname);
                        if (ts!=null) {
                            ts.addProperties(prop);
                        } else {
                            // find the right column of the image name (for matching)
                            for (int i=0; i<headers.size(); i++) {
                                try {
                                    imgname = prop.getProperty(headers.get(i));
                                } catch (Exception ex) {
                                    imgname = null;
                                }
                                if (imgname!=null && !imgname.equals("")) {
                                    ts = t.getTMAspotWithName(imgname);
                                    if (ts!=null) {
                                        ts.addProperties(prop);
                                        imagename_ind = i;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // second: ask the user to specify the column of image name
                    if (ts==null && !userSetIndex) { 
                        String input = (String) JOptionPane.showInputDialog(t, "File " + file.getName() + ":\n"
                                + "Some images listed in this file could not be found.\n"
                        + "To search for the images, please select the column in the file\n"
                                + "that represents the image names.", "Images not found", JOptionPane.INFORMATION_MESSAGE, null, headers.toArray(), headers.get(imagename_ind));
                        if (input != null) {
                            userSetIndex = true;
                            imagename_ind = headers.indexOf(input);
                        } else {
                            userWantsSearchOtherFiles = false;
                            continue;
                        }
                    }

                    // third: try to find the images as specified in image name column
                    if (ts==null) {                    
                        logger.log(java.util.logging.Level.INFO, "Try to find " + prop.getProperty(headers.get(imagename_ind)));
                        if (new File(prop.getProperty(headers.get(imagename_ind))).exists()) {
                            ts = new TMAspot(t, prop.getProperty(headers.get(imagename_ind)));
                            ts.setProperties(prop);
                            t.addTMAspot(ts);
                            t.updateTMATable(ts, true);
                        }                                
                    }

                    // fourth: try to find the images in the same folder like csv
                    if (ts==null) {                    
                        logger.log(java.util.logging.Level.INFO, "Try to find " + file.getParent() + File.separator + prop.getProperty(headers.get(imagename_ind)));
                        if (new File(file.getParent() + File.separator + prop.getProperty(headers.get(imagename_ind))).exists()) {
                            ts = new TMAspot(t, file.getParent() + File.separator + prop.getProperty(headers.get(imagename_ind)));
                            ts.setProperties(prop);
                            t.addTMAspot(ts);
                            t.updateTMATable(ts, true);
                        }                                
                    }

                    // fifth: try to find the images in path
                    if (ts==null) { 
                        for (int p=0; p<t.getPath().size(); p++) {
                            if (new File(t.getPath().get(p) + File.separator + prop.getProperty(headers.get(imagename_ind))).exists()) {
                                ts = new TMAspot(t, t.getPath().get(p) + File.separator + prop.getProperty(headers.get(imagename_ind)));
                                ts.setProperties(prop);
                                t.addTMAspot(ts);
                                t.updateTMATable(ts, true);
                                break;
                            }
                        }
                    }

                    // sixth: ask the user to select a path to the images
                    if (ts==null && userWantsSearchOtherPaths) { 
                        JOptionPane.showMessageDialog(t, "The image " + prop.getProperty(headers.get(imagename_ind))
                        + " could not be found.\n"
                        + "After this dialog, you will be asked to specify a path to the image.\n"
                        + "Note, that the images must have the same name as saved in " + file.getName() + ".\n", 
                                "Image not found", JOptionPane.INFORMATION_MESSAGE);
                        JFileChooser chooser = new JFileChooser(t.getCurrentDir());
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int approve = chooser.showOpenDialog(t);
                        if (approve == JFileChooser.APPROVE_OPTION) {
                            String fname = chooser.getSelectedFile().getAbsoluteFile() + File.separator + prop.getProperty(headers.get(imagename_ind));
                            t.addPath(chooser.getSelectedFile().getAbsoluteFile().toString());
                            if (new File(fname).exists()) {
                                ts = new TMAspot(t, chooser.getSelectedFile().getAbsoluteFile() + File.separator + prop.getProperty(headers.get(imagename_ind)));
                                ts.setProperties(prop);
                                t.addTMAspot(ts);
                                t.updateTMATable(ts, true);
                                t.setCurrentDir(chooser.getSelectedFile().getAbsoluteFile().toString());
                            }
                        } else {
                            userWantsSearchOtherPaths = false;
                            continue;
                        }
                    }                    
                }
            }
            bufRdr.close();
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.WARNING, "Error Loading File "+file.getName());
        } finally {
            t.setStatusMessageLabel(oldStatusText);
            t.setProgressbar(0);
        }
    }

    /**
     * Opens a JFileChooser and loads the selected files.
     * @param t The TMARKER session.
     * @param currentDir_local The current directory (opened by the chooser).
     */
    public static void LoadFilesWithChooser(tmarker t, String currentDir_local) {
        File[] filelist = FileChooser.chooseLoadingFiles(t, currentDir_local);
        LoadFiles(t, filelist);
    }
    
    /**
     * Returns the Debug status of tmarker (0-5).
     * @return The Debug status of tmarker (0=no output, 5= lots of output).
     */
    public int DEBUG() {
        return DEBUG;
    }
    
    /**
     * Loads a list of files into TMARKER.
     * @param t The TMARKER session.
     * @param filelist A list of files to be opened. Depending on the extension, TMARKER decides how to open the files.
     */
    public static void LoadFiles(tmarker t, File[] filelist) {
        t.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        List<String> allowed_ext = new ArrayList<>();
        allowed_ext.add("xml");
        allowed_ext.add("csv");
        allowed_ext.add("tma");
        allowed_ext.add("tif");
        allowed_ext.add("tiff");
        String[] its = ImageIO.getReaderFormatNames();
        for (int i=0; i<its.length; i++) {
            allowed_ext.add(its[i].toLowerCase());
        }
        
        int numTMAspots_before = t.getTMAspots().size();
        if (filelist != null && filelist[0] != null) {
            String currentDir_local = filelist[0].getAbsolutePath();
            t.setCurrentDir(currentDir_local);
            String errorMSG = "";
            for (int j = 0; j < filelist.length; j++) {
                t.setProgressbar((100*j)/filelist.length);
                if (filelist[j]!=null && filelist[j].exists() && allowed_ext.contains(Misc.FilePathStringtoExtension(filelist[j].getName()).toLowerCase())) {
                    try {
                        if (Misc.FilePathStringtoExtension(filelist[j].getName()).equalsIgnoreCase("xml")) {
                            LoadXML(t, filelist[j]);
                        } else if (Misc.FilePathStringtoExtension(filelist[j].getName()).equalsIgnoreCase("csv")) {
                            LoadCSV(t, filelist[j]);
                        } else {
                            LoadImage(t, filelist[j]);
                        }
                    } catch (Exception e) {
                        t.setCursor(Cursor.getDefaultCursor());
                        JOptionPane.showMessageDialog(t, "The file " + filelist[j].getName() + " could not be parsed.\n\n" + e.getMessage(), "Wrong File Format", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    errorMSG += filelist[j].getName() + "\n";
                }   
            }
            if (!errorMSG.equals("")) {
                t.setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(t, "The following files could not be loaded:\n\n " + errorMSG + "\nMaybe the filetype is not supported.", "Wrong File Format", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!t.getTMAspots().isEmpty()) {
                if (t.getTMAspots().size()<=numTMAspots_before) {
                    numTMAspots_before = t.getTMAspots().size()-1;
                }
                if (t.getVisibleTMAspot()==null) {
                    t.showTMAspot(t.getTMAspots().get(numTMAspots_before));
                }
                t.setState_ImagesLoaded();
                
                // revalidate the TMA List
                t.getTMAList().update(t.getTMAList().getGraphics());
            }
                   
            // reset the global filehandling variables
            t.question_answered = false;
            t.convertPoints = false; 
           
        }
        t.setProgressbar(0);
        t.setCursor(Cursor.getDefaultCursor());
        //t.setThumbnailsThread();
    }
    
    /**
     * Sets all TMAspots as selected.
     */
    public void selectAllTMAspots() {
        for (TMAspot ts: getTMAspots()) {
            ts.setSelected(true);
        }
        for (Pluggable p: plugins) {
            p.updateOptionsToTMAspot(getVisibleTMAspot(), getSelectedTMAspots(false));
        }
    }
    
    /**
     * Sets a given TMAspot as selected. All others will be un-selected.
     * @param ts The selected TMAspot.
     */
    public void selectTMAspot(TMAspot ts) {
        for (TMAspot ts_: getTMAspots()) {
            ts.setSelected(ts_==ts);
        }
        for (Pluggable p: plugins) {
            p.updateOptionsToTMAspot(getVisibleTMAspot(), getSelectedTMAspots(false));
        }
    }

    /**
     * Returns the currently selected TMAspots. If none is selected, asks the user to select at least one.
     * @return The selected TMAspots.
     */
    public List<TMAspot> getSelectedTMAspots() {
        return getSelectedTMAspots(true);
    }
    
    /**
     * Returns the currently selected TMAspots. 
     * @param verbose If true and no TMAspot is selected, the user is asked to select one. If false, the user is never asked.
     * @return The currently selected TMAspots.
     */
    public List<TMAspot> getSelectedTMAspots(boolean verbose) {
        List<TMAspot> aTMAspots = new ArrayList<>();
        for (TMAspot ts_: getTMAspots()) {
            if (ts_.isSelected()) {
                aTMAspots.add(ts_);
            }
        }
        if (verbose && aTMAspots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one image in the TMA List.", "No image selected", JOptionPane.INFORMATION_MESSAGE);
        }
        return aTMAspots;
    }
    
    /**
     * Returns the currently visible TMAspot. 
     * @return The currently visible TMAspot. Null if there is none.
     */
    public TMAspot getVisibleTMAspot() {
        if (jPanel3 == null) return null;
        if (jPanel3.getComponentCount() == 0 ) return null;
        try {
            return ((TMA_view_panel) jPanel3.getComponent(0)).getTMAspot();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Removes a TMAspot from the TMARKER session. The user is asked to confirm that.
     * @param ts The TMAspot to be removed.
     */
    public void removeTMAspot(TMAspot ts) {
        if (ts!=null) {
            List<TMAspot> tss = new ArrayList<>();
            tss.add(ts);
            removeTMAspots(tss);
        }
    }
    
    /**
     * Removes TMAspots from the TMARKER session. The user is asked to confirm that.
     * @param tss The TMAspots to be removed.
     */
    public void removeTMAspots(List<TMAspot> tss) {
        if (!tss.isEmpty()) {
            String text;
            if (tss.size()==1) {
                text = "Are you sure to remove " + tss.get(0).getName() + "?";
            } else {
                text = "Are you sure to remove selected images?";
            }
            int c = JOptionPane.showConfirmDialog(this, text, "Confirm Removing Instances", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
            remove(tss);
            }
        }
    }

    /**
     * Returns the panel containing the TMA List.
     * @return The panel with the TMA List.
     */
    public JPanel getTMAList() {
        return jPanel36;
    }
    
    /**
     * Returns the JScrollPane containing the TMA List.
     * @return The JSCrollPane with the TMA List.
     */
    public JScrollPane getTMAListScrollPane() {
        return jScrollPane7;
    }
    
    /**
     * Removes the given TMAspots from the TMA List.
     * @param tss The TMAspots to be removed.
     */
    private void removeTMAspotFromTable(List<TMAspot> tss) {
        for (Component comp : jPanel36.getComponents()) {
            if (tss.contains(((TMAspot_list_panel) comp).getTMAspot())){
                jPanel36.remove(comp);
            }
        }
        ((TitledBorder) (jPanel20.getBorder())).setTitle("TMA List - " + jPanel36.getComponentCount() + " file(s)");
        jPanel20.repaint();
        jPanel36.validate();
        jPanel36.repaint();
    }
    
    /**
     * Removes the given TMAspots from the TMARKER session.
     * @param tss The TMAspots to be removed.
     */
    private void remove(List<TMAspot> tss) {
        TMAspot ts = getVisibleTMAspot();
        removeTMAspotFromTable(tss);
        TMAspots.removeAll(tss);
        if (tss.contains(ts)) {
            if (!TMAspots.isEmpty()){
                showTMAspot(TMAspots.get(0));
                TMAspots.get(0).setSelected(true);
            } else {
                setState_init();
                repaint();
            }
        }
    }
    
    /**
     * Performs Voronoi Sampling for background points sampling. First, all current background
     * points are deleted. Then, a voronoi diagram is created around all remaining gold-standard TMAlabels.
     * The vertices of this diagram are loci of new background points.
     */
    public void performVoronoiSampling() {
        for (TMAspot ts: getSelectedTMAspots()) {
            ts.deleteAllPoints(TMALabel.LABEL_BG, true);
            ts.deleteAllPoints(TMALabel.LABEL_BG, false);
            List<Pnt> pnt_in = new ArrayList<>(ts.getPoints_GoldStandard().size());
            for (TMApoint p: ts.getPoints_GoldStandard()) {
                pnt_in.add(new Pnt(p.x, p.y));
            }
            BufferedImage I = ts.getBufferedImage();
            List<Pnt> pnt_out = DelaunayAp.getAllVoronoiPoints(pnt_in, 0, I.getWidth(), 0, I.getHeight());
            for (Pnt p: pnt_out) {
                TMApoint tp = new TMApoint(ts, (int)p.coord(0), (int)p.coord(1), TMALabel.LABEL_BG);
                tp.setGoldStandard(tp.getTMAspot().getCenter().getGSNumberForLabeling());
                ts.addPoint(tp);
            }
            performBackgroundPointsFiltering(ts);
            if (ts==getVisibleTMAspot()) {
                getTMAView().repaint();
            }
            updateTMATable(ts);
            getTSD().updateSummary(ts);
        }
    }
    
    /**
     * Removes overlapping background points.
     * @param ts The TMAspot to be processed.
     */
    public void performBackgroundPointsFiltering(TMAspot ts) {
        if (ts != null) {
            List<TMApoint> tps = ts.getPoints_GoldStandard();
            for (int i=tps.size()-1; i>=0; i--) {
                TMApoint tp1 = tps.get(i);
                if (tp1.getLabel()==TMALabel.LABEL_BG) {
                    for (int j=i-1; j>=0; j--) {
                        TMApoint tp2 = tps.get(j);
                        if (Math.sqrt(Math.pow(tp1.x-tp2.x, 2) + Math.pow(tp1.y-tp2.y, 2)) <= 3*ts.getParam_r()) {
                            if (tp2.getLabel()==TMALabel.LABEL_BG) {
                                tp2.x = (tp1.x+tp2.x)/2;
                                tp2.y = (tp1.y+tp2.y)/2;
                                ts.getPoints().remove(tp1);
                                break;
                            } else {
                                ts.getPoints().remove(tp1);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Removes the background labels from the given TMAspots.
     * @param tss The TMAspots to be processed.
     */
    private void deleteBackgroundPoints(List<TMAspot> tss) {
        for (TMAspot ts: tss) {
            ts.deleteAllPoints(TMALabel.LABEL_BG, true);
            ts.deleteAllPoints(TMALabel.LABEL_BG, false);
            if (ts==getVisibleTMAspot()) {
                getTMAView().repaint();
            }
            updateTMATable(ts);
            getTSD().updateSummary(ts);
        }
    }

    /**
     * Returns the panel containing the TMA image.
     * @return The TMA View panel.
     */
    public TMA_view_panel getTMAView() {
        return tvp;
    }

    /**
     * Shows a TMAspot in the TMARKER TMA View.
     * @param ts The TMAspot to be displayed.
     */
    public void showTMAspot(TMAspot ts) {
        if (jPanel3.getComponentCount()==0) {
            jPanel3.add(tvp);
        }
        
        tvp.showTMAspot(ts);
        tsd.updateSummary(ts);
        for (Pluggable p: plugins) {
            p.updateOptionsToTMAspot(getVisibleTMAspot(), getSelectedTMAspots(false));
        }
        try {
            if (ts!=null) {
                // update the nucleus annotation
                setLabelRadius(ts.getParam_r());
                
                // update the "whole image annotation"
                if (ts.getProperties()!=null && !ts.getProperties().isEmpty()) {
                    List<String> annotation_names = getAnnotationProperties();
                    List<String> annotation_values = new ArrayList<>();
                    String value;
                    
                    // Annotation 1: Percentage
                    value = ts.getProperties().getProperty(annotation_names.get(0), "");
                    annotation_values.add(value);
                        
                    // Annotation 2: Mitotic Count
                    value = ts.getProperties().getProperty(annotation_names.get(1), "");
                    annotation_values.add(value);
                        
                    // Annotation 3: Pleomorphism
                    value = ts.getProperties().getProperty(annotation_names.get(2), "");
                    annotation_values.add(value);
                    
                    // Annotation 4: Comment
                    value = ts.getProperties().getProperty(annotation_names.get(3), "");
                    annotation_values.add(value);
                    
                    setAnnotationValues(annotation_values);
                }
            }
            
            showTMAspotPreview();

            // select TMAspot if necessary
            if (ts!=null && !getSelectedTMAspots(false).contains(ts)) {
                ts.setSelected(true);
            }
            
        } catch (Exception e) {
            if (DEBUG>0) {
                e.printStackTrace();
            }
        }
    } 
    
    /**
     * Displays the TMAspot preview of the currently visible TMAspot.
     */
    public void showTMAspotPreview() { 
        if (jPanel11.getComponents().length<1) {
            zip.setZoomMin(Double.MIN_VALUE);
            jPanel11.add(zip, java.awt.BorderLayout.WEST);
        }
        zip.setImage(getVisibleTMAspot()==null?null:getVisibleTMAspot().getBufferedImage());
        if (tvp.getImage()!=null) {
            double zf = Math.min((double)jPanel11.getWidth()/(double)((BufferedImage)(tvp.getImage())).getWidth(), (double)jPanel11.getHeight()/(double)((BufferedImage)(tvp.getImage())).getHeight());
            zip.setZoom(zf);
        } else {
            zipl.setImage(null);
        }
    }
    
    /**
     * Displays the TMAspot zoomed view of the currently visible TMAspot.
     * @param x The x-coord of the middle of the preview, relative to the original image.
     * @param y The y-coord of the middle of the preview, relative to the original image.
     */
    public void showTMAspotLocalZoom(int x, int y) { 
        if (jScrollPane3.getViewport().getComponents().length==0) {
            zipl.setZoomMax(Double.MAX_VALUE);
            jScrollPane3.getViewport().add(zipl);
        } 
        if (zipl.getImage() != tvp.getImage()) {
            zipl.setImage(tvp.getImage());
        }
        zipl.scrollRectToVisible(new java.awt.Rectangle((int)(zoomfactor*x-jScrollPane3.getWidth()/2.0), (int)(zoomfactor*y-jScrollPane3.getHeight()/2.0), jScrollPane3.getWidth(), jScrollPane3.getHeight()));
        zipl.setZoom(zoomfactor);
    }
    
    /**
     * Displays the rectangle on the TMAspot preview.
     * @param x The x-coord of the middle of the preview, relative to the original image.
     * @param y The y-coord of the middle of the preview, relative to the original image.
     */
    public void showTMAspotLocalZoomOnPreview(int x, int y) { 
        zip.update(zip.getGraphics());
        Graphics2D g = (Graphics2D) zip.getGraphics();
        g.setColor(Color.RED);
        java.awt.Rectangle rect = new java.awt.Rectangle((int)(zoomfactor*x-jScrollPane3.getWidth()/2.0), (int)(zoomfactor*y-jScrollPane3.getHeight()/2.0), jScrollPane3.getWidth(), jScrollPane3.getHeight());
        double zf = Math.min((double)jPanel11.getWidth()/(double)((BufferedImage)(tvp.getImage())).getWidth(), (double)jPanel11.getHeight()/(double)((BufferedImage)(tvp.getImage())).getHeight()) / zoomfactor;
        g.drawRect((int)(zf*rect.x), (int)(zf*rect.y), (int)(zf*rect.width), (int)(zf*rect.height));
        g.dispose();
    }
    
    /**
     * Returns the progress bar.
     * @return The progress bar.
     */
    public JProgressBar getProgressbar() {
        return jProgressBar1;
    }
    
    /**
     * Sets the progress bar value.
     * @param percentage The progress bar value between 0 and 100.
     */
    public void setProgressbar(int percentage) {
        jProgressBar1.setValue(percentage);
        jProgressBar1.update(jProgressBar1.getGraphics());
    }
    
    /**
     * Returns the status message label.
     * @return The status message label.
     */
    public JLabel getStatusMessageLabel() {
        return jLabel4;
    }
    
    /**
     * Sets the text of the status message label.
     * @param text The status message.
     */
    public void setStatusMessageLabel(String text) {
        jLabel4.setText(text);
        jXStatusBar1.update(jXStatusBar1.getGraphics());
    }
    
    /**
     * Returns the color of the nuclei with the given labelType and staining.
     * @param labelType The nucleus label type (e.g. TMAspot.LABEL_POS).
     * @param staining The staining of the nuclei (e.g. TMAspot.STAINING_0).
     * @return The color of the labels of these nuclei.
     */
    public Color getLabelsColor(byte labelType, byte staining) {
        LegendElementNucleus len = getNucleusToolBarComponent(labelType, staining);
        if (len!=null) {
            return len.getColor();
        } else {
            return Color.BLACK;
        }
    }
    
    /**
     * Sets the color of the nuclei with the given labelType and staining.
     * @param labelType The nucleus label type (e.g. TMAspot.LABEL_POS).
     * @param staining The staining of the nuclei (e.g. TMAspot.STAINING_0).
     * @param c The color of the labels of these nuclei.
     */
    public void setLabelsColor(byte labelType, byte staining, Color c) {
        LegendElementNucleus len = getNucleusToolBarComponent(labelType, staining);
        len.setColor(c);
    }
    
    /**
     * Returns the color of the nuclei with the given labelType and staining.
     * @param labelType The nucleus label type (e.g. TMAspot.LABEL_POS).
     * @param staining The staining of the nuclei (e.g. TMAspot.STAINING_0).
     * @return The color of the labels of these nuclei. The color has alpha value 255.
     */
    public Color getLabelsColorWOAlpha(byte labelType, byte staining) {
        Color c = getLabelsColor(labelType, staining);
        Color c_new = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
        return c_new;
    }
    
    /**
     * Returns the JScrollPane containing the TMA View.
     * @return The JScrollPane containing the TMA View.
     */
    public JScrollPane getTMAViewContainer() {
        return jScrollPane1;
    }
    
    /**
     * Displays the user info visible after loading the images.
     */
    public void setState_ImagesLoaded() {
        infoText = "<html><font size=\"3\" face=\"Arial\"><h2><strong>The images have been loaded successfully.</strong></h2></strong>"
                + "Use the <i><b>View Controls</b></i> on bottom of TMARKER to zoom the image or to hide or unhide groups of found nuclei.<br><br>"
                + "Please continue with following options:<br>"
                + "<ul style=\"margin:6; padding:12;\">"
                + "<li><b>Manual nucleus annotation</b>: Please use the <i>Nucleus Annotation</i> toolbar on top of TMARKER to draw <u>nuclei</u> or <u>regions of interests (ROI)</u>. "
                + "Nuclei can be edited, shifted or deleted. "
                + "Change the nuclei radius to fit the image.<br><br></li>"
                + "<li><b>Whole image annotation</b> (e.g. intensity estimation, pleomorphism estimation, comments, etc.): Please use the "
                + "<i>Whole Image Annotation</i> toolbar on top of TMARKER to annotate the whole image.<br><br>After your image review, "
                + "you might save the annotations with <i>\"File -> Save As...\"</i> as CSV file.<br><br></li>"
                + "<li><b>Automatic TMA processing</b> with plugins: Choose a plugin from <i>\"Tools -> Plugins\"</i>. Standard operations are:"
                + "<ul>"
                + "<li><b>Color deconvolution for automatic nucleus detection</b>: Separate the stained / unstained color channels and detect the nuclei.<br><br></li>"
                + "<li><b>Intensity clustering for automatic staining refinement</b>: Detect 0 / 1+ / 2+ and 3+ nuclei.<br><br></li>"
                + "<li><b>Automatic cancer nucleus classification</b>: Classify cancerous and benign nuclei for staining estimation.<br><br></li>"
                + "</ul>"
                + "<li>You can always <b>save a XML file</b> containing the TMA spots and detected nuclei with <i>\"File -> Save As...\"</i> for future sessions.<br><br></li>"
                + "<li>You can always <b>save a HTML report</b> with <i>\"File -> Save As...\"</i>.</li>"
                + "</ul>"
                + "<b><i>TIP:</i></b> As most analyses are performed only on selected images, you might first want to select all TMA spots in the TMA List on the left.<br><br>"
                + "<b><i>TIP:</i></b> Hold the mouse over buttons and labels to show additional information.<br><br>"
                + "</font></html>";        
        setUserInfo(infoText);
    }
    
    /**
     * Sets the state after starting the program (e.g. user info).
     */
    public void setState_init() {
        jPanel3.removeAll();
        zip.setImage(null);
        zipl.setImage(null);
        if (tsd.isShowing()) {
            tsd.setVisible(false);
        }
        
        infoText = "<html><font size=\"3\" face=\"Arial\"><h1><strong>Welcome to TMARKER</strong></h1></strong>"
                + "TMARKER is a program for the analysis of tissue microarry (TMA) images. Example usages are <b>cell nucleus counting</b>, "
                + "<b>cell nucleus classification</b> or <b>staining estimation</b>.<br><br>"
                + "<a href=\"http://www.comp-path.inf.ethz.ch\"><b>Plugins</b></a> can add functionalities to TMARKER and can be accessed by <i>\"Tools -> Plugins\"</i><br><br>"
                + "There are three ways to get started:<br>"
                + "<ol style=\"margin:5; padding:10;\">"
                + "<li><b>Load TMA images into TMARKER</b> (drag'n'drop image files or open with <i>\"File -> Open...\"</i>).<br><br></li>"
                + "<li><b>Load XML image files</b>, created with TMARKER (drag'n'drop XML files or open with <i>\"File -> Open...\"</i>)."
                + "<br><br>XML files contain the image names as well as nuclei positions. The images itself must exist separately on the hard disk.<br><br></li>"
                + "<li><b>Load a CSV file</b> with a whole patient cohort dataset (drag'n'drop CSV file or open with <i>\"File -> Open...\"</i>).<br><br>"
                + "CSV files are semicolon separated text files with a header row followed by one sample per row. Each column is a measurement "
                + "(e.g. image filename, sex, age, spot-ID, survival status, protein measurements, etc). "
                + "One of the columns must host the path and filename of the images. The images itself must exist separately on the hard disk.<br></li>"
                + "</ol>"
                + "</font></html>";        
        setUserInfo(infoText);
        
        jTabbedPane1.grabFocus();
    }
    
    /**
     * Creates a new Thread to create all the thumbnail images of the TMAspots.
     */
    public void setThumbnailsThread() {
        if (thumbnailsThread==null || !thumbnailsThread.isAlive()) {
            thumbnailsThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try { 
                        for (int i=0;i<getTMAspots().size();i++) {
                            getTMAspots().get(i).getTLP().setThumbnailImage(null);
                        }
                    } catch (OutOfMemoryError e) {
                       logger.log(java.util.logging.Level.INFO, "Not enough memory for thumbnail creation.");
                    } catch (Exception e) {
                        if (tmarker.DEBUG>0) {
                             logger.log(Level.WARNING, e.getMessage(), e);
                        }
                    }
                }
            });
            thumbnailsThread.start();
        }
    }
    
    /**
     * Sets the zoom parameter of the visible TMAspot.
     * @param i The zoom parameter in percent.
     */
    public void setZoomSlider(int i) {
        jSlider1.setValue(i);
    }

    /**
     * Returns the current zoom of the TMAspot.
     * @return The current zoom factor (1 for 100%).
     */
    public double getZoom() {
        return jSlider1.getValue()/100.0;
    }
    
    /**
     * Returns whether positive TMALabels should be drawn.
     * @return True, if positive TMALabels should be drawn.
     */
    public boolean isShowingPosLabels() {
        return jToggleButton9.isSelected();
    }
    
    /**
     * Returns whether negative TMALabels should be drawn.
     * @return True, if negative TMALabels should be drawn.
     */
    public boolean isShowingNegLabels() {
        return jToggleButton10.isSelected();
    }
    
    /**
     * Returns whether unknown TMALabels should be drawn.
     * @return True, if unknown TMALabels should be drawn.
     */
    public boolean isShowingUnkLabels() {
        return jToggleButton11.isSelected();
    }
    
    /**
     * Returns whether or not backgorund labels should be shown.
     * @return True, if background labels should be shown (always true for TMARKER).
     */
     public boolean isShowingBGLabels() {
        return true;
    }
    
     /**
     * Returns whether the nucleus labels are visible.
     * @return True if the nuclei should be visible.
     */
    public boolean isShowingLabels() {
        return (((isShowingStainedLabels() || isShowingUnstainedLabels()) &&
                (isShowingGoldStandardLabels() || isShowingEstimatedLabels())));
    }
    
    /**
     * Whether or not advanced text should be drawn together with the TMALabels.
     * The text has to be specified in the TMALabel.
     * @return True if additional text should be drawn with the TMALabels.
     */
    public boolean isShowingTextLabels() {
        return false;
    }
    
    /**
     * Returns whether or not stained nuclei should be shown.
     * @return True, if manual stained nuclei should be shown.
     */
    public boolean isShowingStainedLabels() {
        return jToggleButton12.isSelected();
    }
    
    /**
     * Returns whether or not unstained nuclei should be shown.
     * @return True, if manual unstained nuclei should be shown.
     */
    public boolean isShowingUnstainedLabels() {
        return jToggleButton13.isSelected();
    }
    
    /**
     * Returns whether or not manual gold-standard nuclei should be shown.
     * @return True, if manual gold-standard nuclei should be shown.
     */
    public boolean isShowingGoldStandardLabels() {
        return jToggleButton14.isSelected();
    }
    
    /**
     * Returns whether or not computer-estimated nuclei should be shown.
     * @return True, if computer-estimated nuclei should be shown.
     */
    public boolean isShowingEstimatedLabels() {
        return jToggleButton15.isSelected();
    }
    
    /**
     * Returns whether or not the cell densitiy heatmap should be shown.
     * @return True, if the cell densitiy heatmap should be shown.
     */
    public boolean isShowingCellDensitySoft() {
        return jToggleButton2.isSelected();
    }
    
    /**
     * Returns the gold-standard labeler number which should be shown in the TMA View.
     * @return The gold-standard labeler number which should be shown in the TMA View (always -1 for TMARKER).
     */
    public byte getGSNumberForViewing() {
        return -1;
    }
    
    /**
     * Returns the gold-standard labeler number.
     * @return The gold-standard labeler number (always 1 for TMARKER).
     */
    public byte getGSNumberForLabeling() {
        return 1;
    }

    /**
     * Sets the temporary directory name (e.g. to write temp files).
     * @param tmp_dir The temp directory name.
     */
    public void setTmpDir(String tmp_dir) {
        this.tmp_dir = tmp_dir;
        File tmp = new File(tmp_dir);
        tmp.mkdir();
        tmp.deleteOnExit();
        this.tmp_dir += fs;
    }
    
    /**
     * Returns the temporary directory name (e.g. to write temp files).
     * @return The temp directory name.
     */
    public String getTmpDir() {
        return tmp_dir;
    }
    
    /**
     * Saves the TMAspots as XML. XML can store all parameters of the program, TMAspots and TMApoints.
     * @param t The current TMARKER session.
     * @param file The file to be saved.
     */
    public static void SaveAsXML(tmarker t, File file) {
        if (file == null) return;
        String text = t.getStatusMessageLabel().getText();
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("TMAspot", TMAspot.class);
        xstream.alias("TMApoint", TMApoint.class);
        xstream.omitField(TMAspot.class, "tc");
        xstream.omitField(TMAspot.class, "slic");
        xstream.omitField(TMAspot.class, "slics");
        xstream.omitField(TMAspot.class, "instances");
        xstream.omitField(TMAspot.class, "tlp");
        xstream.omitField(TMApoint.class, "sp");
        
        try {
            ObjectOutputStream out = xstream.createObjectOutputStream(new BufferedWriter(new FileWriter(file)));
            out.writeObject(t.getUID()); // write Session ID
            out.writeObject(t.getParameterValues()); // write properties
            List<TMAspot> tss = t.getTMAspots();
            for (int i=0; i<tss.size(); i++) {
                TMAspot ts = tss.get(i);
                t.setStatusMessageLabel("Write " + ts.getName() + " to XML ...");
                t.setProgressbar((int)((100.0*i)/tss.size()));
                out.writeObject(ts);
            }

            out.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(t, "An error occurred while writing "
                    + file.getName() + "\nMaybe it's still in use.", "Could not save file", JOptionPane.ERROR_MESSAGE);
            logger.log(java.util.logging.Level.WARNING, ex.getMessage());
        } finally {
            t.setStatusMessageLabel(text);
            t.setProgressbar(0); 
        }
    }
    
    /**
     * Saves the TMAspots and nuclei as text file. For experimental use only.
     * @param t The current TMARKER session.
     * @param file The file to be saved.
     */
    public static void SaveAsCSV(tmarker t, File file) {
        if (file == null) return;
        String text = t.getStatusMessageLabel().getText();
        try {  
            FileWriter writer = new FileWriter(file);
            List<TMAspot> tss = t.getTMAspots();
	    Set<String> headers = tmarker.getProperties(tss);
            String value;
            
            writer.append("Name"); writer.append(";");                
            //writer.append("pos"); writer.append(";");
            //writer.append("neg"); writer.append(";");
            //writer.append("staining estimation [%]"); writer.append(";");
            writer.append("nucleus_x"); writer.append(";");
            writer.append("nucleus_y"); writer.append(";");
            writer.append("nucleus_label"); writer.append(";");
            writer.append("nucleus_staining"); writer.append(";");
            writer.append("nucleus_manuallyDrawn"); writer.append(";");
            int n=0;
            for (String header: headers) {
                if (!header.equals("Name")) {
                    writer.append(header);
                    if (n<headers.size()-1) {
                        writer.append(";");
                    }    
                    n++;
                }
            }
            writer.append("\n");
 
            for (int i=0; i<tss.size(); i++) {
                TMAspot ts = tss.get(i);
                t.setStatusMessageLabel("Write " + ts.getName() + " to CSV ...");
                t.setProgressbar((int)((100.0*i)/tss.size()));
                
                //writer.append(ts.getName()); writer.append(";");                
                //writer.append(Integer.toString(ts.getNumPos_total())); writer.append(";");
                //writer.append(Integer.toString(ts.getNumNeg_total())); writer.append(";");
                //writer.append(Integer.toString(ts.getStainingEstimation())); writer.append(ts.getPoints().isEmpty() ? "\n" : ";");
                
                writer.append(ts.getName());writer.append(";");
                if (ts.getPoints().size()>0) {
                    TMApoint tp = ts.getPoints().get(0);
                    writer.append(Integer.toString(tp.x)); writer.append(";");
                    writer.append(Integer.toString(tp.y)); writer.append(";");
                    writer.append(tp.getLabel() == TMALabel.LABEL_UNK ? "unknown" : (tp.getLabel() == TMALabel.LABEL_POS ? "malignant" : (tp.getLabel() == TMALabel.LABEL_NEG ? "benign" : "background"))); writer.append(";");
                    writer.append(Byte.toString(tp.getStaining())); writer.append(";");
                    writer.append(Boolean.toString(tp.isGoldStandard())); writer.append(";");
                } else {
                    writer.append(";");
                    writer.append(";");
                    writer.append(";");
                    writer.append(";");
                    writer.append(";");
                }
                Properties props = ts.getProperties();
                n=0;
                for (String header : headers) {
                    if (!header.equals("Name")) {
                        if (props.containsKey(header)) {
                            value = props.getProperty(header);
                            writer.append(value);
                        }
                        if (n<headers.size()-1) {
                            writer.append(";");
                        }
                        n++;
                    }
                }
                writer.append("\n");
                
                for (int j=1; j<ts.getPoints().size(); j++) {
                    writer.append(";");
                    TMApoint tp = ts.getPoints().get(j);
                    writer.append(Integer.toString(tp.x)); writer.append(";");
                    writer.append(Integer.toString(tp.y)); writer.append(";");
                    writer.append(tp.getLabel() == TMALabel.LABEL_UNK ? "unknown" : (tp.getLabel() == TMALabel.LABEL_POS ? "malignant" : (tp.getLabel() == TMALabel.LABEL_NEG ? "benign" : "background"))); writer.append(";");
                    writer.append(Byte.toString(tp.getStaining())); writer.append(";");
                    writer.append(Boolean.toString(tp.isGoldStandard())); writer.append(";");
                    n=0;
                    for (String header : headers) {
                        if (!header.equals("Name")) {
                            if (n<headers.size()-1) {
                                writer.append(";");
                            }
                            n++;
                        }
                    }
                    writer.append("\n");
                }
            }
            writer.flush();
	    writer.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(t, "An error occurred while writing "
                    + file.getName() + "\nMaybe it's still in use.", "Could not save file", JOptionPane.ERROR_MESSAGE);
            logger.log(java.util.logging.Level.WARNING, e.getMessage());
        } finally {
            t.setStatusMessageLabel(text);
            t.setProgressbar(0); 
        }
    }
    
    /**
     * Save the selected TMAspots and nuclei.
     * @param t The current TMARKER session.
     * @param file The file to be saved.
     */
    public static void SaveAsCSV_Properties(tmarker t, File file) {
        if (file == null) return;
        String text = t.getStatusMessageLabel().getText();
        try {  
            List<TMAspot> tss = t.getTMAspots();
            Set<String> headers = tmarker.getProperties(tss);
            FileWriter writer = new FileWriter(file);
            String value;
            if (!headers.contains("Name")) {
                writer.append("Name" + ";");
            } 
            int n=0;
            for (String header: headers) {
                writer.append(header);
                if (n<headers.size()-1) {
                    writer.append(";");
                }    
                n++;
            }
            writer.append("\n");

            for (int i=0; i<tss.size(); i++) {
                TMAspot ts = tss.get(i);
                t.setStatusMessageLabel("Write " + ts.getName() + " to CSV ...");
                t.setProgressbar((int)((100.0*i)/tss.size()));

                if (!headers.contains("Name")) {
                    writer.append(ts.getName() + ";");
                }
                Properties props = ts.getProperties();
                n=0;
                for (String header : headers) {
                    if (props.containsKey(header)) {
                        value = props.getProperty(header);
                        writer.append(value);
                    }
                    if (n<headers.size()-1) {
                        writer.append(";");
                    }
                    n++;
                }
                writer.append("\n");
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(t, "An error occurred while writing "
                    + file.getName() + "\nMaybe it's still in use.", "Could not save file", JOptionPane.ERROR_MESSAGE);
            logger.log(java.util.logging.Level.WARNING, e.getMessage());
        } finally {
            t.setStatusMessageLabel(text);
            t.setProgressbar(0); 
        }
    }
    
    /**
     * Returns the list of all property names of the given TMAspots.
     * @param tss The TMAspots to be considered.
     * @return A union over all properties of all given TMAspots.
     */
    public static Set<String> getProperties(List<TMAspot> tss) {
        Set<String> props = new ArraySet<String>();
        for (TMAspot ts : tss) {
            props.addAll(ts.getProperties().stringPropertyNames());
        }
        return props;
    }
    
    /**
     * Calculates the consensus points in all selected TMAspots.
     * @param considerLabel If true, the label of a nuclei has to be unique among all labelers, 
     * or the TMApoint will be discarded. If false, the non-unique label will be transformed to TMAspot.LABEL_UNK.
     */
    public void performConsensusPointCalculation(boolean considerLabel) {
        List<TMAspot> tss = getSelectedTMAspots();
        for (TMAspot ts: tss) {
            ts.calculateConsensusPoints(considerLabel);
            updateTMATable(ts, false);
            if (getVisibleTMAspot().equals(ts)) {
                tvp.repaint();
            }
        }
    }
    
    public void performNonConsensusGSPointsDeletion() {
        List<TMAspot> tss = getSelectedTMAspots();
        for (TMAspot ts: tss) {
            ts.deleteNonConsensusGSPoints();
            updateTMATable(ts, false);
            if (getVisibleTMAspot().equals(ts)) {
                tvp.repaint();
            }
        }
    }

    /**
     * Saves a file in the current TMARKER session. The user can choose a file and
     * the program decides according to the extension what to save.
     * @param t The current TMARKER session.
     * @param currentDir The current directory.
     */
     private static void SaveFile(tmarker t, String currentDir) {
        File file;
        String ext = "tmarker";
        if (t.getVisibleTMAspot()!=null) {
            ext = Misc.FilePathStringtoFilenameWOExtension(t.getVisibleTMAspot().getName());
        } 
        file = FileChooser.chooseSavingFile(t, currentDir, ext);
        
        t.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (file != null) {
            if (Misc.FilePathStringtoExtension(file.getName()).equalsIgnoreCase("xml")) {
                SaveAsXML(t, file);
            } else if (Misc.FilePathStringtoExtension(file.getName()).equalsIgnoreCase("pdf")) {
                SaveAsPDF(t, file);
            } else if (Misc.FilePathStringtoExtension(file.getName()).equalsIgnoreCase("html")) {
                SaveAsHTML(t, file);
            } else if (Misc.FilePathStringtoExtension(file.getName()).equalsIgnoreCase("csv")) {
                SaveAsCSV(t, file);
            } t.setCurrentDir(file.getAbsolutePath());
        }
        t.setCursor(Cursor.getDefaultCursor());
    }

     /**
      * Returns the white balance (background correction) dialog.
      * @return The white balance (background correction) dialog.
      */
    public BgCorrectionDialog getBgCorrectionDialog() {
        return bcd;
    }
    
    /**
     * Returns the TMAspot summary window.
     * @return The TMAspot summary window.
     */
    public TMAspot_summary_Dialog getTSD() {
        return tsd;
    }
    
    /**
     * Adds a search path to the path. E.g. important for the search of image files of the TMAspots.
     * @param p The full path to be added.
     */
    void addPath(String p) {
        path.add(p);
    }
    
    /**
     * Returns the search path. E.g. important for the search of image files of the TMAspots.
     * @return The list of all full paths.
     */
    List<String> getPath() {
        return path;
    }

    /**
     * Defines what to do on initialization of TMARKER. E.g., scrollbars are adjusted, FileDrops are added, window sizes are adjustest.
     */
    private void initComponents2() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doExit();
            }
        });
        
        int processors = Runtime.getRuntime().availableProcessors();
        setStatusMessageLabel("Connected to " + Integer.toString(processors) + " local processor" + (processors != 1 ? "s..." : "..."));
        
        try {
            this.setIconImage(ImageIO.read((getClass().getResource("/tmarker/img/TMARKER32.png"))));
        } catch (Exception ex) {            
        }
        
        // Logo Panel on top left
        BufferedImage img;
        try {
            img = ImageIO.read((getClass().getResource("/tmarker/img/logo_290x120.png")));
        }   catch (Exception ex) {
            img = null;
        }
        LogoPanel lp = new LogoPanel(img);
        lp.setLayout(new java.awt.BorderLayout());
        lp.add(jPanel11, java.awt.BorderLayout.CENTER);
        lp.add(jPanel15, java.awt.BorderLayout.EAST);
        jScrollPane3.getViewport().setOpaque(false);
        jPanel1.remove(jPanel7);
        jPanel1.add(lp, java.awt.BorderLayout.PAGE_START);
        
        // jXStatusBar1
        jXStatusBar1.removeAll();
        JXStatusBar.Constraint c1 = new JXStatusBar.Constraint(
                JXStatusBar.Constraint.ResizeBehavior.FILL);
        jXStatusBar1.add(jLabel4, c1);     // Fill with no inserts - will use remaining space
        JXStatusBar.Constraint c2 = new JXStatusBar.Constraint();
        c2.setFixedWidth(150);
        jXStatusBar1.add(jProgressBar1, c2);  // Fixed width of 150 with no inserts

        getTMAViewContainer().getHorizontalScrollBar().setUnitIncrement(30);
        getTMAViewContainer().getVerticalScrollBar().setUnitIncrement(30);
        jScrollPane7.getHorizontalScrollBar().setUnitIncrement(10);
        jScrollPane7.getVerticalScrollBar().setUnitIncrement(10);
        jScrollPane4.getVerticalScrollBar().setUnitIncrement(10);
        jScrollPane4.getHorizontalScrollBar().setUnitIncrement(10);
        
        // TMA Table
        jPanel36.setComponentPopupMenu(jPopupMenu1);
        
        FileDrop fileDrop = new FileDrop(jPanel3, true, new FileDrop.Listener() {
            @Override
            public void  filesDropped(java.io.File[] files) {
                // handle file drop
                tmarker t = (tmarker) (jMenuBar1.getParent().getParent().getParent());
                tmarker.LoadFiles(t, files);
            }   // end filesDropped
            }); // end FileDrop.Listener
        FileDrop fileDrop1 = new FileDrop(jPanel10, true, new FileDrop.Listener() {
            @Override
            public void  filesDropped(java.io.File[] files) {
                // handle file drop
                tmarker t = (tmarker) (jMenuBar1.getParent().getParent().getParent());
                tmarker.LoadFiles(t, files);
            }   // end filesDropped
            }); // end FileDrop.Listener
        
        
        /*toolbar2 = new Toolbar2(this);
        getContentPane().add(toolbar2, java.awt.BorderLayout.PAGE_START);
        //jPanel4.add(toolbar2);
        
        toolbar1 = new Toolbar1(this);
        //getContentPane().add(toolbar1, java.awt.BorderLayout.PAGE_START);
        jPanel4.add(toolbar1, java.awt.BorderLayout.PAGE_START);
        
        toolbar3 = new Toolbar3(this);
        jPanel5.add(toolbar3, java.awt.BorderLayout.CENTER);
        */
        
        if (aboutBox == null) { aboutBox = new TMARKERAboutBox(this); }
        if (bcd == null) { bcd = new BgCorrectionDialog(this, false); }
        
        // Restore the programm parameters
        restoreParameterValues(false);
        
        // load plugins
        loadPlugins();
        
        // Restore the plugin parameters
        restoreParameterValues(true);
        
        setState_init();
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jSplitPane2.setDividerLocation(462);
                jSplitPane1.setDividerLocation((java.awt.Toolkit.getDefaultToolkit().getScreenSize().width-jSplitPane2.getDividerLocation()-300.0)/(java.awt.Toolkit.getDefaultToolkit().getScreenSize().width-jSplitPane2.getDividerLocation()));
            }
        });
        
        logger.log(java.util.logging.Level.INFO, "Current Working Directory: {0}", getCurrentDir());
    }
    
    /**
     * Performs the manual white balance on all selected TMAspots.
     * @param x The x-coord of the depicted background location.
     * @param y The y-coord of the depicted background location.
     */
    public void performBGCorrection(int x, int y) {
        List<TMAspot> tss = getSelectedTMAspots();
        boolean doCorrection;
        if (tss.size() > 1 && !getBgCorrectionDialog().getUseColor()) {
            int c = JOptionPane.showConfirmDialog(this, "Are you sure to correct all selected images according to the selected pixel?\n"
                    + "All selected images in the TMA List should have background on this position.\n\n"
                    + "Hint: If the images differ too much to each other, you can also do the \n"
                    + "background correction for the single images individually.", "Confirm Background Correction", JOptionPane.YES_NO_OPTION);
            doCorrection = c == JOptionPane.YES_OPTION;
        } else {
            doCorrection = true;
        }
        if (doCorrection) {
            if (getBgCorrectionDialog().getUseColor()) {
                int col = ((BufferedImage)tvp.getImage()).getRGB(x,y);
                for (TMAspot ts: tss) {
                    ts.doBgCorrection(col);
                }
            } else {
                for (TMAspot ts: tss) {
                    ts.doBgCorrection(x,y);
                }
            }
            tvp.update(tvp.getGraphics());
        }
        
        getBgCorrectionDialog().setVisible(false);
        getBgCorrectionDialog().dispose();
    }
   
    /**
     * Performs the automatic white balance on all selected TMAspots.
     * @param asParallelThread If true, the procedure is performed on a parallel thread such that TMARKER can be used in the meanwhile.
     */
    public void performBGCorrectionAutomatic(boolean asParallelThread) {
        List<TMAspot> tss = getSelectedTMAspots();
        if (asParallelThread) {
            BgCorrectionThread bgt = new BgCorrectionThread(tss);
            bgt.start(); 
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (TMAspot ts:tss) {
                ts.doBgCorrectionAutomatic();
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        getBgCorrectionDialog().setVisible(false);
        getBgCorrectionDialog().dispose();
    }
    
    /**
     * Returns the random seed used by TMARKER.
     */
    public void resetRandomSeed() {
        getRandom().setSeed(1);
    }

    /**
     * Add a CSV table to the current study. The CSV file must contain the image
     * names in one column and can contain more information in other columns.
     * One row is one sample (TMA), one column is one information (e.g. protein expression).
     */
    private void addMetainformation() {
        File filelist = FileChooser.chooseCSVFile(this, getCurrentDir());
        if (filelist != null && filelist.exists()) {
            LoadCSV(this, filelist);
            TMAspot ts = getVisibleTMAspot();
            if (ts!=null) {
                getTSD().updatePropertiesTable(ts);
            }
        }
    }

    /**
     * Returns the number of nuclei on all given TMAspots.
     * @param tss The TMAspots to be considered.
     * @return The number of all TMApoints on these TMAspots.
     */
    public static int getNumberNuclei(List<TMAspot> tss) {
        int n=0; 
        for (TMAspot ts: tss) {
            n += ts.getPoints().size();
        }
        return n;
    }

    /**
     * Returns the uuid of this tmarker session.
     * @return The uuid of this tmarker session.
     */
    public UUID getUID() {
        return UID;
    }

    /** 
     * Inner class to add a header and a footer to a PDF file.
     */
    static class HeaderFooter extends PdfPageEventHelper {

        @Override
        public void onEndPage (PdfWriter writer, Document document) {
            Rectangle rect = writer.getBoxSize("art");
            PdfContentByte cb = writer.getDirectContent();
            cb.setColorFill(BaseColor.BLUE );
            
            
            Image logo;
            try {
                logo = Image.getInstance(getClass().getResource("/tmarker/img/logo_290x120.png"));
                switch(writer.getPageNumber() % 2) {
                case 0:
                    writer.getDirectContent().addImage(logo, 0.4f*logo.getWidth(), 0, 0, 0.4f*logo.getHeight(), 36, rect.getTop()-10);
                    ColumnText.showTextAligned(writer.getDirectContent(),
                            Element.ALIGN_RIGHT, new Phrase("TMARKER Report"),
                            rect.getRight(), rect.getTop(), 0);
                    break;
                case 1:
                    writer.getDirectContent().addImage(logo, 0.4f*logo.getWidth(), 0, 0, 0.4f*logo.getHeight(), rect.getRight()-0.4f*logo.getWidth()-18, rect.getTop()-10);
                    ColumnText.showTextAligned(writer.getDirectContent(),
                            Element.ALIGN_LEFT, new Phrase("TMARKER Report"),
                            rect.getLeft(), rect.getTop(), 0);
                    break;
                }
            } catch (IOException | DocumentException ex) {            
                switch(writer.getPageNumber() % 2) {
                case 0:
                    ColumnText.showTextAligned(writer.getDirectContent(),
                            Element.ALIGN_LEFT, new Phrase("TMARKER Report"),
                            rect.getRight(), rect.getTop(), 0);
                    break;
                case 1:
                      ColumnText.showTextAligned(writer.getDirectContent(),
                            Element.ALIGN_LEFT, new Phrase("TMARKER Report"),
                            rect.getRight(), rect.getTop(), 0);
                    break;
                }
            } finally {
                ColumnText.showTextAligned(writer.getDirectContent(),
                        Element.ALIGN_RIGHT, new Phrase(String.format("Page %d", writer.getPageNumber())),
                        (rect.getLeft() + rect.getRight()) / 2, rect.getBottom() - 18, 0);
                cb.setColorFill(BaseColor.BLUE.darker());
                cb.setLineWidth(0f);
                cb.moveTo(36, 773);
                cb.lineTo(559, 773);
                cb.moveTo(36, 69);
                cb.lineTo(559, 69);
                cb.setColorFill(BaseColor.BLACK);
                cb.stroke();
                
            }
        }
    }
    
    /**
     * Creats a new PDF report of the current analysis and saves it on hard disk.
     * @param t The TMARKER session.
     * @param file The file to write.
     */
     public static void SaveAsPDF(tmarker t, File file) {
        if (file == null) return;
        String text = t.getStatusMessageLabel().getText();
        try {  
            // creation of the document with a certain size and certain margins
            // may want to use PageSize.LETTER instead
            Document document = new Document(PageSize.A4, 50, 50, 100, 100);
            document.addAuthor("TMARKER Report");
            document.addCreationDate();
            document.addTitle("TMARKER Report");
                    
            // creation of the different writers
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            writer.setBoxSize("art", new Rectangle(36, 54, 559, 788));

            HeaderFooter event = new HeaderFooter();
            writer.setPageEvent(event);

            // various fonts
            BaseFont bf_helv = BaseFont.createFont(BaseFont.HELVETICA, "Cp1252", false);
            BaseFont bf_times = BaseFont.createFont(BaseFont.TIMES_ROMAN, "Cp1252", false);
            BaseFont bf_courier = BaseFont.createFont(BaseFont.COURIER, "Cp1252", false);
            BaseFont bf_symbol = BaseFont.createFont(BaseFont.SYMBOL, "Cp1252", false);

            document.open();

            //cb.beginText();
            //cb.setFontAndSize(bf_helv, 12);
            //String text = "Following tissue images have been analysed:";
            //cb.showTextAligned(PdfContentByte.ALIGN_CENTER, text, writer.getPageSize().getWidth()/2, 700, 0);
            //cb.endText();

            
            //document.newPage();

            // add text in two paragraphs from top to bottom
            Paragraph par = new Paragraph("Following images have been analysed:");
            par.setAlignment(Paragraph.ALIGN_CENTER);
            par.getFont().setStyle(Font.BOLD);
            document.add(par);
            
            List<TMAspot> tss = t.getTMAspots();
            for (int i=0; i<tss.size(); i++) {
                TMAspot ts = tss.get(i);
                t.setStatusMessageLabel("Write " + ts.getName() + " to PDF ...");
                t.setProgressbar((int)((100.0*i)/tss.size()));
                
                // Collect numbers
                int num_ustai_est = ts.getNumPoints(false, TMALabel.STAINING_0, TMALabel.LABEL_UNK);
                int num_ustai_gst = ts.getNumPoints(true, TMALabel.STAINING_0, TMALabel.LABEL_UNK);
                int num_stai1_est = ts.getNumPoints(false, TMALabel.STAINING_1, TMALabel.LABEL_UNK);
                int num_stai1_gst = ts.getNumPoints(true, TMALabel.STAINING_1, TMALabel.LABEL_UNK);
                int num_stai2_est = ts.getNumPoints(false, TMALabel.STAINING_2, TMALabel.LABEL_UNK);
                int num_stai2_gst = ts.getNumPoints(true, TMALabel.STAINING_2, TMALabel.LABEL_UNK);
                int num_stai3_est = ts.getNumPoints(false, TMALabel.STAINING_3, TMALabel.LABEL_UNK);
                int num_stai3_gst = ts.getNumPoints(true, TMALabel.STAINING_3, TMALabel.LABEL_UNK);
                int num_tot_est = num_ustai_est + num_stai1_est + num_stai2_est + num_stai3_est;
                int num_tot_gst = num_ustai_gst + num_stai1_gst + num_stai2_gst + num_stai3_gst;
                
                // Add TMA Tables
                PdfPTable table = new PdfPTable(new float[] {12, 12, 12, 12, 12, 40});
                table.setSpacingBefore(20);
                table.setSpacingAfter(20);
                table.getDefaultCell().setPadding(0);
                table.getDefaultCell().setHorizontalAlignment(Paragraph.ALIGN_RIGHT);

                Phrase p = new Phrase(ts.getName());
                p.getFont().setStyle("boldunderline");
                PdfPCell cell = new PdfPCell(p);
                cell.setExtraParagraphSpace(20);
                cell.setColspan(5);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                BufferedImage bi_orig = ts.getBufferedImage(false);
                BufferedImage I = new BufferedImage(bi_orig.getWidth(), bi_orig.getHeight(), BufferedImage.TYPE_INT_RGB);
                I.getGraphics().drawImage(bi_orig, 0, 0, null);
                
                for (Pluggable plugin: t.getPlugins()) {
                    plugin.drawInformationPreNuclei(ts, I.getGraphics(), 1, 0, 0, ts.getWidth(), ts.getHeight());
                }
                TMA_view_panel.drawCellCounts(ts, I.getGraphics(), 1, 0, 0, ts.getWidth(), ts.getHeight());
                TMA_view_panel.drawAreas(ts, I.getGraphics(), 1, 0, 0, ts.getWidth(), ts.getHeight());
                for (Pluggable plugin: t.getPlugins()) {
                    plugin.drawInformationPostNuclei(ts, I.getGraphics(), 1, 0, 0, ts.getWidth(), ts.getHeight());
                }
                java.awt.Image img = Toolkit.getDefaultToolkit().createImage(I.getSource());
                cell = new PdfPCell(Image.getInstance(img, null), true);
                cell.setHorizontalAlignment(Paragraph.ALIGN_RIGHT);
                cell.setVerticalAlignment(Paragraph.ALIGN_MIDDLE);
                cell.setRowspan(8);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("PC");
                cell = new PdfPCell(p);
                cell.setExtraParagraphSpace(5);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("Human");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("Clear");
                cell = new PdfPCell(p);
                cell.setExtraParagraphSpace(5);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_ustai_est));
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_0).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_ustai_gst));
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_0).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("1+");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_stai1_est));
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_1).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString((int) Math.round(100.0 * num_stai1_est / num_tot_est)) + " %"); // in percent
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_1).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_stai1_gst));
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_1).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString((int) Math.round(100.0 * num_stai1_gst / num_tot_gst)) + " %"); // in percent
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_1).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("2+");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_stai2_est));
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_2).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString((int) Math.round(100.0 * num_stai2_est / num_tot_est)) + " %"); // in percent
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_2).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_stai2_gst));
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_2).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString((int) Math.round(100.0 * num_stai2_gst / num_tot_gst)) + " %"); // in percent
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_2).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("3+");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_stai3_est));
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_3).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString((int) Math.round(100.0 * num_stai3_est / num_tot_est)) + " %"); // in percent
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_3).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_stai3_gst));
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_3).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString((int) Math.round(100.0 * num_stai3_gst / num_tot_est)) + " %"); // in percent
                p.getFont().setColor(new BaseColor(ts.getCenter().getLabelsColorWOAlpha(TMALabel.LABEL_UNK, TMALabel.STAINING_3).getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("Sum");
                cell = new PdfPCell(p);
                cell.setExtraParagraphSpace(5);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_stai1_est+num_stai2_est+num_stai3_est));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString((int) Math.round(100.0 * (num_stai1_est+num_stai2_est+num_stai3_est) / num_tot_est)) + " %"); // in percent
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_stai1_gst+num_stai2_gst+num_stai3_gst));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString((int) Math.round(100.0 * (num_stai1_gst+num_stai2_gst+num_stai3_gst) / num_tot_gst)) + " %"); // in percent
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("Total");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_tot_est));
                p.getFont().setColor(new BaseColor(Color.BLACK.getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase(Integer.toString(num_tot_gst));
                p.getFont().setColor(new BaseColor(Color.BLACK.getRGB()));
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                p = new Phrase("");
                cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                
                document.add(table);
                
                if ((i+1)%3==0) {
                    document.newPage();
                }                
            }
            
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            par = new Paragraph("Generated with TMARKER - " + dateFormat.format(cal.getTime()));
            par.add(new Phrase("Session ID: " + UID.toString()));
            par.add(new Phrase("(c) Peter J. Schüffler 2014"));
            par.setAlignment(Paragraph.ALIGN_LEFT);
            par.getFont().setStyle(Font.ITALIC);
            document.add(par);
            
            
            // add text at an absolute position
            /*cb.beginText();
            cb.setFontAndSize(bf_times, 14);
            cb.setTextMatrix(100, 300);
            cb.showText("Text at position 100, 300.");
            cb.endText();

            rotated text at an absolute position
            PdfTemplate template = cb.createTemplate(300, 300);                
            template.beginText();
            template.setFontAndSize(bf_times, 14);
            template.showText("Rotated text at position 400, 200.");
            template.endText();

            float rotate = 90;
            float x = 400;
            float y = 200;
            float angle  = (float) (-rotate * (Math.PI / 180));
            float xScale = (float) Math.cos(angle);
            float yScale = (float) Math.cos(angle);
            float xRot   = (float) -Math.sin(angle);
            float yRot   = (float) Math.sin(angle);

            cb.addTemplate(template, xScale, xRot, yRot, yScale, x, y);
             
             */
            document.close();
            writer.close(); 
        } catch (Exception e) {
            JOptionPane.showMessageDialog(t, "An error occurred while writing "
                    + file.getName() + "\nMaybe it's still in use.", "Could not save file", JOptionPane.ERROR_MESSAGE);
            logger.log(java.util.logging.Level.WARNING, e.getMessage());
        } finally {
            t.setStatusMessageLabel(text);
            t.setProgressbar(0); 
        }
    }
     
     /**
     * Creats a new HTML report of the current analysis and saves it on hard disk.
     * A new folder of the same name as file is created to store the image files.
     * @param t The TMARKER session.
     * @param file The file to write.
     */
     public static void SaveAsHTML(tmarker t, File file) {
        if (file == null) return;
        String text = t.getStatusMessageLabel().getText();
        
        try {
            String htmFileName = file.getName();
            String path = file.toURI().getPath().substring(0,file.toURI().getPath().length()-htmFileName.length());
            String folderName = file.getName() + "_files" + File.separator;

            // Create new directory for image files
            File dirfile = new File(path+folderName);
            if (!dirfile.exists()) {
                dirfile.mkdir();
            }

            // Save the header image
            File file_tmp;
            BufferedImage bi;
            file_tmp = new File(path + folderName + "_logo.png");
            try {
                bi = ImageIO.read(t.getClass().getResource("/tmarker/img/logo_290x120.png"));
                ImageIO.write(bi, "png", file_tmp);
            } catch (Exception e) {
                Logger.getLogger(tmarker.class.getName()).log(Level.WARNING, e.getMessage(), e);
            }
            
            List<TMAspot> tss = t.getTMAspots();
            boolean anyHasHumanAnnotation = false; // To determine the size of the rows in the TMAspot table.
            for (TMAspot ts : tss) {
                anyHasHumanAnnotation |= !ts.getPoints_GoldStandard().isEmpty();
            }
            
            // Create HMTL File Header
            char linebreak = '\n';
            String outputText = "<html>" + linebreak + "<head>" + linebreak +
                    "<title>TMARKER Analysis</title>" + linebreak;
            
            // Write jquery JavaScript
            outputText += "<!-- jQuery: required (tablesorter works with jQuery 1.2.3+) -->" + linebreak + "<script type=\"text/javascript\">" + linebreak;
            InputStream fis = t.getClass().getResourceAsStream("/tmarker/misc/jquery-1.11.2.min.js");
            byte[] data = new byte[1024];
            int len;
            while ((len = fis.read(data)) > 0) { 
                outputText += new String(data, "UTF-8");
            }
            fis.close();
            outputText += linebreak;
            outputText += "</script>" + linebreak + linebreak;
            
            // Write sorttable JavaScript
            outputText += "<script type=\"text/javascript\">" + linebreak;
            fis = t.getClass().getResourceAsStream("/tmarker/misc/jquery.tablesorter.min.js");
            while ((len = fis.read(data)) > 0) { 
                outputText += new String(data, "UTF-8");
            }
            fis.close();
            outputText += linebreak;
            outputText += "</script>" + linebreak + linebreak;
            
            outputText += "<script>" + linebreak +
                "	$(function(){" + linebreak +
                (anyHasHumanAnnotation?"		$('#table1 thead th').data(\"sorter\", false);" + linebreak:"") + 
                "		$('#table1').tablesorter({" + linebreak +
                "			widgets        : ['zebra', 'columns']," + linebreak +
                "			usNumberFormat : false," + linebreak +
                "			sortReset      : true," + linebreak +
                "			sortRestart    : true" + linebreak +
                "		});" + linebreak +
                "	});" + linebreak +
                "</script>" + linebreak + linebreak;
            
            // add javascript to save the overview table
            outputText += "<script type=\"text/javascript\">\n" +
                "	<!--\n" +
                "	\n" +
                "	// For IE, handle the table over an iFrame. Write them to the hidden iFrame and\n" +
                "	// then save the content of the iFrame.\n" +
                "	var getCsvFileForIE = function(csvData, filename) {\n" +
                "    	if ( ! supportsDataUri() ) {\n" +
                "               var iframe = document.getElementById('csvDownloadFrame');\n" +
                "   		  iframe = iframe.contentWindow || iframe.contentDocument;\n" +
                "   		  csvData = csvData.replace(/%20/g, \" \");\n" +
                "    		iframe.document.open(\"text/html\", \"replace\");\n" +
                "    		iframe.document.write(csvData);\n" +
                "    		iframe.document.close();\n" +
                "    		iframe.focus();\n" +
                "    		iframe.document.execCommand('SaveAs', true, filename);\n" +
                "  		} else {\n" +
                "  		  if (console && console.log) {\n" +
                "  		    console.log('Trying to call getCsvFileForIE with non IE browser.');\n" +
                "   		  }\n" +
                "  		}\n" +
                "		}\n" +
                " \n" +
                "    // Test if the browser is a IE browser (FALSE) or no IE browser (TRUE).\n" +
                "		var supportsDataUri = function() {\n" +
                "  		var isOldIE = navigator.appName === \"Microsoft Internet Explorer\";\n" +
                "  		var isIE11 = !!navigator.userAgent.match(/Trident\\/7\\./);\n" +
                "  		return ! (isOldIE || isIE11);  //Return true if not any IE\n" +
                "		}\n" +
                "		\n" +
                "		function SaveAsXLS(filename) {\n" +
                "		  //getting values of current time for generating the file name\n" +
                "      var dt = new Date();\n" +
                "      var day = dt.getDate();\n" +
                "      var month = dt.getMonth() + 1;\n" +
                "      var year = dt.getFullYear();\n" +
                "      var hour = dt.getHours();\n" +
                "      var mins = dt.getMinutes();\n" +
                "      var postfix = day + \".\" + month + \".\" + year + \"_\" + hour + \".\" + mins;\n" +
                "      \n" +
                "      // getting the table data\n" +
                "      var data_type = 'data:application/vnd.ms-excel';\n" +
                "      var table_div = document.getElementById('dvData');\n" +
                "      var table_html = table_div.outerHTML.replace(/ /g, '%20');\n" +
                "      \n" +
                "      // creating a link which will open the save as dialog\n" +
                "      var a = document.createElement('a');\n" +
                "      \n" +
                "      // IE does not support Data URI\n" +
                " 		  if ( ! supportsDataUri()  ) {\n" +
                " 		 	  a.href=\"#\";\n" +
                " 		 	  a.data_csv = \"'\" + table_html + \"'\";\n" +
                " 			  a.onclick = getCsvFileForIE(table_html, filename + '_' + postfix + '.xls');\n" +
                " 		  // All other support Data URI, gernerate Data URI\n" +
                " 		  } else {\n" +
                " 	   	  a.href = data_type + ', ' + table_html;\n" +
                "        a.download = filename + '_' + postfix + '.xls';\n" +
                "      }\n" +
                "      // execute the save as dialogue.\n" +
                " 	    a.click();\n" +
                "	  }\n" +
                "  //-->\n" +
                "</script>";
            
            // Write table style CSS
            outputText += "<style type=\"text/css\">" + linebreak;
            fis = t.getClass().getResourceAsStream("/tmarker/misc/theme.default.css");
            while ((len = fis.read(data)) > 0) { 
                outputText += new String(data, "UTF-8");
            }
            fis.close();
            outputText += linebreak;
            outputText += "</style>" + linebreak + linebreak;
            
            outputText += "</head>" + linebreak + "<body>" + linebreak;
            outputText += "<iframe id=\"csvDownloadFrame\" style=\"display:none\"></iframe>" + linebreak;
            outputText += "<table width=\"100%\">" + linebreak
                    + " <tr>" + linebreak
                    + "  <td><h1>TMARKER Report</h1></td>" + linebreak
                    + "  <td align=\"right\"><a href=\"http://comp-path.inf.ethz.ch\"><img alt=\"TMARKER Report\" src = \"" + folderName + "_logo.png\"></a><br><br><br></td>" + linebreak
                    + " </tr> "
                    + "</table>";
            
            // citation hint
            outputText += "Please enjoy <a href=\"http://www.jpathinformatics.org/article.asp?issn=2153-3539;year=2013;volume=4;issue=2;spage=2;epage=2;aulast=Sch%FCffler\">TMARKER and cite it</a> as you use it!<br><br>" + linebreak
                    + "Peter J. Sch&uuml;ffler, Thomas J. Fuchs, Cheng Soon Ong, Peter J. Wild, Niels J. Rupp, Joachim M. Buhmann:<br>" + linebreak
                    + "<b>TMARKER: A free software toolkit for histopathological cell counting and staining estimation.</b><br>" + linebreak
                    + "<i>J Pathol Inform 2013, 4:2, doi: <a href=\"http://dx.doi.org/10.4103/2153-3539.109804\">10.4103/2153-3539.109804</a></i><br><br>";
            

            // Programm Parameters
            outputText += "<br><h2>Program parameters</h2>" + linebreak
                    + "<table cellpadding=\"10\">" + linebreak
                    
                    + " <tr valign=\"top\">" + linebreak
                    + "  <td><b>Number TMA Spots</b></td>" + linebreak
                    + "  <td>" + t.getTMAspots().size() + "</td>" + linebreak
                    + " </tr>" + linebreak
                    
                    + " <tr valign=\"top\">" + linebreak
                    + "  <td><b>Nucleus Radius</b></td>" + linebreak
                    + "  <td>" + Integer.toString(t.getLabelRadius()) + "</td>" + linebreak
                    + " </tr>" + linebreak;
                    
                    if (!t.plugins.isEmpty()) {
                        outputText += " <tr valign=\"top\">" + linebreak
                        + "  <td><b>Plugins</b></td>" + linebreak
                        + "  <td>" + linebreak;
                        for (Pluggable p: t.plugins) {
                            try {
                                outputText += "<a href=\"#" + p.getPluginName().replaceAll(" ", "") + "\">" + p.getPluginName() + "</a><br>" + linebreak;
                            } catch (Exception e) {
                                
                            }
                        }
                        outputText += "  </td>" + linebreak + " </tr>" + linebreak;
                    }
                    outputText += "</table><br>" + linebreak + linebreak;
            
            
            //// Write the TMAspots to HTML
            outputText += "<hr><h2>TMA spot summary</h2>" + linebreak + linebreak;
            
            outputText += "<i>Shown are all TMA spots which have been processed by TMARKER, with their corresponding nuclei counts. <br>" + linebreak +
                    "There are different colors for clear (unstained) nuclei and stained nuclei, as well as for benign, malignant or unknown nuclei. <br> " + linebreak +
                    "The colors are defined in the TMARKER program. <br><br>" + linebreak +
                    
                    "TMARKER can cluster the nuclei by their intensity, and up to three staining intensities (1+, 2+ and 3+) are shown.<br><br>" + linebreak +
                    
                    (anyHasHumanAnnotation?"<b>PC</b> means detected by TMARKER. <b>Human</b> means detected by the user. <b>PC+Human</b> sums up both numbers.<br>":"") + linebreak +
                    "Staining percentages refer to the total number of nuclei, respectively." + 
                    "<br>Images can be enlarged by mouse click.</i><br><br>" + linebreak + linebreak;
            
            outputText += "<div id=\"dvData\">" + linebreak + "<table id=\"table1\" class=\"sortable\">" + linebreak + "<tr>" + linebreak
                    + "<thead>" + linebreak
                    + "<th class=\"topteft\" colspan=\"" + (anyHasHumanAnnotation?3:2) + "\"></th>" + linebreak
                    + "<th class=\"firstColPerDom\" colspan=\"6\">Benign</th>" + linebreak
                    + "<th class=\"firstColPerDom\" colspan=\"7\">Malignant</th>" + linebreak
                    + "<th class=\"firstColPerDom\" colspan=\"6\">Unknown</th>" + linebreak
                    + "<th class=\"firstColPerDom\" colspan=\"7\">Total</th>" + linebreak
                  + "</tr>" + linebreak
                  + "<tr>" + linebreak
                    + "<th>Image</th>" + linebreak
                    + "<th>Name</th>" + linebreak
                    + (anyHasHumanAnnotation?"<th></th>" + linebreak:"")
                    + "<th class=\"firstColPerDom\">Total</th>" + linebreak
                    + "<th>Clear</th>" + linebreak
                    + "<th>Stained</th>" + linebreak
                    + "<th class=\"num123_tot\">1+</th>" + linebreak
                    + "<th class=\"num123_tot\">2+</th>" + linebreak
                    + "<th class=\"num123_tot\">3+</th>" + linebreak
                    + "<th class=\"firstColPerDom\">Total</th>" + linebreak
                    + "<th>Clear</th>" + linebreak
                    + "<th>Stained</th>" + linebreak
                    + "<th class=\"perc\">[%]</th>" + linebreak
                    + "<th class=\"num123_tot\">1+</th>" + linebreak
                    + "<th class=\"num123_tot\">2+</th>" + linebreak
                    + "<th class=\"num123_tot\">3+</th>" + linebreak
                    + "<th class=\"firstColPerDom\">Total</th>" + linebreak
                    + "<th>Clear</th>" + linebreak
                    + "<th>Stained</th>" + linebreak
                    + "<th class=\"num123_tot\">1+</th>" + linebreak
                    + "<th class=\"num123_tot\">2+</th>" + linebreak
                    + "<th class=\"num123_tot\">3+</th>" + linebreak
                    + "<th class=\"firstColPerDom\">Total</th>" + linebreak
                    + "<th>Clear</th>" + linebreak
                    + "<th>Stained</th>" + linebreak
                    + "<th class=\"perc\">[%]</th>" + linebreak
                    + "<th class=\"num123_tot\">1+</th>" + linebreak
                    + "<th class=\"num123_tot\">2+</th>" + linebreak
                    + "<th class=\"num123_tot\">3+</th>" + linebreak
                  + "</tr>" + linebreak
                  + "</thead>" + linebreak + linebreak
                  + "<tbody>" + linebreak;
            
            for (int i=0; i<tss.size(); i++) {
                TMAspot ts = tss.get(i);
                t.setStatusMessageLabel("Write " + ts.getName() + " to HTML ...");
                t.setProgressbar((int)((100.0*i)/tss.size()));
                
                // Save the original image
                file_tmp = new File(path + folderName + ts.getName() + ".jpg");
                BufferedImage bi_orig = ts.getBufferedImage(true);
                bi = new BufferedImage(bi_orig.getWidth(), bi_orig.getHeight(), BufferedImage.TYPE_INT_RGB);
                bi.getGraphics().drawImage(bi_orig, 0, 0, null);
                ImageIO.write(bi, "jpg", file_tmp);

                // Save the processed image
                if (!ts.getPoints().isEmpty() || !ts.getExcludingAreas().isEmpty() || !ts.getIncludingAreas().isEmpty()) {
                    file_tmp = new File(path + folderName + ts.getName() + "_processed.jpg");
                    TMA_view_panel.drawCellCounts(ts, bi.getGraphics(), 1, 0, 0, ts.getWidth(), ts.getHeight());
                    TMA_view_panel.drawAreas(ts, bi.getGraphics(), 1, 0, 0, ts.getWidth(), ts.getHeight());
                    ImageIO.write(bi, "jpg", file_tmp);
                }
                
                // Collect numbers
                //NEG
                int num_neg_ustai_est = ts.getNumPoints(false, TMALabel.STAINING_0, TMALabel.LABEL_NEG);
                int num_neg_ustai_gst = ts.getNumPoints(true, TMALabel.STAINING_0, TMALabel.LABEL_NEG);
                int num_neg_ustai_tot = num_neg_ustai_est + num_neg_ustai_gst;
                int num_neg_stai1_est = ts.getNumPoints(false, TMALabel.STAINING_1, TMALabel.LABEL_NEG);
                int num_neg_stai1_gst = ts.getNumPoints(true, TMALabel.STAINING_1, TMALabel.LABEL_NEG);
                int num_neg_stai1_tot = num_neg_stai1_est + num_neg_stai1_gst;
                int num_neg_stai2_est = ts.getNumPoints(false, TMALabel.STAINING_2, TMALabel.LABEL_NEG);
                int num_neg_stai2_gst = ts.getNumPoints(true, TMALabel.STAINING_2, TMALabel.LABEL_NEG);
                int num_neg_stai2_tot = num_neg_stai2_est + num_neg_stai2_gst;
                int num_neg_stai3_est = ts.getNumPoints(false, TMALabel.STAINING_3, TMALabel.LABEL_NEG);
                int num_neg_stai3_gst = ts.getNumPoints(true, TMALabel.STAINING_3, TMALabel.LABEL_NEG);
                int num_neg_stai3_tot = num_neg_stai3_est + num_neg_stai3_gst;
                int num_neg_stai_est = num_neg_stai1_est + num_neg_stai2_est + num_neg_stai3_est;
                int num_neg_stai_gst = num_neg_stai1_gst + num_neg_stai2_gst + num_neg_stai3_gst;
                int num_neg_stai_tot = num_neg_stai_est + num_neg_stai_gst;
                int num_neg_tot_est = num_neg_ustai_est + num_neg_stai_est;
                int num_neg_tot_gst = num_neg_ustai_gst + num_neg_stai_gst;
                int num_neg_tot_tot = num_neg_tot_est + num_neg_tot_gst;
                
                //POS
                int num_pos_ustai_est = ts.getNumPoints(false, TMALabel.STAINING_0, TMALabel.LABEL_POS);
                int num_pos_ustai_gst = ts.getNumPoints(true, TMALabel.STAINING_0, TMALabel.LABEL_POS);
                int num_pos_ustai_tot = num_pos_ustai_est + num_pos_ustai_gst;
                int num_pos_stai1_est = ts.getNumPoints(false, TMALabel.STAINING_1, TMALabel.LABEL_POS);
                int num_pos_stai1_gst = ts.getNumPoints(true, TMALabel.STAINING_1, TMALabel.LABEL_POS);
                int num_pos_stai1_tot = num_pos_stai1_est + num_pos_stai1_gst;
                int num_pos_stai2_est = ts.getNumPoints(false, TMALabel.STAINING_2, TMALabel.LABEL_POS);
                int num_pos_stai2_gst = ts.getNumPoints(true, TMALabel.STAINING_2, TMALabel.LABEL_POS);
                int num_pos_stai2_tot = num_pos_stai2_est + num_pos_stai2_gst;
                int num_pos_stai3_est = ts.getNumPoints(false, TMALabel.STAINING_3, TMALabel.LABEL_POS);
                int num_pos_stai3_gst = ts.getNumPoints(true, TMALabel.STAINING_3, TMALabel.LABEL_POS);
                int num_pos_stai3_tot = num_pos_stai3_est + num_pos_stai3_gst;
                int num_pos_stai_est = num_pos_stai1_est + num_pos_stai2_est + num_pos_stai3_est;
                int num_pos_stai_gst = num_pos_stai1_gst + num_pos_stai2_gst + num_pos_stai3_gst;
                int num_pos_stai_tot = num_pos_stai_est + num_pos_stai_gst;
                int num_pos_tot_est = num_pos_ustai_est + num_pos_stai_est;
                int num_pos_tot_gst = num_pos_ustai_gst + num_pos_stai_gst;
                int num_pos_tot_tot = num_pos_tot_est + num_pos_tot_gst;
                
                //UNK
                int num_unk_ustai_est = ts.getNumPoints(false, TMALabel.STAINING_0, TMALabel.LABEL_UNK);
                int num_unk_ustai_gst = ts.getNumPoints(true, TMALabel.STAINING_0, TMALabel.LABEL_UNK);
                int num_unk_ustai_tot = num_unk_ustai_est + num_unk_ustai_gst;
                int num_unk_stai1_est = ts.getNumPoints(false, TMALabel.STAINING_1, TMALabel.LABEL_UNK);
                int num_unk_stai1_gst = ts.getNumPoints(true, TMALabel.STAINING_1, TMALabel.LABEL_UNK);
                int num_unk_stai1_tot = num_unk_stai1_est + num_unk_stai1_gst;
                int num_unk_stai2_est = ts.getNumPoints(false, TMALabel.STAINING_2, TMALabel.LABEL_UNK);
                int num_unk_stai2_gst = ts.getNumPoints(true, TMALabel.STAINING_2, TMALabel.LABEL_UNK);
                int num_unk_stai2_tot = num_unk_stai2_est + num_unk_stai2_gst;
                int num_unk_stai3_est = ts.getNumPoints(false, TMALabel.STAINING_3, TMALabel.LABEL_UNK);
                int num_unk_stai3_gst = ts.getNumPoints(true, TMALabel.STAINING_3, TMALabel.LABEL_UNK);
                int num_unk_stai3_tot = num_unk_stai3_est + num_unk_stai3_gst;
                int num_unk_stai_est = num_unk_stai1_est + num_unk_stai2_est + num_unk_stai3_est;
                int num_unk_stai_gst = num_unk_stai1_gst + num_unk_stai2_gst + num_unk_stai3_gst;
                int num_unk_stai_tot = num_unk_stai_est + num_unk_stai_gst;
                int num_unk_tot_est = num_unk_ustai_est + num_unk_stai_est;
                int num_unk_tot_gst = num_unk_ustai_gst + num_unk_stai_gst;
                int num_unk_tot_tot = num_unk_tot_est + num_unk_tot_gst;
                
                //TOT
                int num_tot_ustai_est = num_neg_ustai_est + num_pos_ustai_est + num_unk_ustai_est;
                int num_tot_ustai_gst = num_neg_ustai_gst + num_pos_ustai_gst + num_unk_ustai_gst;
                int num_tot_ustai_tot = num_tot_ustai_est + num_tot_ustai_gst;
                int num_tot_stai1_est = num_neg_stai1_est + num_pos_stai1_est + num_unk_stai1_est;
                int num_tot_stai1_gst = num_neg_stai1_gst + num_pos_stai1_gst + num_unk_stai1_gst;
                int num_tot_stai1_tot = num_tot_stai1_est + num_tot_stai1_gst;
                int num_tot_stai2_est = num_neg_stai2_est + num_pos_stai2_est + num_unk_stai2_est;
                int num_tot_stai2_gst = num_neg_stai2_gst + num_pos_stai2_gst + num_unk_stai2_gst;
                int num_tot_stai2_tot = num_tot_stai2_est + num_tot_stai2_gst;
                int num_tot_stai3_est = num_neg_stai3_est + num_pos_stai3_est + num_unk_stai3_est;
                int num_tot_stai3_gst = num_neg_stai3_gst + num_pos_stai3_gst + num_unk_stai3_gst;
                int num_tot_stai3_tot = num_tot_stai3_est + num_tot_stai3_gst;
                int num_tot_stai_est = num_tot_stai1_est + num_tot_stai2_est + num_tot_stai3_est;
                int num_tot_stai_gst = num_tot_stai1_gst + num_tot_stai2_gst + num_tot_stai3_gst;
                int num_tot_stai_tot = num_tot_stai_est + num_tot_stai_gst;
                int num_tot_tot_est = num_tot_ustai_est + num_tot_stai_est;
                int num_tot_tot_gst = num_tot_ustai_gst + num_tot_stai_gst;
                int num_tot_tot_tot = num_tot_tot_est + num_tot_tot_gst;
                
                //Percentages
                String num_pos_stai_est_pr = String.format("%.0f", 100.0*num_pos_stai_est/num_pos_tot_est).replace("NaN", "-");
                String num_tot_stai_est_pr = String.format("%.0f", 100.0*num_tot_stai_est/num_tot_tot_est).replace("NaN", "-");
                String num_pos_stai_gst_pr = String.format("%.0f", 100.0*num_pos_stai_gst/num_pos_tot_gst).replace("NaN", "-");
                String num_tot_stai_gst_pr = String.format("%.0f", 100.0*num_tot_stai_gst/num_tot_tot_gst).replace("NaN", "-");
                String num_pos_stai_tot_pr = String.format("%.0f", 100.0*num_pos_stai_tot/num_pos_tot_tot).replace("NaN", "-");
                String num_tot_stai_tot_pr = String.format("%.0f", 100.0*num_tot_stai_tot/num_tot_tot_tot).replace("NaN", "-");
                
                // Human annotation exisiting?
                boolean hasHumanAnnotation = !ts.getPoints_GoldStandard().isEmpty();
                
                outputText += "<tr>" + linebreak
                  
                  +  "<td class=\"image\" rowspan=\"" + (hasHumanAnnotation ? "3" : "1") + "\">";
                if (!ts.getPoints().isEmpty() || !ts.getExcludingAreas().isEmpty() || !ts.getIncludingAreas().isEmpty()) {
                    outputText += "<a href=\"" + folderName + ts.getName() + "_processed.jpg\"><img alt=\"Processed Image\" src=\"" + folderName + ts.getName() + "_processed.jpg\" width=\"" + (anyHasHumanAnnotation?"90":"50") + "\"></a>";
                } else {
                    outputText += "<a href=\"" + folderName + ts.getName() + ".jpg\"><img alt=\"Original Image\" src=\"" + folderName + ts.getName() + ".jpg\" width=\"" + (anyHasHumanAnnotation?"90":"50") + "\"></a>";
                }
                outputText +="</td>" + linebreak;
                   
                outputText +="<td rowspan=\"" + (hasHumanAnnotation ? "3" : "1") + "\">";
                if (!ts.getPoints().isEmpty() || !ts.getExcludingAreas().isEmpty() || !ts.getIncludingAreas().isEmpty()) {
                    outputText += "<a href=\"#" + ts.getName().replaceAll(" ", "") + "\">" + ts.getName() + "</a>";
                } else {
                    outputText += "<a href=\"#" + ts.getName().replaceAll(" ", "") + "\">" + ts.getName() + "</a>";
                }
                outputText += "</td>" + linebreak;
                
                if (anyHasHumanAnnotation) {
                    outputText += "<td>PC</td>" + linebreak;
                }
                outputText += "<td class=\"firstColPerDom\">" + myFormatInteger(num_neg_tot_est) + "</td>" + linebreak
                        + "<td class=\"clear_neg\">" + myFormatInteger(num_neg_ustai_est) + "</td>" + linebreak
                        + "<td class=\"stai_neg\">" + myFormatInteger(num_neg_stai_est) + "</td>" + linebreak
                        + "<td class=\"num123_neg_1\">" + myFormatInteger(num_neg_stai1_est) + "</td>" + linebreak
                        + "<td class=\"num123_neg_2\">" + myFormatInteger(num_neg_stai2_est) + "</td>" + linebreak
                        + "<td class=\"num123_neg_3\">" + myFormatInteger(num_neg_stai3_est) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_pos_tot_est) + "</td>" + linebreak
                        + "<td class=\"clear_pos\">" + myFormatInteger(num_pos_ustai_est) + "</td>" + linebreak
                        + "<td class=\"stai_pos\">" + myFormatInteger(num_pos_stai_est) + "</td>" + linebreak
                        + "<td class=\"perc\">" + num_pos_stai_est_pr + "</td>" + linebreak
                        + "<td class=\"num123_pos_1\">" + myFormatInteger(num_pos_stai1_est) + "</td>" + linebreak
                        + "<td class=\"num123_pos_2\">" + myFormatInteger(num_pos_stai2_est) + "</td>" + linebreak
                        + "<td class=\"num123_pos_3\">" + myFormatInteger(num_pos_stai3_est) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_unk_tot_est) + "</td>" + linebreak
                        + "<td class=\"clear_unk\">" + myFormatInteger(num_unk_ustai_est) + "</td>" + linebreak
                        + "<td class=\"stai_unk\">" + myFormatInteger(num_unk_stai_est) + "</td>" + linebreak
                        + "<td class=\"num123_unk_1\">" + myFormatInteger(num_unk_stai1_est) + "</td>" + linebreak
                        + "<td class=\"num123_unk_2\">" + myFormatInteger(num_unk_stai2_est) + "</td>" + linebreak
                        + "<td class=\"num123_unk_3\">" + myFormatInteger(num_unk_stai3_est) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_tot_tot_est) + "</td>" + linebreak
                        + "<td class=\"clear_tot\">" + myFormatInteger(num_tot_ustai_est) + "</td>" + linebreak
                        + "<td class=\"stai_tot\">" + myFormatInteger(num_tot_stai_est) + "</td>" + linebreak
                        + "<td class=\"perc\">" + num_tot_stai_est_pr + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai1_est) + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai2_est) + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai3_est) + "</td>" + linebreak
                      + "</tr>" + linebreak + linebreak;
                
                if (hasHumanAnnotation) {
                    outputText += "<tr><td>Human</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_neg_tot_gst) + "</td>" + linebreak
                        + "<td class=\"clear_neg\">" + myFormatInteger(num_neg_ustai_gst) + "</td>" + linebreak
                        + "<td class=\"stai_neg\">" + myFormatInteger(num_neg_stai_gst) + "</td>" + linebreak
                        + "<td class=\"num123_neg_1\">" + myFormatInteger(num_neg_stai1_gst) + "</td>" + linebreak
                        + "<td class=\"num123_neg_2\">" + myFormatInteger(num_neg_stai2_gst) + "</td>" + linebreak
                        + "<td class=\"num123_neg_3\">" + myFormatInteger(num_neg_stai3_gst) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_pos_tot_gst) + "</td>" + linebreak
                        + "<td class=\"clear_pos\">" + myFormatInteger(num_pos_ustai_gst) + "</td>" + linebreak
                        + "<td class=\"stai_pos\">" + myFormatInteger(num_pos_stai_gst) + "</td>" + linebreak
                        + "<td class=\"perc\">" + num_pos_stai_gst_pr + "</td>" + linebreak
                        + "<td class=\"num123_pos_1\">" + myFormatInteger(num_pos_stai1_gst) + "</td>" + linebreak
                        + "<td class=\"num123_pos_2\">" + myFormatInteger(num_pos_stai2_gst) + "</td>" + linebreak
                        + "<td class=\"num123_pos_3\">" + myFormatInteger(num_pos_stai3_gst) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_unk_tot_gst) + "</td>" + linebreak
                        + "<td class=\"clear_unk\">" + myFormatInteger(num_unk_ustai_gst) + "</td>" + linebreak
                        + "<td class=\"stai_unk\">" + myFormatInteger(num_unk_stai_gst) + "</td>" + linebreak
                        + "<td class=\"num123_unk_1\">" + myFormatInteger(num_unk_stai1_gst) + "</td>" + linebreak
                        + "<td class=\"num123_unk_2\">" + myFormatInteger(num_unk_stai2_gst) + "</td>" + linebreak
                        + "<td class=\"num123_unk_3\">" + myFormatInteger(num_unk_stai3_gst) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_tot_tot_gst) + "</td>" + linebreak
                        + "<td class=\"clear_tot\">" + myFormatInteger(num_tot_ustai_gst) + "</td>" + linebreak
                        + "<td class=\"stai_tot\">" + myFormatInteger(num_tot_stai_gst) + "</td>" + linebreak
                        + "<td class=\"perc\">" + num_tot_stai_gst_pr + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai1_gst) + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai2_gst) + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai3_gst) + "</td>" + linebreak
                      + "</tr>" + linebreak + linebreak;
                
                    outputText += "<tr><td>PC+Human</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_neg_tot_tot) + "</td>" + linebreak
                        + "<td class=\"clear_neg\">" + myFormatInteger(num_neg_ustai_tot) + "</td>" + linebreak
                        + "<td class=\"stai_neg\">" + myFormatInteger(num_neg_stai_tot) + "</td>" + linebreak
                        + "<td class=\"num123_neg_1\">" + myFormatInteger(num_neg_stai1_tot) + "</td>" + linebreak
                        + "<td class=\"num123_neg_2\">" + myFormatInteger(num_neg_stai2_tot) + "</td>" + linebreak
                        + "<td class=\"num123_neg_3\">" + myFormatInteger(num_neg_stai3_tot) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_pos_tot_tot) + "</td>" + linebreak
                        + "<td class=\"clear_pos\">" + myFormatInteger(num_pos_ustai_tot) + "</td>" + linebreak
                        + "<td class=\"stai_pos\">" + myFormatInteger(num_pos_stai_tot) + "</td>" + linebreak
                        + "<td class=\"perc\">" + num_pos_stai_tot_pr + "</td>" + linebreak
                        + "<td class=\"num123_pos_1\">" + myFormatInteger(num_pos_stai1_tot) + "</td>" + linebreak
                        + "<td class=\"num123_pos_2\">" + myFormatInteger(num_pos_stai2_tot) + "</td>" + linebreak
                        + "<td class=\"num123_pos_3\">" + myFormatInteger(num_pos_stai3_tot) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_unk_tot_tot) + "</td>" + linebreak
                        + "<td class=\"clear_unk\">" + myFormatInteger(num_unk_ustai_tot) + "</td>" + linebreak
                        + "<td class=\"stai_unk\">" + myFormatInteger(num_unk_stai_tot) + "</td>" + linebreak
                        + "<td class=\"num123_unk_1\">" + myFormatInteger(num_unk_stai1_tot) + "</td>" + linebreak
                        + "<td class=\"num123_unk_2\">" + myFormatInteger(num_unk_stai2_tot) + "</td>" + linebreak
                        + "<td class=\"num123_unk_3\">" + myFormatInteger(num_unk_stai3_tot) + "</td>" + linebreak
                        + "<td class=\"firstColPerDom\">" + myFormatInteger(num_tot_tot_tot) + "</td>" + linebreak
                        + "<td class=\"clear_tot\">" + myFormatInteger(num_tot_ustai_tot) + "</td>" + linebreak
                        + "<td class=\"stai_tot\">" + myFormatInteger(num_tot_stai_tot) + "</td>" + linebreak
                        + "<td class=\"perc\">" + num_tot_stai_tot_pr + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai1_tot) + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai2_tot) + "</td>" + linebreak
                        + "<td class=\"num123_tot\">" + myFormatInteger(num_tot_stai3_tot) + "</td>" + linebreak
                      + "</tr>" + linebreak + linebreak;
                }

            }
            outputText += "</tbody>" + linebreak + "</table>" + linebreak + "</div>" + linebreak + "<br>"+ linebreak + linebreak;
            outputText += "<input value=\"Save Table (xls)\" type=\"button\" onclick=\"SaveAsXLS('" + file.getName().replace(".html", "") + "')\"><br><br>" + linebreak;
            
            // Save all the images larger overview
            outputText += "<hr><h2>TMA spot overview</h2>";
            
            outputText += "<i>Shown are all TMA spots which have been processed by TMARKER. <br>" + linebreak +
                    "Each TMA spot shows the original image on the right side and the detected nuclei as colored dots on the left side (if any).<br>" + linebreak +
                    "There are different colors for clear (unstained) nuclei and stained nuclei, as well as for benign, malignant or unknown nuclei. <br> " + linebreak +
                    "The colors are defined in the TMARKER program. <br><br>" + linebreak +
                    
                    "TMARKER can cluster the nuclei by their intensity, and up to three staining intensities (1+, 2+ and 3+) are shown,<br>" + linebreak +
                    "each with their loci in the image.<br><br>" + linebreak +
                    
                    "<b>PC</b> means detected by TMARKER. <b>Human</b> means detected by the user. <b>PC+Human</b> sums up both numbers.<br>" + linebreak +
                    "Staining percentages refer to the total number of nuclei, respectively." + 
                    "<br>Images can be enlarged by mouse click.</i><br><br><br>" + linebreak + linebreak;
            
            outputText += "<table cellpadding=\"10\"><tbody>" + linebreak;
            for (TMAspot ts: tss) {
                // Save the TMAspot list panel for PC annotation as image
                file_tmp = new File(path + folderName + ts.getName() + "_tlpP.png");
                bi = Misc.getScreenShot(ts.getTLP());
                bi.getGraphics().setColor(Color.WHITE);
                bi.getGraphics().fillRect(ts.getTLP().getOffsetx(), ts.getTLP().getOffsety(), bi.getWidth()-ts.getTLP().getOffsetx(), bi.getHeight()-ts.getTLP().getOffsety());
                ts.getTLP().drawNucleiCounts(bi.getGraphics(), true, false);
                ImageIO.write(bi, "png", file_tmp);
                
                // Save the TMAspot list panel for Human annotation as image
                file_tmp = new File(path + folderName + ts.getName() + "_tlpH.png");
                bi.getGraphics().fillRect(ts.getTLP().getOffsetx(), ts.getTLP().getOffsety(), bi.getWidth()-ts.getTLP().getOffsetx(), bi.getHeight()-ts.getTLP().getOffsety());
                ts.getTLP().drawNucleiCounts(bi.getGraphics(), false, true);
                ImageIO.write(bi, "png", file_tmp);
                
                // Save the TMAspot list panel for PC and Human annotation as image
                file_tmp = new File(path + folderName + ts.getName() + "_tlpPH.png");
                bi.getGraphics().fillRect(ts.getTLP().getOffsetx(), ts.getTLP().getOffsety(), bi.getWidth()-ts.getTLP().getOffsetx(), bi.getHeight()-ts.getTLP().getOffsety());
                ts.getTLP().drawNucleiCounts(bi.getGraphics(), true, true);
                ImageIO.write(bi, "png", file_tmp);
                
                outputText += " <tr valign=\"top\">" + linebreak
                        + "  <td>" + linebreak
                        + "   " + "<h3><a name=\"" + ts.getName().replaceAll(" ", "") + "\">" + ts.getName() + "</a></h3><br>" + linebreak
                        + "   <img src=\"" + folderName + ts.getName() + "_tlpP.png" + "\"><br><br>" + linebreak
                        + "   <img src=\"" + folderName + ts.getName() + "_tlpH.png" + "\"><br><br>" + linebreak
                        + "   <img src=\"" + folderName + ts.getName() + "_tlpPH.png" + "\"><br><br>" + linebreak
                        + "  </td>" + linebreak
                        + "  <td>" + linebreak;
                        if (!ts.getPoints().isEmpty() || !ts.getExcludingAreas().isEmpty() || !ts.getIncludingAreas().isEmpty()) {
                            outputText += "   <a name=\"" + folderName + ts.getName() + "_processed.jpg\" href=\"" + folderName + ts.getName() + "_processed.jpg\"><img alt=\"Processed Image\" src=\"" + folderName + ts.getName() + "_processed.jpg\" width=\"500\"></a><br><br>" + linebreak;
                        }
                        outputText += "  </td>" + linebreak
                        + "  <td>" + linebreak
                        + "   <a name=\"" + folderName + ts.getName() + ".jpg\" href=\"" + folderName + ts.getName() + ".jpg\"><img alt=\"Original Image\" src=\"" + folderName + ts.getName() + ".jpg\" width=\"500\"></a><br><br>" + linebreak
                        + "  </td>" + linebreak
                        + " </tr>";
            }
            outputText += "</tbod></table>" + linebreak + linebreak;
            
            for (Pluggable p: t.plugins) {
                try {
                    outputText += "<hr>" + linebreak;
                    outputText += "<h2><a name=\"" + p.getPluginName().replaceAll(" ", "") + "\">" + p.getPluginName() + " Plugin Report</a></h2>" + linebreak;
                    String report = p.getHTMLReport(path + folderName).replace("<html>", "").replace("</html>", "");
                    if (report.isEmpty()) {
                        report = "<i>No information available.</i>";
                    }
                    outputText += report + "<br><br>";
                } catch (Exception e) {
                    
                }
            }
            
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            outputText += linebreak + "<br><br><br>" + linebreak + "<hr>" + linebreak + 
                    "<i>Generated with TMARKER v. " + tmarker.REVISION + " - " + dateFormat.format(cal.getTime()) + "<br>" + linebreak +
                    "Session ID: " + UID.toString() + "<br>" + linebreak +
                    "(c) 2015 Peter J. Sch&uuml;ffler</i><br><br>" +
                    "</body>" + linebreak + "</html>";
            
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(outputText);
            bw.close();
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, e.getMessage(), e);
            JOptionPane.showMessageDialog(t, "An error occurred while writing "
                    + file.getName() + "\nMaybe it's still in use.", "Could not save file", JOptionPane.ERROR_MESSAGE);
        } finally {
            t.setStatusMessageLabel(text);
            t.setProgressbar(0); 
        }
    }
     
    /**
     * Returns a String representing the given integer. If i==0, "-" is returned.
     * @param i The integer to be converted.
     * @return i as a string, or "-" for 0.
     */
    static String myFormatInteger(int i) {
        if (i==0) {
            return "-";
        } else {
            return Integer.toString(i);
        }
    }
    
    /**
     * Assigns a special listener to the given toolbar which corrects the orientation of JSliders 
     * when the toolbar gets a new orientation.
     * @param toolBar The toolbar on which the listener is assigned.
     */
    protected void listenForOrientationChange(final JToolBar toolBar) {
      toolBar.addPropertyChangeListener(new PropertyChangeListener() {
         @Override
         public void propertyChange(PropertyChangeEvent evt) {
            Component[] components = toolBar.getComponents();
             for (Component component : components) {
                 if (component instanceof javax.swing.JSlider) {
                     JSlider comp = (JSlider) component;
                     if (toolBar.getOrientation() == SwingConstants.HORIZONTAL) {
                         comp.setOrientation(JSlider.HORIZONTAL);
                         comp.setMaximumSize(new Dimension(comp.getPreferredSize().width, toolBar.getPreferredSize().height));
                     } else {
                         comp.setOrientation(JSlider.VERTICAL);
                         comp.setMaximumSize(new Dimension(toolBar.getPreferredSize().width, comp.getPreferredSize().height));
                     }
                 } else if (component instanceof javax.swing.JLabel) {
                  JLabel comp = (JLabel) component;
                  if (comp.getText().contains("Radius")) {
                      if (toolBar.getOrientation() == SwingConstants.HORIZONTAL) {
                         comp.setText("Nucleus Radius = ");
                      } else {
                         comp.setText("Radius");
                      }
                  }
               }
            }
         }
      });
   }
    
    /**
     * Constructs the clickable nuclei labels on the jToolBar1.
     */
    private void buildToolBar1() {
        int rad = 7;
        int n_own_comp = 3; // three components are already on the tool bar (label, slider, textfield). Add the nuclei in front of them.
        JToolBar toolbar = jToolBar1;
        
        JLabel label = new JLabel("Benign");
        label.setAlignmentX((float) 0.5);
        toolbar.add(label, toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_NEG, TMALabel.STAINING_0, Color.GREEN.brighter()), toolbar.getComponentCount()-n_own_comp);
        ((LegendElement)(toolbar.getComponent(1))).setSelected(true);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_NEG, TMALabel.STAINING_1, Color.GREEN), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_NEG, TMALabel.STAINING_2, Color.GREEN.darker()), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_NEG, TMALabel.STAINING_3, Color.GREEN.darker().darker()), toolbar.getComponentCount()-n_own_comp);
        
        toolbar.add(new JToolBar.Separator(), toolbar.getComponentCount()-n_own_comp);
        label = new JLabel("Malignant");
        label.setAlignmentX((float) 0.5);
        toolbar.add(label, toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_POS, TMALabel.STAINING_0, Color.CYAN), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_POS, TMALabel.STAINING_1, Color.RED), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_POS, TMALabel.STAINING_2, Color.RED.darker()), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_POS, TMALabel.STAINING_3, Color.RED.darker().darker()), toolbar.getComponentCount()-n_own_comp);
        
        toolbar.add(new JToolBar.Separator(), toolbar.getComponentCount()-n_own_comp);
        label = new JLabel("Unknown");
        label.setAlignmentX((float) 0.5);
        toolbar.add(label, toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_UNK, TMALabel.STAINING_0, Color.GRAY.brighter()), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_UNK, TMALabel.STAINING_1, Color.GRAY), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_UNK, TMALabel.STAINING_2, Color.GRAY.darker()), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_UNK, TMALabel.STAINING_3, Color.GRAY.darker().darker()), toolbar.getComponentCount()-n_own_comp);
        
        toolbar.add(new JToolBar.Separator(), toolbar.getComponentCount()-n_own_comp);
        label = new JLabel("Background");
        label.setAlignmentX((float) 0.5);
        toolbar.add(label, toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementNucleus(this, rad, TMALabel.LABEL_BG, TMALabel.STAINING_0, Color.WHITE), toolbar.getComponentCount()-n_own_comp);
        
        toolbar.add(new JToolBar.Separator(), toolbar.getComponentCount()-n_own_comp);
        label = new JLabel("Edit");
        label.setAlignmentX((float) 0.5);
        toolbar.add(label, toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementIcon(this, rad, new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/flippoint.png")), "Flip a Point's Class"), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementIcon(this, rad, new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/flipstaingrad.png")), "Correct a Point's Staining (0 - 3+)"), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementIcon(this, rad, new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/flipstainbin.png")), "Correct a Point's Staining (binary)"), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementIcon(this, rad, new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/delpoint.png")), "Delete a Point"), toolbar.getComponentCount()-n_own_comp);
        
        toolbar.add(new JToolBar.Separator(), toolbar.getComponentCount()-n_own_comp);
        label = new JLabel("ROI");
        label.setAlignmentX((float) 0.5);
        toolbar.add(label, toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementIcon(this, rad, new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/inclarea.png")), "Draw a polygon of an area which will only be considered for processing. Doubleclick finishes the polygon."), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementIcon(this, rad, new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/exclarea.png")), "Draw a polygon of an area which will be ignored for processing. Doubleclick finishes the polygon."), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementIcon(this, rad, new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/switcharea.png")), "Click within a polygon to switch it between including and excluding area."), toolbar.getComponentCount()-n_own_comp);
        toolbar.add(new LegendElementIcon(this, rad, new javax.swing.ImageIcon(getClass().getResource("/tmarker/img/buttons/delarea.png")), "Click within a polygon to be deleted."), toolbar.getComponentCount()-n_own_comp);
        
        NoneSelectedButtonGroup buttongroup = new NoneSelectedButtonGroup();
        for (Component comp: toolbar.getComponents()) {
            String name = comp.getClass().getName();
            if (name.contains(LegendElement.class.getName())) {
                buttongroup.add((LegendElement)comp);
            }
        }
        
        toolbar.add(new JToolBar.Separator(), toolbar.getComponentCount()-n_own_comp);
    }
    
    /**
     * Repaints the nuclei symbols on the tool bar. Shape and color are re-evaluated.
     */
    public void repaintNucleiOnToolBar() {
        for (Component comp: jToolBar1.getComponents()) {
            String name = comp.getClass().getName();
            if (name.contains(LegendElementNucleus.class.getName())) {
                ((LegendElementNucleus)comp).createNucIcon();
            }
        }
    }
    
    /**
     * Returns true, if TMARKER is in the background correction (manual white balance) modus. 
     * Then, a click on the TMA image will select the background color.
     * @return True if TMARKER is in background correction modus (when the white balance dialog is visible).
     */
    public boolean isInBGCorrectionModus() {
        return (bcd!=null && bcd.isVisible());
    }
    
    /**
     * Returns true, if the user clicked on "including ROIs".
     * @return True if the program is in the modus to draw including areas.
     */
    public boolean isInDrawIncludingAreaModus() {
        return ((LegendElement)jToolBar1.getComponent(28)).isSelected();
    }
    
    /**
     * Returns true, if the user clicked on "excluding ROIs".
     * @return True if the program is in the modus to draw excluding areas.
     */
    public boolean isInDrawExcludingAreaModus() {
        return ((LegendElement)jToolBar1.getComponent(29)).isSelected();
    }
    
    /**
     * Returns true, if the user clicked on "switch ROIs".
     * @return True if the program is in the modus to switch areas.
     */
     public boolean isInSwitchAreaModus() {
        return ((LegendElement)jToolBar1.getComponent(30)).isSelected();
    }
    
    /**
     * Returns true, if the user clicked on "delete ROIs".
     * @return True if the program is in the modus to delete areas.
     */
    public boolean isInDeleteAreaModus() {
        return ((LegendElement)jToolBar1.getComponent(31)).isSelected();
    }

    /**
     * Returns true, if one of the ROI buttons is pressed.
     * @return True, if one of the ROI buttons is pressed.
     */
    public boolean isROIselected() {
        return (isInDrawIncludingAreaModus() || isInDrawExcludingAreaModus() || isInDeleteAreaModus());
    }
    
    
    /**
     * Returns the behaviour of a mouse click on a TMAspot.
     * @return The behaviour of a mouse click on a TMAspot (e.g. CLICK_BEHAVIOUR_ADD_POS).
     */
    public int getClickBehaviour() {
        if (((LegendElement)jToolBar1.getComponent(1)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(2)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(3)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(4)).isSelected()) {return tmarker.CLICK_BEHAVIOUR_ADD_NEG; }
        if (((LegendElement)jToolBar1.getComponent(7)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(8)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(9)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(10)).isSelected()) {return tmarker.CLICK_BEHAVIOUR_ADD_POS; }
        if (((LegendElement)jToolBar1.getComponent(13)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(14)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(15)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(16)).isSelected()) {return tmarker.CLICK_BEHAVIOUR_ADD_UNK; }
        
        if (((LegendElement)jToolBar1.getComponent(19)).isSelected()) {return tmarker.CLICK_BEHAVIOUR_ADD_BG; }
        if (((LegendElement)jToolBar1.getComponent(22)).isSelected()) {return tmarker.CLICK_BEHAVIOUR_FLIP; }
        if (((LegendElement)jToolBar1.getComponent(23)).isSelected()) {return tmarker.CLICK_BEHAVIOUR_CORSTAIN_GRAD; }
        if (((LegendElement)jToolBar1.getComponent(24)).isSelected()) {return tmarker.CLICK_BEHAVIOUR_CORSTAIN_BIN; }
        if (((LegendElement)jToolBar1.getComponent(25)).isSelected()) {return tmarker.CLICK_BEHAVIOUR_DELETE; }
        
        return tmarker.CLICK_BEHAVIOUR_NONE;
    }

    /**
     * Returns the current intensity for manually added nuclei.
     * @return One of STAINING_[0-3].
     */
    public byte getCurrentStainingIntensity() {
        if (((LegendElement)jToolBar1.getComponent(2)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(8)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(14)).isSelected()) {return TMALabel.STAINING_1; }
        if (((LegendElement)jToolBar1.getComponent(3)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(9)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(15)).isSelected() ) {return TMALabel.STAINING_2; }
        if (((LegendElement)jToolBar1.getComponent(4)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(10)).isSelected() ||
            ((LegendElement)jToolBar1.getComponent(16)).isSelected()) {return TMALabel.STAINING_3; }
        return TMALabel.STAINING_0;
    }
    
    /**
     * Returns one specific Nucleus element from the tool bar. E.g. it can be used to determine 
     * the color of the labels for these nuclei.
     * @param labelType One of TMALabel.LABEL_POS, TMALabel.LABEL_NEG, TMALabel.LABEL_UNK or TMALabel.LABEL_BG
     * @param staining One of TMALabel.STAINING_0, TMALabel.STAINING_1, TMALabel.STAINING_2 or TMALabel.STAINING_3.
     * @return The LegendElementNucleus on the toolbar which represents the labels for these types of nuclei. Null, if no element is found.
     */
    LegendElementNucleus getNucleusToolBarComponent(byte labelType, byte staining) {
        for (Component comp: jToolBar1.getComponents()) {
            String name = comp.getClass().getName();
            if (name.contains(LegendElementNucleus.class.getName())) {
                if (((LegendElementNucleus)comp).getNucLabel()==labelType && ((LegendElementNucleus)comp).getStaining()==staining) {
                    return ((LegendElementNucleus)comp);
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the nuclei radius.
     * @return The radius of the nuclei.
     */
    public int getLabelRadius() {
        return jSlider2.getValue();
    }
    
    /**
     * Sets the nucleus radius.
     * @param r The radius of the nuclei.
     */
    public void setLabelRadius(int r) {
        jSlider2.setValue(r);
    }
     
    /**
     * Returns the recent parameter settings and program properties.
     * Also returns the parameters settings of the loaded plugins. As convention, a plugin's
     * property name is defined as "PLUGINNAME.propertyName". If two plugins have the same name and they share the same parameter name, 
     * the property might be overwritten.
     * @return The current parameter settings and program properties.
     */
    private Properties getParameterValues() {
        Properties appProps = new Properties();
        appProps.setProperty("tmarker.r", Integer.toString(getLabelRadius()));
        List<String> annotationProps = getAnnotationProperties();
        for (int i=0; i<annotationProps.size(); i++) {
            appProps.setProperty("tmarker.AnnotationProp"+i+"Name", annotationProps.get(i));
        }
        appProps.setProperty("tmarker.colorPOSStaining0", Integer.toString(getLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_0).getRGB()));
        appProps.setProperty("tmarker.colorPOSStaining1", Integer.toString(getLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_1).getRGB()));
        appProps.setProperty("tmarker.colorPOSStaining2", Integer.toString(getLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_2).getRGB()));
        appProps.setProperty("tmarker.colorPOSStaining3", Integer.toString(getLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_3).getRGB()));
        appProps.setProperty("tmarker.colorNEGStaining0", Integer.toString(getLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_0).getRGB()));
        appProps.setProperty("tmarker.colorNEGStaining1", Integer.toString(getLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_1).getRGB()));
        appProps.setProperty("tmarker.colorNEGStaining2", Integer.toString(getLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_2).getRGB()));
        appProps.setProperty("tmarker.colorNEGStaining3", Integer.toString(getLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_3).getRGB()));
        appProps.setProperty("tmarker.colorUNKStaining0", Integer.toString(getLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_0).getRGB()));
        appProps.setProperty("tmarker.colorUNKStaining1", Integer.toString(getLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_1).getRGB()));
        appProps.setProperty("tmarker.colorUNKStaining2", Integer.toString(getLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_2).getRGB()));
        appProps.setProperty("tmarker.colorUNKStaining3", Integer.toString(getLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_3).getRGB()));
        appProps.setProperty("tmarker.colorBGStaining0", Integer.toString(getLabelsColor(TMALabel.LABEL_BG, TMALabel.STAINING_0).getRGB()));
        appProps.setProperty("OD.labelsShapeGst", Integer.toString(od.getLabelsShape_Gst()));
        appProps.setProperty("OD.labelsShapeEst", Integer.toString(od.getLabelsShape_Est()));
        appProps.setProperty("OD.automaticESGSConversion", Boolean.toString(od.isAutomaticESGSConversion()));
        appProps.setProperty("OD.useParallelProgramming", Boolean.toString(od.useParallelProgramming()));
        appProps.setProperty("OD.useLocalPlugins", Boolean.toString(od.getUseLocalPlugins()));
        appProps.setProperty("OD.localPluginFolder", od.getLocalPluginFolder());
        appProps.setProperty("BCD.useColor", Boolean.toString(bcd.getUseColor()));
        
        // Add the plugin properties
        for (Pluggable p: plugins) {
            Properties p_props = p.getParameters();
            Enumeration prop_names = p_props.propertyNames();
            while (prop_names.hasMoreElements()) {
                String prop_name = (String) prop_names.nextElement();
                appProps.setProperty(p.getPluginName().toUpperCase()+ "." + prop_name, p_props.getProperty(prop_name));
            }
        }
        
        return appProps;
    }
    
     /**
      * Saves the program parameters in a tmarker.conf file. The parameters can be 
      * restored in the next program start.
      */
     private void storeParameterValues() {
        FileOutputStream out = null;
        try {
            Properties appProps = new Properties();
            try {
                FileInputStream in = new FileInputStream("tmarker.conf");
                appProps.load(in);
                in.close();
            } catch (Exception e) {                
            } 
            appProps.setProperty("OD.saveParametersOnExit", Boolean.toString(od.storeParamsOnExit()));
            if (od.storeParamsOnExit()) {
                Properties newProps = getParameterValues();
                appProps.putAll(newProps);
            }
            
            out = new FileOutputStream("tmarker.conf");
            appProps.store(out, "TMARKER Program Parameters");
            out.close();
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.WARNING, "Unable to write file tmarker.conf. Maybe no write permission? TMARKER default parameters used.");
        }
     }
     
     /**
      * Restores the parameter values saved in the file tmarker.conf.
      * @param only_plugins If true, the plugin parameters are restored. If false, only the TMARKER paramters.
      */
     private void restoreParameterValues(boolean only_plugins) {
        try {
            Properties appProps = new Properties();
            FileInputStream in = new FileInputStream("tmarker.conf");
            appProps.load(in);
            in.close();
            restoreParameterValues(appProps, only_plugins);
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.WARNING, "File tmarker.conf not found. TMARKER default parameters used.");
        }
     }
     
     /**
      * Restores the parameter values saved in the given properties.
      * Also restores the plugin parameter values saved in the given properties.
      * @param appProps The properties to be restored.
      * @param only_plugins If true, the plugin parameters are restored. If false, only the TMARKER paramters.
      */
     private void restoreParameterValues(Properties appProps, boolean only_plugins) {
        if (!only_plugins) {    
            String value;
            value = appProps.getProperty("tmarker.r"); if (value!=null) { setLabelRadius(Integer.parseInt(value)); }
            List<String> annotationProps = new ArrayList<>();
            for (int i=0; i<4; i++) {
                annotationProps.add(appProps.getProperty("tmarker.AnnotationProp"+i+"Name"));
            }
            setAnnotationProperties(annotationProps);
            value = appProps.getProperty("tmarker.colorPOSStaining0"); if (value!=null) { setLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_0, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorPOSStaining1"); if (value!=null) { setLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_1, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorPOSStaining2"); if (value!=null) { setLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_2, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorPOSStaining3"); if (value!=null) { setLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_3, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorNEGStaining0"); if (value!=null) { setLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_0, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorNEGStaining1"); if (value!=null) { setLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_1, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorNEGStaining2"); if (value!=null) { setLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_2, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorNEGStaining3"); if (value!=null) { setLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_3, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorUNKStaining0"); if (value!=null) { setLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_0, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorUNKStaining1"); if (value!=null) { setLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_1, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorUNKStaining2"); if (value!=null) { setLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_2, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorUNKStaining3"); if (value!=null) { setLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_3, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("tmarker.colorBGStaining0"); if (value!=null) { setLabelsColor(TMALabel.LABEL_BG, TMALabel.STAINING_0, new Color(Integer.parseInt(value), true)); }
            value = appProps.getProperty("OD.saveParametersOnExit"); if (value!=null) { od.setStoreParamsOnExit(Boolean.parseBoolean(value)); }
            value = appProps.getProperty("OD.labelsShapeGst"); if (value!=null) { od.setLabelsShape_Gst(Integer.parseInt(value)); }
            value = appProps.getProperty("OD.labelsShapeEst"); if (value!=null) { od.setLabelsShape_Est(Integer.parseInt(value)); }
            value = appProps.getProperty("OD.automaticESGSConversion"); if (value!=null) { od.setAutomaticESGSConversion(Boolean.parseBoolean(value)); }
            value = appProps.getProperty("OD.useParallelProgramming"); if (value!=null) { od.setUseParallelProgramming(Boolean.parseBoolean(value)); }
            value = appProps.getProperty("OD.useLocalPlugins"); if (value!=null) { od.setUseLocalPlugins(Boolean.parseBoolean(value)); }
            value = appProps.getProperty("OD.localPluginFolder"); if (value!=null) { od.setLocalPluginFolder(value); }
            value = appProps.getProperty("BCD.useColor"); if (value!=null) { bcd.setUseColor(Boolean.parseBoolean(value)); }
        } else {
            // Set the plugin parameters
            for (Pluggable p: plugins) {
                Properties p_props = new Properties();
                Enumeration prop_names = appProps.propertyNames();
                while (prop_names.hasMoreElements()) {
                    String prop_name = (String) prop_names.nextElement();
                    if (prop_name.toUpperCase().startsWith(p.getPluginName().toUpperCase())) {
                        p_props.setProperty(prop_name.replaceFirst(p.getPluginName().toUpperCase()+".", ""), appProps.getProperty(prop_name));
                    }
                }
                p.setParameters(p_props);
            }
        }
            
     }
     
     /**
      * Resets the program parameters to default values.
      */
     void restoreParameterValuesDefaults() {
        setLabelRadius(8);
        
        setLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_0, Color.CYAN);
        setLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_1, Color.RED);
        setLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_2, Color.RED.darker());
        setLabelsColor(TMALabel.LABEL_POS, TMALabel.STAINING_3, Color.RED.darker().darker());
        setLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_0, Color.GREEN.brighter());
        setLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_1, Color.GREEN);
        setLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_2, Color.GREEN.darker());
        setLabelsColor(TMALabel.LABEL_NEG, TMALabel.STAINING_3, Color.GREEN.darker());
        setLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_0, Color.GRAY.brighter());
        setLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_1, Color.GRAY);
        setLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_2, Color.GRAY.darker());
        setLabelsColor(TMALabel.LABEL_UNK, TMALabel.STAINING_3, Color.GRAY.darker());
        setLabelsColor(TMALabel.LABEL_BG, TMALabel.STAINING_0, Color.WHITE);
        od.setStoreParamsOnExit(true);
        od.setLabelsShape_Gst(LABELS_SHAPE_CIRCLE);
        od.setLabelsShape_Est(LABELS_SHAPE_CROSS);
        od.setAutomaticESGSConversion(true);
        od.setUseParallelProgramming(true);
        od.setUseLocalPlugins(false);
        od.setLocalPluginFolder(System.getProperty("user.dir") + File.separator + "plugins");
        
        bcd.setUseColor(true);
        
        
        // Reset the plugin parameters
        for (Pluggable p: plugins) {
            p.setParameterDefaults();
        }
              
     }
    
    
    /**
     * Saves the current TMARKER Analysis on hard disk.
     * @param file The TMARKER Analysis file to be stored.
     */
    public void saveAnalysis(File file) {
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            //objOut.writeObject(new Object[]{infoText, modelsCreated, combinedBiomarkersConstructed, nFilters});
            //objOut.writeObject(mm.getGene());
            //objOut.writeObject(mm.getSamples());
            //objOut.writeObject(mm.getCpGFilters());
            objOut.close(); 
        }
        catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, e.getMessage());
        }
    }
    
    /**
     * Checks online for updates and reportes to the user if there is one.
     * DOES NOT UPDATE TMARKER AUTOMATICALLY
     */
    public void checkForUpdates() {
        String thisRevision = tmarker.REVISION;
        String remoteRevision = "-1";
        try {
            WebConversation wc = new WebConversation();
            WebResponse resp = wc.getResponse("http://www.comp-path.inf.ethz.ch/tmarker/vnuc.txt");
            
            // output is website with version number
            String output = resp.getText();
            if (tmarker.DEBUG > 3) {
                logger.log(java.util.logging.Level.INFO, output);
            }
            
            BufferedReader br = new BufferedReader(new StringReader(output));
            String line = br.readLine().trim();
            while (br.ready() && line.equals("")) {
                line = br.readLine().trim();
            }
            remoteRevision = line;
            
        } catch (MalformedURLException ex) {
            //Logger.getLogger(Frontend_Frame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            //Logger.getLogger(Frontend_Frame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            // Logger.getLogger(Frontend_Frame.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (Exception ex) {
            // Logger.getLogger(Frontend_Frame.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            UpdateDialog.main(this, thisRevision, remoteRevision);
        }
    }

    /**
     * Returns the TMAspot with the given name.
     * @param name The name to be searched for.
     * @return The TMAspot which has the indicated name.
     */
    public TMAspot getTMAspotWithName(String name) {
        List<TMAspot> spots = getTMAspots();
        for (TMAspot ts: spots) {
            if (ts.getName().equals(name)) return ts;
        }
        return null;
    }
    
    /**
     * Opens the white balance (background correction) dialog. Thereafter, TMARKER is
     * in the "background correction mode".
     */
    public void openBgCorrectionDialog() {
        if (bcd == null) {
            bcd = new BgCorrectionDialog(this, false);
        }
        bcd.setVisible(true);
        bcd.toFront();
    }
    
    /**
     * Flips all TMApoints with label LABEL_UNK to a new given label.
     * @param t The current TMARKER session.
     * @param tss The TMAspots to be considered.
     * @param positive If true, the new label will be LABEL_POS. Otherwise LABEL_NEG.
     */
    public static void flipUNKto(tmarker t, List<TMAspot> tss, boolean positive) {
        for (TMAspot ts: tss) {
            List<TMApoint> tps = ts.getPoints(TMALabel.LABEL_UNK);
            for (TMApoint tp: tps) {
                tp.setLabel(positive ? TMALabel.LABEL_POS : TMALabel.LABEL_NEG);
            }
            if (t.getVisibleTMAspot() == ts) {
                t.getTMAView().repaint();
                t.getTSD().updateStainingEstimation(ts);
            }
            t.updateTMATable(ts);
        }
    }
    
    /**
     * Loads the available Plugins and lists them in the Plugins Menu.
     */
    void loadPlugins() {
        try {
            // Preserve Security; important for plugins to be available with the TMARKER webstart version
            System.setSecurityManager(new TMARKERSecurityManager());
            
            if (od.getUseLocalPlugins()) { // Local plugins in a folder (e.g. "/plugins")
                plugins = PluginLoader.loadPlugins(new File(od.getLocalPluginFolder()), Thread.currentThread().getContextClassLoader() );
            } else {
                // online plugins
                try {
                    plugins = PluginLoader.loadPlugins(new URL("https://people.inf.ethz.ch/peschuef/TMARKERDEV/plugins"), getTmpDir(), Thread.currentThread().getContextClassLoader());
                } catch (Exception e) {
                    
                }
            }
            
            if (DEBUG > 0) {
                logger.log(Level.INFO, plugins.size() + " plugin(s) found (step 1 of 2).");
            }
            
            PluginManager manager;
            manager = new TMARKERPluginManager(this);
            for (Pluggable p : plugins) {
                p.setPluginManager(manager);
            }
            
            // add the plugins into the menu
            for (int i = 0; i < plugins.size(); i++) {
                final Pluggable p = plugins.get(i);
                try {
                    JMenuItem mi = new JMenuItem(p.getPluginName(), (p.getIcon()!=null ? p.getIcon():new ImageIcon(getIconImage().getScaledInstance(16, 16, java.awt.Image.SCALE_DEFAULT))));
                    mi.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            p.actionPerformed(evt);
                        }
                    });
                    jMenu5.add(mi, jMenu5.getItemCount()-2);
                    p.start();
                } catch (Exception e) {
                    Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            //System.setSecurityManager(new TMARKERSecurityManager());
            
            if (DEBUG > 0) {
                logger.log(Level.INFO, (jMenu5.getItemCount()-2) + " plugin(s) loaded (step 2 of 2).");
            }
            
        } catch (IOException ex) {
            Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (java.lang.NoClassDefFoundError | Exception ex) {
            Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Error ex) {
            Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Returns all currently loaded plugins.
     * @return All currently loaded plugins.
     */
    public List<Pluggable> getPlugins() {
        return plugins;
    }
}

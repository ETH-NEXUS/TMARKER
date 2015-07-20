/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package survivalanalysis;

import java.awt.Frame;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import tmarker.TMAspot.TMAspot;
import tmarker.delaunay.ArraySet;

/**
 * A dialog to find a good threshold to binarise continuos variables. 
 * @author Peter J. Schueffler
 */
public class ThresholdFinderDialog extends javax.swing.JDialog {

    List<TMAspot> tss;
    String targetproperty;
    Double[] values;
    Double[] returnvalue = null;
    
    /**
     * Creates new form ThresholdFinderDialog
     * Creates new form ThresholdFinderDialog.
     * @param parent The parent frame, for displaying. Can be null.
     * @param targetproperty The property of the TMA spots which should be binarized.
     * @param tss The list of TMAspots which have values on this property and should be binarized.
     * @param threshold_init An optional initial threshold. can be null.
     * @param modal If true, the dialog is modal (default).
     */
    public ThresholdFinderDialog(java.awt.Frame parent, String targetproperty, List<TMAspot> tss, Double[] threshold_init, boolean modal) {
        super(parent, modal);
        initComponents();
        this.tss = tss;
        this.targetproperty = targetproperty;
        setup(targetproperty, tss, threshold_init);
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
        jLabel1 = new javax.swing.JLabel();
        jSlider1 = new javax.swing.JSlider();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        jSlider2 = new javax.swing.JSlider();
        jSlider3 = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Choose Threshold for Groups");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("<html> seems to be numeric.<br> Please select a threshold to separate the survival groups.</html>");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, new javax.swing.JCheckBox(), org.jdesktop.beansbinding.ELProperty.create("${!selected}"), jLabel1, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel1Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        getContentPane().add(jLabel1, gridBagConstraints);

        jSlider1.setPaintTicks(true);
        jSlider1.setSnapToTicks(true);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton1, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jSlider1, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jSlider1Binding");
        bindingGroup.addBinding(binding);

        jSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider1StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        getContentPane().add(jSlider1, gridBagConstraints);

        jButton1.setText("OK");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 10, 0);
        getContentPane().add(jButton1, gridBagConstraints);

        jButton2.setText("Cancel");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 5);
        getContentPane().add(jButton2, gridBagConstraints);

        jLabel3.setText("jLabel3");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton1, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel3, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel3Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        getContentPane().add(jLabel3, gridBagConstraints);

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("Two Groups:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(jRadioButton1, gridBagConstraints);

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText("Three Groups:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(jRadioButton2, gridBagConstraints);

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setText("Categorial:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        getContentPane().add(jRadioButton3, gridBagConstraints);

        jLabel4.setText("jLabel4");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton2, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel4, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel4Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        getContentPane().add(jLabel4, gridBagConstraints);

        jSlider2.setPaintTicks(true);
        jSlider2.setSnapToTicks(true);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton2, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jSlider2, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jSlider2Binding");
        bindingGroup.addBinding(binding);

        jSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider2StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSlider2, gridBagConstraints);

        jSlider3.setPaintTicks(true);
        jSlider3.setSnapToTicks(true);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton2, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jSlider3, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jSlider3Binding");
        bindingGroup.addBinding(binding);

        jSlider3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider3StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 6;
        getContentPane().add(jSlider3, gridBagConstraints);

        jLabel2.setText("t2=");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton2, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel2, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel2Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(jLabel2, gridBagConstraints);

        jLabel5.setText("t=");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton1, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel5, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel5Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(jLabel5, gridBagConstraints);

        jLabel6.setText("t1=");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton2, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel6, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel6Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(jLabel6, gridBagConstraints);

        jLabel7.setText("Treat this covariate as categorical rather than numeric.");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, jRadioButton3, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel7, org.jdesktop.beansbinding.BeanProperty.create("enabled"), "jLabel7Binding");
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(jLabel7, gridBagConstraints);

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider1StateChanged
        if (jSlider1.getValue() < values.length) {
            jSlider1.setToolTipText("t="+values[jSlider1.getValue()]);
            int nsmaller = 0;
            int nlarger = 0;
            double t = values[jSlider1.getValue()];
            Properties props;
            String value;
            double val;

            for (TMAspot ts:tss) {
                props = ts.getProperties();
                if (props!=null) {
                    value = (String) props.get(targetproperty);
                    if (value!=null) {
                        try {
                            val = Double.parseDouble(value.replaceAll(",", "."));
                            if (val < t) {
                                nsmaller++;
                            } else {
                                nlarger++;
                            }
                        } catch (Exception e) {

                        }
                    }
                }
            }

            jLabel3.setText(nsmaller + " obs.    <  " + t + "  <=    " + nlarger + " obs.");
        }
    }//GEN-LAST:event_jSlider1StateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        returnvalue = jRadioButton1.isSelected() ? new Double[]{values[jSlider1.getValue()]} : (jRadioButton2.isSelected() ? new Double[]{values[jSlider2.getValue()], values[jSlider3.getValue()]} : new Double[]{Double.NaN});
        this.dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        returnvalue = null;
        this.dispose();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider2StateChanged
        try {
            jSlider3.setValue(Math.max(jSlider2.getValue(), jSlider3.getValue()));
            if (jSlider2.getValue() < values.length) {
                jSlider2.setToolTipText("t1="+values[jSlider2.getValue()]);
                int nsmaller = 0;
                int nmiddle = 0;
                int nlarger = 0;
                double t1 = values[jSlider2.getValue()];
                double t2 = values[jSlider3.getValue()];
                Properties props;
                String value;
                double val;

                for (TMAspot ts:tss) {
                    props = ts.getProperties();
                    if (props!=null) {
                        value = (String) props.get(targetproperty);
                        if (value!=null) {
                            try {
                                val = Double.parseDouble(value.replaceAll(",", "."));
                                if (val < t1) {
                                    nsmaller++;
                                } else if (val < t2){
                                    nmiddle++;
                                } else {
                                    nlarger++;
                                }
                            } catch (Exception e) {

                            }
                        }
                    }
                }
                jLabel4.setText(nsmaller + " obs.    <  " + t1 + "  <=    " + nmiddle + " obs.    <  " + t2 + "  <=    " + nlarger + " obs.");
            }
        } catch (Exception e) {
        }
    }//GEN-LAST:event_jSlider2StateChanged

    private void jSlider3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider3StateChanged
        try {
            jSlider2.setValue(Math.min(jSlider2.getValue(), jSlider3.getValue()));
            if (jSlider3.getValue() < values.length) {
                jSlider3.setToolTipText("t2="+values[jSlider3.getValue()]);
                int nsmaller = 0;
                int nmiddle = 0;
                int nlarger = 0;
                double t1 = values[jSlider2.getValue()];
                double t2 = values[jSlider3.getValue()];
                Properties props;
                String value;
                double val;

                for (TMAspot ts:tss) {
                    props = ts.getProperties();
                    if (props!=null) {
                        value = (String) props.get(targetproperty);
                        if (value!=null) {
                            try {
                                val = Double.parseDouble(value.replaceAll(",", "."));
                                if (val < t1) {
                                    nsmaller++;
                                } else if (val < t2){
                                    nmiddle++;
                                } else {
                                    nlarger++;
                                }
                            } catch (Exception e) {

                            }
                        }
                    }
                }
                jLabel4.setText(nsmaller + " obs.    <  " + t1 + "  <=    " + nmiddle + " obs.    <  " + t2 + "  <=    " + nlarger + " obs.");
            }
        } catch (Exception e) {
            
        }
    }//GEN-LAST:event_jSlider3StateChanged

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JSlider jSlider2;
    private javax.swing.JSlider jSlider3;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    public void setup(String targetproperty, List<TMAspot> tss, Double[] threshold_init) {
        this.targetproperty = targetproperty;
        this.tss = tss;
        //double minval=Double.MAX_VALUE;
        //double maxval=0;
        String value;
        double val;
        Properties props;
        Set<Double> values = new ArraySet<>();
        for (TMAspot ts:tss) {
            props = ts.getProperties();
            if (props!=null) {
                value = (String) props.get(targetproperty);
                if (value!=null) {
                    try {
                        val = Double.parseDouble(value.replaceAll(",", "."));
                        //minval = Math.min(minval, val);
                        //maxval = Math.max(maxval, val);
                        values.add(val);
                    } catch (Exception e) {
                        
                    }
                }
            }
        }
        Double[] vals = values.toArray(new Double[0]);
        Arrays.sort(vals);
        this.values = vals;
        
        jLabel1.setText("<html><b><i>" + targetproperty + "</i></b> &nbsp;&nbsp;seems to be numeric.<br>" +
                    "Please choose a threshold to separate the survival groups.</html>");
        
        jSlider1.setMinimum(0);
        jSlider1.setMaximum(vals.length-1);
        jSlider1.setMajorTickSpacing(5);
        jSlider1.setMinorTickSpacing(1);
        jSlider1.setValue(vals.length/2);
        if (threshold_init!=null && threshold_init.length==1 && threshold_init[0]!=-1) {
            try {
                int ind = Arrays.asList(vals).indexOf(threshold_init[0]);
                if (ind>=0) {
                    jSlider1.setValue(ind);
                } 
            } catch (Exception e) {
            }
        }
        
        jSlider2.setMinimum(0);
        jSlider2.setMaximum(vals.length-1);
        jSlider2.setMajorTickSpacing(5);
        jSlider2.setMinorTickSpacing(1);
        jSlider2.setValue(vals.length/2);
        jSlider3.setMinimum(0);
        jSlider3.setMaximum(vals.length-1);
        jSlider3.setMajorTickSpacing(5);
        jSlider3.setMinorTickSpacing(1);
        jSlider3.setValue(vals.length/2);
        if (threshold_init!=null && threshold_init.length==2 && threshold_init[0]!=-1) {
            try {
                int ind1 = Arrays.asList(vals).indexOf(threshold_init[0]);
                int ind2 = Arrays.asList(vals).indexOf(threshold_init[1]);
                if (ind1>=0) {
                    jSlider2.setValue(ind1);
                }
                if (ind2>=0) {
                    jSlider3.setValue(ind2);
                }
            } catch (Exception e) {
            }
        }
        
        this.pack();
        this.validate();
        
        if (threshold_init!=null && threshold_init.length>0) {
            jRadioButton2.setSelected(threshold_init.length == 2);
            jRadioButton1.setSelected(threshold_init.length == 1);
            jRadioButton3.setSelected(Double.isNaN(threshold_init[0]));
        }
    }

		/**
     * Returns the precalculated threshold (which is one threshold for binarization,
     * two thresholds for three-split or null for no threshold.
     * @return The precalculated threshold, or null.
     */
    public Double[] getThreshold() {
        return returnvalue;
    }
    
    /**
     * Creates an instance of this dialog, and shows it on the screen.
     * @param parentComponent The parent frame, for displaying. Can be null.
     * @param targetproperty The property of the TMA spots which should be binarized.
     * @param tss The list of TMAspots which have values on this property and should be binarized.
     * @param threshold_init An optional initial threshold. can be null.
     * @return The threshold that the user selected.
     */
    public static Double[] showThresholdFinderDialog(Frame parentComponent, String targetproperty, List<TMAspot> tss, Double[] threshold_init) {
        ThresholdFinderDialog tfd = new ThresholdFinderDialog(parentComponent, targetproperty, tss, threshold_init, true);
        tfd.setLocationRelativeTo(parentComponent);
        tfd.setVisible(true);
        tfd.dispose();
        
        Double[] value = tfd.getThreshold();

        return value;
    }
}

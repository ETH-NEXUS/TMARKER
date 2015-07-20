/*
 * Copyright (C) 2015 peschuef
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package tmarker;

import com.itextpdf.text.Font;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

/**
 * The performanceOverviewPanel shows the current used memory, free memory and
 * maximum available memory. Using the program, you can follow the resources and
 * discover problematic data or procedures which occupy a lot of resources.
 * The monitoring is done in a separate thread.
 * @author Peter J. Schueffler
 */
public class PerformanceOverviewPanel extends javax.swing.JPanel {

    //ArrayList freeMem = new ArrayList();
    ArrayList totMem = new ArrayList();
    ArrayList usedMem = new ArrayList();
    int length = 60; // track the last 60 time points
    int updateTimems = 1000; // every second
    double max = 0; // for scaling in the panel during display
    int maxMem; // the max memory available (constant, given by the -xmx flag during program start)
    Thread performanceThread = null; // the thread for updating and painting the values
    boolean continueThread = true; // for correct interruption of the thread.
    
    /**
     * Creates new form PerformanceOverviewPanel and starts the monitoring process.
     */
    public PerformanceOverviewPanel() {
        initComponents();
        setPreferredSize(new Dimension(100, 50));
        
        for (int i = 0; i<length; i++) {
            //freeMem.add(0);
            totMem.add(0);
            usedMem.add(0);
            maxMem = (int)(Runtime.getRuntime().maxMemory()/1024/1024);
        }
        continueThread();
    }
    
    /**
     * Interrupts the monitoring process. The continueThread flag is set false, 
     * such that the current thread does not continue and terminates.
     */
    public void interrupt() {
        continueThread = false;
    }
    
    /**
     * Continues the monitoring. For this, a new Thread is created and started.
     * As long as the continueThread flag is true, it updates the memory values, 
     * repaints, and sleeps for the given updateTime.
     */
    public void continueThread() {
        continueThread = true;
        performanceThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (continueThread) {
                        updateValues();
                        if (isVisible()) {
                            repaint();
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {

                        }
                    }
                }
            });
        performanceThread.start();
    }
    
    /**
     * Adds the current memory values (free mem, tot mem) and updates the maximum
     * of these for scaling during display.
     */
    public void updateValues() {
        //freeMem.remove(0);
        //freeMem.add((int)(Runtime.getRuntime().freeMemory()/1024/1024));
        totMem.remove(0);
        int totm = (int)(Runtime.getRuntime().totalMemory()/1024/1024);
        totMem.add(totm);
        usedMem.remove(0);
        int usedm = (int) (totm - Runtime.getRuntime().freeMemory()/1024/1024);
        usedMem.add(usedm);
        max = Math.max(max, totm);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(0, 0, 0));
        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(g.getFont().deriveFont(Font.BOLD));
        
        
        double dx = 1.0*getWidth()/(length); // distance between two values on x axis.
        double factor = getHeight()/(max+20); // scale factor for values on y axis (not to be higher than the panel height).
        
        // draw the free memory
        //g.setColor(Color.GREEN);
        //for (int i=1; i<length; i++) {
        //    g.drawLine((int)((i-1)*dx), getHeight()-(int)(factor*(int)(freeMem.get(i-1))), (int)(i*dx), getHeight()-(int)(factor*(int)(freeMem.get(i))));
        //}
        //g.drawString("Free " + freeMem.get(freeMem.size()-1) + " MB", 95, getHeight() -5);

        // draw the total memory used so far
        g.setColor(Color.GREEN);
        for (int i=1; i<length; i++) {
            g.drawLine((int)((i-1)*dx), getHeight()-(int)(factor*(int)(totMem.get(i-1))), (int)(i*dx), getHeight()-(int)(factor*(int)(totMem.get(i))));
        }
        g.drawString("Tot " + totMem.get(totMem.size()-1) + " MB", 185, getHeight() -5);
        
        // draw the used memory
        g.setColor(Color.YELLOW);
        for (int i=1; i<length; i++) {
            g.drawLine((int)((i-1)*dx), getHeight()-(int)(factor*(int)(usedMem.get(i-1))), (int)(i*dx), getHeight()-(int)(factor*(int)(usedMem.get(i))));
        }
        g.drawString("Used " + usedMem.get(usedMem.size()-1) + " MB", 95, getHeight() -5);

        // draw the max memory (constant, given by -xmx flag on start of the program)
        g.setColor(Color.RED);
        g.drawString("Max " + maxMem + " MB", 5, getHeight() -5);

    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
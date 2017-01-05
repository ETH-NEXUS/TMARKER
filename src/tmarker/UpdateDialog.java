/*
 * UpdateDialog.java
 *
 * Created on October 22, 2008, 1:41 PM
 */

package tmarker;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.xml.sax.SAXException;
import tmarker.misc.Download;
import static tmarker.tmarker.logger;

/**
 * The Window that pops up if the user choses to update TMARKER.
 * @author Peter Schueffler
 */
public class UpdateDialog extends javax.swing.JDialog {
    
    boolean isOutOfDate = false;
    String remoteRevision;
    tmarker t;
    
    /**
     * Opens a new UpdateDialog and sets the text an buttons according to the current revision of the program.
     * @param parent The parent frame for this dialog box.
     * @param modal True, if this dialog should be modal.
     */
     public UpdateDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        t = ((tmarker)parent);
        initComponents();
     }
    
    /**
     * Sets the Buttons and writes the text according to the versions that are locally and remotely available.
     * @param thisRevision The local version of TMARKER.
     * @param remoteRevision The remote version of TMARKER.
     */
    private void setButtons(String thisRevision, String remoteRevision) {
        String thisRevision_tmp = thisRevision;
        String remoteRevision_tmp = remoteRevision;
        
        if (remoteRevision == null) {
            jLabel1.setVisible(false);
            jLabel2.setVisible(false);
            jButton1.setVisible(false);
            jLabel3.setVisible(true);
            jProgressBar1.setVisible(false);
            jButton3.setVisible(true);
            jProgressBar1.setEnabled(false);
            return;
        }
        
        
        while (thisRevision_tmp.length()>remoteRevision_tmp.length()) {
            remoteRevision_tmp = remoteRevision_tmp.replaceFirst("\\.", "\\.0");
        }
        while (remoteRevision_tmp.length()>thisRevision_tmp.length()) {
            thisRevision_tmp = thisRevision_tmp.replaceFirst("\\.", "\\.0");
        }
        int thisRevisionInt = Integer.parseInt(thisRevision_tmp.replaceAll("\\.", "").replaceAll("'", ""));
        int remoteRevisionInt = Integer.parseInt(remoteRevision_tmp.replaceAll("\\.", "").replaceAll("'", ""));
        isOutOfDate = remoteRevisionInt > thisRevisionInt;
        boolean connectionFailure = remoteRevisionInt < 0;
        if (connectionFailure) {
            jLabel4.setText("");
            jLabel5.setText("");
        }
        else {
            jLabel4.setText("Local Version: TMARKER v" + thisRevision);
            jLabel5.setText("Online Version: TMARKER v" + remoteRevision);
        }
        jLabel1.setVisible(!connectionFailure && !isOutOfDate);
        jLabel2.setVisible(!connectionFailure && isOutOfDate);
        jButton1.setVisible(!connectionFailure && isOutOfDate);
        jLabel3.setVisible(connectionFailure);
        jProgressBar1.setVisible(!connectionFailure && isOutOfDate);
        jButton3.setVisible(!connectionFailure && isOutOfDate);
        jProgressBar1.setEnabled(false);
    }
    
    /**
     * Workhorse function. Opens a connection to the new TMARKER zip File, downloads it and extracts it in the current program folder. The new update is
     * then used with the next program start of TMARKER.
     */
    public void downloadAndExtractUpdates() {
        
        try {
            
            jButton1.setEnabled(false);
            jLabel6.setText("Downloading...");
            
            //// download the new zipfile
            final Download download = new Download(new URL("http://www.ethz.ch/content/dam/ethz/special-interest/dual/nexus-dam/software/TMARKER/TMARKERv" + remoteRevision + ".zip"), t.getProgramFolder());
            
            Thread downloadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    jProgressBar1.setEnabled(true);
                    while(download.getStatus()==Download.DOWNLOADING || download.getStatus()==Download.PAUSED) {
                        if (download.getStatus()==Download.DOWNLOADING) {
                            jProgressBar1.setValue((int) download.getProgress()/2);
                        }
                    }
                }
            });
            downloadThread.start();
            
            download.getThread().join();
            
            jLabel6.setText("Extracting...");
            
            File zipFile = new File(t.getProgramFolder()+ File.separator + download.getFileName(new URL(download.getUrl())));
            
            //// extract the downloaded zip file into the main folder
            String outputFolder = t.getProgramFolder();
            byte[] buffer = new byte[1024];
                
            //create output directory if not exists
            File folder = new File(outputFolder);
            if(!folder.exists()){
                folder.mkdir();
            }

            //get the zip file content
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(fis);
            
            // Count number of files in zip
            int numFiles = 0;
            while (zis.getNextEntry() != null) {
                numFiles++;
            }
            zis.close();
            fis.close();
            
            //get the zip file content
            fis = new FileInputStream(zipFile);
            zis = new ZipInputStream(fis);
            
            //get the zipped file list entry
            ZipEntry ze; // = zis.getNextEntry();
            
            String rootFolder = "TMARKER";
            int i = 0;
            while((ze = zis.getNextEntry()) != null) {
                
                String fileName = ze.getName().replaceFirst(rootFolder, ".");
                File newFile = new File(outputFolder + File.separator + fileName);
                //newFile.deleteOnExit();

                Logger.getLogger(UpdateDialog.class.getName()).log(Level.INFO, "file unzip : {0}", newFile.getAbsolutePath());

                String sep  = "/" ;
                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                if (fileName.endsWith(sep) || fileName.endsWith(File.separator)) { 
                    new File(newFile.getPath()).mkdir();
                } else {
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                jProgressBar1.setValue(50 + 50*++i/numFiles);
            }
            zis.closeEntry();
            zis.close();
            fis.close();
            zipFile.delete();

            Logger.getLogger(UpdateDialog.class.getName()).log(Level.INFO, "file unzip done.");
            
            jButton1.setEnabled(true);
            jLabel6.setText("Update successful. Please restart TMARKER.");
            jButton2.setText("Restart");
            
            
            isOutOfDate = false;
        } catch(IOException | InterruptedException ex){
            jButton1.setEnabled(true);
            jLabel6.setText("Update NOT successful. " + ex.getMessage());
            Logger.getLogger(UpdateDialog.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }
    
    /**
     * Opens a new Thread which performs downloadAndExtractUpdate();
     */
    private void downloadUpdateInNewThread() {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                downloadAndExtractUpdates();
            }
        });
        thread.start();
    }

    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jButton3 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();

        setTitle("TMARKER Update Check");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("You are using the latest version of TMARKER. Enjoy!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 5, 20);
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel2.setText("A new TMARKER version is available. Please load the latest version.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 5, 20);
        getContentPane().add(jLabel2, gridBagConstraints);

        jButton1.setText("Download new Version");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 20, 5, 20);
        getContentPane().add(jButton1, gridBagConstraints);

        jButton2.setText("Close");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 10, 20);
        getContentPane().add(jButton2, gridBagConstraints);

        jLabel3.setText("Update check failed. No internet connection?");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 10, 20);
        getContentPane().add(jLabel3, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        getContentPane().add(jLabel4, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        getContentPane().add(jLabel5, gridBagConstraints);

        jProgressBar1.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
        getContentPane().add(jProgressBar1, gridBagConstraints);

        jButton3.setText("Go to TMARKER Homepage");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 10, 20);
        getContentPane().add(jButton3, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 25, 0, 20);
        getContentPane().add(jLabel6, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        downloadUpdateInNewThread();       
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        if (jButton2.getText().equals("Close")) {
            this.dispose();
        } else {
            try {
                String cmd = "java -jar TMARKER.jar -w 5000";
                Runtime.getRuntime().exec(cmd);
                System.exit(0);
            } catch (IOException ex) {
                Logger.getLogger(UpdateDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        try {
            BrowserLauncher bl = new BrowserLauncher();
            bl.openURLinBrowser("http://www.nexus.ethz.ch");
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Trying to open http://www.nexus.ethz.ch/#page=Home");
        } catch (BrowserLaunchingInitializingException | UnsupportedOperatingSystemException ex) {
            Logger.getLogger(UpdateDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton3ActionPerformed
    
    /**
     * Opens a new UpdateDialog and sets the text an buttons according to the current revision of the program.
     * @param parent The parent frame for this dialog box.
     * @param thisRevision The current local revision (version) of this program.
     * @param remoteRevision The remote revision (version) of the program.
     * @param verbose If true, the result will be displayed in any case. If false
     * the result will be displayed only if this TMARKER version is out of date.
     * @param installAutomatically If true (and verbose is false), an update will be installed automatically. Else,
     * the user would be asked.
     */
    public void main(final JFrame parent, final String thisRevision, final String remoteRevision, final boolean verbose, final boolean installAutomatically) {
        this.remoteRevision = remoteRevision;
        setButtons(thisRevision, remoteRevision);
        pack();
        setLocationRelativeTo(parent);
        if (verbose) {
            setVisible(true);
        } else {
            if (installAutomatically && isOutOfDate) {
                downloadUpdateInNewThread();
            } else if (isOutOfDate) {
                setVisible(true);
            }
        }
    }
    
    /**
     * Checks online for updates and reports to the user if there is one. DOES
     * NOT UPDATE TMARKER AUTOMATICALLY
     *
     * @param verbose If true, the result will be displayed in any case. If
     * false the result will be displayed only if this TMARKER version is out of
     * date.
     * @param installAutomatically If true, an update will be installed
     * automatically. Else, the user would be asked.
     */
    public void checkForUpdates(final boolean verbose, final boolean installAutomatically) {
        final String thisRevision = tmarker.REVISION;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
            String remoteRevision_ = null;
            try {
                WebConversation wc = new WebConversation();
                WebResponse resp = wc.getResponse("http://www.ethz.ch/content/dam/ethz/special-interest/dual/nexus-dam/software/TMARKER/vnuc.txt");
                
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
                remoteRevision_ = line;
            } catch (MalformedURLException ex) {
                //Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                //Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SAXException ex) {
                //Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                //Logger.getLogger(tmarker.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                main(t, thisRevision, remoteRevision_, verbose, installAutomatically);
            }
            }
        });
            
        thread.start();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JProgressBar jProgressBar1;
    // End of variables declaration//GEN-END:variables

    
}

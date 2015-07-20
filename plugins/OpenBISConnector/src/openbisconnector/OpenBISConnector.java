/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package openbisconnector;

import TMARKERPluginInterface.PluginManager;
import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;
import common.config.Config;
import importer.common.ui.FileChooserTree;
import importer.tmarker.TmarkerFacade;
import importer.tmarker.TmarkerFileChooserTree;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/**
 * The OpenBISConnector is the main openBIS connector plugin class for TMARKER.
 * @author Peter J. Schueffler
 */
public class OpenBISConnector implements TMARKERPluginInterface.Pluggable {

    private PluginManager pluginmanager = null;

    private Config inConfig = null;

    private openBISLoginDialog old = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        OpenBISConnector obc = new OpenBISConnector();
        obc.actionPerformed(null);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void setPluginManager(PluginManager manager) {
        this.pluginmanager = manager;
    }

    @Override
    public Icon getIcon() {
        try {
            return new javax.swing.ImageIcon(getClass().getResource("/openbisconnector/openBIS.png"));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getPluginName() {
        return "OpenBIS Downloader";
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        inConfig = new Config(String.class);
        
        if (old == null) {
            old = new openBISLoginDialog(null, true);
        }
        old.setVisible(true);

        if (old.getReturnStatus() == openBISLoginDialog.RET_OK) {

            inConfig.setUrl(old.getURL());
            inConfig.setUser(old.getUsername());
            inConfig.setPassword(old.getPassword());
            inConfig.setTimeout(old.getTimeoutMS());
            inConfig.setWorkspace(".");

            //IOpenbisServiceFacade service = OpenbisServiceFacade.tryCreate(inConfig.getUser(), inConfig.getPassword(), inConfig.getUrl(), inConfig.getTimeout());

            TmarkerFacade facade = new TmarkerFacade(inConfig, this);

            while (facade.getService() == null && old.getReturnStatus() == openBISLoginDialog.RET_OK) {
                old.setVisible(true);
                if (old.getReturnStatus() == openBISLoginDialog.RET_OK) {
                    inConfig.setUrl(old.getURL());
                    inConfig.setUser(old.getUsername());
                    inConfig.setPassword(old.getPassword());
                    inConfig.setTimeout(old.getTimeoutMS());
                    inConfig.setWorkspace(".");
                    facade = new TmarkerFacade(inConfig, this);
                } else {
                    inConfig.setPassword("");
                    return;
                }
            }
            
            JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker("TMARKER","UA-61194283-1");
            FocusPoint focusPoint = new FocusPoint("OpenBISDownloaderUsage");
            tracker.trackAsynchronously(focusPoint);
            
            final FileChooserTree treeChooser = new TmarkerFileChooserTree(inConfig, facade);
            inConfig.setPassword("");
            treeChooser.init();

            final JScrollPane jScrollPanel = new JScrollPane(treeChooser);
            JFrame frame = new JFrame("Choose a data set in openBis");
            frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            //frame.setIconImage(old.getIconImages().get(0).getScaledInstance(-1, -1, Image.SCALE_DEFAULT));
            frame.setLayout(new BorderLayout());
            frame.add(jScrollPanel);
            frame.setSize(new Dimension(400, 300));
            frame.setVisible(true);
        }
    }

    @Override
    public void setParameterDefaults() {

    }

    @Override
    public void setParameters(Properties parameters) {

    }

    @Override
    public Properties getParameters() {
        return null;
    }

    @Override
    public String getHTMLReport(String HTMLFolderName) {
        String output = "<html>";
        char linebreak = '\n';

        if (inConfig != null) {
            output += "openBIS connection to " + inConfig.getUrl();
        }

        output += "</html>";
        return output;
    }

    @Override
    public void updateOptionsToTMAspot(tmarker.TMAspot.TMAspot visible_TMAspot, List<tmarker.TMAspot.TMAspot> selected_TMAspots) {

    }

    @Override
    public void drawInformationPreNuclei(tmarker.TMAspot.TMAspot ts, Graphics g, double z, int x_min, int y_min, int x_max, int y_max) {

    }

    @Override
    public void drawInformationPostNuclei(tmarker.TMAspot.TMAspot ts, Graphics g, double z, int x_min, int y_min, int x_max, int y_max) {

    }

    @Override
    public BufferedImage showAlternativeImage(tmarker.TMAspot.TMAspot ts) {
        return null;
    }
    
    /**
     * Returns the PluginManager.
     * @return The PluginManager.
     */
    public PluginManager getPluginManager() {
        return pluginmanager;
    }

}
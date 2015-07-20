/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package openbisconnector;

import TMARKERPluginInterface.PluginManager;
import ch.systemsx.cisd.openbis.dss.client.api.v1.IOpenbisServiceFacade;
import ch.systemsx.cisd.openbis.dss.client.api.v1.impl.OpenbisServiceFacade;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.NewDataSetDTO;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.NewDataSetDTOBuilder;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.NewDataSetMetadataDTO;
import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import common.config.Config;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import plugins.TMARKERPluginManager;
import tmarker.TMAspot.TMApoint;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;
import static tmarker.tmarker.logger;

/**
 * The OpenBISUploader is the main openBIS connector plugin class for TMARKER.
 * @author Peter J. Schueffler
 */
public class OpenBISUploader implements TMARKERPluginInterface.Pluggable {

    private PluginManager pluginmanager = null;

    private Config inConfig = null;

    private openBISUploadDialog oud = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        OpenBISUploader obc = new OpenBISUploader();
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
        return "OpenBIS Uploader";
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (pluginmanager.getTMAspots().isEmpty()) {
            JOptionPane.showMessageDialog(null, "There are no images to be uploaded.", "No images loaded", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        inConfig = new Config(String.class);
        
        if (oud == null) {
            oud = new openBISUploadDialog(null, true);
        }
        oud.setVisible(true);

        if (oud.getReturnStatus() == openBISLoginDialog.RET_OK) {
            
            JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker("TMARKER","UA-61194283-1");
            FocusPoint focusPoint = new FocusPoint("OpenBISUploaderUsage");
            tracker.trackAsynchronously(focusPoint);

            inConfig.setUrl(oud.getURL());
            inConfig.setUser(oud.getUsername());
            inConfig.setPassword(oud.getPassword());
            inConfig.setTimeout(oud.getTimeoutMS());
            inConfig.setWorkspace(".");

            if (pluginmanager!=null) {
                IOpenbisServiceFacade facade = null;
                try
                {
                    oud.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    facade = OpenbisServiceFacade.tryCreate(oud.getUsername(), oud.getPassword(),
                            (oud.getURL()==null || oud.getURL().isEmpty() ? "https://nexus-openbis.ethz.ch" : oud.getURL()), 
                            (oud.getTimeoutMS()<=0? 10000 : oud.getTimeoutMS()));
                    
                    tmarker t = ((TMARKERPluginManager)pluginmanager).getTMARKER();
                    Date date = new Date();
                    // create a temp xml file
                    File xmlFile = new File(pluginmanager.getTmpDir() + File.separator + "TMARKER_" + date.toString().replaceAll(" ", "-").replaceAll(":", "-") + ".xml");
                    
                    // Save TMARKER with the TMAspot to XML
                    boolean success = saveAsXML(t, xmlFile);
                    
                    if (success) {
                        // Upload the XML File
                        uploadFile(xmlFile, t.getUID().toString(), xmlFile.getName(), oud.getDisease(), oud.getStainingType(), oud.getDescription(), oud.getSpace(), oud.getProject(), oud.getExperiment(), facade);

                        // Delete the local XML File
                        xmlFile.delete();
                        
                        // Inform user
                        String message = "TMARKER settings and image files have been uploaded to OpenBIS:\n"
                                + oud.getSpace() + "/" + oud.getProject() + "/" + oud.getExperiment() + ".";
                        JOptionPane.showMessageDialog(t, message, "TMARKER successfully uploaded", JOptionPane.INFORMATION_MESSAGE);
                        logger.log(java.util.logging.Level.INFO, message);
                    }
                    
                } catch (Exception e) {
                    // Inform user
                    String message = "An error occured during the OpenBIS upload:\n"
                            + e.getMessage();
                    JOptionPane.showMessageDialog(null, message, "TMARKER successfully uploaded", JOptionPane.ERROR_MESSAGE);
                    logger.log(java.util.logging.Level.WARNING, e.getMessage());
                } finally
                {
                    if (facade != null)
                    {
                        facade.logout();
                    }
                    oud.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
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
    public void updateOptionsToTMAspot(TMAspot visible_TMAspot, List<TMAspot> selected_TMAspots) {

    }

    @Override
    public void drawInformationPreNuclei(TMAspot ts, Graphics g, double z, int x_min, int y_min, int x_max, int y_max) {

    }

    @Override
    public void drawInformationPostNuclei(TMAspot ts, Graphics g, double z, int x_min, int y_min, int x_max, int y_max) {

    }

    @Override
    public BufferedImage showAlternativeImage(TMAspot ts) {
        return null;
    }
    
    /**
     * Returns the PluginManager.
     * @return The PluginManager.
     */
    public PluginManager getPluginManager() {
        return pluginmanager;
    }
    
    
    
    /**
     * Puts a file into openBIS. The file must have several Metadata (Descriptors). Also the location in OpenBIS must be specified.
     * @param file The file to be uploaded.
     * @param id Descriptor: The ID.
     * @param file_name Descriptor: File name.
     * @param disease Descripor: Disease.
     * @param stainingType Descriptor: Staining Type.
     * @param description Descriptor: Description.
     * @param space Location: The OpenBIS space.
     * @param project Location: The OpenBIS project.
     * @param experiment Location: The OpenBIS experiment.
     * @param facade The OpenBIS connection facade.
     * 
     */
    public void uploadFile(File file, String id, String file_name, String disease, String stainingType, String description, String space, String project, String experiment, IOpenbisServiceFacade facade) {
        if (file !=null) {
            
            NewDataSetDTOBuilder builder = new NewDataSetDTOBuilder();
            builder.setDataSetOwnerType(NewDataSetDTO.DataSetOwnerType.EXPERIMENT);
            builder.setDataSetOwnerIdentifier("/" + space + "/" + project + "/" + experiment);
            builder.setFile(file);

            NewDataSetMetadataDTO dataSetMetadata = builder.getDataSetMetadata();
            dataSetMetadata.setDataSetTypeOrNull("TMARKER");

            Map<String, String> map = new HashMap<String, String>();
            map.put("TMARKER_ID", id);
            map.put("TMARKER_FILE_NAME", file_name);
            map.put("TMARKER_DISEASE", disease);
            map.put("TMARKER_STAINING_TYPE", stainingType);
            map.put("TMARKER_DESCRIPTION", description);
            dataSetMetadata.setProperties(map);

            NewDataSetDTO dataSetDTO = builder.asNewDataSetDTO();

            try
            {
                facade.putDataSet(dataSetDTO, file);
            } catch (Exception e) {
                
            }
        }
    }
    
    boolean saveAsXML(tmarker t, File file) {
        if (file == null) {
            return false;
        }
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("TMAspot", TMAspot.class);
        xstream.alias("TMApoint", TMApoint.class);
        xstream.omitField(TMAspot.class, "tc");
        xstream.omitField(TMAspot.class, "tlp");
        xstream.omitField(TMAspot.class, "os");
        xstream.omitField(TMApoint.class, "sp");

        try {
            ObjectOutputStream out = xstream.createObjectOutputStream(new BufferedWriter(new FileWriter(file)));
            out.writeObject(t.getUID()); // write Session ID
            out.writeObject(t.getParameterValues()); // write properties
            List<TMAspot> tss = t.getTMAspots();
            for (int i = 0; i < tss.size(); i++) {
                TMAspot ts = tss.get(i);
                t.setStatusMessageLabel("Write " + ts.getName() + " to XML ...");
                t.setProgressbar((int) ((100.0 * i) / tss.size()));
                out.writeObject(ts);
            }

            out.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(t, "An error occurred while writing "
                    + file.getName() + "\nMaybe it's still in use.", "Could not save file", JOptionPane.ERROR_MESSAGE);
            logger.log(java.util.logging.Level.WARNING, ex.getMessage());
            return false;
        } finally {
            t.setProgressbar(0);
        }
        return true;
    }

}
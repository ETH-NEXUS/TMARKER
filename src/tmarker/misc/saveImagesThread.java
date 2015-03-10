/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker.misc;

import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import tmarker.tmarker;

/**
 * A thread to save a list of temporary images. They are deleted on program exit. This might be done as thread since writing on the harddisk takes a lot of time.
 * @author Peter J. Schueffler
 */
public class saveImagesThread extends Thread {
    
    List<ImagePlus> imgs = new ArrayList<>();
    String name = "";
    String suffix = "";
    String format = "JPG";
    String foldername = "";
    
    /**
     * Creates a new Thread to save given images.
     * @param imgs The images to be saved.
     * @param format The output format (e.g. "jpg").
     * @param foldername The folder in which the images are saved.
     * @param name A name prefix.
     * @param suffix A name suffix (before the extension).
     */
    public saveImagesThread(List<ImagePlus> imgs, String format, String foldername, String name, String suffix) {
        this.imgs = imgs;
        this.format = format;
        this.foldername = foldername;
        this.name=name;
        this.suffix=suffix;
    }
    
    @Override
    public void run() {
        try {
            for (int i=0; i<imgs.size(); i++) {
                File file = new File(foldername + ((name==null||name.equals(""))?"channel":name) + Integer.toString(i+1) + suffix + "." + format.toLowerCase());
                file.deleteOnExit();
                ImageIO.write(imgs.get(i).getBufferedImage(), format, file);   
                imgs.get(i).flush();
            }
        } catch (IOException ex) {
            if (tmarker.DEBUG>0) {
                Logger.getLogger(saveImagesThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
    }
    
}

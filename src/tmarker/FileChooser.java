/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author Peter J. Schueffler
 */
public class FileChooser {
    
    /**
     * Opens a FileChooser Dialog and lets the user select (multiple) files.
     * @param t The parent frame on which the FileChooser is displayed.
     * @param currentDir The current directory which is opened by the FileChooser.
     * @return A list of files that the user selected. Null if the user cancelled.
     */
    public static File[] chooseLoadingFiles(JFrame t, String currentDir) {
        File[] fileArray = null;
    	
        // Current Directory 
        if (currentDir.equals("")) {
            File currentFile = new File(".");
            currentDir = currentFile.getAbsolutePath();
            // currentDir = t.getProgramFolder();
        }
                        
        // Choose headline
        String headline = "Open Files...";
        
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        
        // add filefilters
        ExampleFileFilter filter3 = new ExampleFileFilter();
        filter3.addExtension("tma");
        filter3.setDescription("Classifier file");
        chooser.addChoosableFileFilter(filter3);
        ExampleFileFilter filter2 = new ExampleFileFilter();
        filter2.addExtension("xml");
        filter2.setDescription("Label file");
        chooser.addChoosableFileFilter(filter2);
        ExampleFileFilter filter4 = new ExampleFileFilter();
        filter4.addExtension("csv");
        filter4.setDescription("csv file");
        chooser.addChoosableFileFilter(filter4);
        ExampleFileFilter filter1 = new ExampleFileFilter();
        String[] its = ImageIO.getReaderFormatNames();
        for (String it : its) {
            filter1.addExtension(it);
        }
        filter1.setDescription("Image files");
        chooser.addChoosableFileFilter(filter1);
        if (System.getProperty("os.name").startsWith("Windows")) {
            ExampleFileFilter filter5 = new ExampleFileFilter();
            filter5.addExtension("ndpi");
            filter5.setDescription("NDPI file");
            chooser.addChoosableFileFilter(filter5);
        }
        
        chooser.setCurrentDirectory(new File(currentDir));
        chooser.setDialogTitle(headline);
        int returnVal = chooser.showOpenDialog(t);        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileArray = chooser.getSelectedFiles();
            for (int i=0, n=fileArray.length; i<n; i++) {
                if (!fileArray[i].exists()) {
                    fileArray[i]=null;
                }
            }
            if (fileArray[0] != null) currentDir = fileArray[0].getAbsolutePath();
        } 
        return fileArray;
    }
    
    /**
     * Opens a FileChooser Dialog and lets the user select a single file.
     * @param t The parent frame on which the FileChooser is displayed.
     * @param currentDir The current directory which is opened by the FileChooser.
     * @param extensions Specify the extension that should be allowed to select.
     * @param descriptions Specify the extension descriptions for allowed files.
     * @return A single file that the user selected. Null if the user cancelled.
     */
    public static File chooseLoadingFile(JFrame t, String currentDir, List<String> extensions, List<String> descriptions) {
        File file = null;
    	
        // Current Directory 
        if (currentDir.equals("")) {
            File currentFile = new File(".");
            currentDir = currentFile.getAbsolutePath();
            // currentDir = t.getProgramFolder();
        }
                        
        // Choose headline
        String headline = "Open File...";
        JFileChooser chooser = new JFileChooser();
        
        for (int i=0; i<extensions.size(); i++) {
            ExampleFileFilter filter = new ExampleFileFilter();
            filter.addExtension(extensions.get(i));
            if (descriptions.size()>i) {
                filter.setDescription(descriptions.get(i));
            }
            chooser.addChoosableFileFilter(filter);
            if (i==0) chooser.setFileFilter(filter);
        }
        chooser.setDialogTitle(headline);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setCurrentDirectory(new File(currentDir));
        int returnVal = chooser.showOpenDialog(t);        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            // import Image or xml File
            file = chooser.getSelectedFile();
            if (!file.exists()) {
                file=null;
            }
            if (file != null) currentDir = file.getAbsolutePath();
        } 
        return file;
    }

    /**
     * Opens a FileChooser Dialog and lets the user select a single saving file.
     * @param t The parent frame on which the FileChooser is displayed.
     * @param currentDir The current directory which is opened by the FileChooser.
     * @param standardFilename The default filename.
     * @return A single file that the user selected. Null if the user cancelled.
     */
    public static File chooseSavingFile (JFrame t, String currentDir, String standardFilename) {
        List<String> extensions = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        //extensions.add("pdf");
        //descriptions.add("PDF file for a summary of the analysis");
        extensions.add("html");
        descriptions.add("HTML file for a summary of the analysis");
        extensions.add("xml");
        descriptions.add("XML file for selected images with their nuclei and properties");
        extensions.add("csv");
        descriptions.add("CSV file for selected images with their nuclei");
        
        return chooseSavingFile(t, currentDir, standardFilename, extensions, descriptions);
   }
    
    /**
     * Opens a FileChooser Dialog and lets the user select a single saving file.
     * @param t The parent frame on which the FileChooser is displayed.
     * @param currentDir The current directory which is opened by the FileChooser.
     * @param standardFilename The default filename.
     * @param extensions Specify the extension that should be allowed to select.
     * @param descriptions Specify the extension descriptions for allowed files.
     * @return A single file that the user selected. Null if the user cancelled.
     */
    public static File chooseSavingFile (JFrame t, String currentDir, String standardFilename, List<String> extensions, List<String> descriptions) {
        File savfile = null;
        
        // Current Directory 
        if (currentDir.equals("")) {
            File currentFile = new File(".");
            try {
                currentDir = currentFile.getCanonicalPath();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // OR
            // t.getProgramFolder();
        }
                        
        // headline
        String headline = "Please determine an output file";
        
        JFileChooser chooser = new JFileChooser(currentDir);
        
        for (int i=0; i<extensions.size(); i++) {
            ExampleFileFilter filter = new ExampleFileFilter();
            filter.addExtension(extensions.get(i));
            if (descriptions.size()>i) {
                filter.setDescription(descriptions.get(i));
            }
            chooser.addChoosableFileFilter(filter);
            if (i==0) chooser.setFileFilter(filter);
        }
        chooser.setDialogTitle(headline);
        chooser.setSelectedFile(new File(standardFilename));
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        int returnVal = chooser.showSaveDialog(t);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            // import HTML File
            savfile = chooser.getSelectedFile();
            if (savfile != null) {
                currentDir = savfile.getParent();
            }
            for (int i=0; i<extensions.size(); i++) {
                if (chooser.getFileFilter() == chooser.getChoosableFileFilters()[i+1]) {
                    if (!savfile.getName().endsWith("." + extensions.get(i))) {
                        savfile = new File(savfile.getAbsolutePath() + "." + extensions.get(i));
                    }
                    break;
                }
            }
            if (isWritable(t, savfile)) {
                return savfile;
            }
            else {
                return chooseSavingFile(t, currentDir, standardFilename, extensions, descriptions);
            }
      }
      return null; 
    }
    
    /**
     * Opens a FileChooser Dialog and lets the user select a single Folder.
     * @param t The parent frame on which the FileChooser is displayed.
     * @param currentDir The current directory which is opened by the FileChooser.
     * @return A single file that the user selected. Null if the user cancelled.
     */
    public static File chooseSavingFolder (JFrame t, String currentDir) {
        // Current Directory 
        if (currentDir.equals("")) {
            File currentFile = new File(".");
            try {
                currentDir = currentFile.getCanonicalPath();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // OR
            // t.getProgramFolder();
        }
                        
        // headline
        String headline = "Please determine an output folder";
        
        JFileChooser chooser = new JFileChooser(currentDir);
        chooser.setDialogTitle(headline);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showSaveDialog(t);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            // import HTML File
            return chooser.getSelectedFile();
      }
      return null; 
    }
    
    /**
     * Choose a CSV file to be opend by TMARKER.
     * @param t The parent frame in which the dialog should be opened.
     * @param currentDir The current directory that should be displayed as default.
     * @return A CSV file that the user selected to open. Null, if the dialog was cancelled.
     */
    public static File chooseCSVFile(JFrame t, String currentDir) {
        List<String> exts = new ArrayList<>(1); exts.add("csv");
        List<String> descs = new ArrayList<>(1); descs.add("Semicolon separated files");
        return chooseLoadingFile(t, currentDir, exts, descs);
    }
    
    /**
     * Checks if the given file already exists on the harddisk. If so, the user is asked if it can be overwritten.
     * @param parent The parent frame on which a question dialog might be displayed if the file already exists.
     * @param f The specific file which should be tested.
     * @return True, if the file can be written or overwritten. False if the file should not be overwritten.
     */
    public static boolean isWritable(JFrame parent, File f) {
      if (f.exists())
         return setQuestionDialog(
               parent,
               "File "+f.getName()+" already exists. Overwrite?");
      return true;
   }
    
    /**
     * A simple question dialog. Prints the message on the screen and let the user
     * click YES or NO.
     * @param parent The parent frame to aling the message dialoge with.
     * @param s The message to be printed.
     * @return True, if user clicked YES. Else: False.
     */
   public static boolean setQuestionDialog(JFrame parent, String s) {
      Object[] o = new Object[] { "Yes", "No" };
      int res = JOptionPane.showOptionDialog(parent, s, "",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
            o, "Yes");
      if (res == JOptionPane.NO_OPTION)
         return false;
      return true;
   }
   
}

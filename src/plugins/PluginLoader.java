/*
 * Copyright (c) 2014, Peter J. Schueffler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import TMARKERPluginInterface.Pluggable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tmarker.misc.Misc;
import tmarker.tmarker;

/**
 * The plugin loader class contains methods to load the plugins.
 * Plugins can be contained in the plugin folder of TMARKER or online in an URL.
 * @author Peter J. Schueffler
 */
public class PluginLoader {

    public static List<Pluggable> loadPlugins(File plugDir, ClassLoader parentClassLoader) throws IOException {
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  Try to load plugins from " + plugDir.getAbsolutePath());
        
        File[] plugJars = plugDir.listFiles(new JARFileFilter());
        
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  " + plugJars.length + " plugin(s) listed.");
        
        ClassLoader cl = new URLClassLoader(PluginLoader.fileArrayToURLArray(plugJars), parentClassLoader);
        
        List<Class<Pluggable>> plugClasses = PluginLoader.extractClassesFromJARs(plugJars, cl);
        
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  " + plugClasses.size() + " pluggable plugin(s) loaded.");
        
        return PluginLoader.createPluggableObjects(plugClasses);
    }
    
    public static List<Pluggable> loadPlugins(URL plugURL, String tmp_dir, ClassLoader parentClassLoader) throws IOException {
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  Try to load plugins from " + plugURL);
        
        File[] plugJars = listFilesFromURL2(plugURL, tmp_dir);
        
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  " + plugJars.length + " plugin(s) listed.");
        
        ClassLoader cl = new URLClassLoader(PluginLoader.fileArrayToURLArray(plugJars), parentClassLoader);
        
        List<Class<Pluggable>> plugClasses = PluginLoader.extractClassesFromJARs(plugJars, cl);
        
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  " + plugClasses.size() + " pluggable plugin(s) loaded.");
        
        return PluginLoader.createPluggableObjects(plugClasses);
    }
    
    /**
     * Use this to list the jar files in an online URL, if the server supports listing of contents.
     * @param url The URL (folder) of the jar files.
     * @param tmp_dir The jar files are copied to the local tmp_dir to load them into TMARKER.
     * @return A list of jar files (plugins) which can be loaded into TMARKER.
     * @throws IOException From the function "copyURLToFile".
     */
    public static File[] listFilesFromURL(URL url, String tmp_dir) throws IOException {
        Document doc = Jsoup.connect(url.toString()).get();
        List<File> files = new ArrayList<>();
        int i=0;
        for (Element elem : doc.select("a")) {
            if (i>4) {
                String fname = tmp_dir + File.separator + elem.attr("href");
                if (fname.toLowerCase().endsWith(".jar")) {
                    File file_tmp = new File(fname);
                    FileUtils.copyURLToFile(new URL(url.toString() + "/" + elem.attr("href")), file_tmp);
                    file_tmp.deleteOnExit();
                    files.add(file_tmp);
                }
            }
            i++;
        }
        File[] filearray = new File[files.size()];
        for (i=0; i<files.size(); i++) {
            filearray[i] = files.get(i);
        }
        return filearray;
    }
    
    /**
     * Use this to list the jar files in an online URL, if the server does not support folder content listing.
     * @param url The URL of a webpage which lists the jar files as links.
     * @param tmp_dir The jar files are copied to the local tmp_dir to load them into TMARKER.
     * @return A list of jar files (plugins) which can be loaded into TMARKER.
     * @throws IOException From the function "copyURLToFile".
     */
    public static File[] listFilesFromURL2(URL url, String tmp_dir) throws IOException {
        Document doc = Jsoup.connect(url.toString()).get();
        List<File> files = new ArrayList<>();
        for (Element elem : doc.select("a[href*=.jar]")) {
            String link = elem.attr("href");
            String fname = tmp_dir + Misc.FilePathStringtoFilename(link);
            if (fname.toLowerCase().endsWith(".jar")) {
                File file_tmp = new File(fname);
                FileUtils.copyURLToFile(new URL(url.getProtocol() + "://" + url.getHost() + "/" + link), file_tmp);
                file_tmp.deleteOnExit();
                files.add(file_tmp);
            }
        }
        File[] filearray = new File[files.size()];
        for (int i=0; i<files.size(); i++) {
            filearray[i] = files.get(i);
        }
        return filearray;
    }

    private static URL[] fileArrayToURLArray(File[] files) throws MalformedURLException {
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++) {
            urls[i] = files[i].toURI().toURL();
        }
        return urls;
    }

    private static List<Class<Pluggable>> extractClassesFromJARs(File[] jars, ClassLoader cl) throws IOException {
        List<Class<Pluggable>> classes = new ArrayList<>();
        for (File jar : jars) {
            try {
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to extract JARs from " + jar.getName() + "...");
                
                classes.addAll(PluginLoader.extractClassesFromJAR(jar, cl));
                
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to extract JARs from " + jar.getName() + "... SUCCESS.");
            } catch (Exception e) {
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to extract JARs from " + jar.getName() + "... FAILED.");
                Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        return classes;
    }

    @SuppressWarnings("unchecked")
    private static List<Class<Pluggable>> extractClassesFromJAR(File jar, ClassLoader cl) throws IOException {
        List<Class<Pluggable>> classes = new ArrayList<>();
        JarInputStream jaris = new JarInputStream(new FileInputStream(jar));
        JarEntry ent;
        boolean warning_shown = false;
        while ((ent = jaris.getNextJarEntry()) != null) {
            if (ent.getName().toLowerCase().endsWith(".class")) {
                try {
                    if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "      Try to extract " + ent.getName() + "..."); 
                    
                    Class<?> cls = cl.loadClass(ent.getName().substring(0, ent.getName().length() - 6).replace('/', '.'));
                    
                    if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "      Try to extract " + ent.getName() + "... SUCCESS."); 
                    
                    if (PluginLoader.isPluggableClass(cls)) {
                        if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "        " + ent.getName() + " is pluggable."); 
                        
                        classes.add((Class<Pluggable>) cls);
                    } else {
                        if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "        " + ent.getName() + " is not pluggable."); 
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError ex) {
                    if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "      Try to extract " + ent.getName() + "... FAILED."); 
                    Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, null, ex);
                    if (!warning_shown) {
                        System.err.println("Can't load plugin " + jar.getName() + ". Reason: Can't load Class " + ent.getName());
                        JOptionPane.showMessageDialog(null, "Can't load Plugin " + jar.getName() + ".\n\nReason:\nCan't load Class " + ent.getName(), "Error Loading Plugin.", JOptionPane.ERROR_MESSAGE);
                        warning_shown = true;
                    }
                } 
            }
        }
        jaris.close();
        return classes;
    }

    private static boolean isPluggableClass(Class<?> cls) {
        for (Class<?> i : cls.getInterfaces()) {
            if (tmarker.DEBUG>3) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "          Pluggable: " + Pluggable.class.toString()+ " - Interface: " + i.toString());
            if (i.equals(Pluggable.class)) {
                return true;
            }
        }
        return false;
    }

    private static List<Pluggable> createPluggableObjects(List<Class<Pluggable>> pluggables) {
        List<Pluggable> plugs = new ArrayList<>(pluggables.size());
        for (Class<Pluggable> plug : pluggables) {
            try {
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to create instance from " + plug.getName() + "...");
                
                plugs.add(plug.newInstance());
                
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to create instance from " + plug.getName() + "... SUCCESS.");
            } catch (Error | Exception e) {
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to create instance from " + plug.getName() + "... FAILED.");
                Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, "Can't instantiate plugin: " + plug.getName(), e);
            }
        }
        return plugs;
    }
    
}

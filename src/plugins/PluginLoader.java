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
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tmarker.misc.Misc;
import tmarker.tmarker;

/**
 * The plugin loader class contains methods to load the plugins.
 * Plugins can be contained in the plugin folder of TMARKER or online in an URL.
 * @author Peter J. Schueffler
 */
public class PluginLoader {

    /**
     * Working function: Loads all plugins from a given local directory containing jar files, initializes and returns found Pluggables.
     * @param plugDir The directory with jar files to be searched for Plugins.
     * @param parentClassLoader The classloader to be used to load the plugins.
     * @return A list of initialized plugin objects.
     * @throws IOException If a jar cannot be read.
     */
    public static List<Pluggable> loadPlugins(File plugDir, ClassLoader parentClassLoader) throws IOException {
        if (plugDir.exists()) {
            
            if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  Try to load plugins from {0}", plugDir.getAbsolutePath());

            File[] plugJars = plugDir.listFiles(new JARFileFilter());

            if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  {0} plugin(s) listed.", plugJars.length);

            ClassLoader cl = new URLClassLoader(PluginLoader.fileArrayToURLArray(plugJars), parentClassLoader);

            List<Class<Pluggable>> plugClasses = PluginLoader.extractClassesFromJARs(plugJars, cl);

            if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  {0} pluggable plugin(s) loaded.", plugClasses.size());

            return PluginLoader.createPluggableObjects(plugClasses);
            
        } else {
            
            return new ArrayList<>();
            
        }
    }
    
    /**
     * Working function: Loads all plugins from a given URL listing jar files, downloads them into a temp directory and initializes and returns found Pluggables.
     * @param plugURL The URL with jar files to be scanned for jar files.
     * @param tmp_dir The local temp directory where the jar files are downloaded for initialization.
     * @param selectedPlugins An array indicating the names, Authors and versions of those plugins which should be loaded.
     * @param parentClassLoader The classloader to be used to load the plugins.
     * @return A list of initialized plugin objects.
     * @throws IOException If a jar cannot be read.
     */
    public static List<Pluggable> loadPlugins(URL plugURL, String tmp_dir, String[] selectedPlugins, ClassLoader parentClassLoader) throws IOException {
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  Try to load plugins from {0}", plugURL);
        
        File[] plugJars = downloadSelectedJarFilesFromURL(plugURL, tmp_dir, selectedPlugins);
        
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  {0} plugin(s) listed.", plugJars.length);
        
        ClassLoader cl = new URLClassLoader(PluginLoader.fileArrayToURLArray(plugJars), parentClassLoader);
        
        List<Class<Pluggable>> plugClasses = PluginLoader.extractClassesFromJARs(plugJars, cl);
        
        if (tmarker.DEBUG>0) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "  {0} pluggable plugin(s) loaded.", plugClasses.size());
        
        return PluginLoader.createPluggableObjects(plugClasses);
    }
    
    /**
     * Use this to list the jar files in an online URL, if the server does not support folder content listing.
     * @param url The URL of a webpage which lists the jar files as links.
     * @param tmp_dir The jar files are copied to the local tmp_dir to load them into TMARKER.
     * @param selectedPlugins An array indicating the names, Authors and versions of those plugins which should be loaded.
     * @return A list of jar files (plugins) which can be loaded into TMARKER.
     * @throws IOException From the function "copyURLToFile".
     */
    public static File[] downloadSelectedJarFilesFromURL(URL url, String tmp_dir, String[] selectedPlugins) throws IOException {
        System.setProperty("http.agent", "");
                
        Document doc = Jsoup.connect(url.toString()).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36").get();
        //Document doc = Jsoup.connect(url.toString()).get();
        //System.out.println(doc.body());
        List<File> files = new ArrayList<>();
        Elements elems = doc.select("a[href*=.jar]");
        int k=0;
        for (Element elem : doc.select("a[href*=.jar]")) {
            
            // Get the metainfos (name, author, version)
            Element div = elem.parent().parent();
            Elements elements = div.getElementsByTag("h2");
            String name = elements.text().trim();
            elements = div.getElementsByTag("h3");
            String metainfo = elements.text();
            String author = metainfo.replace("Author: ", "");
            author = author.substring(0, author.indexOf(" Version: "));
            String version = metainfo.substring(metainfo.indexOf("Version: ") + 9);
            
            // get the jar link
            String link = elem.attr("href");
            String fname = tmp_dir + Misc.FilePathStringtoFilename(link);
            if (fname.toLowerCase().endsWith(".jar") && 
                    (selectedPlugins == null || Arrays.asList(selectedPlugins).contains(name+"|||"+version+"|||"+author))) {
                
                tmarker.splashTextAndProgress("dwnl " + name, (int) (20.0+0.2*100.0*k++/elems.size()));
            
                File destination = new File(fname);
                //destination.createNewFile();
                destination.deleteOnExit();
                
                // Possibility 1: doesnt work, since the connection is blocked (Access denied, 403), due to robot protection.
                //FileUtils.copyURLToFile(new URL(url.getProtocol() + "://" + url.getHost() + "/" + link), destination);
                
                // Possibility 2: mimic webbrowser connection, with webbrowser user agent
                try {
                //URL source = new URL(url.getProtocol() + "://" + url.getHost() + link); // former days... (before April 2016, ETH cms 6 change)
                URL source = new URL(link);
                URLConnection sourceConnection = source.openConnection();
                //sourceConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
                sourceConnection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
                sourceConnection.setRequestProperty("Accept","*/*");
                try (InputStream input = sourceConnection.getInputStream()) {
                        copyInputStreamToFile(input, destination);
                    }
                
                files.add(destination);
                } catch (Exception e) {
                    if (tmarker.DEBUG>0) {
                        Logger.getLogger(PluginLoader.class.getName()).log(Level.WARNING, e.getMessage());
                        e.printStackTrace();
                    }
       
                }
            }
        }
        File[] filearray = new File[files.size()];
        for (int i=0; i<files.size(); i++) {
            filearray[i] = files.get(i);
        }
        return filearray;
    }
    
    /**
     * Use this to list all available jar files in an online URL, if the server does not support folder content listing.
     * @param url The URL of a webpage which lists the jar files as links.
     * @return A list of plugins which could be loaded into TMARKER.
     * @throws IOException From the function "Jsoup.connect()".
     */
    public static String[] listAvailablePluginsFromURL(URL url) throws IOException {
        Document doc = Jsoup.connect(url.toString()).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36").get();
        List<String> files = new ArrayList<>();
        for (Element elem : doc.select("a[href*=.jar]")) {
            
            // Get the metainfos (name, author, version)
            String name = elem.parent().parent().getElementsByTag("h2").first().text();
            Element div = elem.parent().parent();
            Elements elements = div.getElementsByTag("h3");
            String metainfo = elements.text();
            String author = metainfo.replace("Author: ", "");
            author = author.substring(0, author.indexOf(" Version: "));
            String version = metainfo.substring(metainfo.indexOf("Version: ") + 9);
            String description = "<html>"+elem.parent().parent().getElementsByTag("p").first().html()+"</html>";
            
            // get the jar link
            String link = elem.attr("href");
            String fname = Misc.FilePathStringtoFilename(link);
            if (fname.toLowerCase().endsWith(".jar")) {
                
                files.add(name+"|||"+version+"|||"+author+"|||"+description);
            }
        }
        String[] filearray = new String[files.size()];
        for (int i=0; i<files.size(); i++) {
            filearray[i] = files.get(i);
        }
        return filearray;
    }

    /**
     * Converts a file array to a URL array.
     * @param files The files to be converted.
     * @return A new URL array with the file urls in it.
     * @throws MalformedURLException If a file cannot be converted.
     */
    private static URL[] fileArrayToURLArray(File[] files) throws MalformedURLException {
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++) {
            urls[i] = files[i].toURI().toURL();
        }
        return urls;
    }

    /**
     * Returns a list of potential Pluggable classes (not yet instantiated) within given jar files.
     * @param jar The jar files from which the classes are to be extracted.
     * @param cl The classloader to extract the classes.
     * @return A list of Pluggables whithin the jar.
     * @throws IOException If a jar cannot be read.
     */
    private static List<Class<Pluggable>> extractClassesFromJARs(File[] jars, ClassLoader cl) throws IOException {
        List<Class<Pluggable>> classes = new ArrayList<>();
        int k=0;
        for (File jar : jars) {
            try {
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to extract JARs from {0}...", jar.getName());
                
                tmarker.splashTextAndProgress(jar.getName(), (int) (40.0+0.20*100.0*k++/jars.length));
                
                classes.addAll(PluginLoader.extractClassesFromJAR(jar, cl));
                
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to extract JARs from {0}... SUCCESS.", jar.getName());
            } catch (Exception e) {
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to extract JARs from {0}... FAILED.", jar.getName());
                Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        return classes;
    }

    /**
     * Returns a list of potential Pluggable classes (not yet instantiated) within a given jar file.
     * @param jar The jar file from which the classes are to be extracted.
     * @param cl The classloader to extract the classes.
     * @return A list of Pluggables whithin the jar.
     * @throws IOException If the jar cannot be read.
     */
    @SuppressWarnings("unchecked")
    private static List<Class<Pluggable>> extractClassesFromJAR(final File jar, ClassLoader cl) throws IOException {
        List<Class<Pluggable>> classes = new ArrayList<>();
        try (JarInputStream jaris = new JarInputStream(new FileInputStream(jar))) {
            JarEntry ent;
            boolean warning_shown = false;
            while ((ent = jaris.getNextJarEntry()) != null) {
                if (ent.getName().toLowerCase().endsWith(".class")) {
                    try {
                        if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "      Try to extract {0}...", ent.getName()); 
                        
                        Class<?> cls = cl.loadClass(ent.getName().substring(0, ent.getName().length() - 6).replace('/', '.'));
                        
                        if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "      Try to extract {0}... SUCCESS.", ent.getName());
                        
                        if (PluginLoader.isPluggableClass(cls)) {
                            if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "        {0} is pluggable.", ent.getName());
                            
                            classes.add((Class<Pluggable>) cls);
                        } else {
                            if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "        {0} is not pluggable.", ent.getName());
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError ex) {
                        if (tmarker.DEBUG>2) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "      Try to extract {0}... FAILED.", ent.getName());
                        if (tmarker.DEBUG>0) System.err.println("Issue loading plugin " + jar.getName() + ". Error: Can't load Class " + ent.getName());
                        if (!warning_shown && tmarker.DEBUG>5) {
                            final JarEntry ent_final = ent;
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    JOptionPane.showMessageDialog(null, "Issue loading plugin " + jar.getName() + ".\n\nError:\nCan't load Class " + ent_final.getName(), "Error Loading Plugin.", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                            thread.start();
                            
                        }
                        warning_shown = true;
                    }
                }
                jaris.closeEntry(); 
            }
        }
        return classes;
    }

    /**
     * Checks whether a plugin is a valid pluggable class (i.e. the test:
     * cls.getInterfaces().get(i).equals(Pluggable.class)).
     * @param cls The list of classes to be checked.
     * @return True, if at least one interface of the class is a Pluggable class.
     */
    private static boolean isPluggableClass(Class<?> cls) {
        for (Class<?> i : cls.getInterfaces()) {
            if (tmarker.DEBUG>3) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "          Pluggable: {0} - Interface: {1}", new Object[]{Pluggable.class.toString(), i.toString()});
            if (i.equals(Pluggable.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to create new instances from the given plugin objects.
     * @param pluggables The list of potential plugin classes.
     * @return The list of instances which could successfully be instantiated.
     */
    private static List<Pluggable> createPluggableObjects(List<Class<Pluggable>> pluggables) {
        List<Pluggable> plugs = new ArrayList<>(pluggables.size());
        int k=0;
        for (Class<Pluggable> plug : pluggables) {
            try {
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to create instance from {0}...", plug.getName());
                
                tmarker.splashTextAndProgress("create " + plug.getName(), (int) (60.0+0.2*100.0*k++/pluggables.size()));
                
                plugs.add(plug.newInstance());
                
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to create instance from {0}... SUCCESS.", plug.getName());
            } catch (Error | Exception e) {
                if (tmarker.DEBUG>1) Logger.getLogger(PluginLoader.class.getName()).log(Level.INFO, "    Try to create instance from {0}... FAILED.", plug.getName());
                Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, "Can't instantiate plugin: " + plug.getName(), e);
            }
        }
        return plugs;
    }
    
}

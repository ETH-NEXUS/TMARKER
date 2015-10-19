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

package TMARKERPluginInterface;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Properties;
import javax.swing.Icon;
import tmarker.TMAspot.TMApoint;
import tmarker.TMAspot.TMAspot;


/**
 * The interface of a plugin. All functions have to be implemented for plugins.
 * The jar file of the plugin is automatically loaded if it is in the plugin folder
 * of TMARKER.
 * @author Peter J. Schueffler
 */
public interface Pluggable {
    
  /**
   * Return the Author of this plugin.
   * @return The Author of this plugin.
   */
  String getAuthor();
  
  /**
   * Returns the version of this plugin.
   * @return The version of this plugin.
   */
  String getVersion();
  
  /**
   * This function is called when TMARKER is started and the plugin is loaded.
   * @return True, if the start process was successful.
   */
  boolean start();
  
  /**
   * This function is called when TMARKER is exited.
   * @return True, if the stop process was successful.
   */
  boolean stop();
  
  /**
   * Sets the PluginManager.
   * @param manager The manager to be set.
   */
  void setPluginManager(PluginManager manager);
  
  /**
   * Returns an Icon of this plugin which is displayed in the plugin menu.
   * @return The Icon of this plugin which is displayed in the plugin menu. Null for no icon.
   */
  Icon getIcon();
  
  /**
   * Returns the name of this plugin. The name will be visible in the plugin menu.
   * @return The name of this plugin.
   */
  String getPluginName();
  
  /**
   * This function is executed when the user clicks on the plugin.
   * @param evt The action which was performed (e.g. a mouse click).
   */
  void actionPerformed(java.awt.event.ActionEvent evt);
  
  /**
   * This function is intended to ets the defaults of the parameters for this plugin. 
   */
  void setParameterDefaults();
  
  /**
   * This function sets the parameters for this plugin.
   * @param parameters The parameters to be set. This is a set of property name and value pairs. 
   */
  void setParameters(Properties parameters);
  
  /**
   * Returns the parameters for this plugin.
   * @return The parameters of this plugin. This is a set of property name and value pairs. 
   */
  Properties getParameters();
  
  /**
   * Returns a HTML String which can be written into the HTML report.
   * @param HTMLFolderName The name of the foldername for e.g. image files shown in the HTML report.
   * @return A HTML string.
   */
  String getHTMLReport(String HTMLFolderName);
  
  /**
   * If the user changed the selection of TMAspots, the plugin might want to change its parameters
   * and adopt them to the current selection. An update should only be done if it is necessary (e.g. if the plugin "isShowing()" or it is in actual use).
     * @param visible_TMAspot The currently visible TMAspot.
     * @param selected_TMAspots The currently selected TMAspots.
   */
  void updateOptionsToTMAspot(TMAspot visible_TMAspot, List<TMAspot> selected_TMAspots);

  /**
   * This method is called when a TMAspot image is displayed and BEFORE the nuclei and ROIs are drawn. You can use it e.g. do draw the plugin's output on the TMAspot.
   * @param ts The TMAspot which is drawn.
   * @param g The graphics on which is drawn. This is actually displayed.
   * @param z The zoom factor of the graphics (i.e. the factor by which the coordinates of your information should be devided).
   * @param x_min The min x-coord which is visible in the zoomable image pane, relative to the original image coordinated (i.e. independent from the zoom).
   * @param y_min The min y-coord which is visible in the zoomable image pane, relative to the original image coordinated (i.e. independent from the zoom).
   * @param x_max The max x-coord which is visible in the zoomable image pane, relative to the original image coordinated (i.e. independent from the zoom).
   * @param y_max The max y-coord which is visible in the zoomable image pane, relative to the original image coordinated (i.e. independent from the zoom).
  */
  public void drawInformationPreNuclei(TMAspot ts, Graphics g, double z, int x_min, int y_min, int x_max, int y_max);
  
  /**
   * This method is called when a TMAspot image is displayed and AFTER the nuclei and ROIs are drawn. You can use it e.g. do draw the plugin's output on the TMAspot.
   * @param ts The TMAspot which is drawn.
   * @param g The graphics on which is drawn. This is actually displayed.
   * @param z The zoom factor of the graphics (i.e. the factor by which the coordinates of your information should be devided).
   * @param x_min The min x-coord which is visible in the zoomable image pane, relative to the original image coordinated (i.e. independent from the zoom).
   * @param y_min The min y-coord which is visible in the zoomable image pane, relative to the original image coordinated (i.e. independent from the zoom).
   * @param x_max The max x-coord which is visible in the zoomable image pane, relative to the original image coordinated (i.e. independent from the zoom).
   * @param y_max The max y-coord which is visible in the zoomable image pane, relative to the original image coordinated (i.e. independent from the zoom).
  */
  public void drawInformationPostNuclei(TMAspot ts, Graphics g, double z, int x_min, int y_min, int x_max, int y_max);
  
  /**
   * This method is called when a TMAspot image is displayed. The original TMAspot image might be replaced by a plugin specific image, which has to be same size
   * as the original TMAspot.
   * @param ts The TMAspot to be displayed.
   * @return The image to be shown alternatively to the orginal TMAspot. Can be null, if the original TMAspot should be shown.
   */
  public BufferedImage showAlternativeImage(TMAspot ts);
  
  /**
   * Allows the plugin to do something after a mouse click on the image has been done.
   * @param ts The TMAspot on which the click was done
   * @param tp The TMApoint in the location of the click (can be null, if none is found).
   * @param evt The MouseClick event.
   */
  public void TMAspotMouseClicked(TMAspot ts, TMApoint tp, MouseEvent evt);
  
}
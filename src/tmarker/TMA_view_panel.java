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

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollPane;
import tmarker.TMAspot.TMAspot;

/**
 * IMPORTANT: Any implementation of TMA_view_panel has to extend a JPanel (since 
 * the TMA_view_panel is displayed and casted as a JPanel).
 * @author Peter J. Schueffler
 */
public interface TMA_view_panel {
    static final Cursor CURSOR_CROSS = new java.awt.Cursor(java.awt.Cursor.CROSSHAIR_CURSOR);
    static final Cursor CURSOR_DEFAULT = new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR);
    static final Cursor CURSOR_HAND = new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR);
    static final Cursor CURSOR_MOVE = new java.awt.Cursor(java.awt.Cursor.MOVE_CURSOR);
    final List<Integer> RECENT_POLYLINE_X = new ArrayList<>(); // for the drawing of including or excluding areas.
    final List<Integer> RECENT_POLYLINE_Y = new ArrayList<>(); // for the drawing of including or excluding areas.
   

    /**
     * Returns the currently displayed TMAspot.
     * @return The currently displayed TMAspot. Null if there is none.
     */
    public TMAspot getTMAspot();
    
    /**
     * Returns whether or not this TMAViewPanel currently displays a point with a given coordinate
     * x and y. X and y come from the original image dimension (i.e. independent from the current zoom).
     * @param x The x-coord of the queried locus.
     * @param y The y-coord of the queried locus.
     * @return True if the locus is currently displayed in the container JScrollPane. False if the locus falls outside of the JScrollPane.
     */
    public boolean isShowing(int x, int y);
    
    /* Displays a TMAspot.
     * @param ts The TMAspot to be displayed.
     */
    public void showTMAspot(TMAspot ts);
    
    /* Displays a TMAspot.
     * @param ts The TMAspot to be displayed.
     * @param forceRepaint If true, the given TMAspot is repainted in any case. If false, 
     * the given TMAspot is only repainted, if it is not equal to the currently visible TMAspot.
     */
    public void showTMAspot(TMAspot ts, boolean forceRepaint);
    
    /**
     * Returns the original image.
     * @return The image.
     */
    public Image getImage();
    
    /**
     * Returns the original image width.
     * @return The image width.
     */
    public int getImageWidth();
    
    /**
     * Returns the original image height.
     * @return The image height.
     */
    public int getImageHeight();

    /**
     * Returns the recent drawn polyline. Important for the drawing of including or excluding areas.
     * @return The recently drawn polyline x-coords.
     */
    public List<Integer> getRecentPolyline_x();
    
    /**
     * Returns the recent drawn polyline. Important for the drawing of including or excluding areas.
     * @return The recently drawn polyline y-coords.
     */
    public List<Integer> getRecentPolyline_y();

    /**
     * Indicates that the user does not want to draw an area anymore. The recent polyline is cleared.
     */
    public void reliefRecentPolyLine();
    
    /**
     * Enables / disables the popup menu.
     * @param b If true, the popup menu is enabled. Otherwise disabled.
     */
    public void enablePopupMenu(boolean b);
    
    /**
     * Repaints the component
     */
    public void repaint();
    
    /**
     * Sets the zoom of the shown image.
     * @param z The zoom (1.0 for 100% zoom) 
     */
    public void setZoom(double z);
    
    /**
     * Returns the zoom of the shown image.
     * @return The zoom (1.0 for 100% zoom) 
     */
    public double getZoom();
    
    /**
     * Sets the parent JScrollPane. It can be used to determine the region of
     * the image which is visible (can speed up scrolling).
     * @param parent The parent JScrollPane. Can be null if there is none.
     */
    public void setParentScrollPane(JScrollPane parent);
    
    /**
     * Sets the coordinates x, y of the image into the center of the current view. After calling, the
     * user sees point x, y of the original image in the middle of the TMA view screen, at the same zoom level as before.
     * @param x The x coordinate as original image coordinate.
     * @param y The y coordinate as original image coordinate.
     */
    public void jumpToVisibleLocus(int x, int y);
    
    public Rectangle getVisibleRect();

}
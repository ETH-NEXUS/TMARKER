/*
 * $Id$
 *
 * Dual-licensed under LGPL (Sun and Romain Guy) and BSD (Romain Guy).
 *
 * Copyright 2005 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * Copyright (c) 2006 Romain Guy <romain.guy@mac.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package tmarker.misc;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * <p><code>ZoomableImagePanel</code> is a generic lightweight container
 * displaying an image the user can zoom in and out using the mouse scroll
 * wheel. The zoom can also be controlled programatically to provide other
 * input methods: keyboard, slider control, etc.</p>
 * <h2>Image</h2>
 * <p>An image can be loaded in the panel at creation time, with a constructor,
 * or using an accesser:
 * <pre>
 * ZoomableImagePanel panel = new ZoomableImagePanel(myImage);
 * panel.setImage(myOtherImage);
 * </pre>
 * Changing the current image with {@link #setImage(Image)} triggers a repaint
 * of the component at the current zoom level. You can set the image to null
 * to prevent the panel from painting it.</p>
 * <p>The image is always painted centered in the component.</p>
 * <h2>Zoom Properties</h2>
 * <p>Zoom on the picture is controlled by four properties:
 * <ul>
 *   <li><i>zoom</i>: The current level of zoom where 1.0 is a zoom at 100%.
 *   Zoom is always comprised between <code>zoomMin</code> and
 *   <code>zoomMax</code>. Default value is 1.0.</li>
 *   <li><i>zoomFactor</i>: The factor by which the zoom is modified when the
 *   user scroll the mouse. A zoom factor of 1.5 will increase or decrease the
 *   zoom level by 50%. A zoom factor lower than 1.0 inverts the mouse wheel
 *   effect. Default value is 1.1, or 10%.</li>
 *   <li><i>zoomMin</i>: The minimm level of zoom. Default value is 0.25, or
 *   25% of the original picture.</li>
 *   <li><i>zoomMax</i>: The maximum level of zoom. Default value is 4.0, or
 *   400% of the original picture.</li>
 * </ul>
 * Each property can be accessed programatically at any time using the
 * appropriate getters and setters.</p>
 * <p>Changing the <code>zoom</code> level triggers a repaint of the component.
 * When you change the zoom boundaries, it is guaranteed the zoom level will be
 * comprised in the appropriate range.</p>
 * <h2>Programatic Control of Zoom Level</h2>
 * <p>Zoom level can be controlled using either one of {@link #setZoom(double)},
 * {@link #increaseZoom()} or {@link #decreaseZoom()}. While the first method
 * lets you specify the level of zoom you want, the other two increase or
 * decrease the zoom level depending on the current zoom factor.</p>
 * <p>The method {@link #increaseZoom()} computes the new zoom level as
 * following:
 * <pre>
 * zoom *= zoomFactor;
 * </pre>
 * The method {@link #decreaseZoom()} computes the new zoom level as
 * following:
 * <pre>
 * zoom /= zoomFactor;
 * </pre>
 * All the methods used for zoom level control ensure the zoom level remains
 * in the range defined by <code>zoomMin</code> and <code>zoomMax</code>.</p>
 * <h2>Mouse Wheel Support</h2>
 * <p>By default, this panel provides support for mouse wheel. Scrolling the
 * wheel increases or decreases the zoom level and updates the display. This
 * behavior can be deactivated to use the panel within a
 * {@link javax.swing.JScrollPane} or to prevent any interaction from the
 * user. You can enable or disable the mouse wheel support at runtime by
 * calling {@link #setMouseWheelEnabled(boolean)}.</p>
 * <h2>Component Opacity</h2>
 * <p>A <code>ZoomableImagePanel</code> is a non-opaque component by default.</p>
 * 
 * @author Romain Guy <romain.guy@mac.com>
 */
public class ZoomableImagePanel extends JPanel {
    /**
     * <p>Identifies a change to the zoom level used to display the image.</p>
     * <p>When the property change event is fired, the old value and the new
     * value are provided as <code>Double</code> instances.</p>
     */
    public static final String ZOOM_CHANGED_PROPERTY = "zoom_level";
    
    /**
     * <p>Identifies a change to the zoom factor used to change zoom level.</p>
     * <p>When the property change event is fired, the old value and the new
     * value are provided as <code>Double</code> instances.</p>
     */
    public static final String ZOOM_FACTOR_CHANGED_PROPERTY = "zoom_factor";
    
    /**
     * <p>Identifies a change to the maximum zoom level used to display the image.</p>
     * <p>When the property change event is fired, the old value and the new
     * value are provided as <code>Double</code> instances.</p>
     */
    public static final String ZOOM_MAX_CHANGED_PROPERTY = "zoom_max";
    
    /**
     * <p>Identifies a change to the minimum zoom level used to display the image.</p>
     * <p>When the property change event is fired, the old value and the new
     * value are provided as <code>Double</code> instances.</p>
     */
    public static final String ZOOM_MIN_CHANGED_PROPERTY = "zoom_min";
    
    /**
     * <p>Identifies a change to the displayed image.</p>
     * <p>When the property change event is fired, the old value and the new
     * value are provided as <code>Image</code> instances.</p>
     */
    public static final String IMAGE_CHANGED_PROPERTY = "image";
    
    // zoom properties
    private double zoom = 1.0;
    private double zoomMax = 4.0;
    private double zoomMin = 0.25;
    private double zoomFactor = 1.1;

    // image cache
    private Image image = null;
    private int cachedWidth = 0;
    private int cachedHeight = 0;
    
    // mouse wheel support
    MouseWheelListener mouseWheelSupport = null;
    
    // parent scroll panel
    JScrollPane parentScrollPane = null;
    
    /**
     * <p>Creates a new zoomable panel with a flow layout, no default image and
     * mouse wheel support.</p>
     */
    public ZoomableImagePanel() {
        this(null, new FlowLayout());
    }
    
    /**
     * <p>Creates a new zoomable panel with the specified layout, no default
     * image and mouse wheel support.</p>
     */
    public ZoomableImagePanel(final LayoutManager layout) {
        this(null, layout);
    }
    
    /**
     * <p>Creates a new zoomable panel with the specified layout and image and
     * mouse wheel support.</p>
     */
    public ZoomableImagePanel(final Image image, final LayoutManager layout) {
        super(layout);
        
        setMouseWheelEnabled(true);
        setOpaque(false);
        setImage(image);
    }
    
    /**
     * Sets the parent JScrollPane. It can be used to determine the region of
     * the image which is visible (can speed up scrolling).
     * @param parent The parent JScrollPane. Can be null if there is none.
     */
    public void setParentScrollPane(JScrollPane parent) {
        this.parentScrollPane = parent;
    }
    
    /**
     * <p>Indicates whether or not the mouse wheel support can be used by the
     * user to increase and decrease the zoom level.</p>
     * 
     * @return true if mouse wheel support is enabled for zoom
     */
    public boolean isMouseWheelEnabled() {
        return mouseWheelSupport != null;
    }
    
    /**
     * <p>Mouse wheel support to increase or decrease the zoom level can be
     * deactivated to avoid conflicts with other mouse wheel enabled components,
     * like {@link javax.swing.JScrollPane}. Disablind the mouse wheel is also
     * useful to use this panel as a simple non-interactive image panel.</p>
     * 
     * @param enabled enables mouse wheel support when true, disables it when
     * false
     */
    public void setMouseWheelEnabled(boolean enabled) {
        if (enabled && mouseWheelSupport == null) {
            mouseWheelSupport = new ZoomSelector();
            addMouseWheelListener(mouseWheelSupport);
        } else if (!enabled && mouseWheelSupport != null) {
            removeMouseWheelListener(mouseWheelSupport);
            mouseWheelSupport = null;
        }
    }

    /**
     * <p>Gets the current zoom level, comprised between {@link #getZoomMin()}
     * and {@link #getZoomMax()}.</p>
     * 
     * @return this panel's current zoom level
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * <p>Sets the zoom level used to display the image and repaints the
     * component</p>
     * <p>If the specified zoom level is lower than {@link #getZoomMin()} or
     * greater {@link #getZoomMax()}, its value is restrained to the nearest
     * boundary.</p>
     * 
     * @param zoom the zoom level used to display this panel's image
     * @see #increaseZoom()
     * @see #decreaseZoom()
     */
    public void setZoom(final double zoom) {
        //if (zoom == this.zoom) {
        //   return;
        //}
        
        double oldValue = this.zoom;
        
        this.zoom = zoom;
        if (this.zoom > zoomMax) {
            this.zoom = zoomMax;
        } else if (this.zoom < zoomMin) {
            this.zoom = zoomMin;
        }
        
        firePropertyChange(ZOOM_CHANGED_PROPERTY,
                           new Double(oldValue),
                           new Double(this.zoom));
        rescaleImage();
    }

    /**
     * <p>Decreases the current zoom level by the current zoom factor and
     * repaints the component. If the zoom factor is lower than 1.0, this
     * method will increase the zoom level.</p>
     * <p>This method ensures the zoom level is always comprised between
     * {@link #getZoomMin()} and {@link #getZoomMax()}.</p>
     * 
     * @see #increaseZoom()
     * @see #setZoom(double)
     */
    public void decreaseZoom() {
        setZoom(zoom / zoomFactor);
    }

    /**
     * <p>Increases the current zoom level by the current zoom factor and
     * repaints the component. If the zoom factor is lower than 1.0, this
     * method will decrease the zoom level.</p>
     * <p>This method ensures the zoom level is always comprised between
     * {@link #getZoomMin()} and {@link #getZoomMax()}.</p>
     * 
     * @see #decreaseZoom()
     * @see #setZoom(double)
     */
    public void increaseZoom() {
        setZoom(zoom * zoomFactor);
    }

    /**
     * <p>Gets the zoom factor, always positive.</p>
     * 
     * @return this panel's current zoom factor
     */
    public double getZoomFactor() {
        return zoomFactor;
    }

    /**
     * <p>Sets the zoom factor used to modify the zoom level on
     * {@link #increaseZoom()} and {@link #decreaseZoom()} calls.</p>
     * <p>If the specified zoom factor is lower than 1.0, it will invert the
     * effects of {@link #increaseZoom()} and {@link #decreaseZoom()}.</p>
     * <p>If the specified zoom factor is lower than or equals 0.0 an
     * <code>IllegalArgumentException</code> is thrown.</p>
     * 
     * @param zoomFactor the zoom factor used to increase or decrease zoom level
     * @see #increaseZoom()
     * @see #decreaseZoom()
     * @throws IllegalArgumentException when <code>zoomFactor</code> is &lt;= 0.0
     */
    public void setZoomFactor(final double zoomFactor) {
        if (zoomFactor <= 0.0) {
            throw new IllegalArgumentException("zoomFactor cannot be < 0.0");
        }

        double oldValue = this.zoomFactor;
        this.zoomFactor = zoomFactor;
        firePropertyChange(ZOOM_FACTOR_CHANGED_PROPERTY,
                           new Double(oldValue),
                           new Double(this.zoomFactor));
    }

    /**
     * <p>Gets the maximum zoom level, always positive.</p>
     * 
     * @return this panel's maximum zoom level
     */
    public double getZoomMax() {
        return zoomMax;
    }

    /**
     * <p>Sets the maximum zoom level. When {@link #increaseZoom()} and
     * {@link #decreaseZoom()} are called, the panel will prevent the zoom level
     * from exceeding the maximum zoom level.</p>
     * <p>If the specified maximum zoom level is lower than 0.0 or lower than
     * the minimum zoom level, an <code>IllegalArgumentException</code> is
     * thrown.</p>
     * <p>This method ensures the zoom level is always comprised between
     * {@link #getZoomMin()} and {@link #getZoomMax()}.</p> 
     * 
     * @param zoomMax the maximum zoom level used to display this panel's image
     * @see #increaseZoom()
     * @see #decreaseZoom()
     * @throws IllegalArgumentException when <code>zoomMax</code> is &lt;= 0.0
     * or &lt; <code>zoomMin</code>
     */
    public void setZoomMax(final double zoomMax) {
        if (zoomMax <= 0.0) {
            throw new IllegalArgumentException("zoomMax cannot be <= 0.0");
        }
        if (zoomMax < zoomMin) {
            throw new IllegalArgumentException("zoomMax cannot be lower than zoomMin");
        }
        
        if (zoomMax == this.zoomMax) {
            return;
        }
        
        double oldValue = this.zoomMax;
        this.zoomMax = zoomMax;
        firePropertyChange(ZOOM_MAX_CHANGED_PROPERTY,
                           new Double(oldValue),
                           new Double(this.zoomFactor));
        //setZoom(zoom);
    }

    /**
     * <p>Gets the minimum zoom level, always positive.</p>
     * 
     * @return this panel's minimum zoom level
     */
    public double getZoomMin() {
        return zoomMin;
    }

    /**
     * <p>Sets the minimum zoom level. When {@link #increaseZoom()} and
     * {@link #decreaseZoom()} are called, the panel will prevent the zoom level
     * from being lower than the minimum zoom level.</p>
     * <p>If the specified minimum zoom level is lower than 0.0 or greater than
     * the maximum zoom level, an <code>IllegalArgumentException</code> is
     * thrown.</p>
     * <p>This method ensures the zoom level is always comprised between
     * {@link #getZoomMin()} and {@link #getZoomMax()}.</p> 
     * 
     * @param zoomMin the minimum zoom level used to display this panel's image
     * @see #increaseZoom()
     * @see #decreaseZoom()
     * @throws IllegalArgumentException when <code>zoomMin</code> is &lt;= 0.0 or
     * &gt; <code>zoomMax</code>
     */
    public void setZoomMin(final double zoomMin) {
        if (zoomMin <= 0.0) {
            throw new IllegalArgumentException("zoomMin cannot be <= 0.0");
        }
        if (zoomMin > zoomMax) {
            throw new IllegalArgumentException("zoomMin cannot be greater than zoomMax");
        }
        
        if (zoomMin == this.zoomMin) {
            return;
        }

        double oldValue = this.zoomMin;
        this.zoomMin = zoomMin;
        firePropertyChange(ZOOM_MIN_CHANGED_PROPERTY,
                           new Double(oldValue),
                           new Double(this.zoomFactor));
        //setZoom(zoom);
    }
    
    /**
     * <p>Gets the currently displayed image, which can be null.</p>
     * 
     * @return this panel's image
     */
    public Image getImage() {
        return image;
    }

    /**
     * <p>Sets the image to be displayed in the panel. If the specified image
     * is null, nothing will be displayed. This method always triggers a repaint
     * of the component.</p> 
     * 
     * @param image this panel's image
     */
    public void setImage(final Image image) {
        Image oldValue = this.image;
        this.image = image;
        if (image != null) {
            this.cachedWidth = image.getWidth(this);
            this.cachedHeight = image.getHeight(this);
        }
        firePropertyChange(IMAGE_CHANGED_PROPERTY,
                           oldValue,
                           this.image);
        repaint();
    }

    /**
     * <p>The preferred size of a <code>ZoomableImagePanel</code> is the size
     * of the currently displayed image.</p>
     *
     * @return the dimension of this panel's image
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(cachedWidth, cachedHeight);
    }
    
    /**
     * <p>Paints this component and its image, centered in the panel. If the
     * current image is null, no image is painted.</p>
     * 
     * @param g the <code>Graphics</code> context in which to paint
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image != null) {
            int image_x = (getWidth() - cachedWidth) / 2;
            int image_y = (getHeight() - cachedHeight) / 2;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (parentScrollPane == null) {
                g.drawImage(image, image_x, image_y, cachedWidth, cachedHeight, this);
            } else {
                // do only draw the visible part of the image to save time
                
                // the coordinates of the visible rectangle in the zoomed preview image
                Rectangle rect = parentScrollPane.getViewport().getViewRect();
                
                // the coordinates of the visible rectangle in the original image (source image).
                int sx = (int)(rect.x/getZoom());
                int sy = (int)(rect.y/getZoom());
                int sw = (int)(rect.width/getZoom());
                int sh = (int)(rect.height/getZoom());
                
                // draw the source rectangle into the destination rectangle
                g.drawImage(image, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, sx, sy, sx+sw, sy+sh, this);
                
                
            }
        }
    }

    // rescales the image after a zoom level update
    private void rescaleImage() {
        int imageWidth = image.getWidth(this);
        int imageHeight = image.getHeight(this);

        int zoomedWidth = (int) (imageWidth * zoom);
        int zoomedHeight = (int) (imageHeight * zoom);
        
        if (zoomedWidth != cachedWidth ||
            zoomedHeight != cachedHeight) {

            cachedWidth = zoomedWidth;
            cachedHeight = zoomedHeight;
            
            revalidate();
            repaint();
        }
    }

    // on mouse wheel scroll even, the zoom level is increased or decreased
    private class ZoomSelector implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            int amount = e.getWheelRotation();
            if (amount <= 0) {
                increaseZoom();
            } else {
                decreaseZoom();
            }

            //rescaleImage(); // is already included in increaseZoom() and decreaseZoom()
        }
    }
}
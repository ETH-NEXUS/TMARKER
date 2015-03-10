/*
 * Copyright (c) 2015, Peter J. Schueffler
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
package tmarker;

import java.awt.Dimension;
import javax.swing.Icon;

/**
 *
 * @author Peter J. Schueffler
 */
public class LegendElementIcon extends LegendElement {
    
    int rad;
    byte label;
    byte staining;
    int shape;
    
    /**
     * Creates new form LegendElement.
     * @param t The parent TMARKER program.
     * @param rad The radius of the nuclei. Not used here.
     * @param icon The icon displayed.
     * @param tttext The tooltiptext for the icon.
     */
    public LegendElementIcon(tmarker t, int rad, Icon icon, String tttext) {
        this.t = t;
        this.rad = rad;
        bg = getBackground();
        setIcon(icon);
        setAlignmentX((float) 0.5);
        setPreferredSize(new Dimension(2*rad+7, 2*rad+7));
        setToolTipText(tttext);
    }

    /**
     * Do nothing on double click.
     */
    @Override
    void doubleClickAction() {
        // Do nothing
    }
}

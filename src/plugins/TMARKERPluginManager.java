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

import java.util.List;
import TMARKERPluginInterface.PluginManager;
import java.awt.Color;
import tmarker.TMAspot.TMAspot;
import tmarker.tmarker;

/**
 *
 * @author Peter J. Schueffler
 */
public class TMARKERPluginManager implements PluginManager {

    tmarker t;
  
    public TMARKERPluginManager(tmarker t) {
        this.t = t;
    }
    
    /**
     * Returns all TMAspots of TMARKER.
     * @return A list with all TMAspots (selected and not selected ones).
     */
    @Override
    public List<TMAspot> getTMAspots() {
        return t.getTMAspots();
    }

    /**
     * Returns all TMAspots which are currently selected by the user.
     * @return A list with all selected TMAspots.
    */
    @Override
    public List<TMAspot> getSelectedTMAspots() {
        return t.getSelectedTMAspots(false);
    }
    
    /**
     * Returns the TMAspot which is currently visible in the TMAview of TMARKER.
     * @return The TMAspot currently visible in TMARKER. Null, if there is none.
     */
    @Override
    public TMAspot getVisibleTMAspot() {
        return t.getVisibleTMAspot();
    }
 
    /**
     * Returns the current TMARKER session.
     * @return The current TMARKER main program.
     */
    //@Override
    //public tmarker getTMARKER() {
    //    return t;
    //}
    
    /**
     * Returns the color of the labels with a specific label and staining intensity.
     * @param staining One of TMALabel.STAINING_0, TMALabel.STAINING_1, TMALabel.STAINING_2, TMALabel.STAINING_3.
     * @param label One of TMALabel.LABEL_POS, TMALabel.LABEL_NEG, TMALabel.LABEL_UNK, TMALabel.LABEL_BG.
     * @return The color of the nuclei with this label and staining.
     */
    @Override
    public Color getLabelsColor(byte label, byte staining) {
        return t.getLabelsColorWOAlpha(label, staining);
    }
    
    /**
     * Repaints the visible TMAspot. Forces the currently opened plugins to paint 
     their information on the spot. Use this function if the plugin calculated
     something which should be made visible on the TMAspot.
     This function only refreshs the display of the spot. If you also want to update
     the cell counts, see updateTMAspot(TMAspot ts)
     * @see #updateTMAspot(tmarker.TMAspot.TMAspot)
     */
    @Override
    public void repaintVisibleTMAspot() {
        t.getTMAView().repaint();
    }

    /**
     * Sets the main progress bar in TMARKER to a specific value (between 0 and 100).
     * Use this if you want to show progress of the plugin's calculation.
     * @param percentage A value between 0 and 100.
     */
    @Override
    public void setProgressbar(int percentage) {
        t.setProgressbar(percentage);
    }

    /**
     * Sets TMARKER's status message to a certain String. Use this if you want to provide
     * information of the current status of the plugin to the user.
     * @param text The message to be shown.
     */
    @Override
    public void setStatusMessageLabel(String text) {
        t.setStatusMessageLabel(text);
    }

    /**
     * Returns the temporary directory of TMARKER, in which temp files can be created
     * (e.g. temp image files). Please take care that temp files are deleted after
     * TMARKER exit: File file = new File(...); file.deleteOnExit();
     * @return TMARKER's temporary directory which should be deleted on exit.
     */
    @Override
    public String getTmpDir() {
        return t.getTmpDir();
    }

    /**
     * Returns the current value of TMARKER's main progress bar.
     * @return A percentage value between 0 and 100.
     */
    @Override
    public int getProgressbarValue() {
        return t.getProgressbar().getValue();
    }

    /**
     * Return the currently shown status message.
     * @return The currently shown status message.
     */
    @Override
    public String getStatusMessageLabelText() {
        return t.getStatusMessageLabel().getText();
    }

    /**
     * Updates the given TMAspot, i.e. recalculates the nucleus counts. Equals updateTMAspot(ts, false);
     * @param ts The TMAspot to be updated. If it is the visible TMAspot, it is repainted, too.
     */
    @Override
    public void updateTMAspot(TMAspot ts) {
        updateTMAspot(ts, false);
    }
    
    /**
     * Updates the given TMAspot, i.e. recalculates the nucleus counts.
     * @param ts The TMAspot to be updated. If it is the visible TMAspot, it is repainted, too.
     * @param forceRepaint If true, the given TMAspot is repainted in any case. If false, 
     * the given TMAspot is only repainted, if it is not equal to the currently visible TMAspot.
     */
    @Override
    public void updateTMAspot(TMAspot ts, boolean forceRepaint) {
        if (ts==t.getVisibleTMAspot()) {
            t.getTMAView().showTMAspot(ts, forceRepaint);
        }
        t.updateTMATable(ts);
        t.getTSD().updateSummary(ts);
    }
    
    /**
     * Returns the current radius of the TMALabels (i.e. nuclei).
     * @return The radius of the nuclei.
     */
    @Override
    public int getLabelRadius() {
        return t.getLabelRadius();
    }
    
    /**
     * Sets the current radius of the TMALabels (i.e. nuclei).
     * @radius The radius of the nuclei.
     */
    @Override
    public void setLabelRadius(int radius) {
        t.setLabelRadius(radius);
    }

    /**
     * Returns whether or not parallel computing can be used (as selected by the user in the main
     * program).
     * @return True, if all processors on the machine can be used. False, if not.
     */
    @Override
    public boolean useParallelProgramming() {
        return t.getOptionDialog().useParallelProgramming();
    }
    
    /**
     * Sets the current directory of the main program. This directory is shown by default when
     * an "Open File" or "Save File" dialog is shown. It can be set when the user selected a file
     * to be opened or saved in a new directory.
     * @param absolutePath The path of the current directory.
     */
    @Override
    public void setCurrentDir(String absolutePath) {
        t.setCurrentDir(absolutePath);
    }
    
    /**
     * Gets the current directory of the main program. This directory can be shown by default when
     * an "Open File" or "Save File" dialog is shown. It can be set when the user selected a file
     * to be opened or saved in a new directory.
     * @return The path of the current directory.
     */
    @Override
    public String getCurrentDir() {
        return t.getCurrentDir();
    }
    
    /**
     * Sets the text in TMARKER's Info View. This can be HTML code.
     * @param text The text which is shown in the Info View.
     */
    @Override
    public void setInfoText(String text) {
        t.setUserInfo(text);
    }
    
    /**
     * Opens the "Open File..." dialog and lets the user to chose a valid, TMARKER readable
     * file(s) to be opened in the main program.
     * @param dir The default directory to be shown.
     */
    public void LoadFilesWithChooser(String dir) {
        tmarker.LoadFilesWithChooser(t, dir);
    }
    
    /**
     * Selects a given TMAspot in the main program.
     * @param ts The TMAspot to be selected. If ts is null or is not found, 
     * no spot is selected.
     */
    public void selectAndShowTMAspot(TMAspot ts) {
        if (ts != null) {
            t.selectTMAspot(ts);
            List<TMAspot> tss = t.getSelectedTMAspots(false);
            if (!tss.isEmpty()) {
                t.showTMAspot(tss.get(0));
            } else {
                t.showTMAspot(null);
            }
        }
    }
    
    
}

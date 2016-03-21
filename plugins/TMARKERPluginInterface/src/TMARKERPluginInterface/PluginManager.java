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

import java.awt.Color;
import java.io.File;
import java.util.List;
import tmarker.TMAspot.TMAspot;

/**
 * PluginManager for TMARKER. Defines all methods which are allowed as interface
 * between plugins and TMARKER (e.g. getters, setters etc...).
 * @author Peter J. Schüffler
 */
public interface PluginManager {

    /**
     * Returns all TMAspots of TMARKER.
     *
     * @return A list with all TMAspots (selected and not selected ones).
     */
    public List<TMAspot> getTMAspots();
    
    /**
     * Returns the TAMspot with a given name.
     * @param name The name of the searched TMAspot.
     *
     * @return The TAMspot with a given name or null if not found.
     */
    public TMAspot getTMAspot(String name);

    /**
     * Returns all TMAspots which are currently selected by the user.
     *
     * @return A list with all selected TMAspots.
     */
    public List<TMAspot> getSelectedTMAspots();

    /**
     * Returns the TMAspot which is currently visible in the TMAview of TMARKER.
     *
     * @return The TMAspot currently visible in TMARKER. Null, if there is none.
     */
    public TMAspot getVisibleTMAspot();

    /**
     * Repaints the visible TMAspot. Forces the currently opened plugins to
     * paint their information on the spot. Use this function if the plugin
     * calculated something which should be made visible on the TMAspot. This
     * function only refreshs the display of the spot. If you also want to
     * update the cell counts, see updateTMAspot(TMAspot ts)
     *
     * @see #updateTMAspot(tmarker.TMAspot.TMAspot)
     */
    public void repaintVisibleTMAspot();

    /**
     * Sets the main progress bar in TMARKER to a specific value (between 0 and
     * 100). Use this if you want to show progress of the plugin's calculation.
     *
     * @param percentage A value between 0 and 100.
     */
    public void setProgressbar(int percentage);

    /**
     * Sets TMARKER's status message to a certain String. Use this if you want
     * to provide information of the current status of the plugin to the user.
     *
     * @param text The message to be shown.
     */
    public void setStatusMessageLabel(String text);

    /**
     * Returns the temporary directory of TMARKER, in which temp files can be
     * created (e.g. temp image files). Please take care that temp files are
     * deleted after TMARKER exit: File file = new File(...);
     * file.deleteOnExit();
     *
     * @return TMARKER's temporary directory which should be deleted on exit.
     */
    public String getTmpDir();

    /**
     * Returns the current value of TMARKER's main progress bar.
     *
     * @return A percentage value between 0 and 100.
     */
    public int getProgressbarValue();

    /**
     * Return the currently shown status message.
     *
     * @return The currently shown status message.
     */
    public String getStatusMessageLabelText();

    /**
     * Updates the given TMAspot, i.e. recalculates the nucleus counts. Equals
     * updateTMAspot(ts, false);
     *
     * @param ts The TMAspot to be updated. If it is the visible TMAspot, it is
     * repainted, too.
     */
    public void updateTMAspot(TMAspot ts);

    /**
     * Updates the given TMAspot, i.e. recalculates the nucleus counts.
     *
     * @param ts The TMAspot to be updated. If it is the visible TMAspot, it is
     * repainted, too.
     * @param forceRepaint If true, the given TMAspot is repainted in any case.
     * If false, the given TMAspot is only repainted, if it is not equal to the
     * currently visible TMAspot.
     */
    public void updateTMAspot(TMAspot ts, boolean forceRepaint);

    /**
     * Returns the color of the labels with a specific label and staining
     * intensity.
     *
     * @param staining One of TMALabel.STAINING_0, TMALabel.STAINING_1,
     * TMALabel.STAINING_2, TMALabel.STAINING_3.
     * @param label One of TMALabel.LABEL_POS, TMALabel.LABEL_NEG,
     * TMALabel.LABEL_UNK, TMALabel.LABEL_BG.
     * @return The color of the nuclei with this label and staining.
     */
    public Color getLabelsColor(byte label, byte staining);

    /**
     * Returns the current radius of the TMALabels (i.e. nuclei).
     *
     * @return The radius of the nuclei.
     */
    public int getLabelRadius();

    /**
     * Sets the current radius of the TMALabels (i.e. nuclei).
     *
     * @param i The radius of the nuclei.
     */
    public void setLabelRadius(int i);

    /**
     * Returns whether or not parallel computing can be used (as selected by the
     * user in the main program).
     *
     * @return True, if all processors on the machine can be used. False, if
     * not.
     * @deprecated Use getNumberProcessors()>1 instead.
     */
    public boolean useParallelProgramming();

    /**
     * Sets the current directory of the main program. This directory is shown
     * by default when an "Open File" or "Save File" dialog is shown. It can be
     * set when the user selected a file to be opened or saved in a new
     * directory.
     *
     * @param absolutePath The path of the current directory.
     */
    public void setCurrentDir(String absolutePath);

    /**
     * Gets the current directory of the main program. This directory can be
     * shown by default when an "Open File" or "Save File" dialog is shown. It
     * can be set when the user selected a file to be opened or saved in a new
     * directory.
     *
     * @return The path of the current directory.
     */
    public String getCurrentDir();

    /**
     * Sets the text in TMARKER's Info View. This can be HTML code.
     *
     * @param text The text which is shown in the Info View.
     */
    public void setInfoText(String text);

    /**
     * Opens the "Open File..." dialog and lets the user to chose a valid,
     * TMARKER readable file(s) to be opened in the main program.
     *
     * @param dir The default directory to be shown.
     */
    public void LoadFilesWithChooser(String dir);

    /**
     * Selects a given TMAspot in the main program.
     *
     * @param ts The TMAspot to be selected. If ts is null or is not found, no
     * spot is selected.
     */
    public void selectAndShowTMAspot(TMAspot ts);

    /**
     * Load a specific file in TMARKER.
     *
     * @param file The file to be loaded. The file is only loaded if TMARKER can
     * interpret it.
     */
    public void loadFile(File file);

    /**
     * Load specific files in TMARKER.
     *
     * @param files The files to be loaded. The files are only loaded if TMARKER
     * can interpret them.
     */
    public void loadFiles(File[] files);
    
    /**
     * Saves the TMARKER session as XML.
     * @param file The file to be saved. Will be overwritten if it already exists.
     */
    public boolean SaveAsXML(File file);
    
    /**
     * Returns the column separator for excel files which the user has set.
     * @return Mostly ";" or ",".
     */
    public String getColumnSeparator();

    /**
     * Returns a list of currently loaded plugins.
     * @return A list of currently loaded plugins.
     */
    public List<Pluggable> getPlugins();
    
    /**
     * Returns the first plugin with given name.
     * @param name The name of the plugin (has to match [p].getPluginName()).
     * @return The loaded plugin with given name. Can be null if not existing.
     */
    public Pluggable getPlugin(String name);
    
    /**
     * Returns the number of processors that can be used for processing.
     * @return The number of processors that can be used for programming.
     */
    public int getNumberProcessors();
    
    /**
     * Returns the maximum heap space memory available.
     * @return The max heap space memory available for computation in byte.
     */
    public long getMaxMemory();
    
    
}

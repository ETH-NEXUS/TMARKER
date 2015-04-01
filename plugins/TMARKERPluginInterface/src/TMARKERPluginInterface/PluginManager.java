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
import java.util.List;
import tmarker.TMAspot.TMAspot;

/**
 * PluginManager for TMARKER. Defines all methods which are allowed as interface between plugins and TMARKER (e.g. getters, setters etc...).
 * @author Peter J. Schüffler
 */
public interface PluginManager {
 
  
  public List<TMAspot> getTMAspots();
  
  public List<TMAspot> getSelectedTMAspots();
  
  public TMAspot getVisibleTMAspot();
  
  public void repaintVisibleTMAspot();
  
  public void setProgressbar(int percentage);
  
  public void setStatusMessageLabel(String text);
  
  public String getTmpDir();
  
  public int getProgressbarValue();
  
  public String getStatusMessageLabelText();
  
  public void updateTMAspot(TMAspot ts);
  
  public void updateTMAspot(TMAspot ts, boolean forceRepaint);
  
  public Color getLabelsColor(byte label, byte staining);
  
  public int getLabelRadius();

  public void setLabelRadius(int i);
  
  public boolean useParallelProgramming();

  public void setCurrentDir(String absolutePath);
  
  public String getCurrentDir();
  
  public void setInfoText(String text);
  
  public void selectAndShowTMAspot(TMAspot ts);

}

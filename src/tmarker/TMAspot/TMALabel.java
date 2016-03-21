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
package tmarker.TMAspot;

import java.awt.Point;

/**
 * This class is a label on a TMAspot. A TMAspot can inherit multiple TMALabels.
 * TMARKER uses the TMALabel for cell nuclei. Other usages might be CISH points or
 * even text annotations.
 * TMALabels extend a 2D Point and have at least a x-coordinate and y-coordinate.
 * TMARKER tries to display all labels of a TMAspot in the image.
 * @author Peter J. Schüffler
 */
public abstract class TMALabel extends Point {
    
    /**
     * The label for clear nuclei.
     */
    public static final transient byte STAINING_0 = 0;
    
    /**
     * The label for mildly stained nuclei.
     */
    public static final transient byte STAINING_1 = 1;
    
    /**
     * The label for moderately stained nuclei.
     */
    public static final transient byte STAINING_2 = 2;
    
    /**
     * The label for markedly stained nuclei.
     */
    public static final transient byte STAINING_3 = 3;
    
    /**
     * The label for unknown nuclei.
     */
    public static final transient byte LABEL_UNK = -1;
    
    /**
     * The label for benign / healthy nuclei.
     */
    public static final transient byte LABEL_NEG = 0;
    
    /**
     * The label for cancerous nuclei.
     */
    public static final transient byte LABEL_POS = 1;
    
    /**
     * The label for background points.
     */
    public static final transient byte LABEL_BG = 2;
    
    /**
     * The String for benign nuclei.
     */
    public static final transient String LABEL_NEG_STRING = "benign";
    
    /**
     * The String for malignant nuclei.
     */
    public static final transient String LABEL_POS_STRING = "malignant";
    
    /**
     * The String for nuclei (foreground).
     */
    public static final transient String LABEL_FG_STRING = "nucleus";
    
    /**
     * The String for background points.
     */
    public static final transient String LABEL_BG_STRING = "background";
    
    
}

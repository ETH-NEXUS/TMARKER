/*
 * Copyright 2015 ETH Zurich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package importer.common.ui;

import java.awt.Component;
import java.io.File;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import importer.common.ui.VFileTreeModel.VFile;

public class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, @SuppressWarnings("hiding") boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus); //To change body of generated methods, choose Tools | Templates.
        if (value instanceof File) {
                setText(((File) value).getName());
        } else if(value instanceof VFile) {
        		setText(((VFile) value).getName());
        }
        return this;
    }
}
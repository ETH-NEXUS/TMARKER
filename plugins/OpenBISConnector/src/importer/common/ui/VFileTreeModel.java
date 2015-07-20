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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;

import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.FileInfoDssDTO;

public class VFileTreeModel implements TreeModel {

	static class VFile {
		private static final long serialVersionUID = 1L;
		
		private final String dataSetCode;
		private final String path;
		private final boolean isDirectory;
		private final List<VFile> children = new ArrayList<VFile>();
		
		public VFile(String dataSetCode, FileInfoDssDTO file) {
			this.dataSetCode = dataSetCode;
			this.path = file.getPathInDataSet();
			this.isDirectory = file.isDirectory();
		}
		
		public VFile(String dataSetCode, FileInfoDssDTO[] files) {
			this(dataSetCode, files[0]);
			
			for(int idx = 1; idx < files.length; idx++) {
				VFile vFile = new VFile(dataSetCode, files[idx]);
				VFile parentDirectoryName = findDirectoryByName(vFile.getParentDirectoryName());
				parentDirectoryName.addChild(vFile);
			}
		}
		
		public void addChild(VFile file) {
			children.add(file);
		}
		
		public VFile findDirectoryByName(String name) {
			if(this.isDirectory() && getName().equals(name)) {
				return this;
			} else {
				for(VFile child:listFiles()) {
					VFile file = child.findDirectoryByName(name);
					if(file != null) {
						return file;
					}
				}
			}
			return null;
		}
		
		public String getParentDirectoryName() {
			int lastIndex = path.lastIndexOf("/");
			if(lastIndex == -1) {
				return "/"; //Root
			} else {
				int previousIndex = -1;
				for(int i = lastIndex-1; i > 0; i--) {
					if(path.charAt(i) == '/') {
						previousIndex = i;
						break;
					}
				}
				return path.substring(previousIndex+1, lastIndex);
			}
		}

		public VFile[] listFiles() {
			return children.toArray(new VFile[children.size()]);
		}
		
		public boolean isDirectory() {
			return isDirectory;
		}
		
		public boolean isFile() {
			return !isDirectory();
		}
		
		
		public String getDataSetCode() {
			return dataSetCode;
		}

		public String getName() {
			if(path.lastIndexOf("/") != -1) {
				return path.substring(path.lastIndexOf("/")+1);
			} else {
				return path;
			}
		}

		public String getPath() {
			return path;
		}
	}
	
    private final VFile root;
    
    public VFileTreeModel(String dataSetCode, FileInfoDssDTO[] files) {
        this.root = new VFile(dataSetCode, files);
    }

    @Override
    public Object getChild(Object parent, int index) {
    	VFile f = (VFile) parent;
        return f.listFiles()[index];
    }

    @Override
    public int getChildCount(Object parent) {
    	VFile f = (VFile) parent;
        if (!f.isDirectory()) {
            return 0;
        } else {
            return f.listFiles().length;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
    	VFile par = (VFile) parent;
    	VFile ch = (VFile) child;
    	
        return Arrays.asList(par.listFiles()).indexOf(ch);
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf(Object node) {
    	VFile f = (VFile) node;
        return f.isFile();
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        //do nothing
    }
    
    @Override
    public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
        //do nothing
    }

    @Override
    public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
        //do nothing
    }

}
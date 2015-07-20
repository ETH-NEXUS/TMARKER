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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import common.config.Config;
import importer.common.facade.IFacade;
import thread.ThreadUtil;

public class FileTreeMouseAdapter extends MouseAdapter {
	
	private final IFacade facade;
	private final Config config;
	
	public FileTreeMouseAdapter(final IFacade facade, final Config config) {
		this.facade = facade;
		this.config = config;
	}
	
    private void myPopupEvent(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        JTree tree = (JTree) e.getSource();
        TreePath path = tree.getPathForLocation(x, y);
        if (path == null) {
            return;
        }

        int numberOfSelected = tree.getSelectionPaths().length;
        final Object file = (Object) path.getLastPathComponent();
        if(file instanceof File && numberOfSelected == 1 && ((File)file).isFile()) {
        	String label = "Open " + ((File)file).getName();
            JPopupMenu popup = new JPopupMenu();
            JMenuItem open = new JMenuItem(label);
            popup.add(open);
            popup.show(tree, x, y);
            open.addActionListener(new ActionListener () {
				@Override
				public void actionPerformed(@SuppressWarnings("hiding") ActionEvent e) {
					//Sequence sequence = Loader.loadSequence(((File)file).getPath(), 0, true);
                    //Icy.getMainInterface().addSequence(sequence);
				}
            });
        }
        
        if(file instanceof VFileTreeModel.VFile && numberOfSelected == 1 && ((VFileTreeModel.VFile)file).isFile()) {
        	String label = "Download " + ((VFileTreeModel.VFile)file).getName();
            JPopupMenu popup = new JPopupMenu();
            JMenuItem open = new JMenuItem(label);
            popup.add(open);
            popup.show(tree, x, y);
            open.addActionListener(new ActionListener () {
				@Override
				public void actionPerformed(@SuppressWarnings("hiding") ActionEvent e) {
					try {
						final String workspace = FileChooser.getWorkspace(config);
						if(workspace != null) {
							ThreadUtil.bgRun(new Runnable()
	                        {
								@Override
								public void run() {
									File downlodedFile = facade.downloadDataSetFile(((VFileTreeModel.VFile)file).getDataSetCode(), ((VFileTreeModel.VFile)file).getPath());
									//Sequence sequence = Loader.loadSequence(((File)downlodedFile).getPath(), 0, true);
				                    //Icy.getMainInterface().addSequence(sequence);
								}
	                        });
						}
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
            });
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            myPopupEvent(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            myPopupEvent(e);
        }
    }
}
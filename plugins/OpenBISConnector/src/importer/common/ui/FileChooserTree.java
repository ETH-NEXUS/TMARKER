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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.FileInfoDssDTO;
import common.config.Config;
import importer.common.facade.IFacade;
import importer.tmarker.TmarkerFacade;
import java.awt.Cursor;
import javax.swing.JDialog;
import javax.swing.JFrame;
import thread.ThreadUtil;

/**
 * @author pkupczyk
 */
public abstract class FileChooserTree extends JTree {

    private static final long serialVersionUID = 1L;

    protected IFacade facade;
    private Config config;

    public FileChooserTree(Config config, IFacade facade) {
        this.config = config;
        this.facade = facade;
        setModel(null);
    }

    public abstract void init();

    protected void init(FileChooserTreeNode root) {
        setModel(new FileChooserTreeModel(root));
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Pop Up to select item to download
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = getClosestRowForLocation(e.getX(), e.getY());
                    if (row>=0) {
                        setSelectionRow(row);
                    }
                    final FileChooserTreeNode node = (FileChooserTreeNode) getLastSelectedPathComponent();

                    if (node != null) {
                        TreePath path = getPathForLocation(e.getX(), e.getY());
                        Rectangle pathBounds = getUI().getPathBounds(FileChooserTree.this, path);

                        if (pathBounds != null && pathBounds.contains(e.getX(), e.getY())) {
                            if (isDownloadable(node)) {
                                JPopupMenu menu = new JPopupMenu();
                                String message = null;
                                if (!facade.isDownloadDataSetFileAvailable()) {
                                    message = "Download and open";
                                } else {
                                    message = "Download all files";
                                }
                                JMenuItem item1 = new JMenuItem(message);
                                item1.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent event) {
                                        ThreadUtil.bgRun(new Runnable() {
                                            @Override
                                            public void run() {
                                                onDownload(node);
                                            }
                                        });
                                    }
                                });
                                menu.add(item1);
                                if (facade.isDownloadDataSetFileAvailable() && facade.isGetAllDataSetFilesAvailable()) {
                                    final FileInfoDssDTO[] datasetFiles = facade.getAllDataSetFiles(node.getDisplayName());
                                    JMenuItem item2 = new JMenuItem("Download separate file");
                                    item2.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent event) {
                                            ThreadUtil.bgRun(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ThreadUtil.invokeLater(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            JDialog newWindow = new JDialog((JFrame) null, node.getDisplayName());

                                                            final JTree fileTree = FileTreeBuilder.getDirectoryTree(new VFileTreeModel(node.getDisplayName(), datasetFiles), new FileTreeMouseAdapter(facade, config));
                                                            final JScrollPane jScrollPanel = new JScrollPane(fileTree);
                                                                        //UIUtil.resize(fileTree);
                                                            //UIUtil.resize(jScrollPanel);
                                                            newWindow.add(jScrollPanel);
                                                            newWindow.setVisible(false);
                                                            newWindow.pack();
                                                        }

                                                    });
                                                }
                                            });
                                        }
                                    });
                                    menu.add(item2);
                                }
                                menu.show(FileChooserTree.this, pathBounds.x, pathBounds.y + pathBounds.height);
                            }
                        }
                    }
                } // direct download on double click
                else if (e.getClickCount()>1) {
                    int row = getClosestRowForLocation(e.getX(), e.getY());
                    if (row>=0) {
                        setSelectionRow(row);
                    }
                    final FileChooserTreeNode node = (FileChooserTreeNode) getLastSelectedPathComponent();

                    if (node != null) {
                        TreePath path = getPathForLocation(e.getX(), e.getY());
                        Rectangle pathBounds = getUI().getPathBounds(FileChooserTree.this, path);

                        if (pathBounds != null && pathBounds.contains(e.getX(), e.getY())) {
                            if (isDownloadable(node)) {
                                if (!facade.isDownloadDataSetFileAvailable()) {
                                    ThreadUtil.bgRun(new Runnable() {
                                        @Override
                                        public void run() {
                                            onDownload(node);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public void onDownload(final FileChooserTreeNode node) {
        try {
            final String workspace = FileChooser.getWorkspace(config);
            
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            if (workspace != null) {
                File file = null;
                try {
                    file = download(node);
                } finally {
                }

                if (file != null) { // If the user cancels the selection the file will be null, on this case the download should not happen
                    // Show progress
                    if (file.isFile()) {
                        if (((TmarkerFacade)facade).getOBC() != null && ((TmarkerFacade)facade).getOBC().getPluginManager() != null) {
                            ((TmarkerFacade)facade).getOBC().getPluginManager().loadFile(file);
                        }
                    } else if (file.isDirectory()) { // Show directory tree
                        final File fileCopy = file;
                        ThreadUtil.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JDialog newWindow = new JDialog((JFrame) null, fileCopy.getName());
                                final JTree fileTree = FileTreeBuilder.getDirectoryTree(new FileTreeModel(new File(fileCopy.getAbsolutePath())), new FileTreeMouseAdapter(facade, config));
                                final JScrollPane jScrollPanel = new JScrollPane(fileTree);
                                //UIUtil.resize(fileTree);
                                //UIUtil.resize(jScrollPanel);
                                newWindow.add(jScrollPanel);
                                newWindow.setVisible(true);
                                newWindow.pack();
                            }

                        });

                    }
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public abstract boolean isDownloadable(FileChooserTreeNode node);

    protected abstract File download(FileChooserTreeNode node);
}
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


import java.awt.BorderLayout;
import java.io.File;
import java.util.concurrent.Callable;

import javax.swing.JFileChooser;

import common.config.Config;
import common.config.DefaultConfig;

/**
 * @author pkupczyk
 */
public class FileChooser extends javax.swing.JFrame
{

    private static final long serialVersionUID = 1L;

    public FileChooser(FileChooserTree tree)
    {
        setTitle("openBIS file chooser");
        getContentPane().add(tree, BorderLayout.CENTER);

    }
    
    public static String getWorkspace(final Config config) throws Exception {
        JFileChooser chooser = new JFileChooser();
                        chooser.setCurrentDirectory(new File(config.getWorkspace()));
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int returnVal = chooser.showOpenDialog(null);

                        if (returnVal == JFileChooser.APPROVE_OPTION)
                        {
                        	String workspace = chooser.getSelectedFile().getAbsolutePath();
                        	config.setWorkspace(workspace);
                            DefaultConfig.save(config);
                            return chooser.getSelectedFile().getAbsolutePath();
                        } else
                        {
                            return null;
                        }
                    }

}
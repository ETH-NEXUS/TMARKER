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
package importer.tmarker;

import java.io.File;
import java.util.List;

import ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import common.config.Config;
import importer.common.facade.IFacade;
import importer.common.ui.FileChooserTree;
import importer.common.ui.FileChooserTreeNode;
import importer.common.ui.FileChooserTreeNode.NodeType;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * @author Peter J. Schueffler
 */
public class TmarkerFileChooserTree extends FileChooserTree {

    private static final long serialVersionUID = 1L;

    public TmarkerFileChooserTree(Config config, IFacade facade) {
        super(config, facade);
    }

    @Override
    public void init() {
        init(getRoot());
    }

    private FileChooserTreeNode getRoot() {
        FileChooserTreeNode root = new FileChooserTreeNode(NodeType.ROOT, "openBIS");

        for (DataSet dataset : facade.listDataSets()) {
            String expID = dataset.getExperimentIdentifier();
            if (expID != null && !expID.isEmpty()) {
                String[] identifierParts = expID.split("/");
                String spaceCode = identifierParts[1];
                String projectCode = identifierParts[2];
                String experimentCode = identifierParts[3];

                FileChooserTreeNode spaceNode = root.getNode(NodeType.SPACE, spaceCode);
                if (spaceNode == null) {
                    spaceNode = new FileChooserTreeNode(NodeType.SPACE, spaceCode);
                    root.addChild(spaceNode);
                }

                FileChooserTreeNode projectNode = spaceNode.getNode(NodeType.PROJECT,
                        projectCode);
                if (projectNode == null) {
                    projectNode = new FileChooserTreeNode(NodeType.PROJECT, projectCode);
                    spaceNode.addChild(projectNode);
                }

                FileChooserTreeNode experimentNode = projectNode.getNode(NodeType.EXPERIMENT, experimentCode);
                if (experimentNode == null) {
                    experimentNode = new FileChooserTreeNode(NodeType.EXPERIMENT, experimentCode);
                    projectNode.addChild(experimentNode);
                }

                experimentNode.addChild(new FileChooserTreeNode(NodeType.DATASET, "Dataset " + dataset.getRegistrationDate().toString() + " (" + dataset.getDataSetTypeCode() + ")", dataset));
            }
        }

        for (Sample sample : facade.listSamples()) {
            String sampleID = sample.getExperimentIdentifierOrNull();
            if (sampleID != null && !sampleID.isEmpty()) {
                String[] identifierParts = sampleID.split("/");
                String spaceCode = identifierParts[1];
                String projectCode = identifierParts[2];
                String experimentCode = identifierParts[3];

                FileChooserTreeNode spaceNode = root.getNode(NodeType.SPACE, spaceCode);
                if (spaceNode == null) {
                    spaceNode = new FileChooserTreeNode(NodeType.SPACE, spaceCode);
                    root.addChild(spaceNode);
                }

                FileChooserTreeNode projectNode = spaceNode.getNode(NodeType.PROJECT,
                        projectCode);
                if (projectNode == null) {
                    projectNode = new FileChooserTreeNode(NodeType.PROJECT, projectCode);
                    spaceNode.addChild(projectNode);
                }

                FileChooserTreeNode experimentNode = projectNode.getNode(NodeType.EXPERIMENT, experimentCode);
                if (experimentNode == null) {
                    experimentNode = new FileChooserTreeNode(NodeType.EXPERIMENT, experimentCode);
                    projectNode.addChild(experimentNode);
                }

                String sampleTag = facade.listDataSetsBySample(sample.getPermId()).isEmpty() ? " (no data set)" : "";
                experimentNode.addChild(new FileChooserTreeNode(NodeType.SAMPLE, "Sample " + sample.getCode() + sampleTag + " (" + sample.getSampleTypeCode() + ")", sample));
            }
        }

        return root;
    }

    @Override
    public boolean isDownloadable(FileChooserTreeNode node) {
        return NodeType.SAMPLE.equals(node.getType()) || NodeType.DATASET.equals(node.getType());
    }

    @Override
    protected File download(FileChooserTreeNode node) {
        if (node.getType().equals(NodeType.SAMPLE)) {
            String samplePermId = ((Sample) node.getObject()).getPermId();
            List<DataSet> dataSets = facade.listDataSetsBySample(samplePermId);
            if (!dataSets.isEmpty()) {
                return facade.downloadDataSet(dataSets.get(0).getCode());
            } else {
                //Inform user
                String message = "No dataset found for sample " + samplePermId;
                JOptionPane.showMessageDialog(null, message, "Could not download file", JOptionPane.ERROR_MESSAGE);
                Logger.getLogger(TmarkerFileChooserTree.class.getName()).log(Level.WARNING, message);
                return null;
            }
        } else if (node.getType().equals(NodeType.DATASET)) {
            return facade.downloadDataSet(((DataSet) node.getObject()).getCode());
        } else {
            return null;
        }
    }

}

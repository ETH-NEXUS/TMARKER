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

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * @author pkupczyk
 */
public class FileChooserTreeModel implements TreeModel
{

    private FileChooserTreeNode root;

    public FileChooserTreeModel(FileChooserTreeNode root)
    {
        this.root = root;
    }

    @Override
    public Object getRoot()
    {
        return root;
    }

    @Override
    public boolean isLeaf(Object node)
    {
        return ((FileChooserTreeNode) node).getChildren().isEmpty();
    }

    @Override
    public int getChildCount(Object parent)
    {
        return ((FileChooserTreeNode) parent).getChildren().size();
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        return ((FileChooserTreeNode) parent).getChildren().get(index);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        return ((FileChooserTreeNode) parent).getChildren().indexOf(child);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newvalue)
    {
    }

    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
    }

}
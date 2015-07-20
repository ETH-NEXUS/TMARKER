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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author pkupczyk
 */
public class FileChooserTreeNode implements Comparable<FileChooserTreeNode>
{
    public enum NodeType
    {
        ROOT, SPACE, PROJECT, EXPERIMENT, SAMPLE, DATASET
    }

    private final NodeType type;

    private final String displayName;

    private final Object object;

    private final Set<FileChooserTreeNode> children = new TreeSet<FileChooserTreeNode>();

    public FileChooserTreeNode(NodeType type, String displayName)
    {
        this.type = type;
        this.displayName = displayName;
        object = null;
    }

    public FileChooserTreeNode(NodeType type, String displayName, Object object)
    {
        this.type = type;
        this.displayName = displayName;
        this.object = object;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public NodeType getType()
    {
        return type;
    }

    public Object getObject()
    {
        return object;
    }

    public void addChild(FileChooserTreeNode child)
    {
        children.add(child);
    }

    public List<FileChooserTreeNode> getChildren()
    {
        return new ArrayList<FileChooserTreeNode>(children);
    }

    @SuppressWarnings("hiding")
    public FileChooserTreeNode getNode(NodeType type, String displayName)
    {
        LinkedList<FileChooserTreeNode> nodesToTest = new LinkedList<FileChooserTreeNode>();
        nodesToTest.add(this);
        while (nodesToTest.size() > 0)
        {
            FileChooserTreeNode nodeToTest = nodesToTest.removeFirst();
            if (nodeToTest.type.equals(type)
                    && nodeToTest.displayName.equals(displayName))
            {
                return nodeToTest;
            } else if (nodeToTest.children.size() > 0)
            {
                nodesToTest.addAll(nodeToTest.children);
            }
        }
        return null;
    }

    @Override
    public int compareTo(FileChooserTreeNode o)
    {
        return displayName.compareTo(o.displayName);
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
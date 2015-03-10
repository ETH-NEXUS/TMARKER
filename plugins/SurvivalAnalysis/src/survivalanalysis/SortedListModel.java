/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package survivalanalysis;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.AbstractListModel;

/**
 *
 * @author Peter J. Schueffler
 */
public class SortedListModel extends AbstractListModel {

    SortedSet<Object> model;
    
    public SortedListModel() {
        model = new TreeSet<Object>();
    }
    
    @Override
    public int getSize() {
        return model.size();
    }
    
    @Override
    public Object getElementAt(int index) {
        return model.toArray()[index];
    }
    
    public void add(Object element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }
    
    public void addElement(Object element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }
    
    public void addAll(Object elements[]) {
        Collection<Object> c = Arrays.asList(elements);
        model.addAll(c);
        fireContentsChanged(this, 0, getSize());
    }
    
    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }
    
    public boolean contains(Object element) {
        return model.contains(element);
    }
    
    public Object firstElement() {
        return model.first();
    }
    
    public Iterator iterator() {
        return model.iterator();
    }
    
    public Object lastElement() {
        return model.last();
    }
    
    public boolean removeElement(Object element) {
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }
    
    public boolean remove(int i) {
        return removeElement(getElementAt(i));
    }
    
    public boolean changeElement(Object element_old, Object element_new) {
        boolean removed = model.remove(element_old);
        if (removed) {
            model.add(element_new);
        }
        return removed;
    }
    
    public boolean containsString(String substring) {
        if (substring.equals("")) return true;
        for (Object e: model.toArray()) {
            if (substring != null) {
                if (e==null && e.toString().contains(substring)) return true;
            }
        }
        return false;
    }
}
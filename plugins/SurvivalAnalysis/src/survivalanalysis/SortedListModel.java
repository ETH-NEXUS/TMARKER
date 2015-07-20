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
 * A SortedListModel is an AbstractListModel in which the entries are sorted.
 * @author Peter J. Schueffler
 */
public class SortedListModel extends AbstractListModel {

    SortedSet<Object> model;
    
    /**
     * Creates a new SortedListModel.
     */
    public SortedListModel() {
        model = new TreeSet<>();
    }
    
    @Override
    public int getSize() {
        return model.size();
    }
    
    @Override
    public Object getElementAt(int index) {
        return model.toArray()[index];
    }
    
    /**
     * Adds an element to the list.
     * @param element The element to be added.
     */
    public void addElement(Object element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }
    
    /**
     * Add elements to the list.
     * @param elements The elements to be added.
     */
    public void addAll(Object elements[]) {
        Collection<Object> c = Arrays.asList(elements);
        model.addAll(c);
        fireContentsChanged(this, 0, getSize());
    }
    
    /**
     * Removes all elements from the list.
     */
    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }
    
    /**
     * Checks whether an element is in the list.
     * @param element The element to be searched.
     * @return True, if the element is in the list.
     */
    public boolean contains(Object element) {
        return model.contains(element);
    }
    
    /**
     * Returns the first element of the list.
     * @return The first element of the list.
     */
    public Object firstElement() {
        return model.first();
    }
    
    /**
     * Returns an iterator over the elements in this set. The elements are returned 
     * in no particular order (unless this set is an instance of some class that provides a guarantee).
     * @return An iterator over the elements in this set.
     */
    public Iterator iterator() {
        return model.iterator();
    }
    
    /**
     * Returns the last element of this list.
     * @return The last element of this list.
     */
    public Object lastElement() {
        return model.last();
    }
    
    /**
     * Removes an element from this list.
     * @param element The element to be removed.
     * @return True if the element could be removed.
     */
    public boolean removeElement(Object element) {
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }
    
    /**
     * Removes an element from this list.
     * @param i The index of the element to be removed.
     * @return True if the element could be removed.
     */
    public boolean remove(int i) {
        return removeElement(getElementAt(i));
    }
    
    /**
     * Exchanges an element by another.
     * @param element_old The element to be removed.
     * @param element_new The element to be added.
     * @return True if the exchange was successful.
     */
    public boolean changeElement(Object element_old, Object element_new) {
        boolean removed = model.remove(element_old);
        if (removed) {
            model.add(element_new);
        }
        return removed;
    }
    
    /**
     * Checks whether an element in this list contains a given (sub)string.
     * @param substring The string to be searched in the list.
     * @return True, if an element contains this string (substring allowed). False otherwise.
     */
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
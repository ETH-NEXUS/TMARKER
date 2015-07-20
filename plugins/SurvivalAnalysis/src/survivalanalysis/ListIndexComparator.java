/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package survivalanalysis;

import java.util.Comparator;
import java.util.List;

/**
 * A ListIndexComparator compares to elements by their indices.
 * @author Peter J. Schueffler
 */
public class ListIndexComparator implements Comparator<Integer> {

    private final List<Double> array;

	/**
     * Creates a new ListIndexComparator from a given double list. This is interesting
     * if the doubles are sorted. 
     * @param array The list for this comparator.
     */
    public ListIndexComparator(List<Double> array) {
        this.array = array;
    }

		/**
     * Creates a list from 0 to n. n is the number of doubles in the array used for
     * creation of this comparator.
     * @return An index list for all numbers in the double array.
     */
    public Integer[] createIndexArray() {
        Integer[] indices = new Integer[array.size()];
        for (int i = 0; i < array.size(); i++) {
            indices[i] = i; // Autoboxing
        }
        return indices;
    }

    @Override
    public int compare(Integer index1, Integer index2) {
        // Autounbox from Integer to int to use as array indexes
        return array.get(index1).compareTo(array.get(index2));
    }
}
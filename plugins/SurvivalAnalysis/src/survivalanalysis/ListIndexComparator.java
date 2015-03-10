/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package survivalanalysis;

import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Peter J. Schueffler
 */
public class ListIndexComparator implements Comparator<Integer> {

    private final List<Double> array;

    public ListIndexComparator(List<Double> array) {
        this.array = array;
    }

    public Integer[] createIndexArray() {
        Integer[] indexes = new Integer[array.size()];
        for (int i = 0; i < array.size(); i++) {
            indexes[i] = i; // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2) {
        // Autounbox from Integer to int to use as array indexes
        return array.get(index1).compareTo(array.get(index2));
    }
}
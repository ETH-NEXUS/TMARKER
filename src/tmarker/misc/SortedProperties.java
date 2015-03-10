/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmarker.misc;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * The SortedProperties class stores the TMARKER parameters in a alphabetically sorted manner.
 * @author Peter J. Schueffler
 */
public class SortedProperties extends Properties {

    /**
     * Overrides, called by the store method.
     */
    @Override
    public synchronized Enumeration<Object> keys() {
        Enumeration keysEnum = super.keys();
        @SuppressWarnings("unchecked")
        Vector<String> keyList = new Vector();
        while (keysEnum.hasMoreElements()) {
            keyList.add((String)(keysEnum.nextElement()));
        }
        Collections.sort(keyList);
        @SuppressWarnings("unchecked")
        Enumeration<Object> enum_out = (Enumeration)(keyList.elements());
        return enum_out;
    }   
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stainingestimation;

/**
 * A String to int converter to bind JSliders to JTextBoxes.
 * @author Peter J. Schueffler
 */
public class StringToIntConverter extends org.jdesktop.beansbinding.Converter<String, Integer> {
    
    @Override
    public Integer convertForward(String value) {
        int out = 0;
        try {
            out = Integer.parseInt(value);
        } catch (Exception ex) {
            
        } finally {
            return out;
        }
    }

    @Override
    public String convertReverse(Integer value) {
        String out = "";
        try {
            out = Integer.toString(value);
        } catch (Exception ex) {
            
        } finally {
            return out;
        }
    }
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package survivalanalysis;

import org.jfree.data.xy.XYDataItem;

/**
 * A XYSurvivalDataItem is a XYDataItem which additionally stores the information of being censored or not.
 * @author Peter J. Schueffler
 */
public class XYSurvivalDataItem extends XYDataItem {

    boolean censor;

    /**
     * Creates a new XYSurvivalDataItem. Runs super(x,y) and stores censor.
     * @param x The x-value (null NOT permitted).
     * @param y The y-value (null NOT permitted).
     * @param censor True: this is an event/death case. False: this is a censored case.
     */
    public XYSurvivalDataItem(double x, double y, boolean censor) {
        super(x, y);
        this.censor = censor;
    }

    /**
     * Wether or not this data item is a event case or censored case.
     * @return True: this is an event case (e.g. death). False: this is a censored case.
     */
    public boolean isEvent() {
        return censor;
    }
}
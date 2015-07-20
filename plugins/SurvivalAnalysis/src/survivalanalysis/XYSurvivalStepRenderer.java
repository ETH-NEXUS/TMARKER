/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package survivalanalysis;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * A renderer to draw a Survival Analysis curve (Kaplan Meier curve).
 * @author Peter J. Schueffler
 */
public class XYSurvivalStepRenderer extends XYStepRenderer {

    @Override
    protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation, XYDataset dataset, int series, int item, double x, double y, boolean negative) {
        super.drawItemLabel(g2, orientation, dataset, series, item, x, y, negative);
        if (!((XYSurvivalDataItem) (((XYSeriesCollection) dataset).getSeries(series).getItems().get(item))).isEvent()) {
            float width = ((BasicStroke) this.getSeriesStroke(series)).getLineWidth();
            g2.drawLine((int) x, (int) (y - 2*width), (int) x, (int) (y + 2*width));
        }
    }
}
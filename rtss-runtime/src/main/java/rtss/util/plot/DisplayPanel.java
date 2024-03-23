package rtss.util.plot;

import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

public class DisplayPanel extends JPanel
{
    public static final long serialVersionUID = 1;

    private List<JFreeChart> charts;

    /**
     * Creates a new panel with the specified layout manager.
     */
    public DisplayPanel(LayoutManager layout)
    {
        super(layout);
        this.charts = new ArrayList<>();
    }

    /**
     * Records a chart as belonging to this panel. It will subsequently be returned by the getCharts() method.
     */
    public void addChart(JFreeChart chart)
    {
        this.charts.add(chart);
    }

    /**
     * Returns an array containing the charts within this panel.
     */
    public JFreeChart[] getCharts()
    {
        int chartCount = this.charts.size();

        JFreeChart[] charts = new JFreeChart[chartCount];
        
        for (int i = 0; i < chartCount; i++)
            charts[i] = (JFreeChart) this.charts.get(i);

        return charts;
    }
}

package rtss.util.plot;

import java.awt.Color;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ChartXYSPlineBasic extends ApplicationFrame
{
    public static final long serialVersionUID = 1;

    private XYSeriesCollection dataset = new XYSeriesCollection();
    private String xLabel;
    private String yLabel;

    public ChartXYSPlineBasic()
    {
        this("");
    }

    public ChartXYSPlineBasic(String title)
    {
        this(title, "", "");
    }
    
    public ChartXYSPlineBasic(String title, String xLabel, String yLabel)
    {
        super(title);
        this.xLabel = xLabel;
        this.yLabel = yLabel;
    }
    
    public ChartXYSPlineBasic addSeries(String name, double[] y)
    {
        double[] x = new double[y.length];
        for (int k = 0; k < y.length; k++)
            x[k] = k;
        return addSeries(name, x, y);
    }

    public ChartXYSPlineBasic addSeries(String name, double[] x, double[] y)
    {
        XYSeries series = new XYSeries(name);
        
        for (int k = 0; k < x.length; k++)
            series.add(x[k], y[k]);
        
        dataset.addSeries(series);
        return this;
    }
    
    public void display() 
    {
        JPanel content = createPanel();
        content.setPreferredSize(new java.awt.Dimension(800, 600));
        getContentPane().add(content);

        pack();
        UIUtils.centerFrameOnScreen(this);
        setVisible(true);
    }    
    
    private JPanel createPanel()
    {
        // create plot
        NumberAxis xAxis = new NumberAxis(xLabel);
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis(yLabel);
        yAxis.setAutoRangeIncludesZero(false);

        XYSplineRenderer renderer1 = new XYSplineRenderer();
        renderer1.setDefaultShapesVisible(false);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer1);
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));

        // create and return the chart panel
        JFreeChart chart = new JFreeChart(getTitle(),
                                          JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        ChartUtils.applyCurrentTheme(chart);
        ChartPanel chartPanel = new ChartPanel(chart);
        return chartPanel;
    }
}

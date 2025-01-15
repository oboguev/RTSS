package rtss.util.plot;

import java.awt.Color;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import rtss.data.DoubleArray;
import rtss.data.population.Population;
import rtss.data.selectors.Gender;
import rtss.util.Util;

public class PopulationChart extends ApplicationFrame
{
    private static final long serialVersionUID = 1L;

    public PopulationChart()
    {
        this("");
    }

    public PopulationChart(String title)
    {
        super(title);
        this.xLabel = "Age";
        this.yLabel = "Population";
    }

    private XYSeriesCollection dataset = new XYSeriesCollection();
    private String xLabel;
    private String yLabel;

    public PopulationChart show(String name, Population p) throws Exception
    {
        double[] m_y = p.asArray(Gender.MALE);
        m_y = Util.multiply(m_y, -1);
        double[] m_x = years(m_y);

        double[] f_y = p.asArray(Gender.FEMALE);
        double[] f_x = years(f_y);

        addSeries(name + " MALE", m_x, m_y);
        addSeries(name + " FEMALE", f_x, f_y);

        return this;
    }

    public PopulationChart show(String name, DoubleArray m, DoubleArray f) throws Exception
    {
        double[] m_y = da2da(m.get());
        double[] m_x = years(m_y);

        double[] f_y = da2da(f.get());
        f_y = Util.multiply(f_y, -1);
        double[] f_x = years(f_y);

        addSeries(name + " MALE", m_x, m_y);
        addSeries(name + " FEMALE", f_x, f_y);

        return this;
    }

    private void addSeries(String name, double[] x, double[] y)
    {
        XYSeries series = new XYSeries(name);

        for (int k = 0; k < x.length; k++)
            series.add(x[k], y[k]);

        dataset.addSeries(series);
    }

    private double[] da2da(Double[] d)
    {
        double[] r = new double[d.length];
        for (int k = 0; k < d.length; k++)
            r[k] = d[k];
        return r;
    }

    private double[] years(double[] y)
    {
        double[] x = new double[y.length];
        for (int k = 0; k < y.length; k++)
            x[k] = k;
        return x;
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
        // JFreeChart chart = new JFreeChart(getTitle(), JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        JFreeChart chart = ChartFactory.createXYLineChart(getTitle(), // chart title
                                                          xLabel, // x axis label
                                                          yLabel, // y axis label
                                                          dataset, // data
                                                          PlotOrientation.HORIZONTAL,
                                                          true, // include legend
                                                          true, // tooltips
                                                          false // urls
        );

        ChartUtils.applyCurrentTheme(chart);
        ChartPanel chartPanel = new ChartPanel(chart);
        return chartPanel;
    }
}

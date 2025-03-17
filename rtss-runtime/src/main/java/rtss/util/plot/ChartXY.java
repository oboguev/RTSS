package rtss.util.plot;

import java.io.ByteArrayOutputStream;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import rtss.util.Util;

// F:\github\jfree-sponsor\jfreechart-demos-1.5.2\target
// java -jar jfreechart-demo-1.5.2-jar-with-dependencies.jar
// java -cp jfreechart-demo-1.5.2-jar-with-dependencies.jar org.jfree.chart.demo.LineChartDemo2
// F:\github\jfree-sponsor\jfreechart-demos-1.5.2\src\main\java\org\jfree\chart\demo

public class ChartXY extends ApplicationFrame
{
    public static void display(String title, double[] y)
    {
        new ChartXY(title, "x", "y")
                .addSeries(title, y)
                .display();
    }

    public static void display(String title, double[] x, double[] y)
    {
        new ChartXY(title, "x", "y")
                .addSeries(title, x, y)
                .display();
    }

    public static final long serialVersionUID = 1;

    private XYSeriesCollection dataset = new XYSeriesCollection();
    private String xLabel;
    private String yLabel;
    private boolean defaultShapesVisible = true;

    public ChartXY()
    {
        this("");
    }

    public ChartXY(String title)
    {
        this(title, "", "");
    }

    public ChartXY(String title, String xLabel, String yLabel)
    {
        super(title);
        this.xLabel = xLabel;
        this.yLabel = yLabel;
    }

    public ChartXY addSeries(String name, double[] y)
    {
        double[] x = new double[y.length];
        for (int k = 0; k < y.length; k++)
            x[k] = k;
        return addSeries(name, x, y);
    }

    public ChartXY addSeries(String name, double[] x, double[] y)
    {
        XYSeries series = new XYSeries(name);

        for (int k = 0; k < x.length; k++)
            series.add(x[k], y[k]);

        dataset.addSeries(series);
        return this;
    }

    public ChartXY defaultShapesVisible(boolean defaultShapesVisible)
    {
        this.defaultShapesVisible = defaultShapesVisible;
        return this;
    }

    public void display()
    {
        JPanel chartPanel = createPanel();
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(chartPanel);
        pack();
        UIUtils.centerFrameOnScreen(this);
        setVisible(true);
    }

    private JPanel createPanel()
    {
        JFreeChart chart = createChart(dataset);
        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    private JFreeChart createChart(XYDataset dataset)
    {
        // create the chart
        JFreeChart chart = ChartFactory.createXYLineChart(getTitle(), // chart title
                                                          xLabel, // x axis label
                                                          yLabel, // y axis label
                                                          dataset, // data
                                                          PlotOrientation.VERTICAL,
                                                          true, // include legend
                                                          true, // tooltips
                                                          false // urls
        );

        // get a reference to the plot for further customization
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultShapesVisible(defaultShapesVisible);
        renderer.setDefaultShapesFilled(true);

        // change the auto tick unit selection to integer units only
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;
    }

    public ChartXY exportImage(int cx, int cy, String fn) throws Exception
    {
        JFreeChart chart = createChart(dataset);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, cx, cy);
        Util.writeAsFile(fn, baos.toByteArray());

        return this;
    }
}

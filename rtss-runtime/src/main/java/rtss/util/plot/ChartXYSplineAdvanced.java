package rtss.util.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.UIUtils;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ChartXYSplineAdvanced extends ApplicationFrame
{
    public static void display(String title, double[] y)
    {
        new ChartXYSplineAdvanced(title, "x", "y")
                .addSeries(title, y)
                .display();
    }

    public static void display2(String title, double[] y1, double[] y2)
    {
        new ChartXYSplineAdvanced(title, "x", "y")
                .addSeries(title + "-1", y1)
                .addSeries(title + "-2", y2)
                .display();
    }

    public static void display(String title, double[] x, double[] y)
    {
        new ChartXYSplineAdvanced(title, "x", "y")
                .addSeries(title, x, y)
                .display();
    }

    public static final long serialVersionUID = 1;

    private XYSeriesCollection dataset = new XYSeriesCollection();
    private String xLabel;
    private String yLabel;
    private List<Boolean> asDots = new ArrayList<Boolean>();
    private boolean showSplinePane = true;

    public ChartXYSplineAdvanced()
    {
        this("");
    }

    public ChartXYSplineAdvanced(String title)
    {
        this(title, "", "");
    }

    public ChartXYSplineAdvanced(String title, String xLabel, String yLabel)
    {
        super(title);
        this.xLabel = xLabel;
        this.yLabel = yLabel;
    }

    public ChartXYSplineAdvanced showSplinePane(boolean b)
    {
        this.showSplinePane = b;
        return this;
    }

    public ChartXYSplineAdvanced addSeries(String name, double[] y)
    {
        double[] x = new double[y.length];
        for (int k = 0; k < y.length; k++)
            x[k] = k;
        return addSeries(name, x, y);
    }

    public ChartXYSplineAdvanced addSeries(String name, double[] x, double[] y)
    {
        XYSeries series = new XYSeries(name);

        for (int k = 0; k < x.length; k++)
            series.add(x[k], y[k]);

        dataset.addSeries(series);
        asDots.add(false);

        return this;
    }

    public ChartXYSplineAdvanced addSeriesAsDots(String name, double[] x, double[] y)
    {
        XYSeries series = new XYSeries(name);

        for (int k = 0; k < x.length; k++)
            series.add(x[k], y[k]);

        dataset.addSeries(series);
        asDots.add(true);

        return this;
    }

    public void display()
    {
        JPanel content = new MyDisplayPanel(this);
        content.setPreferredSize(new java.awt.Dimension(800, 600));
        getContentPane().add(content);

        pack();
        UIUtils.centerFrameOnScreen(this);
        setVisible(true);
    }

    static class MyDisplayPanel extends DisplayPanel
    {
        public static final long serialVersionUID = 1;

        /** Dataset 1. */
        private final XYDataset data1;

        private ChartXYSplineAdvanced chart;

        /**
         * Creates a new instance.
         */
        public MyDisplayPanel(ChartXYSplineAdvanced chart)
        {
            super(new BorderLayout());
            this.chart = chart;
            this.data1 = chart.dataset;
            add(createContent());
        }

        /**
         * Creates a tabbed pane for displaying sample charts.
         */
        private JTabbedPane createContent()
        {
            JTabbedPane tabs = new JTabbedPane();
            if (chart.showSplinePane)
                tabs.add("Splines:", createChartPanel1());
            tabs.add("Lines:", createChartPanel2());
            return tabs;
        }

        /**
         * Creates a chart based on the first dataset, with a fitted linear regression line.
         */
        private ChartPanel createChartPanel1()
        {
            // create plot
            NumberAxis xAxis = new NumberAxis(chart.xLabel);
            xAxis.setAutoRangeIncludesZero(false);
            NumberAxis yAxis = new NumberAxis(chart.yLabel);
            yAxis.setAutoRangeIncludesZero(false);

            XYSplineRenderer renderer1 = new XYSplineRenderer();
            XYPlot plot = new XYPlot(this.data1, xAxis, yAxis, renderer1);
            plot.setBackgroundPaint(Color.LIGHT_GRAY);
            plot.setDomainGridlinePaint(Color.WHITE);
            plot.setRangeGridlinePaint(Color.WHITE);
            plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));

            for (int k = 0; k < chart.asDots.size(); k++)
            {
                if (chart.asDots.get(k))
                {
                    renderer1.setSeriesShapesVisible(k, true);
                    renderer1.setSeriesLinesVisible(k, false);
                    renderer1.setSeriesShape(k, ShapeUtils.createDiamond(10.0f));
                }
            }

            // create and return the chart panel
            String title = chart.getTitle();
            JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            addChart(chart);
            ChartUtils.applyCurrentTheme(chart);
            ChartPanel chartPanel = new ChartPanel(chart);
            return chartPanel;
        }

        /**
         * Creates a chart based on the second dataset, with a fitted power regression line.
         */
        private ChartPanel createChartPanel2()
        {
            // create subplot 1
            NumberAxis xAxis = new NumberAxis(chart.xLabel);
            xAxis.setAutoRangeIncludesZero(false);
            NumberAxis yAxis = new NumberAxis(chart.yLabel);
            yAxis.setAutoRangeIncludesZero(false);

            XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
            XYPlot plot = new XYPlot(this.data1, xAxis, yAxis, renderer1);
            plot.setBackgroundPaint(Color.LIGHT_GRAY);
            plot.setDomainGridlinePaint(Color.WHITE);
            plot.setRangeGridlinePaint(Color.WHITE);
            plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));

            for (int k = 0; k < chart.asDots.size(); k++)
            {
                if (chart.asDots.get(k))
                {
                    renderer1.setSeriesShapesVisible(k, true);
                    renderer1.setSeriesLinesVisible(k, false);
                    renderer1.setSeriesShape(k, ShapeUtils.createDiamond(10.0f));
                }
            }

            // create and return the chart panel
            String title = chart.getTitle();
            JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            addChart(chart);
            ChartUtils.applyCurrentTheme(chart);
            ChartPanel chartPanel = new ChartPanel(chart);
            return chartPanel;
        }
    }
}

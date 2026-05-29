package rtss.util.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
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

    /**
     * Exports the chart as an image file.
     *
     * The exported chart is the same chart as the first visible UI tab:
     * spline chart when {@code showSplinePane} is true, otherwise line chart.
     *
     * The image format is selected from the filename extension:
     * {@code .png}, {@code .jpg}, or {@code .jpeg}.
     */
    public void exportImage(int cx, int cy, String fn) throws IOException
    {
        if (cx <= 0 || cy <= 0)
            throw new IllegalArgumentException("Image dimensions must be positive");

        if (fn == null || fn.trim().length() == 0)
            throw new IllegalArgumentException("Output file name must not be empty");

        JFreeChart jchart = new MyDisplayPanel(this, false).createExportChart();
        File file = new File(fn);
        String lower = fn.toLowerCase();

        if (lower.endsWith(".png"))
        {
            ChartUtils.saveChartAsPNG(file, jchart, cx, cy);
        }
        else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        {
            ChartUtils.saveChartAsJPEG(file, jchart, cx, cy);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported image extension; use .png, .jpg, or .jpeg: " + fn);
        }
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
            this(chart, true);
        }

        private MyDisplayPanel(ChartXYSplineAdvanced chart, boolean createSwingContent)
        {
            super(new BorderLayout());
            this.chart = chart;
            this.data1 = chart.dataset;

            if (createSwingContent)
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
         * Creates the spline chart.
         */
        private JFreeChart createChart1()
        {
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

            String title = chart.getTitle();
            JFreeChart jchart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            addChart(jchart);
            ChartUtils.applyCurrentTheme(jchart);
            return jchart;
        }

        /**
         * Creates a panel for the spline chart.
         */
        private ChartPanel createChartPanel1()
        {
            return new ChartPanel(createChart1());
        }

        /**
         * Creates the line chart.
         */
        private JFreeChart createChart2()
        {
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

            String title = chart.getTitle();
            JFreeChart jchart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            addChart(jchart);
            ChartUtils.applyCurrentTheme(jchart);
            return jchart;
        }

        /**
         * Creates a panel for the line chart.
         */
        private ChartPanel createChartPanel2()
        {
            return new ChartPanel(createChart2());
        }

        private JFreeChart createExportChart()
        {
            if (chart.showSplinePane)
                return createChart1();
            else
                return createChart2();
        }
    }
}

package rtss.util.plot;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import rtss.util.Util;

public class ChartXYSPlineBasic extends ApplicationFrame
{
    public static void display(String title, double[] y)
    {
        new ChartXYSPlineBasic(title, "x", "y")
            .addSeries(title, y)
            .display();        
    }
    
    public static void display(String title, double[] x, double[] y)
    {
        new ChartXYSPlineBasic(title, "x", "y")
            .addSeries(title, x, y)
            .display();        
    }

    public static final long serialVersionUID = 1;

    private List<XYSeries> splineSeries = new ArrayList<>();
    private List<XYSeries> lineSeries = new ArrayList<>();
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
        
        splineSeries.add(series);
        return this;
    }
    
    public ChartXYSPlineBasic addLineSeries(String name, double[] y)
    {
        double[] x = new double[y.length];
        for (int k = 0; k < y.length; k++)
            x[k] = k;
        return addLineSeries(name, x, y);
    }

    public ChartXYSPlineBasic addLineSeries(String name, double[] x, double[] y)
    {
        XYSeries series = new XYSeries(name);
        
        for (int k = 0; k < x.length; k++)
            series.add(x[k], y[k]);
        
        lineSeries.add(series);
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
        JFreeChart chart = createChart();
        ChartPanel chartPanel = new ChartPanel(chart);
        return chartPanel;
    }    
    
    private JFreeChart createChart()
    {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries series : lineSeries)
            dataset.addSeries(series);
        for (XYSeries series : splineSeries)
            dataset.addSeries(series);

        NumberAxis xAxis = new NumberAxis(xLabel);
        xAxis.setAutoRangeIncludesZero(false);

        NumberAxis yAxis = new NumberAxis(yLabel);
        yAxis.setAutoRangeIncludesZero(false);

        XYSplineRenderer splineRenderer = new XYSplineRenderer();
        splineRenderer.setDefaultShapesVisible(false);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, splineRenderer);
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));
        
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();
        lineRenderer.setDefaultShapesVisible(false);
        
        for (int k = 0; k < lineSeries.size(); k++)
            plot.setRenderer(k, lineRenderer, true);

        JFreeChart chart = new JFreeChart(getTitle(),
                                          JFreeChart.DEFAULT_TITLE_FONT, 
                                          plot, 
                                          true);
        ChartUtils.applyCurrentTheme(chart);
        
        return chart;
    }

    public ChartXYSPlineBasic exportImage(int cx, int cy, String fn) throws Exception
    {
        JFreeChart chart = createChart();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, cx, cy);
        Util.writeAsFile(fn, baos.toByteArray());

        return this;
    }
}

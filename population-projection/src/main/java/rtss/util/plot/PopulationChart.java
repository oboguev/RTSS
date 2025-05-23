package rtss.util.plot;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

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
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
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

    public static PopulationChart chart(String title)
    {
        return new PopulationChart(title);
    }

    public static void display(String title, Population p, String name) throws Exception
    {
        new PopulationChart(title).show(name, p).display();
    }

    public static void display(String title, PopulationContext p, String name) throws Exception
    {
        new PopulationChart(title).show(name, p).display();
    }

    public static void display(String title, Population p1, String name1, Population p2, String name2) throws Exception
    {
        new PopulationChart(title).show(name1, p1).show(name2, p2).display();
    }

    public static void display(String title, PopulationContext p1, String name1, PopulationContext p2, String name2) throws Exception
    {
        new PopulationChart(title).show(name1, p1).show(name2, p2).display();
    }

    public static void display(String title, Population p1, String name1, Population p2, String name2, Population p3, String name3) throws Exception
    {
        new PopulationChart(title).show(name1, p1).show(name2, p2).show(name3, p3).display();
    }

    public static void display(String title, PopulationContext p1, String name1, PopulationContext p2, String name2, PopulationContext p3, String name3) throws Exception
    {
        new PopulationChart(title).show(name1, p1).show(name2, p2).show(name3, p3).display();
    }
    
    public static void displayToScale(String title, Population pScale, String nameScale, Population p1, String name1) throws Exception
    {
        new PopulationChart(title).scale(nameScale, pScale).show(name1, p1).display();
    }

    private Population pScale = null;

    private XYSeriesCollection dataset = new XYSeriesCollection();
    private String xLabel;
    private String yLabel;

    public PopulationChart scale(String name, Population p) throws Exception
    {
        pScale = null;
        show(name, p);
        pScale = p;
        return this;
    }

    private final int DAYS_PER_YEAR = 365;

    public PopulationChart show(String name, PopulationContext p) throws Exception
    {
        if (Util.False)
            return show(name, p.toPopulation());

        if (pScale != null)
            p = RescalePopulation.scaleAllTo(p, pScale.sum());

        int days_per_sample = 10;
        int npoints = div_roundup(p.MAX_DAY + 1, days_per_sample);

        double[] m_y = new double[npoints];
        double[] m_x = new double[npoints];

        double[] f_y = new double[npoints];
        double[] f_x = new double[npoints];

        for (int np = 0; np < npoints; np++)
        {
            int nd1 = np * days_per_sample;
            int nd2 = Math.min(nd1 + days_per_sample - 1, p.MAX_DAY);
            int samplesize = nd2 - nd1 + 1;
            Util.assertion(samplesize >= 1);

            m_x[np] = f_x[np] = ((Population.MAX_AGE + 1.0) / (p.MAX_DAY + 1)) * ((nd1 + nd2) / 2);

            m_y[np] = p.sumDays(Gender.MALE, nd1, nd2) * DAYS_PER_YEAR / (double) samplesize;
            f_y[np] = p.sumDays(Gender.FEMALE, nd1, nd2) * DAYS_PER_YEAR / (double) samplesize;
        }

        m_y = Util.multiply(m_y, -1);

        addSeries(combine(name, "муж"), m_x, m_y);
        addSeries(combine(name, "жен"), f_x, f_y);

        return this;
    }

    public PopulationChart show(String name, Population p) throws Exception
    {
        if (pScale != null)
            p = RescalePopulation.scaleAllTo(p, pScale.sum());

        double[] m_y = p.asArray(Gender.MALE);
        m_y = Util.multiply(m_y, -1);
        double[] m_x = years(m_y);

        double[] f_y = p.asArray(Gender.FEMALE);
        double[] f_x = years(f_y);

        addSeries(combine(name, "муж"), m_x, m_y);
        addSeries(combine(name, "жен"), f_x, f_y);

        return this;
    }

    public PopulationChart show(String name, DoubleArray m, DoubleArray f) throws Exception
    {
        double[] m_y = da2da(m.get());
        double[] m_x = years(m_y);

        double[] f_y = da2da(f.get());
        f_y = Util.multiply(f_y, -1);
        double[] f_x = years(f_y);

        addSeries(combine(name, "муж"), m_x, m_y);
        addSeries(combine(name, "жен"), f_x, f_y);

        return this;
    }

    private String combine(String name, String gender)
    {
        if (name != null && name.trim().length() != 0)
            return name.trim() + " " + gender;
        else
            return gender;
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
        JFreeChart chart = createChart();
        ChartPanel chartPanel = new ChartPanel(chart);
        return chartPanel;
    }

    private JFreeChart createChart()
    {
        // create plot
        NumberAxis xAxis = new NumberAxis(xLabel);
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis(yLabel);
        yAxis.setAutoRangeIncludesZero(false);

        XYSplineRenderer renderer = new XYSplineRenderer();
        renderer.setDefaultShapesVisible(false);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
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
        return chart;
    }

    public PopulationChart exportImage(int cx, int cy, String fn) throws Exception
    {
        JFreeChart chart = createChart();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, cx, cy);
        Util.writeAsFile(fn, baos.toByteArray());

        return this;
    }

    private int div_roundup(int a, int b)
    {
        int r = a / b;
        if (r * b != a)
            r++;
        return r;
    }
}

package rtss.util.plot;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
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

// F:\github\jfree-sponsor\jfreechart-demos-1.5.2\target
// java -jar jfreechart-demo-1.5.2-jar-with-dependencies.jar
// java -cp jfreechart-demo-1.5.2-jar-with-dependencies.jar org.jfree.chart.demo.LineChartDemo2

public class ChartXY extends ApplicationFrame
{
    public ChartXY(String title)
    {
        super(title);
    }
    
    public void display() 
    {
        JPanel chartPanel = createDemoPanel();
        chartPanel.setPreferredSize(new java.awt.Dimension(700, 400));
        setContentPane(chartPanel);

        pack();
        UIUtils.centerFrameOnScreen(this);
        setVisible(true);
    }    

    private JPanel createDemoPanel() 
    {
        JFreeChart chart = createChart(createDataset());
        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        return panel;
    }    

    private JFreeChart createChart(XYDataset dataset) 
    {
        // create the chart...
        JFreeChart chart = ChartFactory.createXYLineChart(
            getTitle(),      // chart title
            "X",                      // x axis label
            "Y",                      // y axis label
            dataset,                  // data
            PlotOrientation.VERTICAL,
            true,                     // include legend
            true,                     // tooltips
            false                     // urls
        );

        // get a reference to the plot for further customisation...
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        XYLineAndShapeRenderer renderer
                = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);

        // change the auto tick unit selection to integer units only...
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;
    }
    
    private XYDataset createDataset() 
    {
        XYSeries series1 = new XYSeries("First");
        series1.add(1.0, 1.0);
        series1.add(2.0, 4.0);
        series1.add(3.0, 3.0);
        series1.add(4.0, 5.0);
        series1.add(5.0, 5.0);
        series1.add(6.0, 7.0);
        series1.add(7.0, 7.0);
        series1.add(8.0, 8.0);

        XYSeries series2 = new XYSeries("Second");
        series2.add(1.0, 5.0);
        series2.add(2.0, 7.0);
        series2.add(3.0, 6.0);
        series2.add(4.0, 8.0);
        series2.add(5.0, 4.0);
        series2.add(6.0, 4.0);
        series2.add(7.0, 2.0);
        series2.add(8.0, 1.0);

        XYSeries series3 = new XYSeries("Third");
        series3.add(3.0, 4.0);
        series3.add(4.0, 3.0);
        series3.add(5.0, 2.0);
        series3.add(6.0, 3.0);
        series3.add(7.0, 6.0);
        series3.add(8.0, 3.0);
        series3.add(9.0, 4.0);
        series3.add(10.0, 3.0);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);
        dataset.addSeries(series3);

        return dataset;
    }    
    
    public static void plot(String title, double[] x, double[] y) throws Exception
    {
        ChartXY chart = new ChartXY(title);
        chart.pack();
        UIUtils.centerFrameOnScreen(chart);
        chart.setVisible(true);        
    }

    public void zzzz(String title, double[] x, double[] y) throws Exception
    {
        XYSeries series = new XYSeries(title);
        
        for (int k = 0; k < x.length; k++)
            series.add(x[k], y[k]);
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
    }
}

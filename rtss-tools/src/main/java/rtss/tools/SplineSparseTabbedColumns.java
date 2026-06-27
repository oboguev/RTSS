package rtss.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.FunctionRangeExtenderDirect;
import rtss.util.Clipboard;
import rtss.util.Util;

/*
 * Интерполировать сплайном колонки разделённые TAB.
 * Первая строка -- имена колонок.
 * Первая колонка -- год.
 * Значения могут быть пропущены.
 */
public class SplineSparseTabbedColumns
{
    public static void main(String[] args)
    {
        try
        {
            new SplineSparseTabbedColumns().do_main();
            Util.out("*** Result was placed on the clipboard.");
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private List<String> columnNames;
    private Integer minYear;
    private Integer maxYear;
    private Map<String, Map<Integer, Double>> columnValues;
    private Map<String, Map<Integer, Double>> splines = new HashMap<>();
    private final String sep = "\t";
    private final String nl = "\n";

    private void do_main() throws Exception
    {
        String text = Clipboard.getText();
        if (text == null || text.length() == 0)
            throw new Exception("No data on the clipboard");

        parse(text);

        for (String cname : columnNames)
            spline(cname);

        StringBuilder sb = new StringBuilder("год");
        for (String cname : columnNames)
            sb.append(sep + cname);
        sb.append(nl);

        for (int year = minYear; year <= maxYear; year++)
        {
            sb.append("" + year);
            for (String cname : columnNames)
            {
                sb.append(sep);
                Double v = splines.get(cname).get(year);
                if (v != null)
                    sb.append(String.format("%.2f", v));

            }
            sb.append(nl);
        }

        text = sb.toString();

        if (File.separatorChar == '\\')
            text = text.replace("\n", "\r\n");

        Clipboard.put(text);
    }

    private void spline(String cname) throws Exception
    {
        Map<Integer, Double> m = columnValues.get(cname);
        List<Integer> years = new ArrayList<>(m.keySet());
        Collections.sort(years);
        
        Map<Integer, Double> m2 = new HashMap<>();

        if (years.size() == 1)
        {
            int year = years.get(0);
            m2.put(year,  m.get(year));
        }
        else
        {
            PolynomialSplineFunction sp = null;
            UnivariateFunction f = null;
            sp = new ConstrainedCubicSplineInterpolator().interpolate(l2a_int(years), l2a(years, m));
            f = new FunctionRangeExtenderDirect(sp);

            int min_year = minYear(years);
            int max_year = maxYear(years);

            for (int year = min_year; year <= max_year; year++)
                m2.put(year, f.value(year));
        }

        splines.put(cname, m2);
    }

    /* ====================================================================================== */

    void parse(String text) throws Exception
    {
        columnNames = new ArrayList<>();
        columnValues = new LinkedHashMap<>();
        minYear = null;
        maxYear = null;

        if (text == null)
            throw new Exception("Text is null");

        String[] lines = text.split("\\R", -1);

        // убрать пустые строки в конце, возникающие из-за завершающего LF
        int lineCount = lines.length;
        while (lineCount > 0 && lines[lineCount - 1].isEmpty())
            lineCount--;

        if (lineCount == 0)
            throw new Exception("Text is empty");

        String[] header = lines[0].split("\t", -1);

        if (header.length < 1)
            throw new Exception("Missing header");

        if (!header[0].equals("год"))
            throw new Exception("First column must be named 'год'");

        Set<String> seenColumns = new HashSet<>();

        for (int i = 0; i < header.length; i++)
        {
            String name = header[i].trim();

            if (name.isEmpty())
                throw new Exception("Empty column name at position " + (i + 1));

            if (!seenColumns.add(name))
                throw new Exception("Duplicate column name: " + name);

            if (i != 0)
            {
                columnNames.add(name);
                columnValues.put(name, new LinkedHashMap<>());
            }
        }

        Integer prevYear = null;

        for (int lineNo = 1; lineNo < lineCount; lineNo++)
        {
            String line = lines[lineNo];

            if (line.isEmpty())
                continue;

            String[] cells = line.split("\t", -1);

            if (cells.length > header.length)
                throw new Exception("Too many columns at line " + (lineNo + 1));

            String yearText = cells.length >= 1 ? cells[0].trim() : "";

            if (yearText.isEmpty())
                throw new Exception("Missing year at line " + (lineNo + 1));

            int year;

            try
            {
                year = Integer.parseInt(yearText);
            }
            catch (NumberFormatException ex)
            {
                throw new Exception("Invalid year at line " + (lineNo + 1) + ": " + yearText, ex);
            }

            if (prevYear != null && year <= prevYear)
                throw new Exception("Years must be unique and increasing: " + year);

            prevYear = year;

            if (minYear == null)
                minYear = year;

            maxYear = year;

            for (int i = 1; i < header.length; i++)
            {
                String valueText = i < cells.length ? cells[i].trim() : "";

                if (valueText.isEmpty())
                    continue;

                double value;

                try
                {
                    value = Double.parseDouble(valueText);
                }
                catch (NumberFormatException ex)
                {
                    throw new Exception("Invalid double at line " + (lineNo + 1) + ", column " + header[i] + ": " + valueText, ex);
                }

                columnValues.get(header[i].trim()).put(year, value);
            }
        }
    }

    private int minYear(Collection<Integer> coll)
    {
        List<Integer> list = new ArrayList<>(coll);
        Collections.sort(list);
        return list.get(0);
    }

    private int maxYear(Collection<Integer> coll)
    {
        List<Integer> list = new ArrayList<>(coll);
        Collections.sort(list);
        return list.get(list.size() - 1);
    }

    @SuppressWarnings("unused")
    private int minYear(Map<Integer, Double> m)
    {
        return minYear(m.keySet());
    }

    @SuppressWarnings("unused")
    private int maxYear(Map<Integer, Double> m)
    {
        return maxYear(m.keySet());
    }

    private double[] l2a_int(List<Integer> list)
    {
        double[] a = new double[list.size()];
        for (int k = 0; k < list.size(); k++)
            a[k] = list.get(k);
        return a;
    }

    private double[] l2a(List<Double> list)
    {
        double[] a = new double[list.size()];
        for (int k = 0; k < list.size(); k++)
            a[k] = list.get(k);
        return a;
    }

    private double[] l2a(List<Integer> years, Map<Integer, Double> y2v)
    {
        List<Double> values = new ArrayList<>();
        for (int year : years)
            values.add(y2v.get(year));
        return l2a(values);
    }
}

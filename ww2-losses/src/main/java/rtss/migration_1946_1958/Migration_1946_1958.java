package rtss.migration_1946_1958;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import com.opencsv.CSVReader;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Util;

/**
 * Вычислить количество мигрантов из других республик в городские центры РСФСР за 1946-1958 гг.
 * попадающих в определённое время рождения.
 * 
 * Исходные данные о миграции:
 *     Е. Андреев, Л. Дарский, Т. Харькова, "Население Советского Союза : 1922-1991", 
 *     РАН, научный совет "Проблемы демографии и трудовых ресурсов", М. : Наука, 1993, стр. 177-181
 */
public class Migration_1946_1958
{
    static class YearData
    {
        List<Bin> read_bins = new ArrayList<>();
        Bin[] bins;
        double[] perAge; 
    }
    
    private List<Integer> years = new ArrayList<>();
    private Map<Integer, YearData> year2data = new HashMap<>();

    /*
     * Общее количество мигрантов в РСФСР за 1946-1958 гг., по возрасту мигрантов в 1946 
     * 
     * Для определения количества родившихся с середины 1941 по середину 1945
     * вес корзины указывается пересечением интервалов:
     * 
     * in1946[0] = родившиеся в 45.1 - 46.12, вес = 0.25    
     * in1946[1] = родившиеся в 44.1 - 45.12, вес = 0.75     
     * in1946[2] = родившиеся в 43.1 - 44.12, вес = 1.00
     * in1946[3] = родившиеся в 42.1 - 43.12, вес = 1.00     
     * in1946[4] = родившиеся в 41.1 - 42.12, вес = 0.75     
     * in1946[5] = родившиеся в 40.1 - 41.12, вес = 0.25     
     * in1946[6] = родившиеся в 39.1 - 40.12    
     * in1946[7] = родившиеся в 38.1 - 39.12    
     * in1946[8] = родившиеся в 37.1 - 38.12    
     * in1946[9] = родившиеся в 36.1 - 37.12    
     *    
     * Для определения количества родившихся в 1938-1940 гг.
     * вес корзины указывается пересечением интервалов:
     *    
     * in1946[5] = родившиеся в 40.1 - 41.12, вес = 0.5     
     * in1946[6] = родившиеся в 39.1 - 40.12, вес = 1.0    
     * in1946[7] = родившиеся в 38.1 - 39.12, вес = 1.0    
     * in1946[8] = родившиеся в 37.1 - 38.12, вес = 0.5    
     */
    private double[] in1946 = new double[20];
    
    public static void main(String[] args)
    {
        try
        {
            Migration_1946_1958 m = new Migration_1946_1958();
            m.do_main();
        }
        catch (Exception ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }

        Util.out("");
        Util.out("*** Completed.");
    }

    private void do_main() throws Exception
    {
        load_data();
        interpolate_ages();
        sum_migration_by_age();
        print_born_mid1941_mid1945();
        Util.out("");
        print_born_1938_1940();
    }

    private void print_born_mid1941_mid1945()
    {
        double w0 = 0.25; 
        double w1 = 0.75; 
        double w2 = 1.00; 
        double w3 = 1.00; 
        double w4 = 0.75; 
        double w5 = 0.25;
        
        double x = w0 * in1946[0] + w1 * in1946[1] + w2 * in1946[2] + 
                   w3 * in1946[3] + w4 * in1946[4] + w5 * in1946[5];
        
        Util.out("Количество мигрантов из других республик в городские центры РСФСР за 1946-1958 гг.");
        Util.out("родившихся с середины 1941 г. по середину 1945 г.");
        Util.out(String.format("%.1f тыс. чел.", x));
    }
    
    private void print_born_1938_1940()
    {
        double w5 = 0.5;
        double w6 = 1.0;
        double w7 = 1.0;
        double w8 = 0.5;
        double x = w5 * in1946[5] + w6 * in1946[6] + w7 * in1946[7] + w8 * in1946[8];

        Util.out("Количество мигрантов из других республик в городские центры РСФСР за 1946-1958 гг.");
        Util.out("родившихся в 1938-1940 гг.");
        Util.out(String.format("%.1f тыс. чел.", x));
    }

    /*=====================================================================================*/

    private void sum_migration_by_age()
    {
        for (int year : years)
        {
            YearData yd = year2data.get(year);
            int offset = year - 1946;
            int ix_from = 0;
            int ix_to = offset;
            for (;;)
            {
                if (ix_from >= yd.perAge.length || ix_to >= in1946.length)
                    break;
                in1946[ix_to] += yd.perAge[ix_from];
                ix_from++;
                ix_to++;
            }
        }
    }

    /*=====================================================================================*/
    
    private void interpolate_ages() throws Exception
    {
        for (YearData yd : year2data.values())
            interpolate_ages(yd);
    }

    private void interpolate_ages(YearData yd) throws Exception
    {
        int PointsPerYear = 10;
        TargetPrecision precision = new TargetPrecision().eachBinAbsoluteDifference(0.1);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkNonNegative(false)
                .checkPositive(false)
                .basicSplineType(AkimaSplineInterpolator.class);
        double[] yy = MeanPreservingIterativeSpline.eval(yd.bins, PointsPerYear, options, precision);
        yd.perAge = Bins.ppy2yearly(yy, PointsPerYear);
    }

    /*=====================================================================================*/
    
    private void load_data() throws Exception
    {
        List<String[]> lines;
        String text = Util.loadResource("migration_1946_1958/balance_1946_1958.csv");
        try (CSVReader reader = new CSVReader(new StringReader(text)))
        {
            lines = reader.readAll();
        }        
        
        for (String[] sa : lines)
        {
            switch (sa[0])
            {
            case "всего":
            case "возраст не указан":
                continue;
            case "﻿годы":
                load_years(sa);
                break;
            default:
                load_numbers(sa);
                break;
            }
        }
        
        for (YearData yd : year2data.values())
            yd.bins = Bins.bins(yd.read_bins);
    }
    
    private void load_years(String[] sa)
    {
        for (int k = 1; k < sa.length; k++)
        {
            int year = Integer.parseUnsignedInt(sa[k]);
            years.add(year);
            year2data.put(year, new YearData());
        }
    }

    private void load_numbers(String[] sa) throws Exception
    {
        int yFirst = years.get(0);
        int yLast = years.get(years.size() - 1);
        
        int age1;
        int age2;
        
        String age = sa[0].replace("–", "-");
        if (age.contains("-"))
        {
            String[] s = age.split("-");
            if (s.length != 2)
                throw new Exception("Invalid file data");
            age1 = Integer.parseUnsignedInt(s[0]);
            age2 = Integer.parseUnsignedInt(s[1]);
        }
        else if (age.endsWith("+"))
        {
            age = Util.stripTail(age, "+");
            age1 = Integer.parseUnsignedInt(age);
            age2 = 100;
        }
        else
        {
            age1 = age2 = Integer.parseUnsignedInt(age);
        }
        
        int ix = 1;
        for (int year = yFirst; year <= yLast; year++)
        {
            double x = Double.parseDouble(sa[ix++]);
            Bin bin = new Bin(age1, age2, x);
            year2data.get(year).read_bins.add(bin);
        }
    }
}

package rtss.data.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.util.Util;

/**
 * Сгладить возрастное распределение для устранения 5-летней и 10-летней аккумуляции.
 * 
 * Пирамиды старых переписей выглядят ершисыми из-за склонности тогдашних респондентов при указании своего возраста
 * округлять его до ближайшей цифры кратной 5 или 10 (Б. Ястремский, "Можно ли пользоваться непосредственными данными
 * переписей о возрастном составе населения" // Вестник Статистики, 1920 №5-8, стр. 7-15 и №9-12 статья I-9).
 * 
 * Некоторые другие алгоритмы сглаживания, реализованные для проекта народонаселения ООН, см. в
 * https://timriffe.github.io/DemoTools/articles/smoothing_with_demotools.html
 * https://timriffe.github.io/DemoTools/articles/Age-heaping_quality_with_Demotools.html
 * 
 * Мы не применяем полиномиального сглаживания, т.к. оно имеет тенеденцию чрезмерно заглаживать действительные провалы
 * и поэтому плохо годится для историко-демографических исследований социальных катаклизмов.
 */
public class SmoothPopulation
{
    public static double[] smooth(double[] d) throws Exception
    {
        return smooth(d, "ABC");
    }

    public static double[] smooth(double[] d, String phases) throws Exception
    {
        double[] d0 = d;

        d = d.clone();

        if (phases.contains("A"))
        {
            /*
             * Smooth every 5-year point distributing its excess to 2 points before and 2 points after.
             * For 10-year points starting from age 30, distribute to 3 points before and 3 points after.
             */
            for (int k = 5; k < d.length; k += 5)
            {
                double excess10 = 0;
                boolean is10 = false;
                double excess5;

                if (k + 2 >= d.length)
                    break;

                if (k >= 30 && (k % 10) == 0 && k + 3 < d.length)
                {
                    is10 = true;
                    excess10 = d[k] - (d[k - 3] + d[k - 2] + d[k - 1] + d[k + 1] + d[k + 2] + d[k + 3]) / 6;
                }

                excess5 = d[k] - (d[k - 2] + d[k - 1] + d[k + 1] + d[k + 2]) / 4;

                if (is10 && excess10 > excess5)
                {
                    double excess = excess10;
                    if (excess > 0)
                    {
                        d[k] -= excess * (6.0 / 7.0);
                        d[k - 3] += excess / 7;
                        d[k - 2] += excess / 7;
                        d[k - 1] += excess / 7;
                        d[k + 1] += excess / 7;
                        d[k + 2] += excess / 7;
                        d[k + 3] += excess / 7;
                    }
                }
                else
                {
                    double excess = excess5;
                    if (excess > 0)
                    {
                        d[k] -= excess * (4.0 / 5.0);
                        d[k - 2] += excess / 5;
                        d[k - 1] += excess / 5;
                        d[k + 1] += excess / 5;
                        d[k + 2] += excess / 5;
                    }
                }
            }
        }

        if (phases.contains("B"))
        {
            /*
             * For every odd age, average with two neighbors
             */
            for (int k = 1; k < d.length; k += 2)
                ave(d, k);
        }

        if (phases.contains("C"))
        {
            /*
             * For every even age, average with two neighbors
             */
            for (int k = 2; k < d.length; k += 2)
                ave(d, k);
        }

        if (iswhole(d0))
            d = whole(d, d0);

        if (Util.differ(Util.sum(d), Util.sum(d0)))
            throw new Exception("Error in smoothig");

        /* make sure ages 0-2 are left intact */
        if (Util.differ(d[0], d0[0]) ||
            Util.differ(d[1], d0[1]) ||
            Util.differ(d[2], d0[2]))
        {
            throw new Exception("Error in smoothig: ages 0-2 changed");
        }

        return d;
    }

    private static void ave(double[] d, int k)
    {
        if (k == 0 || k + 1 >= d.length)
            return;

        /* leave ages 0-2 intact */
        if (k - 1 <= 2)
            return;

        double av = (d[k - 1] + d[k + 1]) / 2;
        double excess = d[k] - av;

        d[k] -= excess * (2.0 / 3.0);
        d[k - 1] += excess / 3;
        d[k + 1] += excess / 3;
    }

    private static boolean iswhole(double d)
    {
        return Math.abs(d - Math.round(d)) < 0.01;
    }

    private static boolean iswhole(double[] d)
    {
        for (double x : d)
        {
            if (!iswhole(x))
                return false;
        }

        return true;
    }

    private static double[] whole(double[] d)
    {
        double[] v = new double[d.length];
        for (int k = 0; k < d.length; k++)
            v[k] = Math.round(d[k]);
        return v;
    }

    private static double[] whole(double[] d, double[] d0) throws Exception
    {
        d = whole(d);
        distribute_excess(d, d0);
        return d;
    }

    private static void distribute_excess(double[] d, double[] d0) throws Exception
    {
        double excess = Util.sum(d) - Util.sum(whole(d0));
        int sign = Util.sign(excess);
        if (sign == 0)
            return;

        double[] diff = Util.sub(d, d0);
        List<IndexValue> list = new ArrayList<>();
        for (int k = 0; k < diff.length; k++)
        {
            if (Util.sign(diff[k]) != sign)
                diff[k] = 0;
            else
            {
                diff[k] = Math.abs(diff[k]);
                list.add(new IndexValue(k, diff[k]));
            }
        }

        Collections.sort(list);
        Collections.reverse(list);

        int i = -1;
        while (excess * sign > 0)
        {
            i++;
            if (i >= list.size())
                i = 0;
            d[list.get(i).index] -= sign;
            excess -= sign;
        }
    }

    private static class IndexValue implements Comparable<IndexValue>
    {
        int index;
        double value;

        IndexValue(int index, double value)
        {
            this.index = index;
            this.value = value;
        }

        @Override
        public int compareTo(IndexValue o)
        {
            return Util.sign(this.value - o.value);
        }
    }
}

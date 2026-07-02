package rtss.explore.mortality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityUtil;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.mortality.synthetic.MortalityTableADH;
import rtss.data.mortality.synthetic.MortalityTableGKS;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.rosbris.RosBrisDeathRates;
import rtss.rosbris.RosBrisTerritory;
import rtss.rosbris1959.RosBris1959DeathRates;
import rtss.util.Util;

public class ExporAllMortalityCurves
{
    public static void main(String[] args)
    {
        try
        {
            new ExporAllMortalityCurves().do_main(Gender.MALE);
            new ExporAllMortalityCurves().do_main(Gender.FEMALE);

            Util.out("** Completed");
        }
        catch (Exception ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private Map<Double, SingleMortalityTable> year2smt = new HashMap<>();
    private final double PROMILLE = 1000.0;
    private RosBris1959DeathRates rbdr;

    private void do_main(Gender gender) throws Exception
    {
        CombinedMortalityTable cmt;

        /* ============================================================================================================ */

        /*
         * АДХ-РСФСР 1927-1958
         */
        for (int year = 1927; year <= 1958; year++)
        {
            if (year >= 1941 && year <= 1945)
                continue;
            cmt = MortalityTableADH.getMortalityTable(Area.RSFSR, year);
            year2smt.put(year + 0.0, cmt.getSingleTable(Locality.TOTAL, gender));
        }

        /* ============================================================================================================ */

        /*
         * Таблицы Госкомстата 1958.5 1978.5 1986.5
         */
        cmt = CombinedMortalityTable.load("mortality_tables/RSFSR/1958-1959");
        year2smt.put(1958.5, cmt.getSingleTable(Locality.TOTAL, gender));

        PopulationByLocality p1979 = PopulationByLocality.census(Area.RSFSR, 1979);
        p1979 = null;
        cmt = MortalityTableGKS.getMortalityTable(Area.RSFSR, "1978-1979", p1979);
        year2smt.put(1978.5, cmt.getSingleTable(Locality.TOTAL, gender));

        PopulationByLocality p1989 = PopulationByLocality.census(Area.RSFSR, 1989);
        p1989 = null;
        cmt = MortalityTableGKS.getMortalityTable(Area.RSFSR, "1986-1987", p1989);
        year2smt.put(1986.5, cmt.getSingleTable(Locality.TOTAL, gender));

        /* ============================================================================================================ */

        /*
         * Исторические таблицы 1879 1897 1908
         */
        cmt = CombinedMortalityTable.loadMFT("mortality_tables/Russian-Empire/no63-50governorships-orthodox-1874-1883");
        year2smt.put(1879.0, cmt.getSingleTable(Locality.TOTAL, gender));

        cmt = CombinedMortalityTable.loadMFT("mortality_tables/Russian-Empire/no62-50governorships-orthodox-1896-1897");
        // cmt = CombinedMortalityTable.loadMFT("mortality_tables/Russian-Empire/no64-50governorships-orthodox-1896-1897-variant-1");
        // cmt = CombinedMortalityTable.loadMFT("mortality_tables/Russian-Empire/no65-50governorships-orthodox-1896-1897-variant-2");
        // cmt = CombinedMortalityTable.loadMFT("mortality_tables/Russian-Empire/novoselsky-1896-1897");
        year2smt.put(1897.0, cmt.getSingleTable(Locality.TOTAL, gender));

        cmt = CombinedMortalityTable.loadMFT("mortality_tables/Russian-Empire/no67-50governorships-orthodox-1907-1910-variant-2");
        year2smt.put(1908.0, cmt.getSingleTable(Locality.TOTAL, gender));

        /* ============================================================================================================ */

        /*
         * РосБРИС 1989-2002
         */
        for (int year = 1989; year <= 2022; year++)
        {
            RosBrisDeathRates dr = RosBrisDeathRates.loadMX(RosBrisTerritory.RF_BEFORE_2014, year);
            cmt = dr.toCombinedMortalityTable();
            year2smt.put(year + 0.0, cmt.getSingleTable(Locality.TOTAL, gender));
        }

        /* ============================================================================================================ */

        /*
         * РосБРИС 1959-1988
         */
        for (int year = 1959; year <= 1988; year++)
        {
            cmt = CombinedMortalityTable.loadMFT("mortality_tables/RSFSR/RosBRIS/" + year);
            year2smt.put(year + 0.0, cmt.getSingleTable(Locality.TOTAL, gender));
        }

        /* ============================================================================================================ */

        Util.out("");
        Util.out("Возрастные коэффициенты смертности qx для пола " + gender.name());
        List<Double> years = new ArrayList<>(year2smt.keySet());
        Collections.sort(years);

        StringBuilder sb = new StringBuilder("возраст");
        for (double year : years)
        {
            if (sb.length() != 0)
                sb.append(",");
            sb.append(formatYear(year));
        }
        Util.out(sb.toString());

        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            sb = new StringBuilder();
            sb.append("" + age);

            for (double year : years)
            {
                if (sb.length() != 0)
                    sb.append(",");
                SingleMortalityTable smt = year2smt.get(year);
                double qx = smt.qx()[age];
                sb.append("" + String.format("%.5f", qx * PROMILLE));
            }

            Util.out(sb.toString());
        }

        /* ============================================================================================================ */

        Util.out("");
        Util.out("Возрастные коэффициенты смертности q0/mx для пола " + gender.name());
        years = new ArrayList<>(year2smt.keySet());
        Collections.sort(years);

        sb = new StringBuilder("возраст");
        for (double year : years)
        {
            if (sb.length() != 0)
                sb.append(",");
            sb.append(formatYear(year));
        }
        Util.out(sb.toString());

        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            sb = new StringBuilder();
            sb.append("" + age);

            for (double year : years)
            {
                if (sb.length() != 0)
                    sb.append(",");
                SingleMortalityTable smt = year2smt.get(year);
                double qx = smt.qx()[age];
                double mx = MortalityUtil.qx2mx(qx, gender, age);
                sb.append("" + String.format("%.5f", (age == 0 ? qx : mx) * PROMILLE));
            }

            Util.out(sb.toString());
        }

        /* ============================================================================================================ */

        Util.out("");
        Util.out("Сокращённые возрастные коэффициенты смертности q0/mx для пола " + gender.name());

        List<Bin> bins = new ArrayList<>();
        bins.add(new Bin(0, 0, 0));
        bins.add(new Bin(1, 4, 0));
        bins.add(new Bin(5, 14, 0));
        bins.add(new Bin(15, 24, 0));
        bins.add(new Bin(25, 34, 0));
        bins.add(new Bin(35, 44, 0));
        bins.add(new Bin(45, 54, 0));
        bins.add(new Bin(55, 69, 0));

        years = new ArrayList<>(year2smt.keySet());
        Collections.sort(years);

        sb = new StringBuilder("средний возраст,возраст");
        for (double year : years)
        {
            if (sb.length() != 0)
                sb.append(",");
            sb.append(formatYear(year));
        }
        Util.out(sb.toString());

        for (Bin bin : bins)
        {
            sb = new StringBuilder();
            if (bin.age_x2 == 0)
                sb.append(String.format("%.1f,%d", bin.mid_x, bin.age_x1));
            else
                sb.append(String.format("%.1f,\"%d-%d\"", bin.mid_x, bin.age_x1, bin.age_x2));

            for (double year : years)
            {
                if (sb.length() != 0)
                    sb.append(",");

                SingleMortalityTable smt = year2smt.get(year);
                if (bin.age_x2 == 0)
                {
                    double qx = smt.qx()[bin.age_x1];
                    sb.append("" + String.format("%.5f", qx * PROMILLE));
                }
                else
                {
                    double mx_avg = average_mx(smt, gender, bin.age_x1, bin.age_x2);
                    sb.append("" + String.format("%.5f", mx_avg * PROMILLE));
                }
            }

            Util.out(sb.toString());
        }
    }

    private double average_mx(SingleMortalityTable smt, Gender gender, int age1, int age2) throws Exception
    {
        double mx_avg = 0;

        for (int age = age1; age <= age2; age++)
        {
            double qx = smt.qx()[age];
            double mx = MortalityUtil.qx2mx(qx, gender, age);
            mx_avg += mx;
        }
        
        mx_avg /= age2 - age1 + 1;
        
        return mx_avg;
    }

    private String formatYear(double year)
    {
        long iy = Math.round(year);
        if (iy == year)
            return "" + iy;
        else
            return String.format("%.1f", year);
    }
}

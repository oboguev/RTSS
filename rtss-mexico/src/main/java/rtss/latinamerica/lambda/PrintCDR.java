package rtss.latinamerica.lambda;

import java.util.List;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityUtil;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.math.algorithms.MathUtil;
import rtss.util.Util;

public class PrintCDR
{
    public static void main(String[] args)
    {
        try
        {
            new PrintCDR().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        Util.out("Смертность по LAMBdA, расчёт тремя способами");
        Util.out("");
        Util.out("  Способ 1: qx + межпереписная интерполяция годового прироста населения");
        Util.out("  Способ 2: (лучше) mx из qx");
        Util.out("  Способ 3 (самый надёжный): предрасчитанные mx");
        Util.out("");

        for (String rname : CountryName.rnames())
        {
            for (int year : LambdaPopulation.countryPopulationYears(rname))
            {
                Population p = LambdaPopulation.countryPopulation(rname, year);
                CombinedMortalityTable cmt = LambdaMortalityTable.countryMortalityTable(rname, year);
                if (p == null || cmt == null)
                    continue;

                double deaths = qx_deaths(p, cmt);

                double pstart = p.sum();
                double pend = pstart + yearlyIncrease(rname, year, true, true);
                double pavg = MathUtil.log_average(pstart, pend);
                double cdr1 = deaths / pavg * 1000.0;

                /*----------------------------------------------------------------- */

                deaths = qx2mx_deaths(p, cmt);

                pavg = p.sum();
                double cdr2 = deaths / pavg * 1000.0;

                /*----------------------------------------------------------------- */

                deaths = mx_deaths(p, rname, year, Gender.MALE) + mx_deaths(p, rname, year, Gender.FEMALE);
                pavg = p.sum();
                double cdr3 = deaths / pavg * 1000.0;

                Util.out(String.format("%s %d %.1f %.1f %.1f", rname, year, cdr1, cdr2, cdr3));
            }
        }
    }

    private double qx_deaths(Population p, CombinedMortalityTable cmt) throws Exception
    {
        return qx_deaths(p, cmt, Gender.MALE) + qx_deaths(p, cmt, Gender.FEMALE);
    }

    private double qx_deaths(Population p, CombinedMortalityTable cmt, Gender gender) throws Exception
    {
        double[] qx = cmt.getSingleTable(Locality.TOTAL, gender).qx();
        double sum = 0;

        for (int age = 0; age <= Population.MAX_AGE; age++)
            sum += p.get(gender, age) * qx[age];

        return sum;
    }

    private double qx2mx_deaths(Population p, CombinedMortalityTable cmt) throws Exception
    {
        return qx2mx_deaths(p, cmt, Gender.MALE) + qx2mx_deaths(p, cmt, Gender.FEMALE);
    }

    private double qx2mx_deaths(Population p, CombinedMortalityTable cmt, Gender gender) throws Exception
    {
        double[] qx = cmt.getSingleTable(Locality.TOTAL, gender).qx();
        double[] mx = MortalityUtil.qx2mx(qx, gender);
        double sum = 0;

        for (int age = 0; age <= Population.MAX_AGE; age++)
            sum += p.get(gender, age) * mx[age];

        return sum;
    }

    private double yearlyIncrease(String cname, int year, boolean before, boolean after) throws Exception
    {
        List<Integer> years = LambdaPopulation.countryPopulationYears(cname);
        Integer ybefore = null;
        Integer yafter = null;

        for (int y : years)
        {
            if (before && y < year && (ybefore == null || y > ybefore))
                ybefore = y;

            if (after && y > year && (yafter == null || y < yafter))
                yafter = y;
        }

        if (ybefore == null)
            ybefore = year;

        if (yafter == null)
            yafter = year;

        if (ybefore == yafter)
            return 0;

        Population xp1 = LambdaPopulation.countryPopulation(cname, ybefore);
        Population xp2 = LambdaPopulation.countryPopulation(cname, yafter);

        double p1 = xp1.sum();
        double p2 = xp2.sum();

        int y1 = ybefore;
        int y2 = yafter;

        if (y2 <= y1)
            throw new IllegalArgumentException("y2 must be greater than y1");

        if (year < y1 || year > y2)
            throw new IllegalArgumentException("year must be between y1 and y2");

        if (p1 <= 0 || p2 <= 0)
            throw new IllegalArgumentException("Population must be positive for exponential interpolation");

        double r = (Math.log(p2) - Math.log(p1)) / (y2 - y1);

        double pYear = p1 * Math.exp(r * (year - y1));

        double v = pYear * Math.expm1(r);

        return v;
    }

    private double mx_deaths(Population p, String rname, int year, Gender gender) throws Exception
    {
        double[] mx = LambdaMortalityTable.loadMx(rname, year, gender);
        
        double sum = 0;

        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            sum += p.get(gender, age) * mx[age];
        }

        return sum;
    }
}

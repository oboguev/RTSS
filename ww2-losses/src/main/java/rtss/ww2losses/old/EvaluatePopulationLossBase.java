package rtss.ww2losses.old;

import rtss.data.selectors.BirthDeath;
import rtss.ww2losses.params.AreaParameters;
import rtss.util.Util;

public abstract class EvaluatePopulationLossBase
{
    public static class Population
    {
        /* population at start, end and middle of the period */
        public double start;
        public double mid;
        public double end;
        public double births;
        public double deaths;
    }
    
    /* for each of four war years (mid-1941 to mid-1945) */
    public static class Year
    {
        public Year next;
        public Year prev;
        
        public Year(int nyear)
        {
            this.nyear = nyear;
        }
        
        public int nyear; // 0 to 3 or 4
        
        /* actual population at start, end and middle of the period */
        Population actual = new Population(); 

        /* expected population at start, end and middle of the period, per 1940 rates*/
        Population expected = new Population(); 
    }
    
    public static int NYears;
    
    protected Year[] years;
    protected Year firstYear;
    protected Year lastYear;
    
    /* average yearly growth as multiplier and in promille */
    protected double actualGrowthFactor;
    protected double actualGrowthPromille;
    
    protected AreaParameters params;
    
    public EvaluatePopulationLossBase(AreaParameters params)
    {
        this.params = params;
        NYears = params.NYears;
        years = new Year[NYears];
    }

    protected void init() throws Exception
    {
        for (int ny = 0; ny < NYears; ny++)
            years[ny] = new Year(ny);

        firstYear = years[0];
        lastYear = years[NYears - 1];

        for (int ny = 0; ny < NYears; ny++)
        {
            if (ny != 0)
                years[ny].prev = years[ny - 1];
            if (ny != NYears - 1)
                years[ny].next = years[ny + 1];
        }

        firstYear.actual.start = params.ACTUAL_POPULATION_START;
        lastYear.actual.end = params.ACTUAL_POPULATION_END;
        firstYear.expected.start = params.ACTUAL_POPULATION_START;

        double factor = promille2factor(params.CBR_1940 - params.CDR_1940);
        for (Year y : years)
        {
            Population p = y.expected;
            
            p.end = p.start * factor;
            if (y.next != null)
                y.next.expected.start = y.expected.end;

            p.mid = root(p.start * p.end, 2);
            
            /* define rates as attached to the start of the period */
            p.births = p.start * params.CBR_1940 / 1000.0;
            p.deaths = p.start * params.CDR_1940 / 1000.0;
            
            verify_eq(p.end - p.start, p.births - p.deaths);
        }

        actualGrowthFactor = root(lastYear.actual.end / firstYear.actual.start, NYears);
        actualGrowthPromille = factor2promille(actualGrowthFactor);
    }

    public abstract void evaluate() throws Exception;
    
    protected double root(double x, int level)
    {
        return Math.pow(x, 1.0 / level);
    }
    
    protected double factor2promille(double factor)
    {
        return 1000 * (factor - 1.0); 
    }
    
    protected double promille2factor(double promille)
    {
        return 1 + promille / 1000.0;
    }
    

    protected double factor2promille(double factor, BirthDeath which)
    {
        switch (which)
        {
        case BIRTH:
            return 1000 * (factor - 1.0); 
        case DEATH:
        default:
            return 1000 * (1.0 - factor); 
        }
    }
    
    protected double promille2factor(double promille, BirthDeath which)
    {
        switch (which)
        {
        case BIRTH:
            return 1 + promille / 1000.0;
        case DEATH:
        default:
            return 1 - promille / 1000.0;
        }
    }

    protected void verify_eq(double a, double b) throws Exception
    {
        double adiff = Math.abs(a - b);
        double max = Math.max(Math.abs(a), Math.abs(b));
        if (adiff / max > 0.0001)
            throw new Exception("Data divergence");
    }
    
    protected double expectedDeaths()
    {
        double v = 0;
        for (Year y : years)
        {
            Population p = y.expected;
            v += p.deaths;
        }
        return v;
    }

    protected double expectedBirths()
    {
        double v = 0;
        for (Year y : years)
        {
            Population p = y.expected;
            v += p.births;
        }
        return v;
    }

    protected double actualDeaths()
    {
        double v = 0;
        for (Year y : years)
        {
            Population p = y.actual;
            v += p.deaths;
        }
        return v;
    }

    protected double actualBirths()
    {
        double v = 0;
        for (Year y : years)
        {
            Population p = y.actual;
            v += p.births;
        }
        return v;
    }
    
    protected double excessDeaths()
    {
        return actualDeaths() - expectedDeaths();
    }

    protected double excessBirths()
    {
        return actualBirths() - expectedBirths();
    }

    protected double birthsDeficit()
    {
        return -excessBirths();
    }
    
    protected void print(boolean printRates)
    {
        String sep = " | ";
        String hdr;
        Util.out("");
        
        hdr = " ";
        hdr += sep + "                     EXPECTED                         ";
        hdr += sep + "                      ACTUAL                          ";
        hdr += sep + "  births     excess  ";
        if (printRates)
        {
            hdr += sep + "birth death";
        }
        Util.out(hdr);

        hdr = "Y";
        hdr += sep + "  start       mid        end       births     deaths  ";
        hdr += sep + "  start       mid        end       births     deaths  ";
        hdr += sep + "  deficit    deaths  ";
        if (printRates)
        {
            hdr += sep + "rate  rate";
        }
        Util.out(hdr);

        hdr = "=";
        hdr += sep + "========== ========== ========== ========== ==========";
        hdr += sep + "========== ========== ========== ========== ==========";
        hdr += sep + "========== ==========";
        if (printRates)
        {
            hdr += sep + "===== =====";
        }
        Util.out(hdr);
        
        double totalBirthsDeficit = 0;
        double totalExcessDeaths = 0;
        
        for (Year y : years)
        {
            String msg = String.format("%1d", y.nyear + 1);
            msg += sep + format(y.expected);
            msg += sep + format(y.actual);
            msg += sep + String.format("%10.3f %10.3f", 
                                       y.expected.births - y.actual.births, y.actual.deaths - y.expected.deaths);
            if (printRates)
            {
                double cbr = 1000 * y.actual.births / y.actual.start;
                double cdr = 1000 * y.actual.deaths / y.actual.start;
                msg += sep + String.format("%5.2f %5.2f", cbr, cdr); 
            }

            Util.out(msg);
            
            totalBirthsDeficit += y.expected.births - y.actual.births;
            totalExcessDeaths += y.actual.deaths - y.expected.deaths;
        }
        
        Util.out("");
        Util.out(String.format("Total births deficit: %10.3f", totalBirthsDeficit));
        Util.out(String.format("Total excess deaths: %10.3f", totalExcessDeaths));
    }
    
    private String format(Population p)
    {
        return String.format("%10.3f %10.3f %10.3f %10.3f %10.3f", 
                             p.start, p.mid, p.end, p.births, p.deaths);
    }
}

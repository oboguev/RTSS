package rtss.ww2losses;

import rtss.ww2losses.params.AreaParameters;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class Main
{
    public static void main(String[] args)
    {
        try 
        {
            Main m = new Main();
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
        do_main(Area.RSFSR, 4, 0.68);

        Util.out("");
        Util.out("====================================================================");
        Util.out("");
        Util.out("РСФСР: defactor 1940 birth rates from 1940-1944 birth rates ...");
        Util.out("");
        EvaluatePopulationLossBase epl = new Defactor(AreaParameters.forArea(Area.RSFSR, 4));
        epl.evaluate();

        do_main(Area.USSR, 4, 0.68);
        do_main(Area.USSR, 5, 0.68);
        
        AreaParameters ap;
        ap = AreaParameters.forArea(Area.USSR, 4);
        do_show(Area.USSR, ap, 4.0, 196_716.0, backward_6mo(170_548, ap), 0.68);
    }
    
    private void do_main(Area area, int nyears, double survival_rate) throws Exception
    {
        String syears = null;
        if (nyears == 4)
        {
            syears = "за 4 года (середина 1941 - середина 1945)";
        }
        else if (nyears == 5)
        {
            syears = "за 5 лет (начало 1941 - начало 1946)";
        }
        else
        {
            throw new IllegalArgumentException();
        }
        
        syears += " и средней доживаемости с военного времени до января 1959 года равной " + survival_rate;
        
        Util.out("");
        Util.out("**********************************************************************************************************************************************");
        Util.out("*****   Расчёт для " + area.toString() + " " + syears + ":");
        Util.out("**********************************************************************************************************************************************");
        Util.out("");
        Util.out("Compute minimum births window ...");
        Util.out("");
        new BirthTrough().calcTrough(area);
        
        AreaParameters params = AreaParameters.forArea(area, nyears);
        
        Util.out("");
        Util.out("====================================================================");
        Util.out("");
        
        Util.out("Compute at constant CDR and CBR ...");
        Util.out("");
        EvaluatePopulationLossBase epl = new EvaluatePopulationLossVariantA(params);
        epl.evaluate();

        Util.out("");
        Util.out("====================================================================");
        Util.out("");

        Util.out("Compute at constant excess deaths number ...");
        Util.out("");
        epl = new EvaluatePopulationLossVariantB(params);
        epl.evaluate();

        Util.out("");
        Util.out("====================================================================");
        Util.out("");

        Util.out("Recombining half-year rates ...");
        Util.out("");
        epl = new RecombineRates(params);
        epl.evaluate();
    }
    
    private void do_show(Area area, AreaParameters ap, double nyears, Double actual_start, Double actual_end, double survival_rate) throws Exception
    {
        String pre = "*****   ";
        StringBuffer syears = new StringBuffer(String.format("за %s года (%s - %s)\n%sпри средней доживаемости рождённых в 1941-1945 гг. до переписи 15 января 1959 = %.2f", 
                                                             d2s(nyears), ny2s(1941.5), ny2s(1941.5 + nyears), pre, survival_rate));
        Util.out("");
        Util.out("**********************************************************************************************************************************************");
        Util.out(pre + "Расчёт для " + area.toString() + " " + syears + ":");
        Util.out("**********************************************************************************************************************************************");
        Util.out("");
        
        if (actual_start != null)
            ap.ACTUAL_POPULATION_START = actual_start;
        actual_start = ap.ACTUAL_POPULATION_START;

        if (actual_end != null)
            ap.ACTUAL_POPULATION_END = actual_end;
        actual_end = ap.ACTUAL_POPULATION_END;
        
        double expected_end = prorate(actual_start, ap.CBR_1940 - ap.CDR_1940, nyears);
        double expected_births = num_births(actual_start, nyears, ap);
        double actual_in1959 = actual_in1959(area, 1941.5, 9, nyears);
        
        Util.out(String.format("Наличное население в начале периода: %s", f2k(actual_start)));
        Util.out(String.format("Ожидаемое население в конце периода: %s", f2k(expected_end)));
        Util.out(String.format("Наличное население в конце периода: %s", f2k(actual_end)));
        Util.out(String.format("Общий демографический дефицит в конце периода: %s", f2k(expected_end - actual_end)));
        Util.out(String.format("Ожидаемое число рождений за %s года войны при сохранении в 1941-1945 гг. уровней рождаемости и смертности 1940 года: %s", d2s(nyears), f2k(expected_births)));
        Util.out(String.format("Доживаемость в мирных условиях между 1941-1945 в среднем и 15.1.1959, принимаемая как: %.2f", survival_rate));
        Util.out(String.format("Число родившихся в период за %s года и доживших до переписи 1959 года: %s", d2s(nyears), f2k(actual_in1959)));

        
        
        // ###
    }
    
    /* =================================================================================== */
    
    private String f2k(double v)
    {
        String s = String.format("%,10.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
    
    private String d2s(double f) throws Exception
    {
        String s = String.format("%.1f", f);
        if (s.endsWith(".0"))
            s = Util.stripTail(s, ".0");
        return s;
    }
    
    private String ny2s(double f) throws Exception
    {
        int ny = (int) (f + 0.001);
        f -= ny;
        if (f < 0.3)
            return String.format("начало %d", ny);
        else if (f > 0.8)
            return String.format("конец %d", ny);
        else
            return String.format("середина %d", ny);
    }

    private double prorate(double v, double rate, double years)
    {
        double xrate = (1 + rate / 1000);
        return v * Math.pow(xrate, years);
    }
    
    private double num_births(double start, double nyears, AreaParameters ap)
    {
        return num_births(start, nyears, ap.CBR_1940, ap.CDR_1940);
    }
    
    private double num_births(double start, double nyears, double cbr, double cdr)
    {
        double p = start;
        double total_births = 0;
        
        while (nyears >= 1)
        {
            double births = p * cbr / 1000; 
            double deaths = p * cdr / 1000;
            total_births += births;
            p += births - deaths;
            nyears -= 1;
        }
        
        if (nyears > 0)
        {
            double f = Math.pow(1 + cbr/1000, nyears) - 1;
            double births = p * f;
            total_births += births;
        }
        
        return total_births;
    }
    
    public static double forward_6mo(double v, AreaParameters ap)
    {
        return forward_6mo(v, ap.CBR_1940 - ap.CDR_1940);
    }
    
    public static double forward_6mo(double v, double rate)
    {
        double f = Math.sqrt(1 + rate/1000);
        return v * f;
    }

    public static double backward_6mo(double v, AreaParameters ap)
    {
        return backward_6mo(v, ap.CBR_1946 - ap.CDR_1946);
    }

    public static double backward_6mo(double v, double rate)
    {
        double f = Math.sqrt(1 + rate/1000);
        return v / f;
    }
    
    /*
     * Calculate population registered by the 1959 census, with birth dates in a window @nyears wide 
     * starting from (@start_year + @months_delay/12)
     */
    private double actual_in1959(Area area, double start_year, double months_delay, double nyears) throws Exception
    {
        return actual_in1959(area, start_year + months_delay / 12, nyears);
    }

    /*
     * Calculate population registered by the 1959 census, with birth dates in a window @nyears wide 
     * starting from @start_year
     */
    private double actual_in1959(Area area, double start_year, double nyears) throws Exception
    {
        double sum = 0;
        double end_year = start_year + nyears;
        PopulationByLocality p = PopulationByLocality.census(area, 1959);
        
        for (int age = 0; age <= PopulationByLocality.MAX_AGE; age++)
        {
            double birth_year = 1958 - age;
            double overlap = overlap(birth_year, birth_year + 1, start_year, end_year);
            if (overlap > 0)
            {
                sum += overlap * p.get(Locality.TOTAL, Gender.BOTH, age);
            }
        }

        return sum / 1000;
    }
    
    /*
     * Determine what fraction of [x1 ... x2] overlaps [a1 ... a2].
     * Return value ranges from 0.0 (no overlap at all) to 1.0 (if x1...x2 is fully inside a1...a2),
     * to a value in between in case of a partial overlap. 
     */
    private double overlap(double x1, double x2, double a1, double a2)
    {
        if (x1 >= x2 || a1 >= a2)
            throw new IllegalArgumentException();
        
        /* whole @x is outside of @a range */
        if (x2 <= a1 || x1 >= a2)
            return 0;
        
        /* whole @x is fully within @a range */
        if (x1 >= a1 && x2 <= a2)
            return 1;
        
        /* partial overlap */
        if (x1 < a1)
        {
            return (x2 - a1) / (x2 - x1);
        }
        else
        {
            return (a2 - x1) / (x2 - x1);
        }
    }
}

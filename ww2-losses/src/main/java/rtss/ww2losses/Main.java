package rtss.ww2losses;

import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population_194x.USSR_Expected_Population_In_Early_1946;

import java.math.BigDecimal;

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

        AreaParameters ap = AreaParameters.forArea(Area.USSR, 4);
        double birth_delay_months;
        
        double USSR_1941_START = 195_392;
        double USSR_1941_MID = forward_6mo(USSR_1941_START, ap, 1940);

        double USSR_1946_START = 170_548;
        double USSR_1945_MID = backward_6mo(USSR_1946_START, ap, 1946); 

        /* 
         * СССР с середины 1941 по середину 1945
         * дожитие 0.68
         * окно рождений сдвинуто на 9 месяцев 
         */
        ap = AreaParameters.forArea(Area.USSR, 4);
        ap.immigration = 0;
        ap.survival_rate_194x_1959 = 0.68;
        birth_delay_months = 9;
        do_show(Area.USSR, ap, 4.0, USSR_1941_MID, USSR_1945_MID, birth_delay_months);

        /* 
         * СССР с середины 1941 по середину 1945
         * дожитие 0.62
         * окно рождений сдвинуто на 0 месяцев 
         */
        ap = AreaParameters.forArea(Area.USSR, 4);
        ap.immigration = 0;
        ap.survival_rate_194x_1959 = 0.62;
        birth_delay_months = 0;
        do_show(Area.USSR, ap, 4.0, USSR_1941_MID, USSR_1945_MID, birth_delay_months);

        /* 
         * СССР с середины 1941 по начало 1946
         * дожитие 0.62
         * окно рождений сдвинуто на 0 месяцев 
         */
        ap = AreaParameters.forArea(Area.USSR, 4);
        ap.immigration = 0;
        ap.survival_rate_194x_1959 = 0.62;
        birth_delay_months = 0;
        do_show(Area.USSR, ap, 4.5, USSR_1941_MID, USSR_1946_START, birth_delay_months);
        do_show_forwarding();
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

    private void do_show(
            Area area,
            AreaParameters ap,
            double nyears,
            Double actual_start,
            Double actual_end,
            double birth_months_delay)
            throws Exception
    {
        double start_year = 1941.5;

        String pre = "*****   ";
        StringBuffer syears = new StringBuffer(String
                .format("за %s года (%s - %s)\n%sпри средней доживаемости рождённых в 1941-1945 гг. до переписи 15 января 1959 = %.2f",
                        d2s(nyears), ny2s(start_year, "а"), ny2s(start_year + nyears, "а"), pre, ap.survival_rate_194x_1959));
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
        double expected_births = expected_births(actual_start, nyears, ap);
        double expected_deaths = expected_deaths(actual_start, nyears, ap);
        double original_population_expected_deaths = original_population_expected_deaths(actual_start, nyears, ap);
        StringBuilder birthsFormula = new StringBuilder();
        double actual_in1959 = actual_in1959(area, 1941.5, birth_months_delay, nyears, birthsFormula);
        double overall_deficit = expected_end - actual_end;

        Util.out(String.format("Наличное население в начале периода: %s тыс. чел.", f2k(actual_start)));
        Util.out(String.format("Ожидаемое население в конце периода: %s тыс. чел.", f2k(expected_end)));
        Util.out(String.format("Наличное население в конце периода: %s тыс. чел.", f2k(actual_end)));
        Util.out(String.format("Общий демографический дефицит в конце периода: %s тыс. чел.", f2k(overall_deficit)));

        Util.out(String
                 .format("Ожидаемое число смертей за период в %s года при сохранении в 1941-1945 гг. уровней рождаемости и смертности 1940 года: %s тыс. чел.",
                         d2s(nyears), f2k(expected_deaths)));
        Util.out(String
                 .format("Ожидаемое число смертей в первоначальном населении (не включая рождённых) за период в %s года при сохранении в 1941-1945 гг. уровня смертности 1940 года: %s тыс. чел.",
                         d2s(nyears), f2k(original_population_expected_deaths)));
        Util.out("    (приблизительно, не учитывая изменение возрастной структуры на протяжении периода; в действительности меньше)");
        Util.out(String
                 .format("Ожидаемая численность первоначального населения (не включая рождённых) к концу периода при сохранении в 1941-1945 гг. уровня смертности 1940 года: %s тыс. чел.",
                         f2k(actual_start - original_population_expected_deaths)));
        Util.out("    (приблизительно, не учитывая изменение возрастной структуры на протяжении периода; в действительности больше)");
       
        
        Util.out(String
                .format("Ожидаемое число рождений за период в %s года при сохранении в 1941-1945 гг. уровней рождаемости и смертности 1940 года: %s тыс. чел.",
                        d2s(nyears), f2k(expected_births)));
        
        Util.out(String.format("Доживаемость в мирных условиях между 1941-1945 в среднем и 15.1.1959, принимаемая как: %.2f",
                               ap.survival_rate_194x_1959));
        Util.out(String
                .format("Фактическое число родившихся в период за %s года (с задержкой %s месяцев) и доживших до переписи 1959 года: %s тыс. чел.",
                        d2s(nyears), d2s(birth_months_delay), f2k(actual_in1959)));
        Util.out(String.format("Формула для числа рождений, по данным переписи 1959 года: %s", birthsFormula, toString()));
        
        if (ap.immigration > 0)
        {
            Util.out(String
                    .format("Фактическое число родившихся в период за %s года (с задержкой %s месяцев) и доживших до переписи 1959 года с вычетом миграции 1946-1958 гг. (%s): %s тыс. чел.",
                            f2k(ap.immigration), d2s(nyears), d2s(birth_months_delay), f2k(actual_in1959 - ap.immigration)));
        }
        else
        {
            Util.out(String.format("Миграции нет"));
        }

        double peacetime_births = (actual_in1959 - ap.immigration) / ap.survival_rate_194x_1959;
        Util.out(String.format("В мирных условиях это означало бы рождение за %s-летний период: %s тыс. чел.", d2s(nyears), f2k(peacetime_births)));

        double births_deficit = expected_births - peacetime_births;
        Util.out(String.format("Дефицит рождений + ранняя детская сверхсмертность военных лет: %s тыс. чел.", f2k(births_deficit)));

        double excess_deaths = overall_deficit - births_deficit;
        Util.out(String.format("Сверхсмертность наличного на %s года населения за %s года: %s", ny2s(start_year, "у"), d2s(nyears),
                               f2k(excess_deaths)));

        Util.out(String.format("Доля сверхсмертности наличного на начало войны населения в общем дефиците: %.1f%%",
                               100 * excess_deaths / overall_deficit));
        Util.out(String.format("Доля недорода и ранней детской сверхсмертности в общем дефиците: %.1f%%", 100 * births_deficit / overall_deficit));
    }

    /* =================================================================================== */
    
    /*
     * Форматирование вывода
     */
    private String f2k(double v)
    {
        String s = String.format("%,10.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }

    private String d2s(double f) throws Exception
    {
        String s = new BigDecimal(f).toPlainString();

        if (s.contains("."))
        {
            while (s.endsWith("0") && !s.endsWith(".0"))
                s = Util.stripTail(s, "0");
            if (s.endsWith(".0"))
                s = Util.stripTail(s, ".0");
        }

        return s;
    }

    private String ny2s(double f, String suffix) throws Exception
    {
        int ny = (int) (f + 0.001);
        f -= ny;
        if (f < 0.3)
            return String.format("начало %d", ny);
        else if (f > 0.8)
            return String.format("конец %d", ny);
        else
            return String.format("середин%s %d", suffix, ny);
    }

    /* =================================================================================== */

    /*
     * Ожидаемая численность населения @years спустя при годовом темпе роста @rate
     * и начальном количестве @v
     */
    private double prorate(double v, double rate, double years)
    {
        double xrate = (1 + rate / 1000);
        return v * Math.pow(xrate, years);
    }

    /*
     * Ожидаемое количество рождений за @nyears
     */
    private double expected_births(double start, double nyears, AreaParameters ap)
    {
        return expected_births(start, nyears, ap.CBR_1940, ap.CDR_1940);
    }

    private double expected_births(double start, double nyears, double cbr, double cdr)
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
            // ###
            /*
             * Частичный годовой интервал разбивается на шаги, 
             * результат интегрируется по ним
             */
            final int STEPS = 1000;
            final double stepsize = nyears / STEPS; 
            for (int step = 0; step <= STEPS; step++)
            {
                double births = p * cbr * stepsize / 1000;
                double deaths = p * cdr * stepsize  / 1000;
                total_births += births;
                p += births - deaths;
            }
        }

        return total_births;
    }

    /*
     * Ожидаемое количество смертей за @nyears
     */
    private double expected_deaths(double start, double nyears, AreaParameters ap)
    {
        return expected_deaths(start, nyears, ap.CBR_1940, ap.CDR_1940);
    }

    private double expected_deaths(double start, double nyears, double cbr, double cdr)
    {
        double p = start;
        double total_deaths = 0;

        while (nyears >= 1)
        {
            double births = p * cbr / 1000;
            double deaths = p * cdr / 1000;
            total_deaths += deaths;
            p += births - deaths;
            nyears -= 1;
        }

        if (nyears > 0)
        {
            // ###
            /*
             * Частичный годовой интервал разбивается на шаги, 
             * результат интегрируется по ним
             */
            final int STEPS = 1000;
            final double stepsize = nyears / STEPS; 
            for (int step = 0; step <= STEPS; step++)
            {
                double births = p * cbr * stepsize / 1000;
                double deaths = p * cdr * stepsize  / 1000;
                total_deaths += deaths;
                p += births - deaths;
            }
        }

        return total_deaths;
    }

    
    /*
     * Ожидаемое количество смертей за @nyears в первоначальном населении.
     * Действительное число смертей будет ниже, т.к. максимальное число смертей приходится на первые два года жизни,
     * и после продвижки населения в возрасте на год-два его эффективная смертность упадёт. 
     */
    private double original_population_expected_deaths(double start, double nyears, AreaParameters ap)
    {
        return original_population_expected_deaths(start, nyears, ap.CDR_1940);
    }

    private double original_population_expected_deaths(double start, double nyears, double cdr)
    {
        double p = start;
        double total_deaths = 0;

        while (nyears >= 1)
        {
            double deaths = p * cdr / 1000;
            total_deaths += deaths;
            p -= deaths;
            nyears -= 1;
        }

        if (nyears > 0)
        {
            // ###
            /*
             * Частичный годовой интервал разбивается на шаги, 
             * результат интегрируется по ним
             */
            final int STEPS = 1000;
            final double stepsize = nyears / STEPS; 
            for (int step = 0; step <= STEPS; step++)
            {
                double deaths = p * cdr * stepsize  / 1000;
                total_deaths += deaths;
                p -= deaths;
            }
        }

        return total_deaths;
    }
    
    /*
     * Ожидаемая численность населения 6 месяцев спустя
     * при начальном количестве @v
     */
    public static double forward_6mo(double v, AreaParameters ap, int year)
    {
        if (year == 1940)
            return forward_6mo(v, ap.growth_1940());
        else if (year == 1946)
            return forward_6mo(v, ap.growth_1946());
        else
            throw new IllegalArgumentException();
    }

    public static double forward_6mo(double v, double rate)
    {
        double f = Math.sqrt(1 + rate / 1000);
        return v * f;
    }

    /*
     * Численность населения 6 месяцами ранее
     * при начальном количестве @v
     */
    public static double backward_6mo(double v, AreaParameters ap, int year)
    {
        if (year == 1940)
            return backward_6mo(v, ap.growth_1940());
        else if (year == 1946)
            return backward_6mo(v, ap.growth_1946());
        else
            throw new IllegalArgumentException();
    }

    public static double backward_6mo(double v, double rate)
    {
        double f = Math.sqrt(1 + rate / 1000);
        return v / f;
    }

    /*
     * Опеределить численность населения по переписи 1959 года с датами рождения во временном окне шириной @nyears 
     * начинающемся со (@start_year + @months_delay/12)
     */
    private double actual_in1959(Area area, double start_year, double months_delay, double nyears, StringBuilder sbFormula) throws Exception
    {
        return actual_in1959(area, start_year + months_delay / 12, nyears, sbFormula);
    }

    /*
     * Опеределить численность населения по переписи 1959 года с датами рождения во временном окне шириной @nyears 
     * начинающемся со @start_year
     */
    private double actual_in1959(Area area, double start_year, double nyears, StringBuilder sbFormula) throws Exception
    {
        if (sbFormula != null)
            sbFormula.setLength(0);

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
                if (sbFormula != null)
                {
                    if (sbFormula.length() != 0)
                        sbFormula.append(" + ");
                    if (overlap >= 0.99 && overlap <= 1.01)
                    {
                        sbFormula.append(String.format("ч%d", age));
                    }
                    else
                    {
                        overlap = 0.01 * Math.round(overlap * 100);
                        sbFormula.append(String.format("%s × ч%d", d2s(overlap), age));
                    }
                }
            }
        }

        return sum / 1000;
    }

    /*
     * Определить, какая часть диапазона [x1 ... x2] перекрывает диапазон [a1 ... a2].
     * Возвращает значение от 0.0 (если нет перекрытия) до 1.0 (если x1...x2 целиком внутри a1...a2),
     * или промежуточное значение в случае частичного перекрытия. 
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

    private void do_show_forwarding() throws Exception
    {
        final USSR_Expected_Population_In_Early_1946 x46 = new USSR_Expected_Population_In_Early_1946();
        final double CBR_1940 = USSR_Expected_Population_In_Early_1946.CBR_1940;
        final int MAX_AGE = PopulationByLocality.MAX_AGE;
        PopulationByLocality p;
        double sum;
        
        Util.out("");
        
        p = x46.with_mt_USSR_1938(0);
        sum = p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        Util.out(String.format("Ожидаемая численность наличного на середину 1941 г. населения в начале 1946 по продвижке, в условиях мира\n%s: %s тыс. чел.",
                               "таблица ГКС-СССР-1938, без рождений",
                               f2k(sum)));
        
        p = x46.with_mt_USSR_1938(CBR_1940);
        sum = p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        Util.out(String.format("Ожидаемая численность наличного на середину 1941 г. населения в начале 1946 по продвижке, в условиях мира\n%s: %s тыс. чел.",
                               "таблица ГКС-СССР-1938, с рождениями",
                               f2k(sum)));

        p = x46.with_mt_RSFSR_1940(0);
        sum = p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        Util.out(String.format("Ожидаемая численность наличного на середину 1941 г. населения в начале 1946 по продвижке, в условиях мира\n%s: %s тыс. чел.",
                               "таблица АДХ-РСФСР-1940, без рождений",
                               f2k(sum)));

        p = x46.with_mt_RSFSR_1940(CBR_1940);
        sum = p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        Util.out(String.format("Ожидаемая численность наличного на середину 1941 г. населения в начале 1946 по продвижке, в условиях мира\n%s: %s тыс. чел.",
                               "таблица АДХ-РСФСР-1940, с рождениями",
                               f2k(sum)));
    }
}

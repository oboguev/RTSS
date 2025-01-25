package rtss.ww2losses.helpers;

import rtss.data.ValueConstraint;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.FieldValue;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.util.HalfYearEntries;

public class PrintHalves
{
    private static int MAX_AGE = Population.MAX_AGE;
    private static double PROMILLE = 1000.0;

    public static void print(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        Util.out("");
        Util.out("Данные по полугодиям");
        Util.out("");
        Util.out("    нал.ос    = ожидаемое число смертей в наличном на начало войны населении в условиях мира (за полугодие),");
        Util.out("                для первой половины 1941 фактическое число смертей во всём населении");
        Util.out("    нал.дс    = добавочное число смертей в наличном на начало войны населении из-за войны (за полугодие)");
        Util.out("    мир.ор    = ожидаемое число рождений в условиях мира  (за полугодие)");
        Util.out("    фр        = фактическое число рождений  (за полугодие)");
        Util.out("    фcр.мир   = число смертей (в данном полугодии) от фактических рождений во время войны, ожидаемое при смертности мирного времени");
        Util.out("    фcр       = фактическое число смертей (в данном полугодии) от фактических рождений во время войны, при фактической военной смертности");
        Util.out("    ферт.оч   = ожидаемая численность женщин фертильного возраста 15-54 (в среднем за полугодие)");
        Util.out("    ферт.фч   = фактическая численность женщин фертильного возраста 15-54 (в среднем за полугодие)");
        Util.out("    ферт.нис  = накопленное число избыточных смертей женщин фертильного возраста 15-54 (на начало полугодия)");
        Util.out("    чн.нач    = общая численность населения в начале полугодия");
        Util.out("    чн.кон    = общая численность населения в конце полугодия");
        Util.out("    чн.ср     = средняя численность населения за полугодие");
        Util.out("    чс        = общее фактическое число смертей за полугодие");
        Util.out("    р         = рождаемость (промилле, для полугодия, но в нормировке на год");
        Util.out("    с         = смертность (промилле, для полугодия, но в нормировке на год)");
        Util.out("");
        Util.out("полугодие нал.ос нал.дс мир.ор фр фср.мир фср ферт.оч ферт.фч ферт.нис чн.нач чн.кон чн.ср чс р с");
        Util.out("");
        
        HalfYearEntry he0 = halves.get(0);
        HalfYearEntry he1 = halves.get(1);
        HalfYearEntry he_last = halves.last();

        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;

            double d1 = 0;
            double d2 = 0;

            if (he.accumulated_excess_deaths != null)
                d1 = he.accumulated_excess_deaths.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);

            if (he.next.accumulated_excess_deaths != null)
                d2 = he.next.accumulated_excess_deaths.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);

            double d2_minus_d1 = Math.round(d2 - d1);

            double fert_actual = he.p_actual_without_births_avg.sum(Locality.TOTAL, Gender.FEMALE, 15, 54);
            double fert_loss = 0;
            if (he.accumulated_excess_deaths != null)
                fert_loss = he.accumulated_excess_deaths.sum(Locality.TOTAL, Gender.FEMALE, 15, 54);

            PopulationByLocality p1 = he.p_nonwar_without_births;
            PopulationByLocality p2 = he.next.p_nonwar_without_births;
            PopulationByLocality pavg = p1.avg(p2, ValueConstraint.NONE);
            double fert_expected = pavg.sum(Locality.TOTAL, Gender.FEMALE, 15, 54);

            double total_deaths = d2_minus_d1 + he.expected_nonwar_deaths + he.actual_warborn_deaths;
            
            // population
            double pn1 = he.p_actual_without_births_start.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
            double pn2 = he.p_actual_without_births_end.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
            
            if (he != he0)
            {
                // ###pn1 += accrue(he1);
            }
            
            // ### accrue actual_births - actual_warborn_deaths начиная с he(1) т.е. 1942.вт
            
            double pn_avg = (pn1 + pn2) / 2; 

            double cbr = 2 * PROMILLE * he.actual_births / pn_avg;
            double cdr = 2 * PROMILLE * total_deaths / pn_avg;

            Util.out(String.format("%s %6s %6s %6s %6s %6s %6s %6s %6s %6s %7s %7s %7s %6s %.1f %.1f",
                                   he.toString(),
                                   f2k(he.expected_nonwar_deaths / 1000.0),
                                   f2k(d2_minus_d1 / 1000.0),
                                   f2k(he.expected_nonwar_births / 1000.0),
                                   // ---
                                   f2k(he.actual_births / 1000.0),
                                   f2k(he.actual_warborn_deaths_baseline / 1000.0),
                                   f2k(he.actual_warborn_deaths / 1000.0),
                                   // ---
                                   f2k(fert_expected / 1000.0),
                                   f2k(fert_actual / 1000.0),
                                   f2k(fert_loss / 1000.0),
                                   // ---
                                   f2k(pn1 / 1000.0),
                                   f2k(pn2 / 1000.0),
                                   f2k(pn_avg / 1000.0),
                                   // ---
                                   f2k(total_deaths / 1000.0),
                                   cbr,
                                   cdr));
        }

        Util.out("");
        Util.out("Малое фактическое убывание числа женщин фертильного возраста в условиях войны и быстрое возрастание");
        Util.out("их ожидаемого числа в услових мира обусловлено быстро расширяющейся к основанию демографической пирамидой");
        Util.out("в годах рождения до 1929, т.е. старше 13 лет в 1941 и старше 18 лет в 1946");
        Util.out("");
    }
    
    private double accrue(HalfYearEntry he_from, HalfYearEntry he_to, String field, boolean include_to, boolean nullable) throws Exception
    {
        double vsum = 0;
        
        HalfYearEntry he = he_from;
        for (;;)
        {
            if (he == he_to && !include_to)
                break;
            
            Double v = FieldValue.getDouble(he, field);
            if (nullable && v == null)
                v = 0.0;
            vsum += v;

            if (he == he_to)
                break;
        }
        
        return vsum;
    }

    private static String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}

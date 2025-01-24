package rtss.ww2losses.helpers;

import rtss.data.ValueConstraint;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.util.HalfYearEntries;

public class PrintHalves
{
    private static int MAX_AGE = Population.MAX_AGE;

    public static void print(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        Util.out("");
        Util.out("Данные по полугодиям");
        Util.out("");
        Util.out("    нал.ос    = ожидаемое число смертей в наличном на начало войны населении в условиях мира");
        Util.out("    над.дс    = добавочное число смертей в наличном на начало войны населении из-за войны");
        Util.out("    мир.ор    = ожидаемое число рождений в условиях мира");
        Util.out("    фр        = фактическое число рождений");
        Util.out("    ферт.оч   = ожидаемая численность женщин фертильного возраста 15-54");
        Util.out("    ферт.ч    = фактическая численность женщин фертильного возраста 15-54");
        Util.out("    ферт.нис  = накопленное число избыточных смертей женщин фертильного возраста 15-54");
        Util.out("");
        Util.out("полугодие нал.ос нал.дс мир.ор фр ферт.оч ферт.ч ферт.нис");
        Util.out("");

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
            

            Util.out(String.format("%s %6s %6s %6s %6s %6s %6s %6s",
                                   he.toString(),
                                   f2k(he.expected_nonwar_deaths / 1000.0),
                                   f2k(d2_minus_d1 / 1000.0),
                                   f2k(he.expected_nonwar_births / 1000.0),
                                   f2k(he.actual_births / 1000.0),
                                   f2k(fert_expected / 1000.0),
                                   f2k(fert_actual / 1000.0),
                                   f2k(fert_loss / 1000.0)));
        }

        Util.out("");
        Util.out("Малое фактическое убывание числа женщин фертильного возраста в условиях войны и быстрое возрастание");
        Util.out("их ожидаемого числа в услових мира обусловлено быстро расширяющейся к основанию демографической пирамидой");
        Util.out("в годах рождения до 1929, т.е. старше 13 лет в 1941 и старше 18 лет в 1946");
        Util.out("");
    }

    private static String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}

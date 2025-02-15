package rtss.ww2losses.ageline.warmodel;

import rtss.data.selectors.Gender;
import rtss.math.interpolate.LinearInterpolator;
import rtss.util.Util;
import rtss.ww2losses.Constants;
import rtss.ww2losses.HalfYearEntry;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;

/*
 * Вычислить число избыточных смертей в половозрастной линии за полугодие
 */
public class WarAttritionModel
{
    public WarAttritionModel(
            PopulationContext p1941_mid,
            PopulationContext p1946_actual,
            WarAttritionModelParameters wamp) throws Exception
    {
        initModel(p1941_mid, p1946_actual, wamp);
    }

    /* =========================================================================================== */

    /*
     * Удельные коэффициенты потерь среди мужчин призывного возраста, по полугодиям.
     * Сумма равна 1. 
     */
    private double[] ac_conscripts;

    /*
     * Удельные коэффициенты потерь среди остального населения, по полугодиям.
     * Сумма равна 1. 
     */
    private double[] ac_general;

    /* =========================================================================================== */

    /* 
     * Интенсивность потерь РККА по полугодиям с 1941.1
     * (Г.Ф. Кривошеев и др, "Россия и СССР в войнах XX века : Книга потерь", М. : Вече, 2010, стр. 236, 242, 245)
     */
    private static final double[] rkka_loss_intensity = { 0, 3_137_673, 1_518_213, 1_740_003, 918_618, 1_393_811, 915_019, 848_872, 800_817, 0 };

    /* 
     * Интенсивность оккупации по полугодиям с 1941.1
     * (Госкомстат СССР, "Народное хозяйство СССР в Великой Отечественной войне 1941–1945 гг. : Статистический сборник", М. 1990, стр. 20),
     * интерполяция и обработка в файле occupation_imr.xlsx. 
     */
    private static final double[] occupation_pct = { 0.0, 19.6, 37.8, 40.1, 32.7, 24.2, 14.2, 1.7, 0.0, 0.0 };

    /* 
     * Относительная интенсивность потерь в оккупации и в тылу
     */
    double[] loss_intensity_occupation = { 0.00, 1.60, 1.60, 1.60, 1.60, 1.60, 1.60, 1.60, 1.60, 0.00 };
    double[] loss_intensity_rear = { 0.00, 1.00, 1.00, 1.00, 0.95, 0.90, 0.85, 0.80, 0.75, 0.00 };

    /* =========================================================================================== */

    private void initModel(
            PopulationContext p1941_mid,
            PopulationContext p1946_actual,
            WarAttritionModelParameters wamp) throws Exception
    {
        Util.assertion(wamp.aw_civil_combat >= 0 && wamp.aw_civil_combat <= 1);
        Util.assertion(wamp.aw_conscript_combat >= 0 && wamp.aw_conscript_combat <= 1);

        double[] rkka_loss_intensity_normalized = Util.normalize(rkka_loss_intensity);

        /* доля населения в оккупации и в тылу */
        double[] fraction_occupation = Util.divide(occupation_pct, 100.0);
        double[] fraction_rear = Util.sub(Util.fill_double(fraction_occupation.length, 1), fraction_occupation);

        /* остаток гражданского населения (доля) */
        double[] civil_population_remainder = civil_population_remainder(p1941_mid, p1946_actual, wamp.aw_conscript_combat);

        /* удельные веса потерь в тылу и в оккупации */
        double[] w_occupation = Util.multiply(civil_population_remainder, Util.multiply(fraction_occupation, loss_intensity_occupation));
        double[] w_rear = Util.multiply(civil_population_remainder, Util.multiply(fraction_rear, loss_intensity_rear));
        double[] w_civil = Util.normalize(Util.add(w_occupation, w_rear));

        /* полугодовые веса потерь для мужчин призывного возраста и для остальных */
        ac_conscripts = Util.sumWeightedNormalized(wamp.aw_conscript_combat, rkka_loss_intensity_normalized, 1 - wamp.aw_conscript_combat, w_civil);
        ac_general = Util.sumWeightedNormalized(wamp.aw_civil_combat, rkka_loss_intensity_normalized, 1 - wamp.aw_civil_combat, w_civil);
    }

    private final int N_HALF_YEARS = 10;

    /* 
     * остаток гражданского населения 
     */
    private double[] civil_population_remainder(PopulationContext p1941_mid, PopulationContext p1946_actual, double aw_conscript_combat)
            throws Exception
    {
        int nd_4_5 = 9 * years2days(0.5);
        PopulationContext p1946_actual_born_prewar = p1946_actual.selectByAgeDays(nd_4_5, years2days(Population.MAX_AGE + 1));

        double v1 = civil_population(p1941_mid, aw_conscript_combat);
        double v2 = civil_population(p1946_actual_born_prewar, aw_conscript_combat);
        LinearInterpolator li = new LinearInterpolator(1.0, v1, 10.0, v2);

        double[] r = new double[N_HALF_YEARS];
        r[0] = 1;

        for (int k = 1; k < r.length; k++)
            r[k] = li.interp(k + 0.5) / v1;

        return r;
    }

    /* численность населения, но включая только 20% мужчин призывного возраста */
    private double civil_population(PopulationContext p, double aw_conscript_combat) throws Exception
    {
        double v1 = p.sum();
        p = p.selectByAgeYears(Constants.CONSCRIPT_AGE_FROM, Constants.CONSCRIPT_AGE_TO);
        double v2 = p.sum(Gender.MALE);
        return v1 - aw_conscript_combat * v2;
    }

    /* =========================================================================================== */

    /*
     * Число избыточных смертей за полугодие @he
     * в половозрастной линии населения с полом @gender и средним за полугодие возрастом (в днях) @nd_age
     * при условии начальной (на середину 1941 года) численности линии @initial_population.
     * 
     * Возвращённая величина затем должна быть умножена на loss_intensity для линии.
     */
    public double excessWarDeaths(Gender gender, int nd_age, HalfYearEntry he, double initial_population) throws Exception
    {
        double[] ac = attrition_coefficient(gender, nd_age);
        double excess_war_deaths = ac[he.index()] * initial_population;
        return excess_war_deaths;
    }

    private double[] attrition_coefficient(Gender gender, int nd)
    {
        if (gender == Gender.MALE &&
            nd >= years2days(Constants.CONSCRIPT_AGE_FROM) &&
            nd <= years2days(Constants.CONSCRIPT_AGE_TO))
        {
            return ac_conscripts;
        }
        else
        {
            return ac_general;
        }
    }
}

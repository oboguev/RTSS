package rtss.ww2losses.ageline;

import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.Constants;
import rtss.ww2losses.HalfYearEntry;

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Вычислить число избыточных смертей в половозрастной линии за полугодие
 */
public class WarAttritionModel_New
{
    public WarAttritionModel_New(double aw_general_occupation, double aw_conscripts_rkka_loss) throws Exception
    {
        initModel(aw_general_occupation, aw_conscripts_rkka_loss);
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
    private static final double[] occupation_intensity = { 0.0, 19.6, 37.8, 40.1, 32.7, 24.2, 14.2, 1.7, 0.0, 0.0 };

    /* 
     * Равномерная интенсивность по военным полугодиям с 1941.1
     */
    private static final double[] even_intensity = { 0, 1, 1, 1, 1, 1, 1, 1, 1, 0 };

    private void initModel(double aw_general_occupation, double aw_conscripts_rkka_loss) throws Exception
    {
        /* нормализованный полугодовой коэффициент распределения потерь для не-призывного населения */
        ac_general = wsum(aw_general_occupation, occupation_intensity,
                                   1 - aw_general_occupation, even_intensity);

        /* нормализованный полугодовой коэффициент распределения потерь для призывного населения */
        ac_conscripts = wsum(aw_conscripts_rkka_loss, rkka_loss_intensity,
                                      1.0 - aw_conscripts_rkka_loss, ac_general);
    }

    /*
     * Взвешенная сумма w1*ww1 + w2*ww2
     * 
     * Массивы ww1 и ww2 предварительно нормализуются по сумме всех членов на 1.0
     * (без изменения начальных копий).
     * 
     * Возвращаемый результат также нормализуется. 
     */
    private double[] wsum(double w1, double[] ww1, double w2, double[] ww2) throws Exception
    {
        ww1 = Util.normalize(ww1);
        ww2 = Util.normalize(ww2);

        ww1 = Util.multiply(ww1, w1);
        ww2 = Util.multiply(ww2, w2);

        double[] ww = Util.add(ww1, ww2);
        ww = Util.normalize(ww);

        return ww;
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
        return excess_war_deaths ;
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

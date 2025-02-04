package rtss.ww2losses.ageline;

import rtss.data.selectors.Gender;
import rtss.ww2losses.Constants;
import rtss.ww2losses.HalfYearEntry;

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Вычислить число избыточных смертей в половозрастной линии за полугодие
 */
public class WarAttritionModel
{
    private final double[] ac_general;
    private final double[] ac_conscripts;

    public WarAttritionModel(double[] ac_general, double[] ac_conscripts)
    {
        this.ac_general = ac_general;
        this.ac_conscripts = ac_conscripts;
    }

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
        double excess_war_deaths = ac[ac_index(he)] * initial_population;
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

    /* индекс в массивы ac_xxx */
    private int ac_index(HalfYearEntry he) throws Exception
    {
        return (he.year - 1941) * 2 + he.halfyear.seq(0);
    }
}

package rtss.ww2losses.params;

import rtss.data.rates.Recalibrate;
import rtss.data.rates.Recalibrate.Rates;
import rtss.data.selectors.Area;

public class AreaParameters
{
    public /*final*/ Area area;
    public /*final*/ int NYears;

    /* birth and death rates in 1939 */
    public /*final*/ double CBR_1939;
    public /*final*/ double CDR_1939;

    /* birth and death rates in 1940 */
    public /*final*/ double CBR_1940;
    public /*final*/ double CDR_1940;

    /* 
     * прирост населения в 1940 году, с учётом миграции, 
     * если не задан, то полагается равным естественному приросту (CBR_1940 - CDR_1940) 
     * */
    public final /*final*/ Double growth_1940 = null;

    /* birth and death rates in 1946 */
    public /*final*/ double CBR_1946;
    public /*final*/ double CDR_1946;

    /* 
     * прирост населения в 1946 году, с учётом миграции, 
     * если не задан, то полагается равным естественному приросту (CBR_1946 - CDR_1946) 
     */
    public final /*final*/ Double growth_1946 = null;

    /* население по АДХ на начало 1939 и начало 1940 гг. */
    public double ADH_MALES_1939;
    public double ADH_FEMALES_1939;
    public double ADH_MALES_1940;
    public double ADH_FEMALES_1940;

    /* среднее дожитие родившихся в 1941-1945 гг. до переписи 15 января 1959 года */
    public double survival_rate_194x_1959 = 0.68;

    /* баланс миграции возрастной группы военных лет рожденния в период с войны по 15 января 1959 года */
    public double immigration = 0;

    /* результаты вычислений: вариант с постоянной (по годам войны) рождаемостью и смертностью */

    /* population at the beginning and end of the war */
    public /*final*/ double ACTUAL_POPULATION_START;
    public /*final*/ double ACTUAL_POPULATION_END;

    /* target excess deaths and birth shortage */
    public /*final*/ double ACTUAL_EXCESS_DEATHS;
    public /*final*/ double ACTUAL_BIRTH_DEFICIT;

    /* ======================================================================= */

    public double constant_cbr;
    public double constant_cdr;

    /* результаты вычислений: вариант с переменной (по годам войны) рождаемостью и смертностью */
    public double[] var_cbr;
    public double[] var_cdr;

    protected AreaParameters(Area area, int NYears)
    {
        this.area = area;
        this.NYears = NYears;
        this.var_cbr = new double[NYears];
        this.var_cdr = new double[NYears];
    }

    static public AreaParameters forArea(Area area) throws Exception
    {
        switch (area)
        {
        case RSFSR:
            return new AreaParameters_RSFSR();
        case USSR:
            return new AreaParameters_USSR();
        default:
            throw new IllegalArgumentException();
        }
    }

    public double growth_1940()
    {
        if (growth_1940 != null)
            return growth_1940;
        else
            return CBR_1940 - CDR_1940;
    }

    public double growth_1946()
    {
        if (growth_1946 != null)
            return growth_1946;
        else
            return CBR_1946 - CDR_1946;
    }

    public void build() throws Exception
    {
        /*
         * Перекалибровать на численность населения в начале года
         */
        if (area == Area.USSR)
        {
            Rates r = Recalibrate.m2e(new Rates(CBR_1939, CDR_1939));
            CBR_1939 = r.cbr;
            CDR_1939 = r.cdr;
        }
        else
        {
            CBR_1939 = Recalibrate.m2e(this.area, 1939, CBR_1939);
            CDR_1939 = Recalibrate.m2e(this.area, 1939, CDR_1939);
        }

        CBR_1940 = Recalibrate.m2e(this.area, 1940, CBR_1940);
        CDR_1940 = Recalibrate.m2e(this.area, 1940, CDR_1940);

        CBR_1946 = Recalibrate.m2e(this.area, 1946, CBR_1946);
        CDR_1946 = Recalibrate.m2e(this.area, 1946, CDR_1946);
    }
}

package rtss.ww2losses.params;

import rtss.data.selectors.Area;

public class AreaParameters
{
    /* population at the beginning and end of the war */
    public /*final*/ double ACTUAL_POPULATION_1941_MID;
    public /*final*/ double ACTUAL_POPULATION_1945_MID;
    
    /* target excess deaths and birth shortage */
    public /*final*/ double ACTUAL_EXCESS_DEATHS;
    public /*final*/ double ACTUAL_BIRTH_DEFICIT;
    
    /* birth and death rates in 1940 */
    public /*final*/ double CBR_1940;
    public /*final*/ double CDR_1940;
    
    /* birth and death rates in 1946 */
    public /*final*/ double CBR_1946;
    public /*final*/ double CDR_1946;

    public double constant_cbr;
    public double constant_cdr;

    public double[] var_cbr = new double[4];
    public double[] var_cdr = new double[4];

    static public AreaParameters forArea(Area area)
    {
        switch (area)
        {
        case RSFSR:
            return new AreaParameters_RSFSR();
        case USSR:
            return new AreaParameters_USSR();
        default:
            return null;
        }
    }
}

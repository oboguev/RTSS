package rtss.ww2losses.params;

public class AreaParameters_USSR_5 extends AreaParameters
{
    public AreaParameters_USSR_5()
    {
        /* 5 years */
        super(5);

        /* birth and death rates in 1940 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1940 = 36.1;
        CDR_1940 = 21.7;
        
        /* birth and death rates in 1946 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1946 = 28.5;
        CDR_1946 = 15.8;

        /* population at the beginning and end of the period (early 1941 and early 1946) */
        ACTUAL_POPULATION_START = 195_392;
        ACTUAL_POPULATION_END = 170_548;
        
        /* target excess deaths and birth shortage */
        ACTUAL_EXCESS_DEATHS = 22_720; // ###
        ACTUAL_BIRTH_DEFICIT = 16_181; // ###
    }
}

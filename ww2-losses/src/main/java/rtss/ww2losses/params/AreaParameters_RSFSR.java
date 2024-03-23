package rtss.ww2losses.params;

public class AreaParameters_RSFSR extends AreaParameters
{
    public AreaParameters_RSFSR()
    {
        /* population at the beginning and end of the war */
        ACTUAL_POPULATION_1941_MID = 110_988;
        ACTUAL_POPULATION_1945_MID = 96_601;
        
        /* target excess deaths and birth shortage */
        ACTUAL_EXCESS_DEATHS = 9_555;
        ACTUAL_BIRTH_DEFICIT = 9_980;
        
        /* birth and death rates in 1940 */
        /* Андреев, Дарский, Харькова, "Демографическая история России 1927-1959", стр. 164 */
        CBR_1940 = 34.6;
        CDR_1940 = 23.2;
        
        /* birth and death rates in 1946 */
        /* Андреев, Дарский, Харькова, "Демографическая история России 1927-1959", стр. 165 */
        CBR_1946 = 26.0;
        CDR_1946 = 12.3;
    }
}

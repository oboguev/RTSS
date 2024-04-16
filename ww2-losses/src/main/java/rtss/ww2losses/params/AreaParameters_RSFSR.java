package rtss.ww2losses.params;

import rtss.data.selectors.Area;

public class AreaParameters_RSFSR extends AreaParameters
{
    public AreaParameters_RSFSR()
    {
        /* 4 years */
        super(Area.RSFSR, 4);
        
        /* birth and death rates in 1939 */
        /* Андреев, Дарский, Харькова, "Демографическая история России 1927-1959", стр. 164 */
        CBR_1939 = 39.8;
        CDR_1939 = 23.9;

        /* birth and death rates in 1940 */
        /* Андреев, Дарский, Харькова, "Демографическая история России 1927-1959", стр. 164 */
        CBR_1940 = 34.6;
        CDR_1940 = 23.2;
        
        /* birth and death rates in 1946 */
        /* Андреев, Дарский, Харькова, "Демографическая история России 1927-1959", стр. 165 */
        CBR_1946 = 26.0;
        CDR_1946 = 12.3;
        
        /* население по АДХ на начало 1939 (в границах 1946 года) и на начало 1940 гг. */
        /* Андреев, Дарский, Харькова, "Демографическая история России 1927-1959", стр. 157-160 */
        ADH_MALES_1939 = 51_039_000;
        ADH_FEMALES_1939 = 56_854_000;
        ADH_MALES_1940 = 51_887_000;
        ADH_FEMALES_1940 = 57_791_000;   
        
        /* ================================================================================== */

        /* population at the beginning and end of the war (mid-1941 and mid-1945) */
        ACTUAL_POPULATION_START = 111_656;
        ACTUAL_POPULATION_END = 97_073;
        
        /* target excess deaths and birth shortage */
        ACTUAL_EXCESS_DEATHS = 9_789;
        ACTUAL_BIRTH_DEFICIT = 9_973;
    }
}

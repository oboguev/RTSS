package rtss.ww2losses.params;

import rtss.data.selectors.Area;
import rtss.ww2losses.Main;

public class AreaParameters_USSR extends AreaParameters
{
    public AreaParameters_USSR()
    {
        /* 4 years */
        super(Area.USSR, 4);

        /* birth and death rates in 1939 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1939 = 40.0;
        CDR_1939 = 20.1;

        /* birth and death rates in 1940 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1940 = 36.1;
        CDR_1940 = 21.7;
        
        /* birth and death rates in 1946 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1946 = 28.5;
        CDR_1946 = 15.8;
        
        /* население по АДХ на начало 1939 (в границах 1946 года) и на начало 1940 гг. */
        /* АДХ, "Население Советского Союза", стр. 125-126, 131 */
        ADH_MALES_1939 = 90_013_000;
        ADH_FEMALES_1939 = 98_194_000;
        ADH_MALES_1940 = 92_316_000;
        ADH_FEMALES_1940 = 100_283_000;        

        /* ================================================================================== */

        /* population at the beginning and end of the war (mid-1941 and mid-1945) */
        ACTUAL_POPULATION_START = Main.forward_6mo(195_392, this, 1940);
        ACTUAL_POPULATION_END = Main.backward_6mo(170_548, this, 1946);
        
        /* target excess deaths and birth shortage */
        ACTUAL_EXCESS_DEATHS = 22_720;
        ACTUAL_BIRTH_DEFICIT = 16_181;
    }
}

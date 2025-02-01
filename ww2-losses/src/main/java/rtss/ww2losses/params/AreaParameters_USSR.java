package rtss.ww2losses.params;

import rtss.data.selectors.Area;

public class AreaParameters_USSR extends AreaParameters
{
    public AreaParameters_USSR() throws Exception
    {
        /* 4 years */
        super(Area.USSR);

        /* birth and death rates in 1939 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1939_MIDYEAR = 40.0;
        CDR_1939_MIDYEAR = 20.1;

        /* birth and death rates in 1940 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1940_MIDYEAR = 36.1;
        CDR_1940_MIDYEAR = 21.7;

        /* birth and death rates in 1946 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1946_MIDYEAR = 28.5;
        CDR_1946_MIDYEAR = 15.8;

        /* население по АДХ на начало 1939 (в границах 1946 года) и на начало 1940 гг. */
        /* АДХ, "Население Советского Союза", стр. 125-126, 131 */
        ADH_MALES_1939 = 90_013_000;
        ADH_FEMALES_1939 = 98_194_000;
        ADH_MALES_1940 = 92_316_000;
        ADH_FEMALES_1940 = 100_283_000;

        build();
    }
}

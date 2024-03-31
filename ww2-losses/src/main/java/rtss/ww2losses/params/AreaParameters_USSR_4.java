package rtss.ww2losses.params;

public class AreaParameters_USSR_4 extends AreaParameters
{
    public AreaParameters_USSR_4()
    {
        NYears = 4;

        /* birth and death rates in 1940 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1940 = 36.1;
        CDR_1940 = 21.7;
        
        /* birth and death rates in 1946 */
        /* Андреев, Дарский, Харькова, "Население Советского Союза 1922-1991", стр. 120 */
        CBR_1946 = 28.5;
        CDR_1946 = 15.8;

        /* population at the beginning and end of the war (mid-1941 and mid-1945) */
        ACTUAL_POPULATION_START = forward_6mo(195_392, CBR_1940 - CDR_1940);
        ACTUAL_POPULATION_END = backward_6mo(170_548, CBR_1946 - CDR_1946);
        
        /* target excess deaths and birth shortage */
        ACTUAL_EXCESS_DEATHS = 22_720;
        ACTUAL_BIRTH_DEFICIT = 16_181;
    }
    
    private double forward_6mo(double v, double rate)
    {
        double f = Math.sqrt(1 + rate/1000);
        return v * f;
    }

    private double backward_6mo(double v, double rate)
    {
        double f = Math.sqrt(1 + rate/1000);
        return v / f;
    }
}

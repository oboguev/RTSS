package rtss.rosbris;

public class RosBrisTerritory
{
    public static RosBrisTerritory RF_BEFORE_2014 = new RosBrisTerritory(1100);
    public static RosBrisTerritory RF_AFTER_2014 = new RosBrisTerritory(643);
    public static RosBrisTerritory RF_REPUBLIC_CRIMEA = new RosBrisTerritory(1135);
    public static RosBrisTerritory RF_SEVASTOPOL = new RosBrisTerritory(1167);

    private int code;

    public int code()
    {
        return code;
    }
    
    public RosBrisTerritory(int code)
    {
        this.code = code;
    }
    
    @Override
    public String toString()
    {
        return "" + code;
    }
}

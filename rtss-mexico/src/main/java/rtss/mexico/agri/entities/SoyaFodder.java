package rtss.mexico.agri.entities;

import java.util.HashMap;

public class SoyaFodder extends HashMap<Integer,Double>
{
    private static final long serialVersionUID = 1L;

    public double pct(int year)
    {
        if (year < 1961)
            year = 1961;
        return get(year);
    }
}

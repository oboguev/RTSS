package rtss.data.selectors;

public enum Locality
{
    URBAN ("urban"),
    RURAL ("rural"),
    TOTAL ("total");

    private final String name;       

    private Locality(String s)
    {
        name = s;
    }

    public String toString() 
    {
       return this.name;
    }
    
    public String code()
    {
        switch (this)
        {
        case URBAN:
            return "U";
            
        case RURAL:
            return "R";

        case TOTAL:
            return "T";

        default:
            return "X";
        }
    }
}

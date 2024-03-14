package data.selectors;

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
}

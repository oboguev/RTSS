package rtss.data.selectors;

public enum Area
{
    USSR ("СССР"),
    RSFSR ("РСФСР");

    private final String name;       

    private Area(String s)
    {
        name = s;
    }

    public String toString() 
    {
       return this.name;
    }
}

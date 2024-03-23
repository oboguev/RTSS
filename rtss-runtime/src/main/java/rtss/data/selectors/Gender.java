package rtss.data.selectors;

public enum Gender
{
    MALE ("male"),
    FEMALE ("female"),
    BOTH ("both");

    private final String name;       

    private Gender(String s)
    {
        name = s;
    }

    public String toString() 
    {
       return this.name;
    }
}

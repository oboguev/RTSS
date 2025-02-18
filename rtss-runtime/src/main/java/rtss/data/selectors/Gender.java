package rtss.data.selectors;

public enum Gender
{
    MALE("male"), FEMALE("female"), BOTH("both");

    private final String name;

    private Gender(String s)
    {
        name = s;
    }

    public String toString()
    {
        return this.name;
    }

    public String toShortString()
    {
        switch (this)
        {
        case MALE:
            return "M";
        case FEMALE:
            return "F";
        case BOTH:
            return "B";
        default:
            return "X";
        }
    }

    public static final Gender[] TwoGenders = { Gender.MALE, Gender.FEMALE };
    public static final Gender[] ThreeGenders = { Gender.BOTH, Gender.MALE, Gender.FEMALE };
}

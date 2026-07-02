package rtss.rosbris1959;

public class RosBris1959AgeGroup
{
    public final int from;
    public final int to;

    public RosBris1959AgeGroup(int from, int to)
    {
        this.from = from;
        this.to = to;
    }

    public static final RosBris1959AgeGroup AGE_0 = new RosBris1959AgeGroup(0, 0);
    public static final RosBris1959AgeGroup AGE_1_4 = new RosBris1959AgeGroup(1, 4);
    public static final RosBris1959AgeGroup AGE_5_9 = new RosBris1959AgeGroup(5, 9);
    public static final RosBris1959AgeGroup AGE_10_14 = new RosBris1959AgeGroup(10, 14);
    public static final RosBris1959AgeGroup AGE_15_19 = new RosBris1959AgeGroup(15, 19);
    public static final RosBris1959AgeGroup AGE_20_24 = new RosBris1959AgeGroup(20, 24);
    public static final RosBris1959AgeGroup AGE_25_29 = new RosBris1959AgeGroup(25, 29);
    public static final RosBris1959AgeGroup AGE_30_34 = new RosBris1959AgeGroup(30, 34);
    public static final RosBris1959AgeGroup AGE_35_39 = new RosBris1959AgeGroup(35, 39);
    public static final RosBris1959AgeGroup AGE_40_44 = new RosBris1959AgeGroup(40, 44);
    public static final RosBris1959AgeGroup AGE_45_49 = new RosBris1959AgeGroup(45, 49);
    public static final RosBris1959AgeGroup AGE_50_54 = new RosBris1959AgeGroup(50, 54);
    public static final RosBris1959AgeGroup AGE_55_59 = new RosBris1959AgeGroup(55, 59);
    public static final RosBris1959AgeGroup AGE_60_64 = new RosBris1959AgeGroup(60, 64);
    public static final RosBris1959AgeGroup AGE_65_69 = new RosBris1959AgeGroup(65, 69);
    public static final RosBris1959AgeGroup AGE_70_74 = new RosBris1959AgeGroup(70, 74);
    public static final RosBris1959AgeGroup AGE_75_79 = new RosBris1959AgeGroup(75, 79);
    public static final RosBris1959AgeGroup AGE_80_84 = new RosBris1959AgeGroup(80, 84);
    public static final RosBris1959AgeGroup AGE_85_100 = new RosBris1959AgeGroup(85, 100);

    public static final RosBris1959AgeGroup AllAgeGroups[] = { AGE_0, AGE_1_4, AGE_5_9, AGE_10_14, AGE_15_19, AGE_20_24, AGE_25_29, AGE_30_34,
                                                               AGE_35_39, AGE_40_44, AGE_45_49, AGE_50_54, AGE_55_59, AGE_60_64, AGE_65_69, AGE_70_74,
                                                               AGE_75_79, AGE_80_84, AGE_85_100 };

    public static RosBris1959AgeGroup forAge(int from, int to)
    {
        for (RosBris1959AgeGroup ag : AllAgeGroups)
        {
            if (ag.from == from && ag.to == to)
                return ag;
        }

        return null;
    }

    public static RosBris1959AgeGroup forAge(int from)
    {
        for (RosBris1959AgeGroup ag : AllAgeGroups)
        {
            if (ag.from == from)
                return ag;
        }

        return null;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        RosBris1959AgeGroup other = (RosBris1959AgeGroup) obj;
        return from == other.from && to == other.to;
    }

    @Override
    public int hashCode()
    {
        int result = Integer.hashCode(from);
        result = 31 * result + Integer.hashCode(to);
        return result;
    }

    @Override
    public String toString()
    {
        if (from == to)
            return String.valueOf(from);

        return from + "-" + to;
    }
}

package rtss.rosbris1959;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.csv.CSVSmartReader;
import rtss.data.selectors.Gender;

public class RosBris1959DeathRates
{
    private List<Value> values = new ArrayList<>();

    public static class Value
    {
        public final int year;
        public final Gender gender;
        public final int cause;
        public final RosBris1959AgeGroup age;
        public final double value;

        public Value(int year, Gender gender, int cause, RosBris1959AgeGroup age, double value)
        {
            this.year = year;
            this.gender = gender;
            this.cause = cause;
            this.age = age;
            this.value = value;
        }
    }
    
    public static RosBris1959DeathRates load() throws Exception
    {
        CSVSmartReader csv = CSVSmartReader.fromResource("RosBRIS.1959/DRc5a1959-1988.txt");
        
        Map<RosBris1959AgeGroup,Integer> age2col = new HashMap<>();
        
        int colYear = csv.column("Year");
        int colSex = csv.column("Sex");
        int colCause = csv.column("Cause");
        for (RosBris1959AgeGroup ag : RosBris1959AgeGroup.AllAgeGroups)
        {
            age2col.put(ag, csv.column("Drac" + ag.from));
        }
        
        // #####

        return null;
    }
}

package rtss.mexico.population;

import rtss.util.Util;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MexPopulation
{
    public static void main(String[] args)
    {
        try
        {
            double x = censuses[censuses.length - 1].inYearPosition();
            x = 0;
        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }
    
    static Census[] censuses = {
                               new Census(1895, 10, 20, 0),
                               new Census(1900, 10, 28, 0),
                               new Census(1910, 10, 27, 0),
                               new Census(1921, 11, 30, 0),
                               new Census(1930, 5, 15, 0),
                               new Census(1940, 3, 6, 0),
                               new Census(1950, 6, 6, 0),
                               new Census(1960, 6, 8, 0),
                               new Census(1970, 1, 28, 0),
                               new Census(1980, 6, 4, 0),
                               new Census(1990, 3, 12, 0),
                               new Census(2000, 2, 14, 0),
                               new Census(2010, 6, 12, 0),
                               new Census(2020, 3, 15, 0)
    };
    
    static class Census
    {
        public Census(int year, int month, int day, int population) 
        {
            this.year = year;
            this.month = month;
            this.day = day;
            this.population = population ;
        }
        
        public double inYearPosition()
        {
            Calendar calendar = new GregorianCalendar(year , month - 1, day);
            int ndays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);            
            double r = calendar.get(Calendar.DAY_OF_YEAR) - 1;
            return r / ndays;
        }
        
        public int year;
        public int month;
        public int day;
        public long population;
    }
}

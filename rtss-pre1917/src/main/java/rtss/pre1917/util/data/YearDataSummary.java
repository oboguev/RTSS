package rtss.pre1917.util.data;

public class YearDataSummary
{
    public Integer first_year;
    public Integer last_year;
    public int nyears;

    public Long population;
    public Long births;
    public Long deaths;
    public Long migration;

    public Double cbr;
    public Double cdr;
    public Double ngr;

    public Long avg_population;
    public Double cbr2;
    public Double cdr2;
    public Double ngr2;

    public void add(YearData yd)
    {
        if (first_year == null)
            first_year = yd.year;
        last_year = yd.year;

        this.births += yd.births;
        this.deaths += yd.deaths;
        this.migration += yd.migration;

        this.cbr += yd.cbr;
        this.cdr += yd.cdr;
        this.ngr += yd.ngr;

        this.cbr2 += yd.cbr2;
        this.cdr2 += yd.cdr2;
        this.ngr2 += yd.ngr2;

        nyears++;
    }

    public YearDataSummary getSummary()
    {
        YearDataSummary s = new YearDataSummary();

        s.first_year = this.first_year;
        s.last_year = this.last_year;
        s.nyears = this.nyears;

        s.cbr = this.cbr / nyears;
        s.cdr = this.cdr / nyears;
        s.ngr = this.ngr / nyears;

        s.cbr2 = this.cbr2 / nyears;
        s.cdr2 = this.cdr2 / nyears;
        s.ngr2 = this.ngr2 / nyears;

        s.migration = this.migration;

        return s;
    }
}

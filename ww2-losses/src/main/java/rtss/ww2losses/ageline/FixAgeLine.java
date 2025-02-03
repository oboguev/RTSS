package rtss.ww2losses.ageline;

import rtss.data.selectors.Gender;

public class FixAgeLine
{
    public Gender gender;
    public double age1;
    public double age2;

    public FixAgeLine(Gender gender, double age1, double age2)
    {
        this.gender = gender;
        this.age1 = age1;
        this.age2 = age2;
    }
}

package rtss.pre1917.data;

public class ValueByGender
{
    public Long male;
    public Long female;
    public Long both;
    
    public ValueByGender dup()
    {
        ValueByGender x = new ValueByGender();
        
        x.male = this.male;
        x.female = this.female;
        x.both = this.both;
        
        return x;
    }
}

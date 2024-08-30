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
    
    public void merge(ValueByGender v) 
    {
        male = merge(male, v.male);
        female = merge(female, v.female);
        both = merge(male, v.both);
    }
    
    private Long merge(Long v1, Long v2)
    {
        if (v1 != null && v2 != null)
        {
            return v1 + v2;
        }
        else if (v1 != null)
        {
            return v1;
        }
        else if (v2 != null)
        {
            return v2;
        }
        else
        {
            return null;
        }
    }
}

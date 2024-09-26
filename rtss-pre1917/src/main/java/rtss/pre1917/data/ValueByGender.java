package rtss.pre1917.data;

public class ValueByGender
{
    public Long male;
    public Long female;
    public Long both;
    
    public final URValue urValue;
    
    public ValueByGender(URValue urValue)
    {
        this.urValue = urValue;
    }
    
    public ValueByGender dup(URValue urValue)
    {
        ValueByGender x = new ValueByGender(urValue);
        
        x.male = this.male;
        x.female = this.female;
        x.both = this.both;
        
        return x;
    }
    
    public void merge(ValueByGender v) 
    {
        boolean clearFM = oneNull(male, v.male) || oneNull(female, v.female); 

        male = merge(male, v.male);
        female = merge(female, v.female);
        both = merge(both, v.both);
        
        if (clearFM)
        {
            male = null;
            female = null;
        }
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
    
    private boolean oneNull(Long v1, Long v2)
    {
        if (v1 == null && v2 == null)
            return false; 
        else if (v1 != null && v2 != null)
            return false; 
        else
            return true;
    }

    public void adjustBirths()
    {
        // ###
    }
}

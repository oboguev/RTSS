package rtss.pre1917.data;

import rtss.pre1917.data.URValue.URValueWhich;
import rtss.util.Util;

public class ValueByGender
{
    public Long male;
    public Long female;
    public Long both;
    
    public final URValue urValue;
    public final URValueWhich which;
    
    public ValueByGender(URValue urValue, URValueWhich which)
    {
        this.urValue = urValue;
        this.which = which;
    }
    
    public ValueByGender dup(URValue urValue)
    {
        ValueByGender x = new ValueByGender(urValue, which);
        
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
    
    public void recalcAsSum(ValueByGender v1, ValueByGender v2)
    {
        male = merge(v1.male, v2.male);
        female = merge(v1.female, v2.female);
        both = merge(v1.both, v2.both);
    }
    
    public static final double MaleFemaleBirthRatio = 1.055;

    public boolean adjustBirths()
    {
        if (male != null && female != null)
        {
            long min_female = Math.round(male / MaleFemaleBirthRatio);
            
            if (female < min_female)
            {
                long old_female = female; 
                female = min_female;
                both = male + female;
                
                double pct = female - old_female;
                pct = 100.0 * pct / old_female;
                
                String msg = String.format("Исправлено число рождений девочек %d %s (%s) увеличено на %.1f%%",
                                           urValue.territoryYear.year,
                                           urValue.territoryYear.territory.name,
                                           which.toString(),
                                           pct);
                Util.out(msg);
                return true;
            }
        }
        
        return false;
    }
}

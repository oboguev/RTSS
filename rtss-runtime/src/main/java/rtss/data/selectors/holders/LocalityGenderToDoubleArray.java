package rtss.data.selectors.holders;

import rtss.data.ValueConstraint;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/*
 * Map (locality,gender) to double array and operate its values
 */
public class LocalityGenderToDoubleArray
{
    private int maxindex;
    private ValueConstraint vc;
    
    private GenderToDoubleArray rural;
    private GenderToDoubleArray urban;
    private GenderToDoubleArray total;
    
    public LocalityGenderToDoubleArray(int maxindex, ValueConstraint vc)
    {
        this.maxindex = maxindex;
        this.vc = vc;
        clear();
    }
    
    public LocalityGenderToDoubleArray(LocalityGenderToDoubleArray a)
    {
        this.maxindex = a.maxindex;
        this.vc = a.vc;
        this.rural = new GenderToDoubleArray(a.rural);
        this.urban = new GenderToDoubleArray(a.urban);
        this.total = new GenderToDoubleArray(a.total);
    }
    
    public LocalityGenderToDoubleArray clone()
    {
        return new LocalityGenderToDoubleArray(this);
    }

    public void clear()
    {
        rural = new GenderToDoubleArray(maxindex, vc);
        urban = new GenderToDoubleArray(maxindex, vc);
        total = new GenderToDoubleArray(maxindex, vc);
    }
    
    private GenderToDoubleArray forLocality(Locality locality) throws Exception
    {
        switch (locality)
        {
        case RURAL:  return rural;
        case URBAN:  return urban;
        case TOTAL:  return total;
        default: throw new IllegalArgumentException();
        }
    }
    
    public Double get(Locality locality, Gender gender, int index) throws Exception
    {
        return forLocality(locality).get(gender, index);
    }

    public void put(Locality locality, Gender gender, int index, double v) throws Exception
    {
        set(locality, gender, index, v);
    }

    public void set(Locality locality, Gender gender, int index, double v) throws Exception
    {
        forLocality(locality).set(gender, index, v);
    }
    
    public void zero()
    {
        rural.zero();
        urban.zero();
        total.zero();
    }
    
    public void nullsToZero()
    {
        rural.nullsToZero();
        urban.nullsToZero();
        total.nullsToZero();
    }

    public void fillStartFrom(LocalityGenderToDoubleArray a)
    {
        rural.fillStartFrom(a.rural);
        urban.fillStartFrom(a.urban);
        total.fillStartFrom(a.total);
    }
}

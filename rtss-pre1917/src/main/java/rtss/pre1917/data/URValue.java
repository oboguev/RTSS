package rtss.pre1917.data;

public class URValue
{
    public ValueByGender total = new ValueByGender();
    public ValueByGender rural = new ValueByGender();
    public ValueByGender urban = new ValueByGender();
    
    public Long all()
    {
        return total.both;
    }
    
    public URValue dup()
    {
        URValue x = new URValue();
        
        x.total = this.total.dup();
        x.rural = this.rural.dup();
        x.urban = this.urban.dup();
        
        return x;
    }
}

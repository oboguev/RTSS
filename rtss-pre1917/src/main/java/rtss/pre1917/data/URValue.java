package rtss.pre1917.data;

public class URValue
{
    public Long all;
    public ValueByGender rural = new ValueByGender();
    public ValueByGender urban = new ValueByGender();
    
    public URValue dup()
    {
        URValue x = new URValue();
        
        x.all = this.all;
        x.rural = this.rural.dup();
        x.urban = this.urban.dup();
        
        return x;
    }
}

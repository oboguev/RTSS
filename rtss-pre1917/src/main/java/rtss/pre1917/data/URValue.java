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

    public void merge(URValue v) throws Exception
    {
        total.merge(v.total);
        rural.merge(v.rural);
        urban.merge(v.urban);
        
        // check that total is compatible with rural + urban
        check_urt(total.both, rural.both, urban.both);
        check_urt(total.male, rural.male, urban.male);
        check_urt(total.female, rural.female, urban.female);

        // check that xxx.both is compatible with xxx.male + xxx.female
        check_bmf(total);
        check_bmf(urban);
        check_bmf(rural);
    }
    
    private void check_urt(Long vt, Long vr, Long vu) throws Exception
    {
        if (vr == null && vu == null)
            return;
        
        if (vr == null)
            vr = 0L;

        if (vu == null)
            vu = 0L;
        
        if (vt != null && vt != vr + vu)
            throw new Exception("Mismatch: total != urban + rural");
    }

    private void check_bmf(ValueByGender v) throws Exception
    {
        Long vb = v.both;
        Long vm = v.male;
        Long vf = v.female;
        
        if (vm == null && vf == null)
            return;
        
        if (vm == null)
            vm = 0L;
        
        if (vf == null)
            vf = 0L;
        
        if (vb != null && vb != vm + vf)
            throw new Exception("Mismatch: both != male + female");
    }
}

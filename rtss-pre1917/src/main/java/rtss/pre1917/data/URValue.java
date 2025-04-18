package rtss.pre1917.data;

/*
 * Содержит данные раздельно для городского и сельского населения, и их сумму.
 */
public class URValue
{
    public static enum URValueWhich
    {
        URBAN, RURAL, TOTAL;

        public String toString()
        {
            switch (this)
            {
            case URBAN:
                return "городское";
            case RURAL:
                return "сельское";
            case TOTAL:
                return "городское+сельское";
            default:
                return "";
            }
        }
    }
    
    public ValueByGender total = new ValueByGender(this, URValueWhich.TOTAL);
    public ValueByGender rural = new ValueByGender(this, URValueWhich.RURAL);
    public ValueByGender urban = new ValueByGender(this, URValueWhich.URBAN);

    public final TerritoryYear territoryYear;

    public URValue(TerritoryYear territoryYear)
    {
        this.territoryYear = territoryYear;
    }

    public Long all()
    {
        return total.both;
    }

    public URValue dup(TerritoryYear territoryYear)
    {
        URValue x = new URValue(territoryYear);

        x.total = this.total.dup(x);
        x.rural = this.rural.dup(x);
        x.urban = this.urban.dup(x);

        return x;
    }

    public void merge(URValue v) throws Exception
    {
        validate();
        v.validate();
        
        boolean mOnlyTotal = this.hasOnlyTotal(Gender.MALE) || v.hasOnlyTotal(Gender.MALE);  
        boolean fOnlyTotal = this.hasOnlyTotal(Gender.FEMALE) || v.hasOnlyTotal(Gender.FEMALE);  
        boolean bOnlyTotal = this.hasOnlyTotal(Gender.BOTH) || v.hasOnlyTotal(Gender.BOTH);  
        
        total.merge(v.total);
        rural.merge(v.rural);
        urban.merge(v.urban);
        
        if (mOnlyTotal)
            leaveOnlyTotal(Gender.MALE);
        if (fOnlyTotal)
            leaveOnlyTotal(Gender.FEMALE);
        if (bOnlyTotal)
            leaveOnlyTotal(Gender.BOTH);

        validate();
    }
    
    public void validate() throws Exception
    {
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

    public void adjustFemaleBirths()
    {
        if (rural.adjustFemaleBirths() || urban.adjustFemaleBirths())
        {
            total.recalcAsSum(rural, urban);
        }
        else
        {
            total.adjustFemaleBirths();
        }
    }

    public void leaveOnlyTotalBoth()
    {
        rural.clear();
        urban.clear();
        total.leaveOnlyBoth();
    }

    public void leaveOnlyTotal(Gender gender) throws Exception
    {
        urban.clear(gender);
        rural.clear(gender);
    }
    
    public String toString()
    {
        return String.format("%s: U = (%s), R = (%s), T = (%s)",
                             territoryYear.toString(),
                             urban.toString(),
                             rural.toString(),
                             total.toString());
    }
    
    public boolean hasOnlyTotal(Gender gender) throws Exception
    {
        return urban.get(gender) == null && rural.get(gender) == null && total.get(gender) != null;
    }
}

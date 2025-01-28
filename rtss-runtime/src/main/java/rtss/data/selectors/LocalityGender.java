package rtss.data.selectors;

public class LocalityGender
{
    public static final LocalityGender TOTAL_MALE = new LocalityGender(Locality.TOTAL, Gender.MALE); 
    public static final LocalityGender TOTAL_FEMALE = new LocalityGender(Locality.TOTAL, Gender.FEMALE); 
    
    public static final LocalityGender URBAN_MALE = new LocalityGender(Locality.URBAN, Gender.MALE); 
    public static final LocalityGender URBAN_FEMALE = new LocalityGender(Locality.URBAN, Gender.FEMALE); 

    public static final LocalityGender RURAL_MALE = new LocalityGender(Locality.RURAL, Gender.MALE); 
    public static final LocalityGender RURAL_FEMALE = new LocalityGender(Locality.RURAL, Gender.FEMALE); 

    public Locality locality;
    public Gender gender;
    
    private LocalityGender(Locality locality, Gender gender)
    {
        this.locality = locality;
        this.gender = gender;
    }

    private LocalityGender(LocalityGender lg)
    {
        this.locality = lg.locality;
        this.gender = lg.gender;
    }
}

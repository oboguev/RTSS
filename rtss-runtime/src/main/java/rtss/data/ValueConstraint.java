package rtss.data;

public enum ValueConstraint
{
    NONE,
    POSITIVE,
    NON_NEGATIVE;
    
    public void validate(double v) throws Exception
    {
        switch (this)
        {
        case NONE:
            break;
        
        case POSITIVE:
            if (v <= 0)
                throw new Exception("Неверное (не-положительное) значение");
            break;

        case NON_NEGATIVE:
            if (v < 0)
                throw new Exception("Неверное (отрицательное) значение");
            break;
        }
    }
}

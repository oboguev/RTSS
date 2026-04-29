package rtss.pre1917.data.migration;

public class MissingMigrationDataException extends Exception
{
    private static final long serialVersionUID = 1L;

    public MissingMigrationDataException(String message)
    {
        super(message);
    }
}
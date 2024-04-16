package rtss.util;

import com.fasterxml.uuid.Generators;

public class FastUUID
{
    private static final String uniqueBase = Generators.timeBasedGenerator().generate().toString().replace("-", "");
    private static long seq = 0;

    public static synchronized String getUniqueId()
    {
        return uniqueBase + "-" + (seq++);
    }
}

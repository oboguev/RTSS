package rtss.config;

import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import rtss.util.Util;

public class Config
{
    private static Map<String, Object> config;

    @SuppressWarnings("unused")
    private Config()
    {
        // no default constructor
    }

    private static Map<String, Object> config() throws Exception
    {
        if (config == null)
        {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            config = mapper.readValue(Util.loadResource("rtss-config.yml"), new TypeReference<Map<String, Object>>()
            {
            });
        }

        return config;
    }

    /* =========================================== */

    public static Object asObject(String path) throws Exception
    {
        return asObject(path, null);
    }

    public static Object asRequiredObject(String path) throws Exception
    {
        Object o = asString(path);
        if (o == null)
            throw new Exception("Value for settigng " + path + " is missing");
        return o;
    }

    public static Object asObject(String path, Object defval) throws Exception
    {
        Object o = PropertyUtils.getProperty(config(), path);
        if (o == null)
            o = defval;
        return o;
    }

    /* =========================================== */

    public static String asString(String path) throws Exception
    {
        return asString(path, null);
    }

    public static String asRequiredString(String path) throws Exception
    {
        String s = asString(path);
        if (s == null)
            throw missing(path);
        return s;
    }

    public static String asString(String path, String defval) throws Exception
    {
        Object o = PropertyUtils.getProperty(config(), path);
        if (o == null)
            o = defval;
        if (o == null)
            return null;
        if (!(o instanceof String))
            throw new Exception("Value for settigng " + path + " is not a string");
        return (String) o;
    }
    
    private static Exception missing(String path) throws Exception
    {
        throw new Exception("Value for settigng " + path + " is missing");
    }

    private static Exception not(String path, String type) throws Exception
    {
        throw new Exception(String.format("Value of property %s is not a %s", path, type));
    }

    /* =========================================== */

    /*
     * Get long property by path
     */
    public static long asLong(String path, Long defval) throws Exception
    {
        Object o = asObject(path);
        if (o == null)
            o = defval;
        if (o == null)
            throw missing(path);
        return toLong(path, o);
    }

    public static long asRequiredLong(String path, Long defval) throws Exception
    {
        Object o = asObject(path);
        if (o == null)
            throw missing(path);
        return toLong(path, o);
    }

    public static long asUnsignedLong(String path, Long defval) throws Exception
    {
        Object o = asObject(path);
        if (o == null)
            o = defval;
        if (o == null)
            throw missing(path);
        return toUnsignedLong(path, o);
    }

    public static long asRequiredUnsignedLong(String path, Long defval) throws Exception
    {
        Object o = asObject(path);
        if (o == null)
            throw missing(path);
        return toUnsignedLong(path, o);
    }

    private static long toLong(String path, Object o) throws Exception
    {
        if (o instanceof Long)
        {
            return ((Long) o).longValue();
        }
        else if (o instanceof Integer)
        {
            return ((Integer) o).longValue();
        }
        else if (o instanceof String)
        {
            return Long.parseLong((String) o);
        }
        else
        {
            throw not(path, "long");
        }
    }

    private static long toUnsignedLong(String path, Object o) throws Exception
    {
        long v = toLong(path, o);
        if (v < 0)
            throw new Exception("Value for settigng " + path + " is negative");
        return v;
    }

    /* =========================================== */

    /*
     * Get int property by path
     */
    public static int asInteger(String path, Integer defval) throws Exception
    {
        Object o = asObject(path);
        if (o == null)
            o = defval;
        if (o == null)
            throw missing(path);
        return toInteger(path, o);
    }

    public static int asRequiredInteger(String path, Integer defval) throws Exception
    {
        Object o = asObject(path);
        if (o == null)
            throw missing(path);
        return toInteger(path, o);
    }

    public static int asUnsignedInteger(String path, Integer defval) throws Exception
    {
        Object o = asObject(path);
        if (o == null)
            o = defval;
        if (o == null)
            throw missing(path);
        return toUnsignedInteger(path, o);
    }

    public static int asRequiredUnsignedInteger(String path, Integer defval) throws Exception
    {
        Object o = asObject(path);
        if (o == null)
            throw missing(path);
        return toUnsignedInteger(path, o);
    }

    private static int toInteger(String path, Object o) throws Exception
    {
        if (o instanceof Integer)
        {
            return ((Integer) o).intValue();
        }
        else if (o instanceof Long)
        {
            long v = ((Long) o).longValue();
            if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE)
                throw not(path, "integer");
            return (int) v;
        }
        else if (o instanceof String)
        {
            return Integer.parseInt((String) o);
        }
        else
        {
            throw not(path, "int");
        }
    }

    private static int toUnsignedInteger(String path, Object o) throws Exception
    {
        int v = toInteger(path, o);
        if (v < 0)
            throw new Exception("Value for settigng " + path + " is negative");
        return v;
    }
}

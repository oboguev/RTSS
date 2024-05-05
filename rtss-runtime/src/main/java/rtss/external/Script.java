package rtss.external;

import java.util.Map;

import rtss.util.Util;

/**
 * Load script resource and expand its placeholders with actual parameter valus
 */
public class Script
{
    public static String script(String path, String... param) throws Exception
    {
        return script(path, null, param);
    }

    public static String script(String path, Map<String, String> m) throws Exception
    {
        return script(path, m, null);
    }

    public static String script(String path, Map<String, String> m, String[] param) throws Exception
    {
        String s = Util.loadResource(path);
        s = expand(s, m, param);
        return s;
    }

    public static String expand(String template, Map<String, String> m, String... param) throws Exception
    {
        if (m != null)
        {
            for (String key : m.keySet())
                template = expand(template, key, m.get(key));
        }
        
        if (param != null)
        {
            for (int k = 0; k < param.length; k += 2)
                template = expand(template, param[k], param[k + 1]);
        }
        
        return template;
    }

    public static String expand(String template, String key, String value) throws Exception
    {
        return template.replace("${" + key + "}", value);
    }
}

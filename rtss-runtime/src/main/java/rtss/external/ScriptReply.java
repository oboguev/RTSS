package rtss.external;

import java.util.HashMap;
import java.util.Map;

public class ScriptReply
{
    public static Map<String,String> keysFromReply(String reply, String[] keys) throws Exception
    {
        Map<String,String> m = new HashMap<>();
        
        for (String line : reply.replace("\r", "").split("\n"))
        {
            line = line.trim();
            for (String key : keys)
            {
                String ks = key + ": ";
                if (line.startsWith(ks))
                {
                    String value = line.substring(ks.length());
                    value = value.trim();
                    if (m.containsKey(key))
                        throw new Exception("Duplicate response key: " + key);
                    m.put(key,  value);
                }
            }
        }
        
        for (String key : keys)
        {
            if (!m.containsKey(key))
                throw new Exception("Response missing key: " + key);
        }
        
        return m;
    }
}

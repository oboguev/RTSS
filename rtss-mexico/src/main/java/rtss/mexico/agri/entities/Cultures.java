package rtss.mexico.agri.entities;

import java.util.HashMap;
import java.util.Map;

/*
 * Набор даныных о сельскохозяйственных культурах
 */
public class Cultures 
{
    private Map<String, Culture> m = new HashMap<>();
    
    public boolean contains(String cname)
    {
        return m.containsKey(cname);
    }
    
    public void add(Culture c) throws Exception
    {
        if (m.containsKey(c.name))
            throw new Exception("Duplicate culture " + c.name);
        m.put(c.name, c);
    }
}

package rtss.mexico.agri.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.mexico.agri.loader.LoadCultureDefinitions;

/*
 * Набор даныных о сельскохозяйственных культурах
 */
public class Cultures 
{
    private Map<String, Culture> m = new HashMap<>();
    
    public List<String> names()
    {
        List<String> list = new ArrayList<>(m.keySet());
        Collections.sort(list);
        return list;
    }
    
    public boolean contains(String cname) throws Exception
    {
        CultureDefinition cd = LoadCultureDefinitions.load().get(cname);
        return m.containsKey(cd.name);
    }
    
    public void add(Culture c) throws Exception
    {
        if (m.containsKey(c.name))
            throw new Exception("Duplicate culture " + c.name);
        m.put(c.name, c);
    }
    
    public Culture get(String cname) throws Exception
    {
        CultureDefinition cd = LoadCultureDefinitions.load().get(cname);
        return m.get(cd.name);
    }

    public void remove(Culture c) throws Exception
    {
        m.remove(c.name);
    }
}

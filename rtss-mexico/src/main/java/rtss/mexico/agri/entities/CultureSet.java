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
public class CultureSet
{
    private Map<String, Culture> m = new HashMap<>();

    public List<String> names()
    {
        List<String> list = new ArrayList<>(m.keySet());
        Collections.sort(list);
        return list;
    }

    public List<Culture> cultures() throws Exception
    {
        List<Culture> list = new ArrayList<>();
        for (String cname : names())
            list.add(get(cname));
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

    public void deleteYearRange(int y1, int y2) throws Exception
    {
        for (Culture c : m.values())
            c.deleteYearRange(y1, y2);
    }

    public CultureSet dup() throws Exception
    {
        CultureSet cs = new CultureSet();
        for (Culture c : m.values())
            cs.m.put(c.name, c.dup());
        return cs;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        try
        {
            String sep = "";
            for (Culture c : cultures())
            {
                sb.append(sep);
                sb.append(c.toString());
                sep = ", ";
            }
        }
        catch (Exception ex)
        {
            return "Not dispayable (exception)";
        }
        
        return sb.toString();
    }
}

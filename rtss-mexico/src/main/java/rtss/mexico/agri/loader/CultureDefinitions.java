package rtss.mexico.agri.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.mexico.agri.entities.CultureDefinition;

public class CultureDefinitions
{
    private List<CultureDefinition> definitions = new ArrayList<>();
    private Map<String, CultureDefinition> lc2cd = new HashMap<>();
    
    public CultureDefinition get(String cname)
    {
        return lc2cd.get(cname.toLowerCase());
    }

    public CultureDefinition getRequired(String cname) throws Exception
    {
        CultureDefinition cd = get(cname);
        if (cd == null)
            throw new Exception("Культура не определена: " + cname);
        return cd;
    }
    
    public void add(CultureDefinition cd) throws Exception
    {
        map_name(cd.name, cd);
        map_name(cd.name_ru, cd);
        map_name(cd.name_en, cd);
        for (String alias : cd.aliases)
            map_name(alias, cd);
        definitions.add(cd);
    }
    
    private void map_name(String name, CultureDefinition cd) throws Exception
    {
        if (name == null)
            return;
        name = name.trim();
        if (name.equals(""))
            return;

        String lc = name.toLowerCase();
        CultureDefinition xcd = lc2cd.get(lc);
        if (xcd == null)
        {
            lc2cd.put(lc, cd);
        }
        else if (xcd != cd)
        {
            throw new Exception("Conflicting use of culture name " + name);
        }
    }
}

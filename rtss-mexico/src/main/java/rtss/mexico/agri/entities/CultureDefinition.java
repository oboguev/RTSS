package rtss.mexico.agri.entities;

import java.util.HashSet;
import java.util.Set;

public class CultureDefinition
{
    public String name;
    public String name_ru;
    public String name_en;
    public Set<String> aliases = new HashSet<>();
    public String category;
    public Double kcal_kg;
    public Double seed_pct;
    public Double fodder_pct;
}

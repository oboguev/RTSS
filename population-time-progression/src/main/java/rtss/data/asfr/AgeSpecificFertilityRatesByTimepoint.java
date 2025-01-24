package rtss.data.asfr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Содержит ASFR индексированные по временным точкам.
 * Синтаксис точки произволен, но обычно "год.номер" (номер внутри года).
 */
public class AgeSpecificFertilityRatesByTimepoint
{
    private Map<String, AgeSpecificFertilityRates> m = new HashMap<>();

    public void setForYear(String timepoint, AgeSpecificFertilityRates asfr)
    {
        if (m.containsKey(timepoint))
            throw new IllegalArgumentException();
        m.put(timepoint, asfr);
    }

    public AgeSpecificFertilityRates getForTimepoint(String timepoint)
    {
        return m.get(timepoint);
    }

    /*
     * список возрастных корзин
     */
    public List<String> ageGroups()
    {
        for (AgeSpecificFertilityRates asfr : m.values())
            return asfr.ageGroups();
        return null;
    }
}

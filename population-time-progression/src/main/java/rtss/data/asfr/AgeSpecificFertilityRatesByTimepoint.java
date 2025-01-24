package rtss.data.asfr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.util.Util;

/*
 * Содержит ASFR индексированные по временным точкам.
 * Синтаксис точки произволен, но обычно "год.номер" (номер внутри года).
 */
public class AgeSpecificFertilityRatesByTimepoint
{
    private Map<String, AgeSpecificFertilityRates> m = new HashMap<>();

    public void setForTimepoint(String timepoint, AgeSpecificFertilityRates asfr)
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

    /* ============================================================================================== */

    @Override
    public String toString()
    {
        if (m.size() == 0)
            return "";

        try
        {
            StringBuilder sb = new StringBuilder();

            // header
            sb.append("timepoint");
            for (String s : ageGroups())
                sb.append("," + s);
            sb.append(Util.nl);

            // year-lines
            for (String tp : Util.sort(m.keySet()))
            {
                dump(sb, tp);
            }

            return sb.toString();

        }
        catch (Exception ex)
        {
            return "unable to display";
        }
    }

    private void dump(StringBuilder sb, String tp) throws Exception
    {
        sb.append(tp);
        AgeSpecificFertilityRates asfr = getForTimepoint(tp);
        for (String ag : asfr.ageGroups())
            sb.append(String.format(",%.1f", asfr.forAgeGroup(ag)));
        sb.append(Util.nl);
    }
}

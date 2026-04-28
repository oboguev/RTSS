package rtss.pre1917;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.opencsv.CSVWriter;

import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.URValue;
import rtss.pre1917.data.ValueByGender;

public class ExportData
{
    private List<String> columns = new ArrayList<>();
    private Map<TerritoryNameYearKey, Map<String, String>> m = new HashMap<>();

    private ExportData()
    {
        
    }

    public static ExportData forRaw()
    {
        ExportData ed = new ExportData();
        
        ed.columns.add("территория");
        ed.columns.add("год");
        ed.addDetailedColumns("цск.чн");
        ed.addDetailedColumns("угви.чн");
        ed.addDetailedColumns("угви.чр");
        ed.addDetailedColumns("угви.чс");
        ed.columns.add("мигр");
        ed.columns.add("стаб");
        
        return ed;
    }

    public static ExportData forFinal()
    {
        ExportData ed = new ExportData();
        
        ed.columns.add("территория");
        ed.columns.add("год");
        ed.columns.add("чн");
        ed.columns.add("чр");
        ed.columns.add("чс");
        ed.columns.add("мигр");
        ed.columns.add("р");
        ed.columns.add("с");
        ed.columns.add("еп");
        ed.columns.add("стаб");
        ed.columns.add("vr.ok");
        
        return ed;
    }

    private void addDetailedColumns(String which)
    {
        addDetailedColumns(which, "горсел");
        addDetailedColumns(which, "гор");
        addDetailedColumns(which, "сел");
    }

    private void addDetailedColumns(String which, String ur)
    {
        addDetailedColumns(which, ur, "мж");
        addDetailedColumns(which, ur, "м");
        addDetailedColumns(which, ur, "ж");
    }

    private void addDetailedColumns(String which, String ur, String gender)
    {
        columns.add(String.format("%s.%s.%s", which, ur, gender));
    }

    /* ================================================================================================= */

    private List<String> getTerritoryNames()
    {
        Set<String> names = new TreeSet<>();

        for (TerritoryNameYearKey key : m.keySet())
            names.add(key.territoryName);

        List<String> list = new ArrayList<>(names);
        Collections.sort(list);

        return list;
    }

    private List<Integer> getTerritoryYears(String teritoryName)
    {
        List<Integer> list = new ArrayList<>();
        for (TerritoryNameYearKey key : m.keySet())
        {
            if (key.territoryName.equals(teritoryName))
                list.add(key.year);
        }

        Collections.sort(list);

        return list;
    }

    /* ================================================================================================= */

    public void export(String fpath) throws Exception
    {
        List<String[]> lines = new ArrayList<>();
        lines.add(columns.toArray(new String[0]));

        for (String territoryName : getTerritoryNames())
        {
            for (int year : getTerritoryYears(territoryName))
            {
                Map<String, String> mv = m.get(new TerritoryNameYearKey(territoryName, year));
                List<String> line = new ArrayList<>();
                line.add(territoryName);
                line.add("" + year);

                for (String col : columns)
                {
                    if (col.equals("территория") || col.equals("год"))
                        continue;
                    String value = mv.get(col);
                    if (value == null)
                        value = "";
                    line.add(value);
                }

                lines.add(line.toArray(new String[0]));
            }
        }
        
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(fpath), StandardCharsets.UTF_8))
        {
            bw.write('\uFEFF'); // UTF-8 BOM for Excel
            try (CSVWriter writer = new CSVWriter(bw))
            {
                for (String[] line : lines)
                {
                    writer.writeNext(line);
                }
            }
        }
    }

    /* ================================================================================================= */

    /*
     * Used for export Raw
     */
    public void add(String territoryName, int year, TerritoryYear tyCSK, TerritoryYear tyUGVI, long saldo, boolean stable)
    {
        Map<String, String> mv = new HashMap<>();

        if (tyCSK != null)
        {
            addValues(mv, "цск.чн", tyCSK.population);
        }

        if (tyUGVI != null)
        {
            addValues(mv, "угви.чн", tyUGVI.population);
            addValues(mv, "угви.чр", tyUGVI.births);
            addValues(mv, "угви.чс", tyUGVI.deaths);
        }

        addValue(mv, "мигр", saldo);
        if (stable)
            addValue(mv, "стаб", "*");

        TerritoryNameYearKey key = new TerritoryNameYearKey(territoryName, year);
        if (m.containsKey(key))
            throw new IllegalArgumentException("Duplicate key: " + key);
        m.put(key, mv);
    }

    /*
     * Used for export Final
     */
    public void add(String territoryName, int year, Long population, Long births, Long deaths, Long saldo, boolean stable, Double cbr, Double cdr, Double ngr, boolean vrok)
    {
        Map<String, String> mv = new HashMap<>();

        addValue(mv, "чн", population);
        addValue(mv, "чр", births);
        addValue(mv, "чс", deaths);
        addValue(mv, "мигр", saldo);
        
        if (stable)
            addValue(mv, "стаб", "*");
        
        addRateValue(mv, "р", cbr);
        addRateValue(mv, "с", cdr);
        addRateValue(mv, "еп", ngr);

        addValue(mv, "vr.ok", vrok ? 1L : 0L);

        TerritoryNameYearKey key = new TerritoryNameYearKey(territoryName, year);
        if (m.containsKey(key))
            throw new IllegalArgumentException("Duplicate key: " + key);
        m.put(key, mv);
    }

    /* ================================================================================================= */

    private void addValues(Map<String, String> mv, String prefix, URValue value)
    {
        if (value != null)
        {
            addValues(mv, prefix + ".горсел", value.total);
            addValues(mv, prefix + ".гор", value.urban);
            addValues(mv, prefix + ".сел", value.rural);
        }
    }

    private void addValues(Map<String, String> mv, String prefix, ValueByGender value)
    {
        if (value != null)
        {
            addValue(mv, prefix + ".мж", value.both);
            addValue(mv, prefix + ".м", value.male);
            addValue(mv, prefix + ".ж", value.female);
        }
    }

    private void addValue(Map<String, String> mv, String key, Long value)
    {
        if (key.equals("территория") || key.equals("год") || !columns.contains(key))
            throw new IllegalArgumentException("Invalid key: " + key);

        if (value != null)
        {
            mv.put(key, "" + value);
        }
    }

    private void addRateValue(Map<String, String> mv, String key, Double value)
    {
        if (key.equals("территория") || key.equals("год") || !columns.contains(key))
            throw new IllegalArgumentException("Invalid key: " + key);

        if (value != null)
        {
            mv.put(key, String.format("%.1f", value));
        }
    }

    private void addValue(Map<String, String> mv, String key, String value)
    {
        if (key.equals("территория") || key.equals("год") || !columns.contains(key))
            throw new IllegalArgumentException("Invalid key: " + key);

        if (value != null)
        {
            mv.put(key, value);
        }
    }
}

package rtss.util.excel;

import java.util.List;

import rtss.data.selectors.Gender;

public class ExcelLoader
{
    protected static final String[] keyMales = { "male", "males", "мужчины", "муж", "муж.", "м" };
    protected static final String[] keyFemales = { "female", "females", "женщины", "жен", "жен.", "ж" };
    protected static final String[] keyAge = { "age", "ages", "возраст", "возрасты", "возрастная группа", "возрастные группы" };

    protected static List<Object> loadAges(String path, Gender gender) throws Exception
    {
        return loadValues(path, gender, keyAge);
    }

    protected static List<Object> loadValues(String path, Gender gender, String... matchingColumnNames) throws Exception
    {
        return Excel.loadColumn(path, key(gender), matchingColumnNames);
    }

    protected static String[] key(Gender gender) throws Exception
    {
        switch (gender)
        {
        case MALE:
            return keyMales;
        case FEMALE:
            return keyFemales;
        default:
            throw new Exception("Invalid gender selector");
        }
    }
}
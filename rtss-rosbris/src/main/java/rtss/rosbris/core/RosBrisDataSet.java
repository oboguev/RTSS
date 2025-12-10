package rtss.rosbris.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rtss.util.Util;

public class RosBrisDataSet
{
    private static Logger logger = LoggerFactory.getLogger(RosBrisDataSet.class);

    public static class DataEntry
    {
        private Map<String, String> values = new HashMap<>();

        private DataEntry(String[] keys, String vals[])
        {
            for (int k = 0; k < vals.length; k++)
            {
                values.put(keys[k], vals[k]);
            }
        }

        public boolean has(String key)
        {
            return values.containsKey(key);
        }

        public String asString(String key) throws Exception
        {
            if (!values.containsKey(key))
                throw new Exception("DataEntry has no value for key: " + key);
            return values.get(key).trim();
        }

        public String asStringOptional(String key) throws Exception
        {
            if (!values.containsKey(key))
                return null;
            return values.get(key).trim();
        }

        public int asInt(String key) throws Exception
        {
            String s = asString(key);

            if (s.equals("") || s.equals("."))
            {
                throw new Exception("DataEntry value is not available, key: " + key + ", value: " + s);
            }

            try
            {
                return Integer.valueOf(s);
            }
            catch (Exception ex)
            {
                throw new Exception("DataEntry value is not int, key: " + key + ", value: " + s, ex);
            }
        }

        public double asDouble(String key) throws Exception
        {
            String s = asString(key);

            if (s.equals("") || s.equals("."))
            {
                throw new Exception("DataEntry value is not available, key: " + key + ", value: " + s);
            }

            try
            {
                return Double.valueOf(s);
            }
            catch (Exception ex)
            {
                throw new Exception("DataEntry value is not double, key: " + key + ", value: " + s, ex);
            }
        }
    }

    private List<DataEntry> values = new ArrayList<>();

    public List<DataEntry> entries()
    {
        return values;
    }

    public static RosBrisDataSet load(String path) throws Exception
    {
        RosBrisDataSet ds = new RosBrisDataSet();
        ds.loadFromFile(path);
        return ds;
    }

    private void loadFromFile(String path) throws Exception
    {
        String[] lines = Util.loadResource(path)
                .replace("\r", "")
                .split("\n");

        String[] keys = null;
        int n = 0;

        for (String line : lines)
        {
            n++;
            line = line.trim();
            if (line.length() == 0)
                continue;

            String[] vals = line.split(",");

            if (keys == null)
            {
                keys = vals;
                continue;
            }

            if (vals.length < keys.length)
            {
                logger.warn("Partial data in file {}, line {}, expected: {}, actual: {}",
                            path, n, keys.length, vals.length);
            }
            else if (vals.length != keys.length)
            {
                logger.error("Unexpected number of entries in file {}, line {}, expected: {}, actual: {}",
                             path, n, keys.length, vals.length);
                throw new Exception("Unexpected number of entries in file");
            }

            values.add(new DataEntry(keys, vals));
        }
    }

    public RosBrisDataSet selectEq(String key, String value) throws Exception
    {
        RosBrisDataSet ds = new RosBrisDataSet();
        for (DataEntry de : values)
        {
            if (de.asString(key).equals(value))
                ds.values.add(de);
        }
        return ds;
    }

    public RosBrisDataSet append(RosBrisDataSet ds)
    {
        values.addAll(ds.values);
        return this;
    }
}

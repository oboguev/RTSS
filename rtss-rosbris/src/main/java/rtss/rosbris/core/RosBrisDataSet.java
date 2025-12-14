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
            
            if (vals.length > keys.length)
            {
                logger.error("Unexpected number of entries in file {}, line {}, expected: {}, actual: {}",
                             path, n, keys.length, vals.length);
                throw new Exception("Unexpected number of entries in file");
            }

            DataEntry de = new DataEntry(keys, vals);

            if (vals.length < keys.length && !ignorePartialLine(path, keys, vals))
            {
                logger.warn("Partial data in file {}, line {}, expected: {}, actual: {}, content: {}",
                            path, n, keys.length, vals.length, line);
            }

            values.add(de);
        }
    }

    public RosBrisDataSet selectEq(String key, long value) throws Exception
    {
        return selectEq(key, Long.toString(value));
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
    
    private boolean ignorePartialLine(String path, String[] keys, String[] vals)
    {
        if (path.equals("RosBRIS/PopDa/PopDa2012-2022.txt") || path.equals("RosBRIS/PopDa/PopDa1989-2014.txt"))
        {
            if (keys.length == 105 && vals.length == 4)
            {
                // 1135,R,B
                // 1135,R,F
                // 1135,R,M
                // 1135,T,B
                // 1135,T,F
                // 1135,T,M
                // 1135,U,B
                // 1135,U,F
                // 1135,U,M
                // 1140,R,B
                // 1140,R,F
                // 1140,R,M
                // 1145,R,B
                // 1145,R,F
                // 1145,R,M
                // 1167,R,B
                // 1167,R,F
                // 1167,R,M
                // 1167,T,B
                // 1167,T,F
                // 1167,T,M
                // 1167,U,B
                // 1167,U,F
                // 1167,U,M
                   
                return true;
            }
        }
        
        return false;
    }
}

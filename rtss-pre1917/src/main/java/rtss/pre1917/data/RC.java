package rtss.pre1917.data;

import java.util.List;

public class RC
{
    public static Object get(List<List<Object>> rc, int row, int col)
    {
        if (row >= rc.size())
            return null;
        List<Object> r = rc.get(row);
        if (col >= r.size())
            return null;
        return r.get(col);
    }
}

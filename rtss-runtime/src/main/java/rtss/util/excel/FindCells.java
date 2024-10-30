package rtss.util.excel;

import java.util.List;

public class FindCells
{
    public static RowCol findRequiredVerticalCells(ExcelRC rc, String... strings) throws Exception
    {
        RowCol rowcol = findVerticalCells(rc, strings);
    
        if (rowcol == null)
        {
            String msg = "Cannot find cells containing ";
            String sep = "";
            for (String s : strings)
            {
                msg += sep;
                msg += '"';
                msg += s;
                msg += '"';
                sep = ", ";
            }
            throw new Exception(msg);
        }

        return rowcol;
    }

    public static RowCol findVerticalCells(ExcelRC rc, String... strings) throws Exception
    {
        for (int nr = 0; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            List<Object> rowObjects = rc.get(nr);
            
            for (int nc = 0 ; nc < rowObjects.size(); nc++)
            {
                if (match(rc, nr, nc, strings))
                    return new RowCol(nr, nc);
            }
            
        }

        return null;
    }

    private static boolean match (ExcelRC rc, int nr, int nc, String... strings) throws Exception
    {
        int xnr = nr;
        
        for (String s : strings)
        {
            String o = rc.asString(xnr, nc);
            if (o == null)
                o = "";

            if (s == null)
                s = "";
            
            if (!s.equals(o))
                return false;

            xnr++;
        }

        return true;
    }
}

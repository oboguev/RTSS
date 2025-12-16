package rtss.losses199x.util;

import java.util.ArrayList;
import java.util.List;

import rtss.util.Util;

public class PrintTable
{
    public int ncols;
    public int nrows;

    private List<List<String>> rows = new ArrayList<>();

    public PrintTable(int nrows, int ncols)
    {
        this.nrows = nrows;
        this.ncols = ncols;
        for (int nr = 0; nr < nrows; nr++)
            fillRow();
    }

    public void addRow()
    {
        nrows++;
        fillRow();
    }

    private void fillRow()
    {
        List<String> row = new ArrayList<>();

        for (int nc = 0; nc < ncols; nc++)
            row.add("");

        rows.add(row);
    }

    public void put(int nr, int nc, String value)
    {
        try
        {
            rows.get(nr).set(nc, value);
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    public String get(int nr, int nc)
    {
        return rows.get(nr).get(nc);
    }

    public void print()
    {
        int[] width = new int[ncols];

        for (int nc = 0; nc < ncols; nc++)
        {
            width[nc] = 1;
            for (int nr = 0; nr < nrows; nr++)
            {
                String value = get(nr, nc);
                int len = value.length();
                if (len > width[nc])
                    width[nc] = len;
            }
        }

        for (int nr = 0; nr < nrows; nr++)
        {
            StringBuilder sb = new StringBuilder();

            for (int nc = 0; nc < ncols; nc++)
            {
                if (sb.length() != 0)
                    sb.append(" ");
                String value = get(nr, nc);
                while (value.length() < width[nc])
                    value = " " + value;
                sb.append(value);
            }

            Util.out(sb.toString());
        }
    }
}

package rtss.migration_1946_1958;

import java.io.StringReader;
import java.util.List;

import com.opencsv.CSVReader;

import rtss.util.Util;

public class Migration_1946_1958
{
    public static void main(String[] args)
    {
        try
        {
            Migration_1946_1958 m = new Migration_1946_1958();
            m.do_main();
        }
        catch (Exception ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }

        Util.out("");
        Util.out("*** Completed.");
    }

    private void do_main() throws Exception
    {
        List<String[]> lines;
        String text = Util.loadResource("migration_1946_1958/balance_1946_1958.csv");
        try (CSVReader reader = new CSVReader(new StringReader(text)))
        {
            lines = reader.readAll();
        }        
        
        for (String[] sa : lines)
        {
            switch (sa[0])
            {
            case "всего":
            case "возраст не указан":
                continue;
            case "годы":
                load_years(sa);
                break;
            default:
                // ###
                break;
            }
        }
    }
    
    private void load_years(String[] sa)
    {
        for (int k = 0; k < sa.length; k++)
        {
            
        }
    }
}

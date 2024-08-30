package rtss.tools;

import rtss.util.Clipboard;
import rtss.util.Util;

public class ClipboardConverter
{
    public static void main(String[] args)
    {
        try
        {
            String s = Clipboard.getText();
            if (s == null)
            {
                Util.err("Clipboard is empty");
                return;
            }
            s = s.replace('\n', ' ');
            s = s.replace("\r", "");
            s = s.replace("\'", "");
            s = s.replace(";", "");
            s = s.replace("I", "");
            s = s.replace("І", "");
            s = Util.despace(s);
            s = s.replace(". ", ".");
            s = s.replace(" .", ".");
            s = s.replace(' ', '\t');
            s = s.replace('.', '@');
            s = s.replace(',', '.');
            s = s.replace('@', ',');
            
            String[] sa = s.split("\t");
            StringBuilder sb = new StringBuilder();

            add(sb, sa, 0);
            add(sb, sa, 1);
            add(sb, sa, 2);
            sb.append("\t");
            sb.append("\t");

            add(sb, sa, 3);
            add(sb, sa, 4);
            add(sb, sa, 5);
            sb.append("\t");
            sb.append("\t");
            
            add(sb, sa, 6);
            add(sb, sa, 7);
            add(sb, sa, 8);

            Clipboard.put(sb.toString());
            
            Util.out("Transformed: " + sb.toString());
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }
    
    private static void add(StringBuilder sb, String[] sa, int k)
    {
        if (sa.length > k)
        {
            if (k != 0)
                sb.append('\t');
            sb.append(sa[k]);
        }
    }
}

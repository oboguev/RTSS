package rtss.tools;

import org.apache.commons.lang3.StringUtils;

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
            
            s = convert_4(s);

            Clipboard.put(s);
            Util.out("Transformed: " + s);
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }
    
    @SuppressWarnings("unused")
    private static String convert_4(String s) throws Exception
    {
        s = s.replace(" ", ",");
        s = s.replace(".", ",");
        StringBuilder sb = new StringBuilder ();
        for (String xs : s.split("\n"))
        {
            sb.append(convert_4a(xs) + "\n");
        }
        
        return sb.toString();
    }
    
    private static String convert_4a(String s) throws Exception
    {
        int ccount = s.length() - s.replace(",", "").length();
        
        if (ccount == 3 || ccount == 5 || ccount == 7)
        {
            StringBuilder sb = new StringBuilder ();
            int i = 0 ;
            for (char c : s.toCharArray())
            {
                if (c == ',')
                {
                    i++;
                    if ((i % 2) == 0)
                        c = '\n';
                }
                sb.append(c);
            }
            return sb.toString();
        }
        else
        {
            return s;
        }
    }


    @SuppressWarnings("unused")
    private static String convert_3(String s) throws Exception
    {
        s = s.replace(".", ",");
        s = s.replace("-", ",");
        s = s.replace("°", "0");
        s = s.replace("о", "0");
        s = s.replace("О", "0");
        s = s.replace("і", "1");
        s = s.replace("б", "6");

        s = s.replace("■", "");
        s = s.replace("•", "");
        s = s.replace("І", "1");
        s = s.replace("I", "1");
        
        return s;
    }

    @SuppressWarnings("unused")
    private static String convert_2(String s) throws Exception
    {
        s = Util.despace(s);
        s = s.replace(".", ",");
        s = s.replace(" ", "\n");
        return s;
    }
    
    @SuppressWarnings("unused")
    private static String convert_1(String s) throws Exception
    {
        if (s.startsWith("."))
            s = "1" + s;

        s = s.replace("\'", "");
        s = s.replace(";", "");
        s = s.replace("I", "");
        s = s.replace("І", "");
        s = s.replace(" 1\n", " 1");
        s = s.replace(' ', '\t');
        s = s.replace(". ", ".");
        s = s.replace(" .", ".");
        s = s.replace("i", "1");
        s = s.replace("o", "0");
        s = s.replace("о", "0");

        s = cleanCharset(s);

        s = preprocessLines(s);
        s = s.replace('\n', ' ');
        s = s.replace("\r", "");
        s = Util.despace(s).trim();
        s = fixCommasDots(s);
        s = s.replace(' ', '\t');

        // s = s.replace('.', '@');
        // s = s.replace(',', '.');
        // s = s.replace('@', ',');

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

        return sb.toString();
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

    private static String preprocessLines(String s) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        String sa[] = s.split("\n");
        for (String xs : sa)
        {
            if (sb.length() != 0)
                sb.append("\n");
            sb.append(preprocessLine(xs));
        }
        return sb.toString();
    }

    private static String preprocessLine(String s) throws Exception
    {
        StringBuilder sb = new StringBuilder();

        s = Util.despace(s).trim();

        int k = 0;
        char c;

        while (k < s.length())
        {
            /*
             * Remove spaces till . or ,
             */
            int ndigits = 0;
            for (; k < s.length();)
            {
                c = s.charAt(k++);
                if (c == ' ')
                    continue;
                sb.append(c);
                if (c >= '0' && c <= '9')
                    ndigits++;
                if ((c == '.' || c == ',') && ndigits > 1)
                    break;
            }

            /*
             * Remove spaces (till digit)
             */
            for (; k < s.length();)
            {
                c = s.charAt(k++);
                if (c == ' ')
                    continue;
                sb.append(c);
                break;
            }

            if (k < s.length())
            {
                c = s.charAt(k);
                if (c == ' ')
                {
                    sb.append(c);
                    k++;
                }
            }
        }

        return sb.toString();
    }

    private static String fixCommasDots(String s)
    {
        StringBuilder sb = new StringBuilder();

        for (String token : s.split(" "))
        {
            if (sb.length() != 0)
                sb.append(" ");
            sb.append(fixCommasDotsToken(token));
        }

        return sb.toString();
    }

    private static String fixCommasDotsToken(String s)
    {
        boolean hasDot = s.contains(".");
        boolean hasComma = s.contains(",");

        if (hasComma && hasDot)
        {
            s = s.replace('.', '@');
            s = s.replace(',', '.');
            s = s.replace('@', ',');
            return s;
        }
        else if (hasComma)
        {
            return s.replace(',', '.');
        }
        else if (hasDot)
        {
            if (StringUtils.countMatches(s, ".") > 1)
            {
                s = s.replaceFirst("\\.", ",");
            }
            return s;
        }
        else
        {
            return s;
        }
    }

    private static String cleanCharset(String s)
    {
        StringBuilder sb = new StringBuilder ();
        for (char c : s.toCharArray())
        {
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n')
            {
                sb.append(c);
            }
            else if (c >= 0 && c <= '9')
            {
                sb.append(c);
            }
            else if (c == ',' || c == '.')
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

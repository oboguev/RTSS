package rtss.util;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
// import java.awt.datatransfer.Clipboard;

public class Clipboard
{
    /**
     * Put text to the clipboard
     */
    public static void put(String text)
    {
        StringSelection stringSelection = new StringSelection(text);
        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public static void put(double[] x)
    {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < x.length; k++)
        {
            sb.append(String.format("%f", x[k]));
            sb.append("\n");
        }
        put(sb.toString());
    }

    public static void put(String sep, double[]... xa)
    {
        StringBuilder sb = new StringBuilder();
        int lines = xa[0].length;

        for (int line = 0; line < lines; line++)
        {
            String xsep = "";
            for (double[] da : xa)
            {
                sb.append(xsep);
                sb.append(String.format("%f", da[line]));
                xsep = sep;
            }
            sb.append("\n");
        }

        put(sb.toString());
    }

    public static void put(String sep, int[] ia, double[]... xa)
    {
        StringBuilder sb = new StringBuilder();
        int lines = xa[0].length;

        for (int line = 0; line < lines; line++)
        {
            sb.append(String.format("%d", ia[line]));

            for (double[] da : xa)
            {
                sb.append(sep);
                sb.append(String.format("%f", da[line]));
            }
            sb.append("\n");
        }

        put(sb.toString());
    }
    
    public static String getText() throws Exception
    {
        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Object data = clipboard.getData(DataFlavor.stringFlavor);
        if (data != null && data instanceof String)
            return (String) data;
        else
            return null;
    }
}

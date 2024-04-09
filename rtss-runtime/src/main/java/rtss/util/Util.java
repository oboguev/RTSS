package rtss.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Util
{
    public static boolean True = true;
    public static boolean False = false;

    public static final String EOL;
    public static final String nl = "\n";

    static
    {
        if (File.separatorChar == '/')
            EOL = "\n";
        else
            EOL = "\r\n";
    }

    public static void out(String s)
    {
        System.out.println(s);
    }

    public static void err(String s)
    {
        System.out.flush();

        try
        {
            Thread.sleep(100);
        }
        catch (Exception ex)
        {
        }

        System.err.println(s);
    }

    public static String dirFile(String dir, String file) throws Exception
    {
        File f = new File(dir);
        f = new File(f, file);
        return f.getCanonicalFile().getAbsolutePath();
    }

    public static byte[] readFileAsByteArray(String path) throws Exception
    {
        return Files.readAllBytes(Paths.get(path));
    }

    public static String readFileAsString(String path) throws Exception
    {
        byte[] bytes = readFileAsByteArray(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeAsFile(String path, byte[] bytes) throws Exception
    {
        Files.write(Paths.get(path), bytes);
    }

    public static void writeAsFile(String path, String data) throws Exception
    {
        writeAsFile(path, data.getBytes(StandardCharsets.UTF_8));
    }

    public static char lastchar(String s) throws Exception
    {
        if (s == null)
            return 0;
        int len = s.length();
        if (len == 0)
            return 0;
        return s.charAt(len - 1);
    }

    public static String stripTail(String s, String tail) throws Exception
    {
        if (!s.endsWith(tail))
            throw new Exception("stripTail: [" + s + "] does not end with [" + tail + "]");
        return s.substring(0, s.length() - tail.length());
    }

    public static String stripStart(String s, String start) throws Exception
    {
        if (!s.startsWith(start))
            throw new Exception("stripTail: [" + s + "] does not start with [" + start + "]");
        return s.substring(start.length());
    }

    public static boolean eq(String s1, String s2) // throws Exception
    {
        if (s1 == null && s2 == null)
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.equals(s2);
    }

    public static String replace_start(String word, String pre, String post) throws Exception
    {
        if (!word.startsWith(pre))
            throw new Exception("[" + word + "] does not start with [" + pre + "]");

        String term = word.substring(pre.length());

        return post + term;
    }

    public static String replace_tail(String word, String pre, String post) throws Exception
    {
        if (!word.endsWith(pre))
            throw new Exception("[" + word + "] does not end with [" + pre + "]");

        String base = word.substring(0, word.length() - pre.length());

        return base + post;
    }

    public static String despace(String text) throws Exception
    {
        if (text == null)
            return text;
        text = text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        text = text.replaceAll("\\s+", " ").trim();
        if (text.equals(" "))
            text = "";
        return text;
    }

    public static byte[] loadResourceAsBytes(String path) throws Exception
    {
        return Files.readAllBytes(Paths.get(Util.class.getClassLoader().getResource(path).toURI()));
    }

    public static String loadResource(String path) throws Exception
    {
        return new String(loadResourceAsBytes(path), StandardCharsets.UTF_8);
    }

    public static String f2s(double f) throws Exception
    {
        String s = String.format("%f", f);
        if (!s.contains("."))
            return s;
        while (s.endsWith("0") && !s.endsWith(".0"))
            s = Util.stripTail(s, "0");
        if (s.endsWith(".0"))
            s = Util.stripTail(s, ".0");
        return s;
    }

    // check if values differ
    public static boolean differ(double a, double b)
    {
        return differ(a, b, 0.00001);
    }

    // check if values differ
    public static boolean differ(double a, double b, double diff)
    {
        return Math.abs(a - b) / Math.max(Math.abs(a), Math.abs(b)) > diff;
    }

    // min of array values
    public static double min(final double[] y)
    {
        double min = y[0];
        for (int k = 1; k < y.length; k++)
        {
            if (y[k] < min)
                min = y[k];
        }
        return min;
    }

    // max of array values
    public static double max(final double[] y)
    {
        double max = y[0];
        for (int k = 1; k < y.length; k++)
        {
            if (y[k] > max)
                max = y[k];
        }
        return max;
    }

    // sum of array values
    public static double sum(final double[] y)
    {
        double sum = 0;
        for (int k = 0; k < y.length; k++)
            sum += y[k];
        return sum;
    }

    // average value over array
    public static double average(final double[] y)
    {
        return sum(y) / y.length;
    }

    // return a new array with values representing y[] / f
    public static double[] divide(final double[] y, double f)
    {
        double[] yy = new double[y.length];
        for (int x = 0; x < y.length; x++)
            yy[x] = y[x] / f;
        return yy;
    }

    // extract a subsection [x1...x2] from am array of doubles
    public static double[] splice(final double[] y, int x1, int x2)
    {
        double[] yy = new double[x2 - x1 + 1];
        for (int x = x1; x <= x2; x++)
        {
            yy[x - x1] = y[x];
        }
        return yy;
    }

    // insert y[] into yy[x ... x + yy.length - 1]
    public static void insert(double[] yy, final double[] y, int x)
    {
        for (int k = 0; k < y.length; k++)
            yy[x + k] = y[k];
    }

    // clone array of doubles
    public static double[] dup(final double[] y)
    {
        if (y.length == 0)
            return new double[0];
        else
            return splice(y, 0, y.length - 1);
    }

    // generate sequence of integers k1 ... k2 
    public static int[] seq_int(int k1, int k2)
    {
        int[] seq = new int[k2 - k1 + 1];
        for (int k = 0; k < seq.length; k++)
            seq[k] = k + k1;
        return seq;
    }

    // generate sequence of doubles  
    public static double[] seq_double(final int[] si)
    {
        double[] seq = new double[si.length];
        for (int k = 0; k < si.length; k++)
            seq[k] = si[k];
        return seq;
    }
    
    public static boolean isPositive(final double[] yy)
    {
        for (double y : yy)
        {
            if (y <= 0)
                return false;
        }
        
        return true;
    }

    public static boolean isNonNegative(final double[] yy)
    {
        for (double y : yy)
        {
            if (y < 0)
                return false;
        }
        
        return true;
    }

    public static void print(String title, final double[] y, int start_year)
    {
        Util.out(title);
        Util.out("");
        for (int x = 0; x < y.length; x++)
        {
            Util.out(String.format("%4d  %f", x + start_year, y[x]));
        }
        Util.out("-------- end of " + title + " -------- ");
        Util.out("");
    }

    public static String removeComments(String lines)
    {
        lines = removeComments(lines, "#");
        lines = removeComments(lines, "//");
        return lines;
    }

    public static String removeComments(String lines, String tag)
    {
        StringBuilder sb = new StringBuilder();

        for (String line : lines.replace("\r\n", "\n").split("\n"))
        {
            if (line != null)
            {
                int k = line.indexOf(tag);
                if (k != -1)
                    line = line.substring(0, k);
                sb.append(line);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static String trimLines(String lines)
    {
        StringBuilder sb = new StringBuilder();

        for (String line : lines.replace("\r\n", "\n").split("\n"))
        {
            if (line != null)
            {
                sb.append(line.trim());
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static String removeEmptyLines(String lines)
    {
        StringBuilder sb = new StringBuilder();

        for (String line : lines.replace("\r\n", "\n").split("\n"))
        {
            if (line != null && line.length() != 0)
            {
                sb.append(line);
                sb.append("\n");
            }
        }

        return sb.toString();
    }
    
    public static void unused(Object... o)
    {
    }

    public static void noop()
    {
        // for debugging
    }
}

package rtss.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Util
{
    public static String EOL;
    public static boolean True = true;
    public static boolean False = false;

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
    
    public static String loadResource(String path) throws Exception
    {
        byte[] bytes = Files.readAllBytes(Paths.get(Util.class.getClassLoader().getResource(path).toURI()));
        return new String(bytes, StandardCharsets.UTF_8);
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

    public static double min(double[] y)
    {
        double min = y[0];
        for (int k = 1; k < y.length; k++)
        {
            if (y[k] < min)
                min = y[k];
        }
        return min;
    }

    public static double max(double[] y)
    {
        double max = y[0];
        for (int k = 1; k < y.length; k++)
        {
            if (y[k] > max)
                max = y[k];
        }
        return max;
    }

    public static double sum(double[] y)
    {
        double sum = 0;
        for (int k = 0; k < y.length; k++)
            sum += y[k];
        return sum;
    }
    
    public static double average(double[] y)
    {
        return sum(y) / y.length;
    }

    public static double[] splice(double[] y, int yx1, int yx2)
    {
        double[] yy = new double[yx2 - yx1 + 1];
        for (int yx = yx1; yx <= yx2; yx++)
        {
            yy[yx - yx1] = y[yx];
        }
        return yy;
    }
    
    public static void noop()
    {
        // for debugging
    }
}

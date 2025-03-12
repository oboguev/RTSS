package rtss.util;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rtss.config.Config;

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
            noop();
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
        try
        {
            return Files.readAllBytes(Paths.get(Util.class.getClassLoader().getResource(path).toURI()));
        }
        catch (Exception ex)
        {
            throw new Exception("Unable to load resource " + path, ex);
        }
    }

    public static String loadResource(String path) throws Exception
    {
        return new String(loadResourceAsBytes(path), StandardCharsets.UTF_8);
    }

    public static String f2s(double f) throws Exception
    {
        String s = new BigDecimal(Double.valueOf(f).toString()).toPlainString();

        if (s.contains("."))
        {
            while (s.endsWith("0") && !s.endsWith(".0"))
                s = Util.stripTail(s, "0");
            if (s.endsWith(".0"))
                s = Util.stripTail(s, ".0");
        }

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
        if (!isValid(a) || !isValid(b) || !isValid(diff))
            return true;
        return Math.abs(a - b) / Math.max(Math.abs(a), Math.abs(b)) > diff;
    }

    public static boolean differ(double[] a, double[] b)
    {
        return differ(a, b, 0.00001);
    }

    public static boolean differ(double[] a, double[] b, double diff)
    {
        if (a.length != b.length)
            throw new IllegalArgumentException("array dimensions differ");

        for (int k = 0; k < a.length; k++)
        {
            if (differ(a[k], b[k], diff))
                return true;
        }

        return false;
    }

    public static boolean same(double a, double b)
    {
        return !differ(a, b);
    }

    public static boolean same(double[] a, double[] b)
    {
        return !differ(a, b);
    }

    public static boolean same(double a, double b, double diff)
    {
        return !differ(a, b, diff);
    }

    public static boolean same(double[] a, double[] b, double diff)
    {
        return !differ(a, b, diff);
    }

    public static void checkSame(double a, double b) throws ArithmeticValidationException
    {
        if (!same(a, b))
            throw new ArithmeticValidationException("Values differ");
    }

    public static void checkSame(double[] a, double[] b) throws ArithmeticValidationException
    {
        if (!same(a, b))
            throw new ArithmeticValidationException("Values differ");
    }

    public static void checkSame(double a, double b, double diff) throws ArithmeticValidationException
    {
        if (!same(a, b, diff))
            throw new ArithmeticValidationException("Values differ");
    }

    public static void checkSame(double[] a, double[] b, double diff) throws ArithmeticValidationException
    {
        if (!same(a, b, diff))
            throw new ArithmeticValidationException("Values differ");
    }

    public static boolean isValid(double v)
    {
        return Double.isFinite(v);
    }

    public static void checkValid(double v) throws ArithmeticValidationException
    {
        if (!isValid(v))
            throw new ArithmeticValidationException("Not a valid number");
    }

    public static void checkNonNegative(double v) throws ArithmeticValidationException
    {
        if (v < 0)
            throw new ArithmeticValidationException("Unexpected negative number");
    }

    public static void checkValidNonNegative(double v) throws ArithmeticException
    {
        checkValid(v);
        checkNonNegative(v);
    }

    public static double validate(double v) throws ArithmeticValidationException
    {
        if (!isValid(v))
            throw new ArithmeticValidationException("Not a valid number");
        return v;
    }

    public static void checkValid(double[] v) throws ArithmeticValidationException
    {
        for (double vv : v)
        {
            if (!isValid(vv))
                throw new ArithmeticValidationException("Not a valid number");
        }
    }

    public static void checkNonNegative(double[] v) throws ArithmeticValidationException
    {
        for (int k = 0; k < v.length; k++)
        {
            if (v[k] < 0)
                throw new ArithmeticValidationException("Unexpected negative number");
        }
    }

    public static void checkValidNonNegative(double[] v) throws ArithmeticValidationException
    {
        for (double vv : v)
        {
            checkValid(vv);
            checkNonNegative(vv);
        }
    }

    public static double[] validate(double[] v) throws ArithmeticValidationException
    {
        for (double vv : v)
        {
            if (!isValid(vv))
                throw new ArithmeticValidationException("Not a valid number");
        }

        return v;
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
    public static int sum(final int[] y)
    {
        int sum = 0;
        for (int k = 0; k < y.length; k++)
            sum += y[k];
        return sum;
    }

    public static double sum(final double[] y)
    {
        double sum = 0;
        for (int k = 0; k < y.length; k++)
            sum += y[k];
        return sum;
    }

    public static double sum_range(final double[] y, int x1, int x2)
    {
        int size = x2 - x1 + 1;
        if (size <= 0)
            throw new IllegalArgumentException("array sum_range : negative or empty size");

        double sum = 0;
        for (int x = x1; x <= x2; x++)
            sum += y[x];
        return sum;
    }

    public static double sum(Collection<Double> y)
    {
        double sum = 0;
        for (Double v : y)
            sum += v;
        return sum;
    }

    public static double lastElement(double[] a)
    {
        return a[a.length - 1];
    }

    public static double preLastElement(double[] a)
    {
        return a[a.length - 2];
    }

    // per-element array addition
    public static double[] add(final double[] a, final double[] b) throws Exception
    {
        if (a.length != b.length)
            throw new IllegalArgumentException();
        double[] v = new double[a.length];
        for (int k = 0; k < a.length; k++)
            v[k] = a[k] + b[k];
        return v;
    }

    // per-element array substraction
    public static double[] sub(final double[] a, final double[] b) throws Exception
    {
        if (a.length != b.length)
            throw new IllegalArgumentException();
        double[] v = new double[a.length];
        for (int k = 0; k < a.length; k++)
            v[k] = a[k] - b[k];
        return v;
    }

    // add b to each element of input array
    public static double[] add(final double[] a, final double b)
    {
        double[] v = new double[a.length];
        for (int k = 0; k < a.length; k++)
            v[k] = a[k] + b;
        return v;
    }

    // average value over array
    public static double average(final double[] y)
    {
        return sum(y) / y.length;
    }

    // normalize array so the sum of its elements is 1.0
    public static double[] normalize(final double[] y)
    {
        return normalize(y, 1.0);
    }

    // normalize array so the sum of its elements is @sum
    public static double[] normalize(final double[] y, double sum)
    {
        return multiply(y, sum / sum(y));
    }

    /*
     * Взвешенная сумма w1*ww1 + w2*ww2
     * 
     * Массивы ww1 и ww2 предварительно нормализуются по сумме всех членов на 1.0
     * (без изменения начальных копий).
     * 
     * Возвращаемый результат также нормализуется. 
     */
    public static double[] sumWeightedNormalized(double w1, double[] ww1, double w2, double[] ww2) throws Exception
    {
        ww1 = Util.normalize(ww1);
        ww2 = Util.normalize(ww2);

        ww1 = Util.multiply(ww1, w1);
        ww2 = Util.multiply(ww2, w2);

        double[] ww = Util.add(ww1, ww2);
        ww = Util.normalize(ww);

        return ww;
    }

    // sign of the value
    public static int sign(double d)
    {
        if (d > 0)
            return 1;
        else if (d < 0)
            return -1;
        else
            return 0;
    }

    // check if @x is with range [x1...x2], where x1 and x2 can be inversed 
    public static boolean within(double x, double x1, double x2)
    {
        double xx1 = Math.min(x1, x2);
        double xx2 = Math.max(x1, x2);
        return x >= xx1 && x <= xx2;
    }

    // return a new array with values representing y[] * f
    public static double[] multiply(final double[] y, double f)
    {
        double[] yy = new double[y.length];
        for (int x = 0; x < y.length; x++)
            yy[x] = y[x] * f;
        return yy;
    }

    public static int[] multiply(final int[] y, int f)
    {
        int[] yy = new int[y.length];
        for (int x = 0; x < y.length; x++)
            yy[x] = y[x] * f;
        return yy;
    }

    // return a new array with values representing y[] * f[]
    public static double[] multiply(final double[] y, double[] f) throws Exception
    {
        Util.assertion(y.length == f.length);
        double[] yy = new double[y.length];
        for (int x = 0; x < y.length; x++)
            yy[x] = y[x] * f[x];
        return yy;
    }

    // return a new array with values representing y[] / f
    public static double[] divide(final double[] y, double f)
    {
        double[] yy = new double[y.length];
        for (int x = 0; x < y.length; x++)
            yy[x] = y[x] / f;
        return yy;
    }

    // return a new array with values representing y[] / f[]
    public static double[] divide(final double[] y, double[] f) throws Exception
    {
        Util.assertion(y.length == f.length);
        double[] yy = new double[y.length];
        for (int x = 0; x < y.length; x++)
            yy[x] = Util.validate(y[x] / f[x]);
        return yy;
    }

    // extract a subsection [x1...x2] from am array of doubles
    public static double[] splice(final double[] y, int x1, int x2)
    {
        int size = x2 - x1 + 1;
        if (size <= 0)
            throw new IllegalArgumentException("array splice : negative size");

        double[] yy = new double[size];
        for (int x = x1; x <= x2; x++)
            yy[x - x1] = y[x];
        return yy;
    }

    // insert src[] into dst[x ... x + yy.length - 1]
    public static void insert(double[] dst, final double[] src, int x)
    {
        for (int k = 0; k < src.length; k++)
            dst[x + k] = src[k];
    }

    // clone array of doubles
    public static double[] dup(final double[] y)
    {
        if (y.length == 0)
            return new double[0];
        else
            return splice(y, 0, y.length - 1);
    }

    // clone array of integers
    public static int[] dup(final int[] y)
    {
        int[] r = new int[y.length];
        for (int k = 0; k < y.length; k++)
            r[k] = y[k];
        return r;
    }

    // clone array of doubles in reverse order
    public static double[] reverse(final double[] y)
    {
        if (y.length == 0)
            return new double[0];

        double[] d = new double[y.length];
        for (int k = 0; k < y.length; k++)
            d[k] = y[y.length - 1 - k];
        return d;
    }

    public static void fill(final double[] y, double v)
    {
        for (int k = 0; k < y.length; k++)
            y[k] = v;
    }

    public static double[] fill_double(int size, double v)
    {
        double[] y = new double[size];
        for (int k = 0; k < y.length; k++)
            y[k] = v;
        return y;
    }

    // concatenate arrays a and d
    public static double[] concat(double[] a, double... b)
    {
        double[] r = new double[a.length + b.length];
        int ix = 0;

        for (int k = 0; k < a.length; k++)
            r[ix++] = a[k];

        for (int k = 0; k < b.length; k++)
            r[ix++] = b[k];

        return r;
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

    public static boolean isNegative(final double[] yy)
    {
        for (double y : yy)
        {
            if (y >= 0)
                return false;
        }

        return true;
    }

    public static boolean isMonotonicallyDecreasing(final double[] y, boolean strict)
    {
        if (y.length == 0)
            return true;

        boolean b = true;

        for (int k = 0; k < y.length - 1; k++)
        {
            if (strict)
            {
                b = b && y[k] > y[k + 1];
            }
            else
            {
                b = b && y[k] >= y[k + 1];
            }
        }

        return b;
    }

    // absolute values of the array
    public static double[] abs(double[] y)
    {
        double[] yy = new double[y.length];
        for (int k = 0; k < y.length; k++)
            yy[k] = Math.abs(y[k]);
        return yy;
    }

    // variance of the array
    public static double averageDeviation(double[] y)
    {
        if (y.length == 0)
            return 0;

        // mean of array values
        double mean = sum(y) / y.length;

        // variance
        double variance = 0.0;
        for (double v : y)
            variance += Math.abs(v - mean);

        return variance / y.length;
    }
    
    public static double[] repeat(int times, double v)
    {
        double[] y = new double[times];
        for (int k = 0; k < times; k++)
            y[k] = v;
        return y;
    }

    // Gini coefficient (a measure of concentration), ranges 0 to 1
    public static double gini(double[] y)
    {
        checkValidNonNegative(y);

        // Sort the array in ascending order
        y = dup(y);
        Arrays.sort(y);

        double n = y.length;
        double sum = 0;

        // Calculate the sum of the weighted values
        for (int i = 0; i < n; i++)
            sum += (i + 1) * y[i];

        // double gini = (2 * sum) / (n * sum(y)) - (n + 1) / n;

        sum /= sum(y);

        // sum ranges from (n + 1) / 2 to n
        double gini = (2 * sum - n - 1) / (n - 1);

        Util.assertion(gini >= 0 && gini <= 1);

        return gini;
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

    public static <T extends Comparable<T>> List<T> sort(Collection<T> coll)
    {
        List<T> list = new ArrayList<T>();
        list.addAll(coll);
        Collections.sort(list);
        return list;
    }

    public static int[] toIntArray(List<Integer> list)
    {
        int[] a = new int[list.size()];
        int k = 0;
        for (int v : list)
            a[k++] = v;
        return a;
    }

    public static double[] toDoubleArray(List<Double> list)
    {
        double[] a = new double[list.size()];
        int k = 0;
        for (double v : list)
            a[k++] = v;
        return a;
    }

    /*
     * Return stack frame string.
     * depth = 0 => for the place of the call
     * depth = 1 => for caller of the place of the call
     */
    public static String stackFrame(int depth)
    {
        boolean gotit = false;
        int ix = 0;

        for (StackTraceElement t : Thread.currentThread().getStackTrace())
        {
            if (t.getClassName().equals(Util.class.getCanonicalName()) && t.getMethodName().equals("stackFrame"))
            {
                gotit = true;
            }
            else if (gotit)
            {
                if (ix == depth)
                    return t.getClassName() + "." + t.getMethodName() + " line " + t.getLineNumber();
                ix++;
            }
        }

        return "";
    }
    
    public static Double[] boxArray(double[] a)
    {
        Double[] v = new Double[a.length];
        for (int k = 0; k < a.length; k++)
            v[k] = a[k];
        return v;
    }

    public static void assertion(boolean b)
    {
        assertion(b, null);
    }

    public static void assertion(String msg, boolean b)
    {
        assertion(b, msg);
    }

    public static void assertion(boolean b, String msg)
    {
        if (!b)
        {
            if (msg != null && msg.length() != 0)
                throw new RuntimeException("внутренняя ошибка: " + msg);
            else
                throw new RuntimeException("внутренняя ошибка");
        }
    }

    private static int availableProcessors = 0;

    public static synchronized int availableProcessors() throws Exception
    {
        if (availableProcessors != 0)
            return availableProcessors;

        int physicalCores = Runtime.getRuntime().availableProcessors();

        int cores = (int) Math.round(physicalCores * 0.75);

        Integer minp = Config.asOptionalUnsignedInteger("cpus.min", null);
        if (minp != null)
            cores = Math.max(minp, cores);

        Integer maxp = Config.asOptionalUnsignedInteger("cpus.max", null);
        if (maxp != null)
            cores = Math.min(maxp, cores);

        availableProcessors = cores;

        Util.out(String.format("    [Using %d processors (parallel threads) out of %d available physical cores]",
                               availableProcessors,
                               physicalCores));

        return cores;
    }

    public static void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (Exception ex)
        {
            noop();
        }
    }

    public static void unused(Object... o)
    {
    }

    public static void noop()
    {
        // for debugging
    }
}

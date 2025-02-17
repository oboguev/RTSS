package rtss.util.plot;

import java.io.File;

/*
 * Instructs the code to capture charts/images
 */
public class CaptureImages
{
    public final String dir;
    public final String prefix;
    public final String suffix;
    public final int cx;
    public final int cy;
    
    public CaptureImages(String dir, String prefix, String suffix, int cx, int cy)
    {
        this.dir = dir;
        this.prefix = prefix;
        this.suffix = suffix;
        this.cx = cx;
        this.cy = cy;
    }
    
    private static ThreadLocal<CaptureImages> current = new ThreadLocal<>();

    public static void capture(String dir, String prefix, String suffix, int cx, int cy)
    {
        current.set(new CaptureImages(dir, prefix, suffix, cx, cy));
    }

    public static void stop()
    {
        current.set(null);
    }

    public static CaptureImages get()
    {
        return current.get();
    }
    
    public String path(String fn) throws Exception
    {
        File fd = new File(dir);
        fd.mkdirs();
        
        if (suffix != null && suffix.length() != 0)
        {
            int dot = fn.lastIndexOf('.');
            
            if (dot != -1)
            {
                String fn1 = fn.substring(0, dot);
                String fn2 = fn.substring(dot);
                fn = fn1 + suffix + fn2;
            }
            else
            {
                fn += suffix;
            }
        }
        
        if (prefix != null)
            fn = prefix + fn;

        File fp = new File(fd, fn);
        
        return fp.getAbsoluteFile().getCanonicalPath();
    }
}

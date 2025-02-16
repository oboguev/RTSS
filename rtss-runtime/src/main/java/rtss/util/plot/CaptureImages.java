package rtss.util.plot;

import java.io.File;

/*
 * Instructs the code to capture charts/images
 */
public class CaptureImages
{
    public final String dir;
    public final String prefix;
    public final int cx;
    public final int cy;
    
    public CaptureImages(String dir, String prefix, int cx, int cy)
    {
        this.dir = dir;
        this.prefix = prefix;
        this.cx = cx;
        this.cy = cy;
    }
    
    private static ThreadLocal<CaptureImages> current = new ThreadLocal<>();

    public static void capture(String dir, String prefix, int cx, int cy)
    {
        current.set(new CaptureImages(dir, prefix, cx, cy));
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
        
        if (prefix != null)
            fn = prefix + fn;
        File fp = new File(fd, fn);
        
        return fp.getAbsoluteFile().getCanonicalPath();
    }
}

package rtss.external.Osier;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import rtss.config.Config;
import rtss.external.ProcessRunner;
import rtss.util.Util;

/**
 * Execute Osier commands locally
 */
public class OsierLocal extends ProcessRunner implements OsierCall
{
    private boolean log = false;
    private boolean hasExcel = false;

    public OsierLocal setLog(boolean log)
    {
        this.log = log;
        return this;
    }

    @Override
    public synchronized String execute(String s, boolean reuse) throws Exception
    {
        try
        {
            if (!reuse || os == null)
                stop();

            if (os == null)
                start();

            return execute(s);
        }
        catch (Throwable ex)
        {
            Util.err("*** Unable to execute Osier");
            ex.printStackTrace();
            return null;
        }
        finally
        {
            if (!reuse || os == null)
                stop();
        }
    }

    private void start() throws Exception
    {
        // super.start(Config.asRequiredString("Osier.executable"));
        super.start("cscript " + getStdinWrapperScriptFile());
    }

    @Override
    public void stop() throws Exception
    {
        try
        {
            if (hasExcel && os != null)
            {
                // try to stop excel-way first
                String script = Util.loadResource("osier-excel/stop-excel.vbs");
                os.write(script.getBytes(StandardCharsets.UTF_8));
                os.flush();
                Util.sleep(2000);
            }
        }
        catch (Throwable ex)
        {
            Util.noop();
        }

        super.stop();
    }

    private String execute(String script) throws Exception
    {
        String nl = "\n";

        if (!script.endsWith(nl))
            script += nl;

        log("");
        log("**** Sending to Osier for execution at " + Instant.now().toString());
        log("");
        log(script);

        String cmd_begin = String.format("WScript.StdOut.WriteLine \"%s\"", Osier.BEGIN_SCRIPT);
        String cmd_end = String.format("WScript.StdOut.WriteLine \"%s\"", Osier.END_SCRIPT);

        script = cmd_begin + nl + script + nl + cmd_end + nl + OsierScript.EXEC;

        os.write(script.getBytes(StandardCharsets.UTF_8));
        os.flush();
        hasExcel = true;

        boolean seen_first = false;
        boolean seen_begin = false;
        boolean seen_end = false;
        Instant ts0 = Instant.now();

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            if (!seen_first)
            {
                log("");
                log("**** Response: ");
                log("");
                seen_first = true;
            }

            if (!seen_begin)
            {
                seen_begin = line.equals(Osier.BEGIN_SCRIPT);
                if (!seen_begin)
                {
                    Duration duration = Duration.between(ts0, Instant.now());
                    if (duration.toSeconds() > 30)
                    {
                        // must see begin within a second at most and certainly within 30 seconds
                        Util.err("Reply from Osier did not start within 30 seconds");
                        return null;
                    }
                }
            }
            else
            {
                log(line);

                if (line.equals(Osier.END_SCRIPT))
                {
                    seen_end = true;
                    break;
                }
                
                if (sb.length() != 0)
                    sb.append(nl);
                sb.append(line);
            }
        }

        if (seen_begin && seen_end)
            return sb.toString();
        else
            return null;
    }

    private void log(String s)
    {
        if (log)
            Util.out(s);
    }

    @Override
    public String ping(String tag)
    {
        return tag;
    }
    
    private static String StdinWrapperScriptFile = null;
    
    private static synchronized String getStdinWrapperScriptFile() throws Exception
    {
        if (StdinWrapperScriptFile == null)
        {
            File tf = File.createTempFile("rtss-osier-execute-stdin-", ".vbs");
            String path = tf.getAbsoluteFile().getCanonicalPath();
            String script = Util.loadResource("osier-excel/execute-stdin.vbs");
            Util.writeAsFile(path, script);
            StdinWrapperScriptFile = path;
        }

        return StdinWrapperScriptFile;
    }
}

package rtss.external.Osier;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import rtss.config.Config;
// import rtss.config.Config;
import rtss.external.ProcessRunner;
import rtss.external.ShutdownHook;
import rtss.util.Util;

/**
 * Execute Osier commands locally
 */
public class OsierLocal extends ProcessRunner implements OsierCall
{
    private boolean log = false;
    private boolean hasExcel = false;
    private boolean definedStartupScript = false;
    private boolean uploadedStartupScript = false;
    private String startupScript = null;
    private static final String nl = "\n";
    private static ShutdownHook shutdownHook = null;  

    public OsierLocal setLog(boolean log)
    {
        this.log = log;
        return this;
    }
    
    @Override
    public void setDefaultStartupScript(boolean visible) throws Exception
    {
        setStartupScript(OsierScript.getDefaultStartupScript(visible));
    }
    
    @Override
    public void setStartupScript(String sc) throws Exception
    {
        if (!sc.endsWith(nl))
            sc += nl;
        startupScript = sc;
        definedStartupScript = true;
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
        killBackgroundExcelInstances();
        
        if (shutdownHook == null)
            shutdownHook = ShutdownHook.add(this::do_stop);
        
        // super.start(Config.asRequiredString("Osier.executable"));
        super.start("cscript " + getStdinWrapperScriptFile());
    }

    @Override
    public void stop() throws Exception
    {
        if (shutdownHook != null)
        {
            shutdownHook.remove();
            shutdownHook = null;
        }

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
        
        uploadedStartupScript = false;

        killBackgroundExcelInstances();
    }
    
    private void do_stop()
    {
        try
        {
            // Util.out("Calling OsierLocal shutdown hook");
            stop();
        }
        catch (Exception ex)
        {
            Util.noop();
        }
    }

    private String execute(String script) throws Exception
    {
        if (!definedStartupScript)
        {
            boolean visible = Config.asBoolean("Osier.excel.visible", false);
            setDefaultStartupScript(visible);
        }

        if (!script.endsWith(nl))
            script += nl;
        
        if (!uploadedStartupScript)
            script = startupScript + script;

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
        uploadedStartupScript = true;

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
                    // must see begin within a second at most and certainly within 30 seconds
                    Duration duration = Duration.between(ts0, Instant.now());
                    if (duration.toSeconds() > 30)
                        throw new Exception("Reply from Osier did not start within 30 seconds");
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
        
        throw new Exception("Did not receive a reply from Osier");
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
    
    /* =========================================================================== */
    
    private static void killBackgroundExcelInstances()
    {
        try
        {
            do_killBackgroundExcelInstances();
        }
        catch (Exception ex)
        {
            Util.err("Failed to terminate automated Excel instances");
        }
    }
    
    /*
     * Java ProcessHandle API is broken in JDK 17 (Windows), it returns no parent 
     * or full child information, and no command line, therefore use WMIC instead. 
     */
    private static void do_killBackgroundExcelInstances() throws Exception
    {
        Set<Long> embeddedExcels = new HashSet<>(); 

        String psout = ProcessRunner.gatherOutput("wmic process get commandline,processid");
        for (String line : psout.split("\n"))
        {
            line = Util.despace(line).toLowerCase();
            if (line.contains("\\excel.exe") && line.contains("/automation") && line.contains("-embedding"))
            {
                String[] sa = line.split(" ");
                String pid_str = sa[sa.length - 1];
                embeddedExcels.add(Long.parseUnsignedLong(pid_str));
            }
        }
        
        for (ProcessHandle ph : ProcessHandle.allProcesses().toList())
        {
            if (ph.isAlive() && embeddedExcels.contains(ph.pid()))
            {
                ProcessHandle.Info info = ph.info();

                if (!isCurrentUser(info))
                    continue;
                
                if (!info.command().isPresent())
                    continue;
                
                String cmd = info.command().get().toLowerCase();
                if (!cmd.endsWith("\\excel.exe"))
                    continue;
                
                Util.out(String.format("Killing background Excel with pid %d", ph.pid()));
                ph.destroyForcibly();
            }
        }
    }
    
    private static boolean isCurrentUser(ProcessHandle.Info info)
    {
        ProcessHandle.Info cpinfo = ProcessHandle.current().info();
        return cpinfo.user().isPresent() && info.user().isPresent() && cpinfo.user().get().equals(info.user().get());
    }
}

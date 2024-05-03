package rtss.external.Osier;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import rtss.config.Config;
import rtss.util.Util;

/**
 * Execute Osier commands locally
 */
public class OsierLocal implements OsierCall
{
    private Process process;
    private InputStream is; // connected to process stdout + stderr
    private OutputStream os; // connected to process stdin
    private BufferedReader reader;
    private boolean log = false;
    private StopProcessHook stopHook;

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
        String cmd = String.format("%s", Config.asRequiredString("Osier.executable"));
        cmd = Util.despace(cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd.split(" ")).redirectErrorStream(true);
        process = pb.start();
        stopHook = new StopProcessHook(process);
        stopHook.addHook();
        is = process.getInputStream();
        reader = new BufferedReader(new InputStreamReader(is));
        os = process.getOutputStream();
    }

    @Override
    public void stop() throws Exception
    {
        safeClose(reader);
        reader = null;

        safeClose(is);
        is = null;

        safeClose(os);
        os = null;

        if (stopHook != null)
        {
            stopHook.removeHook();
            stopHook = null;
        }

        if (process != null)
        {
            process.destroy();
            try
            {
                process.waitFor(3, TimeUnit.SECONDS);
            }
            catch (InterruptedException ex)
            {
                // ignore
            }

            process = null;
        }
    }

    private void safeClose(Closeable c)
    {
        try
        {
            if (c != null)
                c.close();
        }
        catch (Throwable ex)
        {
            // ignore
        }
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

        String cmd_begin = String.format("echo %s", Osier.BEGIN_SCRIPT);
        String cmd_end = String.format("echo %", Osier.END_SCRIPT);

        script = cmd_begin + nl + script + nl + cmd_end + nl;

        os.write(script.getBytes(StandardCharsets.UTF_8));
        os.flush();

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

    static class StopProcessHook extends Thread
    {
        private Process process;

        public StopProcessHook(Process process)
        {
            this.process = process;
        }

        @Override
        public void run()
        {
            process.destroy();
        }

        void addHook()
        {
            Runtime.getRuntime().addShutdownHook(this);
        }

        void removeHook()
        {
            try
            {
                Runtime.getRuntime().removeShutdownHook(this);
            }
            catch (IllegalStateException ex)
            {
                // ignore, in shutdown already
            }
        }
    }

    @Override
    public String ping(String tag)
    {
        return tag;
    }
}

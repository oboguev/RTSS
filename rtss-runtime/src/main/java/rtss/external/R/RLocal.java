package rtss.external.R;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import rtss.config.Config;
import rtss.external.ProcessRunner;
import rtss.util.Util;

/**
 * Execute R scripts locally
 */
public class RLocal extends ProcessRunner implements RCall
{
    private boolean log = false;

    public RLocal setLog(boolean log)
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
            Util.err("*** Unable to execute R");
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
        super.start(Config.asRequiredString("R.executable"));
    }

    @Override
    public void stop() throws Exception
    {
        super.stop();
    }

    private String execute(String script) throws Exception
    {
        String nl = "\n";

        if (!script.endsWith(nl))
            script += nl;

        log("");
        log("**** Sending to R for execution at " + Instant.now().toString());
        log("");
        log(script);

        String cmd_begin = String.format("cat(\"%s\\n\")", R.BEGIN_SCRIPT);
        String cmd_end = String.format("cat(\"%s\\n\")", R.END_SCRIPT);

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
                seen_begin = line.equals(R.BEGIN_SCRIPT);
                if (!seen_begin)
                {
                    Duration duration = Duration.between(ts0, Instant.now());
                    if (duration.toSeconds() > 30)
                    {
                        // must see begin within a second at most and certainly within 30 seconds
                        Util.err("No reply from R started within 30 seconds");
                        return null;
                    }
                }
            }
            else if (line.startsWith("> ") || line.startsWith("+ "))
            {
                // ignore echo lines
            }
            else
            {
                log(line);

                if (line.equals(R.END_SCRIPT))
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
}

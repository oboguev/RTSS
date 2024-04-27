package rtss.external.server.R;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import rtss.config.Config;
import rtss.external.R.RLocal;
import rtss.types.T2;

/**
 * Server for R calls.
 * Runs on the same machine where R is hosted.
 */
@RestController
@RequestMapping("/R")
public class RServer
{
    @GetMapping("/ping")
    public String ping(@RequestParam Optional<String> tag)
    {
        if (tag.isPresent())
            return tag.get();
        else
            return "Hello World";
    }
    
    @PostMapping(path = "/execute", consumes=MediaType.APPLICATION_JSON_VALUE)
    public synchronized String execute(@RequestBody T2<String,Boolean> x) throws Exception
    {
        return getLocal().execute(x.a, x.b);
    }
    
    @GetMapping("/stop")
    public void stop() throws Exception
    {
        getLocal().stop();
    }
    
    private static RLocal rlocal;
    
    private static synchronized RLocal getLocal() throws Exception
    {
        if (rlocal == null)
            rlocal = new RLocal().setLog(Config.asBoolean("R.server.log", false));
        return rlocal;
    }
}

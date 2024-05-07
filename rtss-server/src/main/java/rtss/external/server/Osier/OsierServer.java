package rtss.external.server.Osier;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import rtss.config.Config;
import rtss.external.Osier.OsierLocal;
import rtss.types.T2;

/**
 * Server for Osier calls.
 * Runs on the same machine where Osier is hosted.
 */
@RestController
@RequestMapping("/Osier")
public class OsierServer
{
    @GetMapping("/ping")
    public String ping(@RequestParam Optional<String> tag)
    {
        if (tag.isPresent())
            return tag.get();
        else
            return "Hello World";
    }
    
    @PostMapping(path = "/setStartupScript", consumes=MediaType.TEXT_PLAIN_VALUE, produces = "text/plain")
    public synchronized String setStartupScript(@RequestBody String sc) throws Exception
    {
        getLocal().setStartupScript(sc);
        return "OK";
    }

    @PostMapping(path = "/execute", consumes=MediaType.APPLICATION_JSON_VALUE, produces = "text/plain")
    public synchronized String execute(@RequestBody T2<String,Boolean> x) throws Exception
    {
        return getLocal().execute(x.a, x.b);
    }
    
    @GetMapping("/stop")
    public void stop() throws Exception
    {
        getLocal().stop();
    }
    
    private static OsierLocal olocal;
    
    private static synchronized OsierLocal getLocal() throws Exception
    {
        if (olocal == null)
            olocal = new OsierLocal().setLog(Config.asBoolean("Osier.server.log", false));
        return olocal;
    }
}

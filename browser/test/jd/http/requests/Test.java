package jd.http.requests;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;

public class Test {

    public static void main(String[] args) throws IOException {
        Browser br = new Browser();
        Logger logger = Logger.getLogger("test");
        ConsoleHandler ch = new ConsoleHandler();
        logger.addHandler(ch);
        logger.setLevel(Level.ALL);

        ch.setLevel(Level.ALL);
        br.setLogger(logger);
        br.setVerbose(true);
        br.setDebug(true);
        br.setFollowRedirects(true);
        
        br.getHeaders().put("Referer", "http://facebook.com/pages/");
        br.getPage("http://uploadboy.com/");
        // prevent referrer (see directhttp/recaptcha)
        br.setCurrentURL(null);
        br.getPage("http://api.recaptcha.net/challenge?k=" + "6Lcu6f4SAAAAABuG2JGXfAszg3j5uYZFHwIRAr6u");
        // as you can see referrer sent!
    }
}
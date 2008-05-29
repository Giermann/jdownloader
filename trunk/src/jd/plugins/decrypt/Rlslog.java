package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Rlslog extends PluginForDecrypt {
    static private final String host = "Rlslog";
    private String version = "1.0.0.0";
    private static final String HOSTER_LIST = "HOSTER_LIST";
    private String[] hosterList;

    private Pattern patternSupported = getSupportPattern("(http://[*]rlslog.net(/[+]/[+]/#comments|/[+]/#comments))");

    // private Pattern PAT_CAPTCHA = Pattern.compile("<TD><IMG
    // SRC=\"/gfx/secure/");
    // private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\"
    // CLASS=\"BUTTON\" VALUE=\"Zum Download\" onClick=\"if)|(<INPUT
    // TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\"Download\" onClick=\"if)");

    public Rlslog() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        this.setConfigEelements();
        this.hosterList = JDUtilities.splitByNewline(getProperties().getStringProperty(HOSTER_LIST, ""));
    }

    private boolean checkLink(String link) {

        for (String hoster : hosterList) {
            if (hoster.trim().length() > 2 && link.toLowerCase().contains(hoster.toLowerCase().trim())) return true;
        }
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Rlslog Comment Parser";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        logger.info("Rlslog Comment Parser");
        String followcomments = parameter.substring(0, parameter.indexOf("/#comments"));/*
                                                                                         * FIXME
                                                                                         * falls
                                                                                         * url
                                                                                         * schon
                                                                                         * auf
                                                                                         * eine
                                                                                         * weitere
                                                                                         * comment
                                                                                         * page
                                                                                         * zeigt
                                                                                         */
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);
                String[] Links = Plugin.getHttpLinks(reqinfo.getHtmlCode(), null);

                for (int i = 0; i < Links.length; i++) {

                    if (checkLink(Links[i])) {/*
                                                 * links adden, die in der
                                                 * hosterlist stehen
                                                 */
                        decryptedLinks.add(this.createDownloadlink(Links[i]));
                    }

                    if (Links[i].contains(followcomments) == true) {
                        /* weitere comment pages abrufen */
                        URL url2 = new URL(Links[i]);
                        RequestInfo reqinfo2 = getRequest(url2);
                        String[] Links2 = Plugin.getHttpLinks(reqinfo2.getHtmlCode(), null);
                        for (int j = 0; j < Links2.length; j++) {

                            if (checkLink(Links2[j])) {/*
                                                         * links adden, die in
                                                         * der hosterlist stehen
                                                         */
                                decryptedLinks.add(this.createDownloadlink(Links2[j]));
                            }
                        }
                    }

                }
                step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void setConfigEelements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, getProperties(), HOSTER_LIST, JDLocale.L("plugins.decrypt.rlslog.hosterlist", "Liste der zu suchenden Hoster(Ein Hoster/Zeile)")));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
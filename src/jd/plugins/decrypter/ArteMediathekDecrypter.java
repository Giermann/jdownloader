//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "arte.tv", "concert.arte.tv" }, urls = { "http://www\\.arte\\.tv/guide/[a-z]{2}/\\d+\\-\\d+/[A-Za-z0-9\\-_]+", "http://concert\\.arte\\.tv/(de|fr)/[a-z0-9\\-]+" }, flags = { 0, 0 })
public class ArteMediathekDecrypter extends PluginForDecrypt {

    private static final String EXCEPTION_LINKOFFLINE      = "EXCEPTION_LINKOFFLINE";

    private static final String TYPE_CONCERT               = "http://(www\\.)?concert\\.arte\\.tv/(de|fr)/[a-z0-9\\-]+";
    private static final String TYPE_GUIDE                 = "http://((videos|www)\\.)?arte\\.tv/guide/[a-z]{2}/[0-9\\-]+";

    private static final String V_NORMAL                   = "V_NORMAL";
    private static final String V_SUBTITLED                = "V_SUBTITLED";
    private static final String V_SUBTITLE_DISABLED_PEOPLE = "V_SUBTITLE_DISABLED_PEOPLE";
    private static final String V_AUDIO_DESCRIPTION        = "V_AUDIO_DESCRIPTION";
    private static final String http_300                   = "http_300";
    private static final String http_800                   = "http_800";
    private static final String http_1500                  = "http_1500";
    private static final String http_2200                  = "http_2200";
    private static final String LOAD_LANGUAGE_URL          = "LOAD_LANGUAGE_URL";
    private static final String LOAD_LANGUAGE_GERMAN       = "LOAD_LANGUAGE_GERMAN";
    private static final String LOAD_LANGUAGE_FRENCH       = "LOAD_LANGUAGE_FRENCH";
    private static final String THUMBNAIL                  = "THUMBNAIL";

    final String[]              formats                    = { http_300, http_800, http_1500, http_2200 };

    private static boolean      pluginloaded               = false;
    private int                 languageVersion            = 1;
    private String              parameter;

    public ArteMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** TODO: Rewrite this baby, then also use a json parser... */
    /*
     * E.g. smil (rtmp) url:
     * http://www.arte.tv/player/v2/webservices/smil.smil?json_url=http%3A%2F%2Farte.tv%2Fpapi%2Ftvguide%2Fvideos%2Fstream
     * %2Fplayer%2FD%2F045163-000_PLUS7-D%2FALL%2FALL.json&smil_entries=RTMP_SQ_1%2CRTMP_MQ_1%2CRTMP_LQ_1
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        /* Load host plugin to access some static methods later */
        JDUtilities.getPluginForHost("arte.tv");
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString();
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (parameter.matches(TYPE_CONCERT)) {
            if (!br.containsHTML("id=\"section\\-player\"")) {
                decryptedLinks.add(createofflineDownloadLink(parameter));
                return decryptedLinks;
            }
        } else {
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
            }
            int status = br.getHttpConnection().getResponseCode();
            if (br.getHttpConnection().getResponseCode() == 400 || br.containsHTML("<h1>Error 404</h1>") || (!parameter.contains("tv/guide/") && status == 200)) {
                decryptedLinks.add(createofflineDownloadLink(parameter));
                return decryptedLinks;
            }
            /* new arte+7 handling */
            if (status == 301 || status == 302) {
                br.setFollowRedirects(true);
                if (br.getRedirectLocation() != null) {
                    parameter = br.getRedirectLocation();
                    br.getPage(parameter);
                }
            } else if (status != 200) {
                decryptedLinks.add(createofflineDownloadLink(parameter));
                return decryptedLinks;
            }
        }

        try {
            decryptedLinks.addAll(getDownloadLinks(this.br));
        } catch (final Exception e) {
            if (e instanceof DecrypterException && e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                decryptedLinks.add(createofflineDownloadLink(parameter));
                return decryptedLinks;
            }
            throw e;
        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    @SuppressWarnings({ "deprecation", "unchecked", "unused" })
    private ArrayList<DownloadLink> getDownloadLinks(final Browser ibr) throws Exception {
        try {
            ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
            ArrayList<String> selectedFormats = new ArrayList<String>();
            ArrayList<String> selectedLanguages = new ArrayList<String>();
            HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
            String title = null;
            String fid;
            String thumbnailUrl = null;
            final String plain_domain = new Regex(parameter, "([a-z]+\\.arte\\.tv)").getMatch(0);
            final String plain_domain_decrypter = plain_domain.replace("arte.tv", "artejd_decrypted_jd.tv");
            final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
            ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

            // this allows drop to frame, and prevents subsequent NPE!
            br = ibr.cloneBrowser();
            String hybridAPIUrl = null;

            if (parameter.matches(TYPE_CONCERT)) {
                fid = new Regex(parameter, "concert\\.arte\\.tv/(.+)").getMatch(0);
                if (!br.containsHTML("class=\"video\\-container\"")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                fid = br.getRegex("\"http://concert\\.arte\\.tv/[a-z]{2}/player/(\\d+)").getMatch(0);
                hybridAPIUrl = "http://concert.arte.tv/%s/player/%s";
            } else {
                /* Make sure not to download trailers or announcements to movies by grabbing the whole section of the videoplayer! */
                final String video_section = br.getRegex("(<section class=\\'focus\\' data-action=.*?</section>)").getMatch(0);
                if (video_section == null) {
                    return null;
                }
                fid = new Regex(video_section, "/stream/player/[A-Za-z]{1,5}/([^<>\"/]*?)/").getMatch(0);
                if (fid == null) {
                    if (!br.containsHTML("arte_vp_config=")) {
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    return null;
                }
                /*
                 * first "ALL" can e.g. be replaced with "HBBTV" to only get the HBBTV qualities. Also possible:
                 * https://api.arte.tv/api/player/v1/config/fr/051939-015-A?vector=CINEMA
                 */
                hybridAPIUrl = "http://org-www.arte.tv/papi/tvguide/videos/stream/player/%s/%s/ALL/ALL.json";
            }
            if (cfg.getBooleanProperty(LOAD_LANGUAGE_URL, false)) {
                selectedLanguages.add(this.getUrlLang());
            } else {
                if (cfg.getBooleanProperty(LOAD_LANGUAGE_GERMAN, false)) {
                    selectedLanguages.add("de");
                }
                if (cfg.getBooleanProperty(LOAD_LANGUAGE_FRENCH, false)) {
                    selectedLanguages.add("fr");
                }
            }
            for (final String selectedLanguage : selectedLanguages) {
                final String apiurl = this.getAPIUrl(hybridAPIUrl, selectedLanguage, fid);
                br.getPage(apiurl);
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                final LinkedHashMap<String, Object> videoJsonPlayer = (LinkedHashMap<String, Object>) entries.get("videoJsonPlayer");
                title = (String) videoJsonPlayer.get("VTI");
                final String description = (String) videoJsonPlayer.get("VDE");
                final String errormessage = (String) entries.get("msg");
                if (errormessage != null) {
                    final DownloadLink offline = createofflineDownloadLink(parameter);
                    offline.setFinalFileName(title + errormessage);
                    offline.setComment(description);
                    ret.add(offline);
                    return ret;
                }
                if (thumbnailUrl == null) {
                    thumbnailUrl = (String) videoJsonPlayer.get("programImage");
                }
                final String vru = (String) videoJsonPlayer.get("VRU");
                final String vra = (String) videoJsonPlayer.get("VRA");
                if (vra == null || vru == null) {
                    return null;
                }
                /*
                 * In this case the video is not yet released and there usually is a value "VDB" which contains the release-date of the
                 * video --> But we don't need that - right now, such videos are simply offline and will be added as offline.
                 */
                final String expired_message = jd.plugins.hoster.ArteTv.getExpireMessage(selectedLanguage, convertDateFormat(vra), convertDateFormat(vru));
                if (expired_message != null) {
                    final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                    link.setComment(description);
                    link.setProperty("offline", true);
                    link.setFinalFileName(expired_message + "_" + title);
                    newRet.add(link);
                    return newRet;
                }
                final Collection<Object> vsr_quals = ((LinkedHashMap<String, Object>) videoJsonPlayer.get("VSR")).values();
                /* One packagename for every language */
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);

                for (final Object o : vsr_quals) {
                    final LinkedHashMap<String, Object> qualitymap = (LinkedHashMap<String, Object>) o;
                    final int videoBitrate = ((Number) qualitymap.get("bitrate")).intValue();
                    final int width = ((Number) qualitymap.get("width")).intValue();
                    final int height = ((Number) qualitymap.get("height")).intValue();
                    final String versionCode = (String) qualitymap.get("versionCode");
                    final String versionLibelle = (String) qualitymap.get("versionLibelle");
                    final String versionShortLibelle = (String) qualitymap.get("versionShortLibelle");
                    final String url = (String) qualitymap.get("url");

                    final int format_code = getFormatCode(versionShortLibelle, versionCode);
                    final String short_lang_current = get_short_lang_from_format_code(format_code);
                    final String quality_intern = selectedLanguage + "_" + get_intern_format_code_from_format_code(format_code) + "_http_" + videoBitrate;
                    final String linkid = fid + "_" + quality_intern;
                    final String filename = title + "_" + getLongLanguage(selectedLanguage) + "_" + get_user_format_from_format_code(format_code) + "_" + width + "x" + height + "_" + videoBitrate + ".mp4";
                    /* Ignore HLS/RTMP versions */
                    if (!url.startsWith("http") || url.contains(".m3u8")) {
                        logger.info("Skipping " + filename + " because it is not a supported streaming format");
                        continue;
                    }
                    if (!short_lang_current.equals(selectedLanguage)) {
                        logger.info("Skipping " + filename + " because it is not the selected language");
                        continue;
                    }
                    final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));

                    link.setFinalFileName(filename);
                    try {
                        /* JD2 only */
                        link.setContentUrl(parameter);
                    } catch (Throwable e) {
                        /* Stable */
                        link.setBrowserUrl(parameter);
                    }
                    link._setFilePackage(fp);
                    link.setProperty("directURL", url);
                    link.setProperty("directName", filename);
                    link.setProperty("apiurl", apiurl);
                    link.setProperty("VRA", convertDateFormat(vra));
                    link.setProperty("VRU", convertDateFormat(vru));
                    link.setProperty("quality_intern", quality_intern);
                    link.setProperty("langShort", selectedLanguage);
                    link.setProperty("mainlink", parameter);
                    try {
                        if (link.getComment() == null) {
                            link.setComment(description);
                        }
                        link.setContentUrl(parameter);
                        link.setLinkID(linkid);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                        link.setBrowserUrl(parameter);
                        link.setProperty("LINKDUPEID", linkid);
                    }
                    link.setAvailable(true);
                    bestMap.put(quality_intern, link);
                }

                /* Build a list of selected formats */
                for (final String format : formats) {
                    if (cfg.getBooleanProperty(format, false)) {
                        if (cfg.getBooleanProperty(V_NORMAL, false)) {
                            selectedFormats.add(selectedLanguage + "_1_" + format);
                        }
                        /* 1 = German, 2 = French, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
                        if (cfg.getBooleanProperty(V_SUBTITLED, false)) {
                            selectedFormats.add(selectedLanguage + "_3_" + format);
                        }
                        if (cfg.getBooleanProperty(V_SUBTITLE_DISABLED_PEOPLE, false)) {
                            selectedFormats.add(selectedLanguage + "_4_" + format);
                        }
                        if (cfg.getBooleanProperty(V_AUDIO_DESCRIPTION, false)) {
                            selectedFormats.add(selectedLanguage + "_5_" + format);
                        }

                    }
                }
            }

            if (bestMap.isEmpty()) {
                logger.warning("Decrypter broken");
                return null;
            }

            /* Add selected & existing formats */
            for (final String selectedFormat : selectedFormats) {
                final DownloadLink thisformat = bestMap.get(selectedFormat);
                if (thisformat != null) {
                    newRet.add(thisformat);
                }
            }

            if (cfg.getBooleanProperty(THUMBNAIL, false)) {
                final DownloadLink link = createDownloadlink("directhttp://" + thumbnailUrl);
                link.setFinalFileName(title + ".jpg");
                newRet.add(link);
            }
            if (newRet.size() > 1) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(newRet);
            }
            return newRet;
        } catch (final NullPointerException en) {
            en.printStackTrace();
        }
        return null;
    }

    /* Non-subtitled versions, 3 = Subtitled versions, 4 = Subtitled versions for disabled people, 5 = Audio descriptions */
    private int getFormatCode(final String versionShortLibelle, final String versionCode) throws DecrypterException {
        /* versionShortLibelle: What is UTH?? */
        if (versionShortLibelle == null || versionCode == null) {
            throw new DecrypterException("Decrypter broken");
        }
        int lint;
        if (versionCode.equals("VO") && parameter.matches(TYPE_CONCERT)) {
            /* Special case - no different versions available --> We already got the version we want */
            lint = languageVersion;
        } else if ("VOF-STA".equalsIgnoreCase(versionCode) || "VOF-STMF".equals(versionCode) || "VA-STMA".equals(versionCode)) {
            /* Definitly NOT subtitled: VF-STMF */
            lint = 3;
        } else if (versionCode.equals("VOA-STMA")) {
            lint = 4;
        } else if (versionCode.equals("VAAUD")) {
            lint = 5;
        } else if (versionShortLibelle.equals("DE") || versionShortLibelle.equals("VA") || versionCode.equals("VO-STA")) {
            /* German */
            lint = 1;
        } else if (versionShortLibelle.equals("FR") || versionShortLibelle.equals("VF") || versionShortLibelle.equals("VOF") || versionShortLibelle.equals("VOSTF") || versionCode.equals("VO") || versionCode.equals("VF-STMF")) {
            /* French - use same number than for german as the handling has changed. */
            lint = 2;
        } else {
            /* Unknown - use language inside the link */
            /* Unknown language Strings so far: VOA */
            /* This should never happen... */
            lint = languageVersion;
            throw new DecrypterException("Decrypter broken");
        }
        return lint;
    }

    /* 1 = No subtitle, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
    private String get_user_format_from_format_code(final int version) {
        switch (version) {
        case 1:
            /* German */
            return "no_subtitle";
        case 2:
            /* French */
            return "no_subtitle";
        case 3:
            return "subtitled";
        case 4:
            return "subtitled_handicapped";
        case 5:
            return "audio_description";
        default:
            /* Obviously this should never happen */
            return "WTF_PLUGIN_FAILED";
        }
    }

    /**
     * Inout: Normal formatCode Output: formatCode for internal use (1+2 = 1) 1=German, 2 = French, both no_subtitle --> We only need the
     * 'no subtitle' information which has the code 1.
     */
    private int get_intern_format_code_from_format_code(final int formatCode) {
        if (formatCode == 1 || formatCode == 2) {
            return 1;
        } else {
            return formatCode;
        }
    }

    /* 1 = No subtitle, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
    private String get_short_lang_from_format_code(final int version) {
        switch (version) {
        case 1:
            /* German */
            return "de";
        case 2:
            /* French */
            return "fr";
        default:
            /* Obviously this should never happen */
            return "WTF_PLUGIN_FAILED";
        }
    }

    private String getAPIUrl(final String hybridAPIlink, final String lang, final String id) {
        String apilink;
        if (hybridAPIlink.contains("concert.arte.tv")) {
            apilink = String.format(hybridAPIlink, lang, id);
        } else {
            final String api_language = this.artetv_api_language(lang);
            final String id_without_lang = id.substring(0, id.length() - 1);
            final String id_with_lang = id_without_lang + api_language;
            apilink = String.format(hybridAPIlink, api_language, id_with_lang);
        }
        return apilink;
    }

    private String artetv_api_language() {
        return artetv_api_language(getUrlLang());
    }

    private String artetv_api_language(final String lang) {
        String apilang;
        if ("de".equals(lang)) {
            apilang = "D";
        } else {
            apilang = "F";
        }
        return apilang;
    }

    private String getLongLanguage(final String shortLanguage) {
        String long_language;
        if (shortLanguage.equals("de")) {
            long_language = "german";
        } else {
            long_language = "french";
        }
        return long_language;
    }

    private DownloadLink createofflineDownloadLink(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

    private String convertDateFormat(String s) {
        if (s == null) {
            return null;
        }
        if (s.matches("\\d+/\\d+/\\d+ \\d+:\\d+:\\d+ \\+\\d+")) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.getDefault());
            SimpleDateFormat convdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            try {
                Date date = null;
                try {
                    date = df.parse(s);
                    s = convdf.format(date);
                } catch (Throwable e) {
                    df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.ENGLISH);
                    date = df.parse(s);
                    s = convdf.format(date);
                }
            } catch (Throwable e) {
                return s;
            }
        }
        return s;
    }

    @SuppressWarnings("unused")
    private String getURLFilename(final String parameter) {
        String urlfilename;
        if (parameter.matches(TYPE_CONCERT)) {
            urlfilename = new Regex(parameter, "concert\\.arte\\.tv/(de|fr)/(.+)").getMatch(1);
        } else {
            urlfilename = new Regex(parameter, "arte\\.tv/guide/[a-z]{2}/(.+)").getMatch(0);
        }
        return urlfilename;
    }

    private String getUrlLang() {
        final String lang = new Regex(parameter, "(concert\\.arte\\.tv|guide)/(\\w+)/.+").getMatch(1);
        return lang;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
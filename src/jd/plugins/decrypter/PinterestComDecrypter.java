//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.uio.UIOManager;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pinterest.com" }, urls = { "https?://(?:(?:www|[a-z]{2})\\.)?pinterest\\.com/(?!pin/|resource/)[^/]+/[^/]+/" }, flags = { 0 })
public class PinterestComDecrypter extends PluginForDecrypt {

    public PinterestComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     unsupported_urls                    = "https://(?:www\\.)?pinterest\\.com/(business/create/|android\\-app:/.+|ios\\-app:/.+)";
    private static final boolean    force_api_usage                     = true;

    private ArrayList<DownloadLink> decryptedLinks                      = null;
    private String                  parameter                           = null;
    private FilePackage             fp                                  = null;
    private boolean                 enable_description_inside_filenames = jd.plugins.hoster.PinterestCom.defaultENABLE_DESCRIPTION_IN_FILENAMES;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        decryptedLinks = new ArrayList<DownloadLink>();
        enable_description_inside_filenames = SubConfiguration.getConfig("pinterest.com").getBooleanProperty(jd.plugins.hoster.PinterestCom.ENABLE_DESCRIPTION_IN_FILENAMES, enable_description_inside_filenames);
        /* Correct link - remove country related language-subdomains (e.g. 'es.pinterest.com'). */
        final String linkpart = new Regex(param.toString(), "pinterest\\.com/(.+)").getMatch(0);
        parameter = "https://www.pinterest.com/" + linkpart;
        br.setFollowRedirects(true);
        if (parameter.matches(unsupported_urls)) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        /* Sometimes html can be very big */
        br.setLoadLimit(br.getLoadLimit() * 4);
        final boolean loggedIN = getUserLogin(false);
        String fpName = null;
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        String numberof_pins = br.getRegex("class=\"value\">(\\d+(?:\\.\\d+)?)</span> <span class=\"label\">Pins</span>").getMatch(0);
        if (numberof_pins == null) {
            numberof_pins = br.getRegex("class=\'value\'>(\\d+(?:\\.\\d+)?)</span> <span class=\'label\'>Pins</span>").getMatch(0);
        }
        if (numberof_pins == null) {
            numberof_pins = br.getRegex("name=\"pinterestapp:pins\" content=\"(\\d+)\"").getMatch(0);
        }
        fpName = br.getRegex("class=\"boardName\">([^<>]*?)<").getMatch(0);
        if (fpName == null) {
            fpName = linkpart.replace("/", "_");
        }
        if (numberof_pins == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final long lnumberof_pins = Long.parseLong(numberof_pins.replace(".", ""));
        if (lnumberof_pins == 0) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));

        String json_source = br.getRegex("P\\.main\\.start\\((\\{.*?\\})\\);[\t\n\r]+").getMatch(0);
        if (json_source == null) {
            json_source = br.getRegex("P\\.startArgs\\s*=\\s*(\\{.*?\\});[\t\n\r]+").getMatch(0);
        }
        if (json_source == null) {
            json_source = br.getRegex("id=\\'jsInit1\\'>(\\{.*?\\})</script>").getMatch(0);
        }
        if (json_source == null && force_api_usage) {
            // error handling, this has to be always not null!
            return null;
        }

        if (loggedIN || force_api_usage) {
            String nextbookmark = null;

            /* First, get the first 25 pictures from their site. */
            final String board_id = PluginJSonUtils.getJson(json_source, "board_id");
            final String source_url = new Regex(parameter, "pinterest\\.com(/.+)").getMatch(0);
            if (board_id == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            int i = 0;
            do {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }

                // less confusing logic! should only enter after first round! We can use decryptedLinks size!
                if (i > 0) {
                    // no results found on base, we should abort to prevent infinite loop
                    if (decryptedLinks.isEmpty()) {
                        return null;
                    }
                    // page logic
                    final long page = 25;
                    // not required.
                    final String module = ""; // "&module_path=App%3ENags%3EUnauthBanner%3EUnauthHomePage%3ESignupForm%3EUserRegister(wall_class%3DdarkWall%2C+container%3Dinspired_banner%2C+show_personalize_field%3Dfalse%2C+next%3Dnull%2C+force_disable_autofocus%3Dnull%2C+is_login_form%3Dnull%2C+show_business_signup%3Dnull%2C+auto_follow%3Dnull%2C+register%3Dtrue)";
                    String getpage = "/resource/BoardFeedResource/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=%7B%22options%22%3A%7B%22board_id%22%3A%22" + board_id + "%22%2C%22add_pin_rep_with_place%22%3Afalse%2C%22board_url%22%3A%22" + Encoding.urlEncode(source_url) + "%22%2C%22page_size%22%3A" + page + "%2C%22add_vase%22%3Atrue%2C%22access%22%3A%5B%5D%2C%22board_layout%22%3A%22default%22%2C%22bookmarks%22%3A%5B%22" + Encoding.urlEncode(nextbookmark) + "%22%5D%2C%22prepend%22%3Atrue%7D%2C%22context%22%3A%7B%7D%7D" + module + "&_=" + System.currentTimeMillis();
                    // referrer should always be of the first request!
                    final Browser ajax = br.cloneBrowser();
                    prepAPIBR(ajax);
                    ajax.getPage(getpage);
                    json_source = ajax.toString();
                }
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json_source);
                ArrayList<Object> resource_data_list = (ArrayList) entries.get("resource_data_cache");
                if (resource_data_list == null) {
                    /*
                     * Not logged in ? Sometimes needed json is already given in html code! It has minor differences compared to the API.
                     */
                    resource_data_list = (ArrayList) entries.get("resourceDataCache");
                }

                // new website response (tested without login) -raztoki20160405
                final LinkedHashMap<String, Object> test = (LinkedHashMap<String, Object>) entries.get("_dv");
                // every entry of _dv has what we need + its own cover which we wont bother about.
                if (test != null && !test.isEmpty()) {
                    for (final Map.Entry<String, Object> entry : test.entrySet()) {
                        if (Long.parseLong(entry.getKey()) == Long.parseLong(board_id)) {
                            // _did within resource_data_list == ids within _dv
                            continue;
                        }

                        final LinkedHashMap<String, Object> single_pinterest_data = (LinkedHashMap<String, Object>) entry.getValue();
                        proccessLinkedHashMap(single_pinterest_data, board_id, source_url);
                    }
                    nextbookmark = (String) jd.plugins.hoster.DummyScriptEnginePlugin.walkJson(resource_data_list, "{1}/resource/options/bookmarks/{0}");
                    logger.info("Decrypter " + decryptedLinks.size() + " of " + lnumberof_pins + " pins");
                    i++;
                } else {
                    // json
                    /* Find correct list of PINs */
                    for (Object o : resource_data_list) {
                        entries = (LinkedHashMap<String, Object>) o;
                        o = entries.get("data");
                        if (o != null && o instanceof ArrayList) {
                            resource_data_list = (ArrayList) o;
                            break;
                        }
                    }
                    for (final Object pint : resource_data_list) {
                        final LinkedHashMap<String, Object> single_pinterest_data = (LinkedHashMap<String, Object>) pint;
                        proccessLinkedHashMap(single_pinterest_data, board_id, source_url);
                    }
                    nextbookmark = (String) jd.plugins.hoster.DummyScriptEnginePlugin.walkJson(entries, "resource/options/bookmarks/{0}");
                    logger.info("Decrypter " + decryptedLinks.size() + " of " + lnumberof_pins + " pins");
                    i++;
                }
            } while (nextbookmark != null && !nextbookmark.equals("-end-"));
        } else {
            decryptSite();
            if (lnumberof_pins > 25) {
                UIOManager.I().showMessageDialog("Please add your pinterest.com account at Settings->Account manager to find more than 25 images");
            }
        }

        return decryptedLinks;
    }

    private void proccessLinkedHashMap(LinkedHashMap<String, Object> single_pinterest_data, final String board_id, final String source_url) throws DecrypterException {
        final String type = (String) single_pinterest_data.get("type");
        if (type == null || !type.equals("pin")) {
            /* Skip invalid objects! */
            return;
        }
        final LinkedHashMap<String, Object> single_pinterest_pinner = (LinkedHashMap<String, Object>) single_pinterest_data.get("pinner");
        final LinkedHashMap<String, Object> single_pinterest_images = (LinkedHashMap<String, Object>) single_pinterest_data.get("images");
        final LinkedHashMap<String, Object> single_pinterest_images_original = (LinkedHashMap<String, Object>) single_pinterest_images.get("orig");

        final String pin_directlink = (String) single_pinterest_images_original.get("url");
        final String pin_id = (String) single_pinterest_data.get("id");
        final String description = (String) single_pinterest_data.get("description");
        final String username = (String) single_pinterest_pinner.get("username");
        final String pinner_name = (String) single_pinterest_pinner.get("full_name");
        if (pin_id == null || pin_directlink == null || pinner_name == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        String filename = pin_id;
        final String content_url = "http://www.pinterest.com/pin/" + pin_id + "/";
        final DownloadLink dl = createDownloadlink(content_url);

        if (description != null) {
            dl.setComment(description);
            dl.setProperty("description", description);
            if (enable_description_inside_filenames) {
                filename += "_" + description;
            }
        }
        filename = encodeUnicode(filename);

        dl.setContentUrl(content_url);
        dl.setLinkID("pinterest://" + pin_id);
        dl.setProperty("free_directlink", pin_directlink);
        dl.setProperty("boardid", board_id);
        dl.setProperty("source_url", source_url);
        dl.setProperty("username", username);
        dl.setProperty("decryptedfilename", filename);
        dl.setName(filename + ".jpg");
        dl.setAvailable(true);
        dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        fp.add(dl);
        decryptedLinks.add(dl);
        distribute(dl);
    }

    private void decryptSite() {
        /*
         * Also possible using json of P.start.start( to get the first 25 entries: resourceDataCache --> Last[] --> data --> Here we go --->
         * But I consider this as an unsafe method.
         */
        final String[] linkinfo = br.getRegex("<div class=\"bulkEditPinWrapper\">(.*?)class=\"creditTitle\"").getColumn(0);
        if (linkinfo == null || linkinfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            decryptedLinks = null;
            return;
        }
        for (final String sinfo : linkinfo) {
            String description = new Regex(sinfo, "title=\"([^<>\"]*?)\"").getMatch(0);
            if (description == null) {
                description = new Regex(sinfo, "<p class=\"pinDescription\">([^<>]*?)<").getMatch(0);
            }
            final String directlink = new Regex(sinfo, "\"(https?://[a-z0-9\\.\\-]+/originals/[^<>\"]*?)\"").getMatch(0);
            final String pin_id = new Regex(sinfo, "/pin/(\\d+)/").getMatch(0);
            if (pin_id == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                decryptedLinks = null;
                return;
            }
            String filename = pin_id;
            final String content_url = "http://www.pinterest.com/pin/" + pin_id + "/";
            final DownloadLink dl = createDownloadlink(content_url);
            dl.setContentUrl(content_url);
            dl.setLinkID("pinterest://" + pin_id);
            dl._setFilePackage(fp);
            if (directlink != null) {
                dl.setProperty("free_directlink", directlink);
            }

            if (description != null) {
                dl.setComment(description);
                dl.setProperty("description", description);
                if (enable_description_inside_filenames) {
                    filename += "_" + description;
                }
            }
            filename = encodeUnicode(filename);

            dl.setProperty("decryptedfilename", filename);
            dl.setName(filename + ".jpg");
            dl.setAvailable(true);
            dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            decryptedLinks.add(dl);
            distribute(dl);
        }
    }

    private DownloadLink getOffline(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings({ "deprecation" })
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("pinterest.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            jd.plugins.hoster.PinterestCom.login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    private void prepAPIBR(final Browser br) throws PluginException {
        jd.plugins.hoster.PinterestCom.prepAPIBR(br);
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

}

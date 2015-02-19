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

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "parelliconnect.com" }, urls = { "http://www\\.parelliconnect\\.com/resources" }, flags = { 0 })
public class ParelliSavvyClubComDecrypter extends PluginForDecrypt {

    public ParelliSavvyClubComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String fast_linkcheck = "fast_linkcheck";
    private boolean             fastlinkcheck  = false;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // final String parameter = param.toString();
        fastlinkcheck = SubConfiguration.getConfig("parelliconnect.com").getBooleanProperty(fast_linkcheck, false);
        br.setFollowRedirects(true);
        /* Some profiles can only be accessed if they accepted others as followers --> Log in if the user has added his twitter account */
        if (getUserLogin(false)) {
            logger.info("Account available and we're logged in");
        } else {
            logger.info("No account available or login failed --> Returning nothing because account is needed");
            return decryptedLinks;
        }
        /* There is only one link we can decrypt. */
        br.getPage("http://www.parelliconnect.com/ajax/resources/vault_media_details");
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("result");
        for (final Object o : ressourcelist) {
            final LinkedHashMap<String, Object> ressource = (LinkedHashMap<String, Object>) o;
            final LinkedHashMap<String, Object> media_item = (LinkedHashMap<String, Object>) ressource.get("media_item");

            final String package_media_type = (String) media_item.get("media_type");
            final String package_title = (String) media_item.get("title");
            final String package_releasedate = (String) media_item.get("releasedate");
            final String package_description = (String) media_item.get("description");
            final ArrayList<Object> media_videos = (ArrayList) media_item.get("media_videos");
            final ArrayList<Object> media_pdfs = (ArrayList) media_item.get("media_videos");
            /* Maybe we got a single video */
            if (media_videos == null && media_pdfs == null) {
                final boolean package_active = ((Boolean) media_item.get("active")).booleanValue();
                if (!package_active) {
                    logger.warning("WTF");
                }
                final String package_media_id = Integer.toString(((Number) media_item.get("id")).intValue());
                final DownloadLink dl = makeDownloadlink(package_media_id, package_media_type, package_title, package_releasedate, null);
                decryptedLinks.add(dl);
            } else {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(package_title + "_" + package_releasedate);
                fp.setComment(package_description);
                if (media_videos != null) {
                    for (final Object media_video : media_videos) {
                        final LinkedHashMap<String, Object> single_video = (LinkedHashMap<String, Object>) media_video;
                        final boolean active = ((Boolean) single_video.get("active")).booleanValue();
                        if (!active) {
                            logger.warning("WTF");
                        }
                        final String title = (String) single_video.get("title");
                        final String releasedate = (String) single_video.get("releasedate");
                        final String media_id = Integer.toString(((Number) single_video.get("id")).intValue());
                        final DownloadLink dl = makeDownloadlink(media_id, "mp4", title, releasedate, fp);
                        decryptedLinks.add(dl);
                    }
                }

                if (media_pdfs != null) {
                    for (final Object media_pdf : media_pdfs) {
                        final LinkedHashMap<String, Object> single_pdf = (LinkedHashMap<String, Object>) media_pdf;
                        final boolean active = ((Boolean) single_pdf.get("active")).booleanValue();
                        if (!active) {
                            logger.warning("WTF");
                        }
                        final String title = (String) single_pdf.get("title");
                        final String releasedate = (String) single_pdf.get("releasedate");
                        final String media_id = Integer.toString(((Number) single_pdf.get("id")).intValue());
                        final DownloadLink dl = makeDownloadlink(media_id, "pdf", title, releasedate, fp);
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        return decryptedLinks;
    }

    private DownloadLink makeDownloadlink(final String content_id, final String type, final String title, final String releasedate, final FilePackage fp) {
        String ext;
        final DownloadLink dl;
        if (type.equals("pdf")) {
            dl = createDownloadlink("http://www.parelliconnect.com/ajax/resources/" + content_id + "/vault_display_pdf");
            ext = ".pdf";
        } else {
            dl = createDownloadlink("http://www.parelliconnect.com/ajax/resources/" + content_id + "/vault_display_video");
            ext = ".mp4";
        }
        if (fastlinkcheck) {
            dl.setAvailable(true);
        }
        dl.setProperty("decryptedtitle", title);
        dl.setProperty("decryptedreleasedate", releasedate);
        dl.setName(title + "_" + releasedate + ext);
        dl._setFilePackage(fp);
        return dl;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings({ "deprecation", "static-access" })
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("parelliconnect.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.ParelliSavvyClubCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

}

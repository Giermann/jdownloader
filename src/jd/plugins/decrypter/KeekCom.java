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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "keek.com" }, urls = { "https?://(?:www\\.)?keek\\.com/profile/[^/]+" }, flags = { 0 })
public class KeekCom extends PluginForDecrypt {

    public KeekCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String profilename = parameter.substring(parameter.lastIndexOf("/") + 1);
        final String fpName = profilename;

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));

        final String numberof_ids_s = this.br.getRegex("<li>([0-9\\.,]+) Keeks</li>").getMatch(0);
        final long numberof_ids = Long.parseLong(numberof_ids_s.replace(",", "").replace(".", ""));
        final int max_ids_per_page = 200;
        final int max_ids_per_page_first_page = 48;
        int page = 1;
        String loadmore_id = null;

        if (numberof_ids == 0) {
            logger.info("User has zero content!");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        do {
            if (this.isAbort()) {
                logger.info("User aborted decryption");
                break;
            }
            if (page == 1) {
                loadmore_id = this.br.getRegex("data-load-more-keek-id=\"([^<>\"]*?)\"").getMatch(0);
            } else {
                this.br.getPage("/profile/" + profilename + "/next?filter=keeks&page=" + page + "&keekId=" + Encoding.urlEncode(loadmore_id) + "&maxId=&size=500&instart_disable_injection=true");
                this.br.getRequest().setHtmlCode(this.br.toString().replace("\\", ""));
                loadmore_id = getJson("keekId");
            }
            String[] ids = br.getRegex("data-active=\"false\" data\\-keek\\-id=\"([^<>\"]*?)\"").getColumn(0);
            if (ids == null || ids.length == 0) {
                ids = br.getRegex("data\\-keek\\-id=\"([^<>\"]*?)\"").getColumn(0);
            }
            if (ids == null || ids.length == 0) {
                /* Either plugin broken or we got everything */
                break;
            }
            for (final String singleID : ids) {
                final String url_content = "https://www.keek.com/keek/" + singleID;
                final DownloadLink dl = createDownloadlink(url_content);
                dl._setFilePackage(fp);
                dl.setLinkID(singleID);
                dl.setContentUrl(url_content);
                dl.setName(singleID);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            if (page == 1 && ids.length < max_ids_per_page_first_page) {
                /* Fail safe */
                break;
            } else if (page > 1 && ids.length < max_ids_per_page) {
                /* Fail safe */
                break;
            }
            page++;
        } while (decryptedLinks.size() < numberof_ids && loadmore_id != null);

        if (decryptedLinks.size() == 0 && !this.isAbort()) {
            return null;
        }

        return decryptedLinks;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }
}

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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freedisc.pl" }, urls = { "http://(www\\.)?freedisc\\.pl/[A-Za-z0-9\\-_]+,d\\-\\d+([A-Za-z0-9\\-_,]+)?" }, flags = { 0 })
public class FreeDiscPlFolder extends PluginForDecrypt {

    public FreeDiscPlFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_FOLDER = "http://(www\\.)?freedisc\\.pl/[A-Za-z0-9\\-_]+,d\\-\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String username = new Regex(parameter, "freedisc\\.pl/([A-Za-z0-9\\-_]+),d\\-\\d+").getMatch(0);
        final String fpName = br.getRegex("<title>([^<>\"]*?)\\-  Freedisc\\.pl</title>").getMatch(0);
        // style='float: left; overflow: auto;'><a href="
        final String[] links = br.getRegex("class=\\'name\\'><a href=\"(/[^<>\"]*?,f\\-[^<>\"]*?)\"").getColumn(0);
        final String[] folders = br.getRegex("class=\\'name\\'><a href=\"([A-Za-z0-9\\-_]+,d\\-\\d+[^<>\"]*?)\"").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0) && br.containsHTML("class=\"directoryText previousDirLinkFS\"")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (links != null && links.length > 0) {
            for (String singleLink : links) {
                if (!singleLink.startsWith("/")) {
                    singleLink = "/" + singleLink;
                }
                singleLink = "http://freedisc.pl" + singleLink;
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        if (folders != null && folders.length > 0) {
            for (final String singleLink : folders) {
                decryptedLinks.add(createDownloadlink("http://freedisc.pl" + singleLink));
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

//EmbedDecrypter 0.2.8
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "boobinspector.com" }, urls = { "http://(www\\.)?boobinspector\\.com/videos/\\d+" }, flags = { 0 })
public class BoobinspectorComDecrypter extends PluginForDecrypt {

    public BoobinspectorComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String externID = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        decryptedLinks = jd.plugins.decrypter.PornEmbedParser.findEmbedUrls(this.br, filename);
        if (decryptedLinks != null && decryptedLinks.size() > 0) {
            return decryptedLinks;
        }
        decryptedLinks = new ArrayList<DownloadLink>();
        /* No embed url found --> Probably video is selfhosted */
        final DownloadLink main = createDownloadlink(parameter.replace("boobinspector.com/", "boobinspectordecrypted.com/"));
        decryptedLinks.add(main);
        return decryptedLinks;
    }

}
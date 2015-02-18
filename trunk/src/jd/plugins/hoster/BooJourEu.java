//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.Random;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "boojour.eu" }, urls = { "http://(www\\.)?boojour\\.eu/\\?v=[A-Z0-9]+" }, flags = { 0 })
public class BooJourEu extends PluginForHost {

    public BooJourEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.boojour.eu/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Removed for Copyright Infringement")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String varA = br.getRegex("name=\"([^<>\"]*?)\" id=\"a\"").getMatch(0);
        final String varB = br.getRegex("name=\"([^<>\"]*?)\" id=\"b\"").getMatch(0);
        final String[][] otherStuff = br.getRegex("<input type=\"hidden\" value=\"(\\d+)\" class=\"textbox\" name=\"(\\d+)\"").getMatches();
        if (varA == null || varB == null || otherStuff.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String postData = varA + "=" + varB + "&" + otherStuff[1][1] + "=" + otherStuff[1][0] + "&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100);
        br.postPage(downloadLink.getDownloadURL(), postData);
        String dllink = br.getRegex("url: \\'(http://[a-z0-9\\-]+\\.boojour\\.eu/[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://\\d+\\.boojour\\.eu/v/\\d+/[a-z0-9]+\\.flv)\\'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
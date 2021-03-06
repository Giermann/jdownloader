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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "workupload.com" }, urls = { "http://(?:www\\.|en\\.)?workupload\\.com/file/[A-Za-z0-9]+" }, flags = { 0 })
public class WorkuploadCom extends PluginForHost {

    public WorkuploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://workupload.com/tos";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;

    private String               fid               = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        link.setLinkID(fid);
        br.getPage("https://workupload.com/file/" + fid);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("img/404\\.jpg\"|>Whoops\\! 404")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<td>Dateiname:</td><td>([^<>\"]*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"intro\">[\n\t\r ]*?<b>([^<>\"]+)</b>").getMatch(0);
        }
        String filesize = br.getRegex("<td>Dateigröße:</td><td>([^<>\"]*?)<").getMatch(0);
        if (filename == null || filesize == null) {
            Regex filenameSize = br.getRegex("<p class=\"intro\">[\n\t\r ]*?<b>(.*?)</b>[^\n\t\r <>\"]*?(\\d+(?:\\.\\d+)? ?(KB|MB|GB))[^\n\t\r <>\"]*?");
            if (filename == null) {
                filename = filenameSize.getMatch(0);
            }
            if (filesize == null) {
                filesize = filenameSize.getMatch(1);
            }
        }
        if (filesize == null) {
            filesize = br.getRegex("(\\d+(?:\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("(\\d+(?:\\.\\d+)? ?(?:B(?:ytes?)?))").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String first_url = this.br.getURL();
        // String dllink = checkDirectLink(downloadLink, directlinkproperty);
        String dllink = null;
        if (dllink == null) {
            this.br.getPage("/start/" + fid);
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            this.br.getPage("/api/file/getDownloadServer/" + fid);
            dllink = PluginJSonUtils.getJson(this.br, "url");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        }
        this.br.getHeaders().put("Referer", first_url);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
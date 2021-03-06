//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.File;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;

/**
 * @author raztoki
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "M3u8" }, urls = { "m3u8s?://.+?\\.m3u8" }, flags = { 0 })
public class GenericM3u8 extends PluginForHost {

    private String customFavIconHost = null;

    public GenericM3u8(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getHost(DownloadLink link, Account account) {
        if (link != null) {
            return Browser.getHost(link.getDownloadURL());
        }
        return super.getHost(link, account);
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) throws Exception {
        String url = "http" + link.getDownloadURL().substring(4);
        link.setUrlDownload(url);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        checkFFProbe(downloadLink, "Download a HLS Stream");
        if (downloadLink.getBooleanProperty("encrypted")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Encrypted HLS is not supported");
        }

        br = new Browser();

        this.setBrowserExclusive();

        // first get
        br.getPage(downloadLink.getDownloadURL());

        HLSDownloader downloader = new HLSDownloader(downloadLink, br, downloadLink.getDownloadURL());
        StreamInfo streamInfo = downloader.getProbe();
        if (streamInfo == null) {
            return AvailableStatus.FALSE;
        }
        String videoq = null;
        String audioq = null;
        String extension = "m4a";

        for (Stream s : streamInfo.getStreams()) {
            if ("video".equalsIgnoreCase(s.getCodec_type())) {

                extension = "mp4";
                if (s.getHeight() > 0) {
                    videoq = s.getHeight() + "p";
                }
            } else if ("audio".equalsIgnoreCase(s.getCodec_type())) {
                if (s.getBit_rate() != null) {
                    if (s.getCodec_name() != null) {
                        audioq = s.getCodec_name() + " " + (Integer.parseInt(s.getBit_rate()) / 1024) + "kbits";
                    } else {
                        audioq = (Integer.parseInt(s.getBit_rate()) / 1024) + "kbits";
                    }
                } else {
                    if (s.getCodec_name() != null) {
                        audioq = s.getCodec_name();
                    }

                }
            }
        }

        String name = new File(downloadLink.getDownloadURL()).getName();

        name = name.substring(0, name.length() - 5);
        if (videoq != null && audioq != null) {
            name += " (" + videoq + " " + audioq + ")";
        } else if (videoq != null) {
            name += " (" + videoq + ")";
        } else if (audioq != null) {
            name += " (" + audioq + ")";
        }

        name += "." + extension;
        downloadLink.setName(name);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        if (downloadLink.getBooleanProperty("encrypted")) {

            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Encrypted HLS is not supported");
        }
        // requestFileInformation(downloadLink);
        String master = downloadLink.getDownloadURL();
        dl = new HLSDownloader(downloadLink, br, master);
        dl.startDownload();

    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }
}
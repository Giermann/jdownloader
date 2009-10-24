//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-rapid.cz" }, urls = { "http://[\\w\\.]*?(share-rapid\\.(com|info|cz|eu|info|net|sk)|((mediatack|rapidspool|e-stahuj|premium-rapidshare|qiuck|rapidshare-premium|share-credit|share-free)\\.cz)|((strelci|share-ms|)\\.net)|jirkasekyrka\\.com|((kadzet|universal-share)\\.com)|sharerapid\\.(biz|cz|net|org|sk)|stahuj-zdarma\\.eu)/stahuj/[0-9]+/.+" }, flags = { 2 })
public class ShareRapidCz extends PluginForHost {

    public ShareRapidCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://share-rapid.com/dobiti/?zeme=1");
    }

    @Override
    public String getAGBLink() {
        return "http://share-rapid.com/informace/";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        // Complete list of all domains, maybe they buy more....
        // http://share-rapid.com/informace/
        String downloadlinklink = link.getDownloadURL();
        if (downloadlinklink != null) downloadlinklink = downloadlinklink.replaceAll("(share-rapid\\.(com|info|cz|eu|info|net|sk)|((mediatack|rapidspool|e-stahuj|premium-rapidshare|qiuck|rapidshare-premium|share-credit|share-free)\\.cz)|((strelci|share-ms|)\\.net)|jirkasekyrka\\.com|((kadzet|universal-share)\\.com)|sharerapid\\.(biz|cz|net|org|sk)|stahuj-zdarma\\.eu)", "share-rapid\\.com");
        link.setUrlDownload(downloadlinklink);
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        br.getPage("http://share-rapid.com/prihlaseni/");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.put("login", Encoding.urlEncode(account.getUser()));
        form.put("pass1", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        if (!br.containsHTML("Kredit:</td>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String trafficleft = br.getMatch("Kredit:</td><td>(.*?)<a");
        ai.setTrafficLeft(Regex.getSize(trafficleft));
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Již Vám došel kredit a vyčerpal jste free limit")) throw new PluginException(LinkStatus.ERROR_FATAL, "Not enough traffic left to download this file!");
        String dllink = br.getRegex("\"(http://s[0-9]{1,2}\\.share-rapid\\.com/download.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        br.setFollowRedirects(true);
        if (br.containsHTML("Nastala chyba 404")||br.containsHTML("Soubor byl smazán")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = Encoding.htmlDecode(br.getRegex("<title>(.*?) - Share-Rapid</title>").getMatch(0));
        String filesize = Encoding.htmlDecode(br.getRegex("Velikost:</td>.*?<td class=\"h\"><strong>.*?(.*?)</strong></td>").getMatch(0));
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("class=\"important\" href=\"(.*?)\">Click to download! <").getMatch(0);
        if (dllink == null && br.containsHTML("Stahování je přístupné pouze přihlášeným uživatelům")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users"); }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT, "Please contact the support jdownloader.org");
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
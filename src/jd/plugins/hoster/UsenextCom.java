package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 3, names = { "usenext.com" }, urls = { "" }, flags = { 0 })
public class UsenextCom extends UseNet {

    public UsenextCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.usenext.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://www.usenext.com/terms";
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface UsenextConfigInterface extends UsenetConfigInterface {

    };

    @Override
    public Class<UsenextConfigInterface> getConfigInterface() {
        return UsenextConfigInterface.class;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        try {
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                br.getPage("https://www.usenext.com/UsenextDE/MemberAreaInt/misc/tutorial/tuIndex.cfm?sLangToken=ENG");
                final String accountStatus = br.getRegex("Account status:.*?<span class=\".*?\">(.*?)</span>").getMatch(0);
                if (!StringUtils.equalsIgnoreCase(accountStatus, "OK")) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "SNUUID") == null) {
                account.clearCookies("");
                br.getPage("https://www.usenext.com/");
                final Form login = new Form();
                login.setAction("/Account/LogInAjax");
                login.setMethod(MethodType.POST);
                login.put("Username", Encoding.urlEncode(account.getUser()));
                login.put("Password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                final String url = br.getRegex("\"url\"\\s*:\\s*\"(https?.*?)\"").getMatch(0);
                if (url != null) {
                    final String url2 = Encoding.unescapeYoutube(url);
                    br.getPage(url2);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie(getHost(), "SNUUID") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }

            br.getPage("/UseNeXTDE/MemberAreaInt/obj/user/usEdit.cfm?sLangToken=ENG");
            account.saveCookies(br.getCookies(getHost()), "");
            final String userName = br.getRegex("Username</label>.*?value=\"(avi-.*?)\"").getMatch(0);
            if (userName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(USENET_USERNAME, userName.trim());
            }
            final String accountStatus = br.getRegex("Account status:.*?<span class=\".*?\">(.*?)</span>").getMatch(0);
            if (!StringUtils.equalsIgnoreCase(accountStatus, "OK")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account Status: " + accountStatus, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String downloadVolume = br.getRegex("Download Volume:.*?<span>(.*?)</").getMatch(0);
            if (downloadVolume == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                final String trafficLeft = downloadVolume.replaceAll("\r\n", "").replaceAll("\t+", " ").trim();
                ai.setTrafficLeft(trafficLeft);
            }
            br.getPage("/UseNeXTDE/MemberAreaInt/obj/user/uscontract.cfm?sLangToken=ENG");
            final String validUntil = br.getRegex("Subscription through:</td>.*?<td>(\\d+/\\d+/\\d+)</").getMatch(0);
            final String bucketType = br.getRegex("My UseNeXT plan:</td>.*?<td>(.*?)</").getMatch(0);
            if (bucketType != null) {
                ai.setStatus(bucketType);
            } else {
                ai.setStatus("Unknown UseNeXT plan");
            }
            if (validUntil != null) {
                final long date = TimeFormatter.getMilliSeconds(validUntil, "MM/dd/yyyy", null);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                }
            }
            // TODO: check this
            account.setMaxSimultanDownloads(30);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("news.usenext.de", true, 563));
        return ret;
    }

}

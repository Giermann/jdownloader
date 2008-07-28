package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class FileUploadnet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "file-upload.net";

    private static final String PLUGIN_NAME = HOST;

    private static final String PLUGIN_VERSION = "1.0.0.0";

    private static final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final Pattern PAT_Member = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_Download = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_View = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html)", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_SUPPORTED = Pattern.compile(PAT_Download.pattern() + "|" + PAT_View.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String downloadurl;

    public FileUploadnet() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
                /* LinkCheck für DownloadFiles */
                downloadurl = downloadLink.getDownloadURL();
                requestInfo = HTTP.getRequest(new URL(downloadurl));
                if (!requestInfo.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = requestInfo.getRegexp("<h1>Download \"(.*?)\"</h1>").getFirstMatch();
                    String filesize;
                    if ((filesize = requestInfo.getRegexp("e:</b></td><td>(.*?)Kbyte<td>").getFirstMatch()) != null) {
                        downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return true;
                }
            } else if (new Regex(downloadLink.getDownloadURL(), PAT_View).matches()) {
                /* LinkCheck für DownloadFiles */
                downloadurl = downloadLink.getDownloadURL();
                requestInfo = HTTP.getRequest(new URL(downloadurl));
                if (!requestInfo.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = requestInfo.getRegexp("<h1>Bildeigenschaften von \"(.*?)\"</h1>").getFirstMatch();
                    String filesize;
                    if ((filesize = requestInfo.getRegexp("e:</b>(.*?)Kbyte").getFirstMatch()) != null) {
                        downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return true;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    public void handle( DownloadLink downloadLink) {
        if (step == null) return null;
        try {
            /* Nochmals das File überprüfen */
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }

            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
                /* DownloadFiles */
                downloadurl = requestInfo.getRegexp("action=\"(.*?)\" method=\"post\"").getFirstMatch();
                Form form = requestInfo.getForms()[0];
                form.withHtmlCode = false;
                requestInfo = form.getRequestInfo(false);
            } else if (new Regex(downloadLink.getDownloadURL(), PAT_View).matches()) {
                /* DownloadFiles */
                downloadurl = requestInfo.getRegexp("<center>\n<a href=\"(.*?)\" rel=\"lightbox\"").getFirstMatch();
                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), null, downloadLink.getDownloadURL(), false);
            } else {
                downloadLink.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }

            /* Datei herunterladen */
            HTTPConnection urlConnection = requestInfo.getConnection();
            String filename = getFileNameFormHeader(urlConnection);
            if (urlConnection.getContentLength() == 0) {
                downloadLink.setStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            downloadLink.setDownloadMax(urlConnection.getContentLength());
            downloadLink.setName(filename);
            long length = downloadLink.getDownloadMax();
            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setFilesize(length);
            dl.setChunkNum(1);
            dl.setResume(false);
            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                downloadLink.setStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            return step;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
        return step;
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.file-upload.net/to-agb.html";
    }
}

//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.http.requests.FormData;
import jd.http.requests.GetRequest;
import jd.http.requests.HeadRequest;
import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;
import jd.http.requests.RequestVariable;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.PublicSuffixList;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyAuthException;

public class Browser {
    // we need this class in here due to jdownloader stable 0.9 compatibility
    public class BrowserException extends IOException {

        private static final long serialVersionUID = 1509988898224037320L;
        private final Request     request;

        public BrowserException(final String message, final Request request) {
            this(message, request, null);
        }

        public BrowserException(IOException exception, final Request request) {
            this(null, request, exception);
        }

        public BrowserException(final String message, final Request request, final Exception e) {
            super(message, e);
            this.request = request;
        }

        public Request getRequest() {
            return this.request;
        }

    }

    private static final HashMap<String, Cookies> COOKIES            = new HashMap<String, Cookies>();
    private static ProxySelectorInterface         GLOBAL_PROXY       = null;
    private static Logger                         LOGGER             = null;

    private static HashMap<String, Integer>       REQUEST_INTERVAL_LIMIT_MAP;

    private static HashMap<String, Long>          REQUESTTIME_MAP;

    private static int                            TIMEOUT_CONNECT    = 30000;

    private static int                            TIMEOUT_READ       = 30000;

    private Boolean                               defaultSSLTrustALL = null;

    public static ProxySelectorInterface _getGlobalProxy() {
        return Browser.GLOBAL_PROXY;
    }

    private static String removeURLPathTraversal(String path) {
        if (path == null || !path.contains("./")) {
            return path;
        }
        final boolean endsWithSlash = path.endsWith("/");
        final String pathParts[] = path.split("/");
        for (int i = 0; i < pathParts.length; i++) {
            if (".".equals(pathParts[i])) {
                pathParts[i] = null;
            } else if ("..".equals(pathParts[i])) {
                if (i > 0) {
                    int j = i - 1;
                    while (true && j > 0) {
                        if (pathParts[j] != null) {
                            pathParts[j] = null;
                            break;
                        }
                        j--;
                    }
                }
                pathParts[i] = null;
            } else if (i > 0 && pathParts[i].length() == 0) {
                pathParts[i] = "/";
            }
        }
        StringBuilder ret = new StringBuilder();
        for (final String pathPart : pathParts) {
            if (pathPart != null) {
                if ("/".equals(pathPart) || pathPart.length() == 0) {
                    ret.append("/");
                } else {
                    if (ret.length() > 0 && ret.charAt(ret.length() - 1) != '/') {
                        ret.append("/");
                    }
                    ret.append(pathPart);
                }
            }
        }
        if (endsWithSlash && (ret.length() == 0 || ret.charAt(ret.length() - 1) != '/')) {
            ret.append("/");
        }
        return ret.toString();
    }

    private static String removeURLPathMultipleSlash(String path, boolean removeDoubleSlash) {
        if (path == null || !path.contains("//")) {
            return path;
        }
        if (removeDoubleSlash) {
            /**
             * http: // webmasters.stackexchange.com/questions/8354/what-does-the-double-slash-mean-in-urls
             */
            path = path.replaceAll("/{2,}", "/");
        } else {
            path = path.replaceAll("/{3,}", "/");
        }
        return path;
    }

    /**
     * Returns a corrected url, where multiple / and ../. are removed
     * 
     * @param url
     * @return
     */
    public static String correctURL(String url, final boolean removeDoubleSlash) {
        if (url == null) {
            return url;
        }
        /** full URL */
        String[] urlParts = new Regex(url, "^(?i-)([a-z]+)://([^/\\?#]+)([^\\?&]+)?(\\?.*)?$").getRow(0);
        String[] locationParts = null;
        String protPart = null;
        String hostPart = null;
        String pathPart = null;
        String queryPart = null;
        if (urlParts != null) {
            protPart = urlParts[0];
            hostPart = urlParts[1];
            pathPart = urlParts[2];
            queryPart = urlParts[3];
        } else {
            /** full/relative locations */
            locationParts = new Regex(url, "^([^\\?&]+)?(\\?.+)?$").getRow(0);
            if (locationParts != null) {
                pathPart = locationParts[0];
                queryPart = locationParts[1];
            }
        }

        if (StringUtils.isNotEmpty(pathPart) && (pathPart.contains("//") || pathPart.contains("./"))) {
            pathPart = Browser.removeURLPathMultipleSlash(pathPart, removeDoubleSlash);
            pathPart = Browser.removeURLPathTraversal(pathPart);
        }

        if (urlParts != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(protPart);
            sb.append("://");
            sb.append(hostPart);
            if (StringUtils.isEmpty(pathPart)) {
                sb.append("/");
            } else {
                if (!pathPart.startsWith("/")) {
                    sb.append("/");
                }
                sb.append(pathPart);
            }
            if (!StringUtils.isEmpty(queryPart)) {
                sb.append(queryPart);
            }
            return sb.toString();
        } else if (locationParts != null) {
            StringBuilder sb = new StringBuilder();
            if (!StringUtils.isEmpty(pathPart)) {
                sb.append(pathPart);
            }
            if (!StringUtils.isEmpty(queryPart)) {
                sb.append(queryPart);
            }
            return sb.toString();
        }
        System.out.println("WARNING: correctURL failed for " + url);
        return url;
    }

    public static String getBasicAuthfromURL(final String url) {
        if (url == null) {
            return null;
        }
        final String basicauth = new Regex(url, "(ftp|https?)://(.+)@.*?($|/)").getMatch(1);
        if (basicauth != null && basicauth.contains(":")) {
            return Encoding.Base64Encode(basicauth);
        }
        return null;
    }

    public static int getGlobalReadTimeout() {
        return Browser.TIMEOUT_READ;
    }

    public static String getHost(final String url) {
        return Browser.getHost(url, false);
    }

    private static final Pattern HOST_IP_PATTERN1    = Pattern.compile("^(?:[a-z0-9]{2,64}://)?[a-z0-9]{2,64}://(?:[^\\s@]+?@)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HOST_IP_PATTERN2    = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HOST_DOMAIN_PATTERN = Pattern.compile("^(?:[a-z0-9]{2,64}://)?[a-z0-9]{2,64}://(?:[^\\s@]+?@)?([a-z0-9\\-\\.]+?)(/|$|:\\d+$|:\\d+/)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /*
     * this method extracts domain/ip from given url. optional keeps existing subdomains
     */
    public static String getHost(final String url, final boolean includeSubDomains) {
        if (url == null) {
            return null;
        }
        final String trimURL = url.trim();
        /* direct ip with protocol */
        String ret = new Regex(trimURL, Browser.HOST_IP_PATTERN1).getMatch(0);
        if (ret == null) {
            /* direct ip without protocol */
            ret = new Regex(trimURL, Browser.HOST_IP_PATTERN2).getMatch(0);
        }
        if (ret != null) {
            return ret;
        }
        /* normal url */
        ret = new Regex(trimURL, Browser.HOST_DOMAIN_PATTERN).getMatch(0);
        if (ret != null && includeSubDomains == false) {
            /* cut off all subdomains */
            final PublicSuffixList psl = PublicSuffixList.getInstance();
            if (psl != null) {
                final String domain = psl.getDomain(ret.toLowerCase(Locale.ENGLISH));
                if (domain != null) {
                    return domain;
                }
            }
            int indexPoint = ret.lastIndexOf(".");
            indexPoint = ret.lastIndexOf(".", indexPoint - 1);
            if (indexPoint >= 0) {
                /* we enter this branch only if a subdomain exists */
                ret = ret.substring(indexPoint + 1);
            }
        }
        if (ret != null) {
            return ret.toLowerCase(Locale.ENGLISH);
        }
        return url;
    }

    /**
     * 
     * 
     * @param url
     * @return
     * @throws MalformedURLException
     */

    public static String getHost(final URL url) {
        return Browser.getHost(url.getHost());
    }

    /**
     * Sets the global connect timeout
     * 
     * @param valueMS
     */
    public static void setGlobalConnectTimeout(final int valueMS) {
        Browser.TIMEOUT_CONNECT = valueMS;
    }

    public static void setGlobalLogger(final Logger logger) {
        Browser.LOGGER = logger;
    }

    public static void setGlobalProxy(final ProxySelectorInterface p) {
        Browser.GLOBAL_PROXY = p;
    }

    /**
     * Sets the global readtimeout in ms
     * 
     * @param valueMS
     */
    public static void setGlobalReadTimeout(final int valueMS) {
        Browser.TIMEOUT_READ = valueMS;
    }

    public static void setGlobalVerbose(final boolean b) {
        Browser.VERBOSE = b;
    }

    private boolean        keepResponseContentBytes = false;

    private int[]          allowedResponseCodes     = new int[0];

    private static boolean VERBOSE                  = false;

    public static String correctURL(final String url) {
        return Browser.correctURL(url, false);
    }

    /**
     * Lädt über eine URLConnection eine Datei herunter. Zieldatei ist file.
     * 
     * @param file
     * @param con
     * @return Erfolg true/false
     * @throws IOException
     */
    public static void download(final File file, final URLConnectionAdapter con) throws IOException {
        if (file.isFile()) {
            if (file.exists() && !file.delete()) {
                throw new IOException("Could not overwrite file: " + file);
            }
        }
        final File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        file.createNewFile();
        FileOutputStream fos = null;
        InputStream input = null;
        boolean okay = false;
        try {
            fos = new FileOutputStream(file, false);
            input = con.getInputStream();
            final long length;
            if (con.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING) == null) {
                length = con.getCompleteContentLength();
            } else {
                length = -1;
            }
            final byte[] b = new byte[32767];
            long done = 0;
            int len;
            while ((len = input.read(b)) != -1) {
                fos.write(b, 0, len);
                done += len;
            }
            okay = length < 0 || length == done;
        } finally {
            try {
                input.close();
            } catch (final Throwable e) {
            }
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            if (okay == false) {
                file.delete();
            }
        }
    }

    public static int getGlobalConnectTimeout() {
        return Browser.TIMEOUT_CONNECT;
    }

    public static Logger getGlobalLogger() {
        return Browser.LOGGER;
    }

    public static synchronized void setRequestIntervalLimitGlobal(final String host, final int i) {
        final String domain = Browser.getHost(host);
        if (domain == null) {
            return;
        }
        if (Browser.REQUEST_INTERVAL_LIMIT_MAP == null) {
            Browser.REQUEST_INTERVAL_LIMIT_MAP = new HashMap<String, Integer>();
            Browser.REQUESTTIME_MAP = new HashMap<String, Long>();
        }
        Browser.REQUEST_INTERVAL_LIMIT_MAP.put(domain, i);
    }

    private static HashMap<String, ArrayList<Long>> REQUESTS_THRESHOLD_TIME_HISTORY_MAP;
    private static HashMap<String, Long>            REQUESTS_THRESHOLD_INTERVAL_MAP;
    private static HashMap<String, Integer>         REQUESTS_THRESHOLD_REQUESTS_MAP;

    /**
     * sets request thresholds based on upper burstable limit. eg. 20(x) requests over 60000(y)[=1min]. Then on after it sets limit between
     * interval.
     * 
     * @author raztoki
     * @since JD2
     * @param host
     * @param i
     *            (ms)
     * @param x
     *            (requests)
     * @param y
     *            (ms)
     * @throws Exception
     */
    public static synchronized void setRequestIntervalLimitGlobal(final String host, final int i, final int x, final long y) throws Exception {
        if (x <= 0 || y <= 0 || i <= 0) {
            throw new Exception("'x' and 'y' and 'i' have to be above zero!");
            // return;
        }
        final String domain = Browser.getHost(host);
        if (domain == null) {
            throw new Exception("Browser.getHost(host) returned null");
            // return;
        }
        // Utilise existing method
        Browser.setRequestIntervalLimitGlobal(domain, i);
        // here we go.
        if (Browser.REQUESTS_THRESHOLD_INTERVAL_MAP == null) {
            Browser.REQUESTS_THRESHOLD_INTERVAL_MAP = new HashMap<String, Long>();
            Browser.REQUESTS_THRESHOLD_REQUESTS_MAP = new HashMap<String, Integer>();
            Browser.REQUESTS_THRESHOLD_TIME_HISTORY_MAP = new HashMap<String, ArrayList<Long>>();
        }
        Browser.REQUESTS_THRESHOLD_REQUESTS_MAP.put(domain, x);
        Browser.REQUESTS_THRESHOLD_INTERVAL_MAP.put(domain, y);
    }

    private static synchronized void waitForPageAccess(final Browser browser, final Request request) throws InterruptedException {
        final String host = Browser.getHost(request.getUrl());
        ArrayList<Long> ts = null;
        if (Browser.REQUESTS_THRESHOLD_INTERVAL_MAP != null && Browser.REQUESTS_THRESHOLD_INTERVAL_MAP.containsKey(host)) {
            ts = Browser.REQUESTS_THRESHOLD_TIME_HISTORY_MAP.get(host);
            if (ts == null) {
                ts = new ArrayList<Long>();
            }
        }
        try {
            if (ts != null) {
                final long maxInterval = Browser.REQUESTS_THRESHOLD_INTERVAL_MAP.get(host);
                final int arrayMaxSize = Browser.REQUESTS_THRESHOLD_REQUESTS_MAP.get(host);
                final ArrayList<Long> currentStill = new ArrayList<Long>();
                for (final Long t : ts) {
                    long time = System.currentTimeMillis() - t;
                    if (time < maxInterval) {
                        currentStill.add(t);
                    }
                }
                ts = currentStill;
                if (ts.size() < arrayMaxSize) {
                    return;
                }
                // since ArrayList preserves order, oldest entry should always be 0
                final long requestsThresholdOldestTimestamp = ts.get(0);
                final int globalLimit = Browser.REQUEST_INTERVAL_LIMIT_MAP.get(host);
                final long globalLastRequest = Browser.REQUESTTIME_MAP.get(host) == null ? System.currentTimeMillis() : Browser.REQUESTTIME_MAP.get(host);
                // effectively you could have to wait x minutes before slot opens up, all depends on how wide the y interval is. Once the
                // burst map is full we then use specified average wait(i) based upon the last request
                // current time - oldest timestamp should give us the elapsed time.
                long wait1 = System.currentTimeMillis() - requestsThresholdOldestTimestamp;
                // now determine how long until it expires against maxInterval
                wait1 = maxInterval - wait1;
                long wait2 = globalLimit - (System.currentTimeMillis() - globalLastRequest);
                if (wait1 < 0) {
                    wait1 = 0;
                }
                if (wait2 < 0) {
                    wait2 = 0;
                }
                if (ts.size() < arrayMaxSize && (wait1 > maxInterval || wait2 > wait1)) {
                    // only enter if we are equal to or less than arrayMaxSize.
                    Thread.sleep(wait1);
                } else {
                    Thread.sleep(wait2);
                }
                return;
            }
            Integer localLimit = null;
            Integer globalLimit = null;
            Long localLastRequest = null;
            Long globalLastRequest = null;

            if (browser.requestIntervalLimitMap != null) {
                localLimit = browser.requestIntervalLimitMap.get(host);
                localLastRequest = browser.requestTimeMap.get(host);
            }
            if (Browser.REQUEST_INTERVAL_LIMIT_MAP != null) {
                globalLimit = Browser.REQUEST_INTERVAL_LIMIT_MAP.get(host);
                globalLastRequest = Browser.REQUESTTIME_MAP.get(host);
            }
            if (localLimit == null && globalLimit == null) {
                return;
            }
            if (localLastRequest == null && globalLastRequest == null) {
                return;
            }
            if (localLimit != null && localLastRequest == null) {
                return;
            }
            if (globalLimit != null && globalLastRequest == null) {
                return;
            }

            if (globalLimit == null) {
                globalLimit = 0;
            }
            if (localLimit == null) {
                localLimit = 0;
            }
            if (localLastRequest == null) {
                localLastRequest = System.currentTimeMillis();
            }
            if (globalLastRequest == null) {
                globalLastRequest = System.currentTimeMillis();
            }
            final long dif = Math.max(localLimit - (System.currentTimeMillis() - localLastRequest), globalLimit - (System.currentTimeMillis() - globalLastRequest));

            if (dif > 0) {
                // System.out.println("Sleep " + dif + " before connect to " +
                // request.getUrl().getHost());
                Thread.sleep(dif);
                // waitForPageAccess(request);
            }
        } finally {
            if (browser.requestTimeMap != null) {
                browser.requestTimeMap.put(host, System.currentTimeMillis());
            }
            if (Browser.REQUESTTIME_MAP != null) {
                Browser.REQUESTTIME_MAP.put(host, System.currentTimeMillis());
            }
            if (ts != null) {
                ts.add(System.currentTimeMillis());
                Browser.REQUESTS_THRESHOLD_TIME_HISTORY_MAP.put(host, ts);
            }
        }
    }

    private String                           acceptLanguage   = "de, en-gb;q=0.9, en;q=0.8";

    /*
     * -1 means use default Timeouts
     * 
     * 0 means infinite (DO NOT USE if not needed)
     */
    private int                              connectTimeout   = -1;

    private HashMap<String, Cookies>         cookies          = new HashMap<String, Cookies>();

    private boolean                          cookiesExclusive = true;

    private String                           currentURL       = null;

    private String                           customCharset    = null;
    private boolean                          debug            = false;
    private boolean                          doRedirects      = false;
    private RequestHeader                    headers;
    private int                              limit            = 2 * 1024 * 1024;
    private Logger                           logger           = null;
    private ProxySelectorInterface           proxy;
    private int                              readTimeout      = -1;
    private Request                          request;
    private HashMap<String, Integer>         requestIntervalLimitMap;

    private HashMap<String, Long>            requestTimeMap;
    private HashMap<String, ArrayList<Long>> requestTimeStampMap;

    private boolean                          verbose          = false;

    public Browser() {
        final Thread currentThread = Thread.currentThread();
        /**
         * use BrowserSettings from current thread if available
         */
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            this.proxy = settings.getProxySelector();
            this.debug = settings.isDebug();
            this.verbose = settings.isVerbose();
            this.logger = settings.getLogger();
        }
    }

    /**
     * Assures that the browser does not download any binary files in textmode
     * 
     * @param request
     * @throws BrowserException
     */
    private void checkContentLengthLimit(final Request request) throws BrowserException {
        if (request != null && request.getHttpConnection() != null) {
            long length = -1;
            final int limit = this.getLoadLimit();
            request.setReadLimit(limit);
            length = request.getHttpConnection().getLongContentLength();
            if (length >= 0 && length > limit) {
                request.disconnect();
                throw new BrowserException("Content-length too big:" + length + ">" + limit, request);
            }
        }
    }

    /**
     * Clears all cookies for the given URL. URL has to be a valid. if (url == null), all cookies are cleared.
     * 
     * @param url
     */
    public void clearCookies(final String url) {
        if (url == null) {
            this.cookies.clear();
        }
        final String host = Browser.getHost(url);
        final Iterator<String> it = this.getCookies().keySet().iterator();
        String check = null;
        while (it.hasNext()) {
            check = it.next();
            if (check == null) {
                this.cookies.remove(null);
            } else if (check.contains(host)) {
                this.cookies.get(check).clear();
                break;
            }
        }
    }

    public Browser cloneBrowser() {
        final Browser br = new Browser();
        br.requestIntervalLimitMap = this.requestIntervalLimitMap;
        br.requestTimeMap = this.requestTimeMap;
        br.acceptLanguage = this.acceptLanguage;
        br.connectTimeout = this.connectTimeout;
        br.currentURL = this.currentURL;
        br.doRedirects = this.doRedirects;
        br.defaultSSLTrustALL = this.defaultSSLTrustALL;
        br.setCustomCharset(this.customCharset);
        br.getHeaders().putAll(this.getHeaders());
        br.limit = this.limit;
        br.readTimeout = this.readTimeout;
        br.request = this.getRequest();
        br.cookies = this.cookies;
        br.cookiesExclusive = this.cookiesExclusive;
        br.debug = this.debug;
        br.verbose = this.verbose;
        br.logger = this.logger;
        br.proxy = this.proxy;
        br.keepResponseContentBytes = this.keepResponseContentBytes;
        br.allowedResponseCodes = this.allowedResponseCodes;
        return br;
    }

    public boolean containsHTML(final String regex) {
        return new Regex(this, regex).matches();
    }

    /**
     * Creates a new Request object based on a form
     * 
     * @param form
     * @return
     * @throws Exception
     */
    public Request createFormRequest(final Form form) throws Exception {
        String base = null;
        String action = null;
        final Request lRequest = this.getRequest();
        if (lRequest != null) {
            /* take current url as base url */
            base = lRequest.getUrl();
        }
        try {
            final String sourceBase = this.getRegex("<base.*?href=\"(.+?)\"").getMatch(0);
            if (sourceBase != null) {
                /* take baseURL in case we've found one in current request */
                new URL(sourceBase.trim());
                base = sourceBase;
            }
        } catch (final Throwable e) {
        }
        action = form.getAction(base);
        if (action == null) {
            throw new NullPointerException("no valid action url");
        }
        // action = action;
        switch (form.getMethod()) {
        case GET:
            final String varString = form.getPropertyString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+")) {
                    action += "&";
                } else if (action.matches("[^\\?]*")) {
                    action += "?";
                }
                action += varString;
            }
            return this.createGetRequest(action);
        case POST:
            if (form.getEncoding() == null || !form.getEncoding().toLowerCase().endsWith("form-data")) {
                return this.createPostRequest(action, form.getRequestVariables(), form.getEncoding());
            } else {
                final PostFormDataRequest request = (PostFormDataRequest) this.createPostFormDataRequest(action);
                if (form.getEncoding() != null) {
                    request.setEncodeType(form.getEncoding());
                }
                final int size = form.getInputFields().size();
                for (int i = 0; i < size; i++) {
                    final InputField entry = form.getInputFields().get(i);
                    if (form.getPreferredSubmit() != null && entry.getType() != null && entry.getType().equalsIgnoreCase("submit") && form.getPreferredSubmit() != entry) {
                        continue;
                    }
                    if (entry.getValue() == null) {
                        // continue;
                    } else if (entry.getType() != null && entry.getType().equalsIgnoreCase("image")) {
                        request.addFormData(new FormData(entry.getKey() + ".x", entry.getProperty("x", (int) (Math.random() * 100) + "")));
                        request.addFormData(new FormData(entry.getKey() + ".y", entry.getProperty("y", (int) (Math.random() * 100) + "")));
                    } else if (entry.getType() != null && entry.getType().equalsIgnoreCase("file")) {
                        request.addFormData(new FormData(entry.getKey(), entry.getFileToPost().getName(), entry.getFileToPost()));
                    } else if (entry.getKey() != null && entry.getValue() != null) {
                        request.addFormData(new FormData(entry.getKey(), entry.getValue()));
                    }
                }
                return request;
            }
        }
        return null;

    }

    public Request createGetRequest(String url) throws IOException {
        return new GetRequest(this.getURL(url));
    }

    public Request createHeadRequest(String url) throws IOException {
        return new HeadRequest(this.getURL(url));
    }

    /* this is buggy as we must set correct referer! */
    @Deprecated
    public Request createGetRequestRedirectedRequest(final Request oldRequest) throws IOException {
        return this.createRedirectFollowingRequest(oldRequest);
    }

    public Request createPostFormDataRequest(String url) throws IOException {
        return new PostFormDataRequest(this.getURL(url));
    }

    /**
     * Creates a new postrequest based an an requestVariable ArrayList
     */
    private Request createPostRequest(String url, final List<RequestVariable> post, final String encoding) throws IOException {
        final PostRequest request = new PostRequest(this.getURL(url));
        if (post != null) {
            request.addAll(post);
        }
        String requestContentType = encoding;
        final RequestHeader lHeaders = this.headers;
        if (lHeaders != null) {
            final String browserContentType = lHeaders.remove(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE);
            if (requestContentType == null) {
                requestContentType = browserContentType;
            }
        }
        if (requestContentType == null) {
            requestContentType = "application/x-www-form-urlencoded";
        }
        request.setContentType(requestContentType);
        return request;
    }

    /**
     * Creates a new POstrequest based on a variable HashMap
     */
    public Request createPostRequest(final String url, final LinkedHashMap<String, String> post) throws IOException {
        return this.createPostRequest(url, PostRequest.variableMaptoArray(post), null);
    }

    /**
     * Creates a postrequest based on a querystring
     */
    public Request createPostRequest(final String url, final String post) throws MalformedURLException, IOException {
        return this.createPostRequest(url, Request.parseQuery(post));
    }

    /**
     * rfc2616
     * 
     * @param request
     * @return
     * @throws BrowserException
     */
    public Request createRedirectFollowingRequest(final Request request) throws BrowserException {
        if (request == null) {
            throw new IllegalArgumentException("Request is null");
        }
        String location = request.getLocation();
        if (StringUtils.isEmpty(location)) {
            throw new IllegalStateException("Request does not contain a redirect");
        }
        location = this.getURL(location);
        final int responseCode = request.getHttpConnection().getResponseCode();
        Request newRequest = null;
        switch (responseCode) {
        case 200:
        case 201:
            if (request instanceof HeadRequest) {
                newRequest = request.cloneRequest();
            } else {
                newRequest = new GetRequest(request);
            }
            break;
        case 301:
            if (request instanceof HeadRequest || request instanceof GetRequest) {
                newRequest = request.cloneRequest();
            } else {
                /* it seems getRequest is expected although rfc says that post can be kept */
                newRequest = new GetRequest(request);
            }
            break;
        case 302:
        case 303:
            if (request instanceof HeadRequest) {
                newRequest = request.cloneRequest();
            } else {
                newRequest = new GetRequest(request);
            }
            break;
        case 307:
        case 308:
            newRequest = request.cloneRequest();
            break;
        default:
            Logger logger = this.getLogger();
            if (logger != null && this.isVerbose()) {
                LogSource.exception(logger, new IllegalStateException("ResponseCode " + responseCode + " is unsupported!"));
            }
            return null;
        }
        newRequest.setURL(location);
        return newRequest;
    }

    public Request createRequest(final Form form) throws Exception {
        return this.createFormRequest(form);
    }

    public Request createRequest(final String downloadURL) throws Exception {
        return this.createGetRequest(downloadURL);
    }

    public void disconnect() {
        try {
            this.getRequest().disconnect();
        } catch (final Throwable ignore) {
        }
    }

    /**
     * Downloads the contents behind con to file. if(con ==null), the latest request is downloaded. Useful for redirects
     * 
     * @param file
     * @param con
     * @throws IOException
     */
    public void downloadConnection(final File file, URLConnectionAdapter con) throws IOException {
        if (con == null) {
            con = this.getRequest().getHttpConnection();
        }
        Browser.download(file, con);
    }

    public String followConnection() throws IOException {
        final Logger llogger = this.getLogger();
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            throw new IllegalStateException("Request is null");
        }
        if (lRequest.getHtmlCode() != null) {
            if (llogger != null) {
                llogger.warning("Request has already been read");
            }
            return lRequest.getHTMLSource();
        }
        return this.loadConnection(lRequest.getHttpConnection()).getHTMLSource();
    }

    /**
     * Zeigt debuginformationen auch im Hauptprogramm an
     * 
     * @param b
     */
    public void forceDebug(final boolean b) {
        this.debug = b;
    }

    public void forwardCookies(final Request request) {
        if (request != null) {
            final String host = Browser.getHost(request.getUrl());
            final Cookies cookies = this.getCookies().get(host);
            if (cookies != null) {
                for (final Cookie cookie : cookies.getCookies()) {
                    // Pfade sollten verarbeitet werden...TODO
                    if (cookie.isExpired()) {
                        continue;
                    }
                    request.getCookies().add(cookie);
                }
            }
        }
    }

    public String getAcceptLanguage() {
        return this.acceptLanguage;
    }

    /**
     * @return the allowedResponseCodes
     */
    public int[] getAllowedResponseCodes() {
        return this.allowedResponseCodes;
    }

    public String getBaseURL() {
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            return null;
        }
        final String url = lRequest.getUrl();
        final String base = new Regex(url, "(https?://.+)/").getMatch(0);
        if (base != null) {
            return base + "/";
        }
        throw new WTFException("no baseURL for " + url);
    }

    /**
     * returns current ConnectTimeout
     * 
     * @return
     */
    public int getConnectTimeout() {
        return this.connectTimeout < 0 ? Browser.TIMEOUT_CONNECT : this.connectTimeout;
    }

    public String getCookie(final String url, final String key) {
        final String host = Browser.getHost(url);
        final Cookies cookies = this.getCookies(host);
        final Cookie cookie = cookies.get(key);
        return cookie != null ? cookie.getValue() : null;
    }

    private HashMap<String, Cookies> getCookies() {
        return this.cookiesExclusive ? this.cookies : Browser.COOKIES;
    }

    public Cookies getCookies(final String url) {
        final String host = Browser.getHost(url);
        Cookies cookies2 = this.getCookies().get(host);
        if (cookies2 == null) {
            this.getCookies().put(host, cookies2 = new Cookies());
        }
        return cookies2;
    }

    public void getDownload(final File file, final String urlString) throws IOException {
        URLConnectionAdapter con = null;
        try {
            con = this.openGetConnection(URLDecoder.decode(urlString, "UTF-8"));
            Browser.download(file, con);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public Form getForm(final int i) {
        final Form[] forms = this.getForms();
        return forms.length <= i ? null : forms[i];
    }

    /**
     * Returns the first form that has an input filed with name key
     * 
     * @param key
     * @return
     */
    public Form getFormbyKey(final String key) {
        for (final Form f : this.getForms()) {
            if (f.hasInputFieldByName(key)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Returns the first form that has input field with 'key' that equals 'value'.
     * 
     * @since JD2
     * @param key
     * @param value
     * @return
     */
    public Form getFormByInputFieldKeyValue(final String key, final String value) {
        for (final Form f : this.getForms()) {
            for (final InputField field : f.getInputFields()) {
                if (key != null && key.equals(field.getKey())) {
                    if (value == null && field.getValue() == null) {
                        return f;
                    }
                    if (value != null && value.equals(field.getValue())) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public Form getFormbyProperty(final String property, final String name) {
        for (final Form form : this.getForms()) {
            if (form.getStringProperty(property) != null && form.getStringProperty(property).equalsIgnoreCase(name)) {
                return form;
            }
        }
        return null;
    }

    /**
     * Returns the first form with an Submitvalue of name<br />
     * Note: String needs to be urlEncoded as values it's comparing against are!
     * 
     * 
     * @param name
     * @return
     */
    public Form getFormBySubmitvalue(final String name) {
        for (final Form form : this.getForms()) {
            try {
                form.setPreferredSubmit(name);
                return form;
            } catch (final IllegalArgumentException e) {
            }
        }
        return null;
    }

    public Form[] getForms() {
        return Form.getForms(this);
    }

    public Form[] getForms(final String downloadURL) throws IOException {
        this.getPage(downloadURL);
        return this.getForms();
    }

    public RequestHeader getHeaders() {
        if (this.headers == null) {
            this.headers = new RequestHeader();
        }
        return this.headers;
    }

    public String getHost() {
        final Request lRequest = this.getRequest();
        return lRequest == null ? null : Browser.getHost(lRequest.getUrl(), false);
    }

    public URLConnectionAdapter getHttpConnection() {
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            return null;
        }
        return lRequest.getHttpConnection();
    }

    /**
     * Gets Browser upper page load limit Byte value.
     * 
     * @since JD2
     * @param i
     */
    public int getLoadLimit() {
        return this.limit;
    }

    public Logger getLogger() {
        final Logger llogger = this.logger;
        if (llogger != null) {
            return llogger;
        }
        return Browser.LOGGER;
    }

    public String getMatch(final String string) {
        return this.getRegex(string).getMatch(0);
    }

    public String getPage(final String string) throws IOException {
        return this.loadConnection(this.openRequestConnection(this.createGetRequest(string))).getHTMLSource();
    }

    public String getPage(final URL url) throws IOException {
        return this.getPage(url.toString());
    }

    public ProxySelectorInterface getProxy() {
        return this.proxy;
    }

    /**
     * returns current ReadTimeout
     * 
     * @return
     */
    public int getReadTimeout() {
        return this.readTimeout < 0 ? Browser.TIMEOUT_READ : this.readTimeout;
    }

    /**
     * If automatic redirectfollowing is disabled, you can get the redirect URL if there is any.
     * 
     * @return
     */
    public String getRedirectLocation() {
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            return null;
        }
        return lRequest.getLocation();
    }

    public Regex getRegex(final Pattern compile) {
        return new Regex(this, compile);
    }

    public Regex getRegex(final String string) {
        return new Regex(this, string);
    }

    /**
     * Gets the latest request
     * 
     * @return
     */
    public Request getRequest() {
        return this.request;
    }

    public ProxySelectorInterface getThreadProxy() {
        final Thread currentThread = Thread.currentThread();
        /**
         * return BrowserSettings from current thread if available
         */
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            return settings.getProxySelector();
        }
        return null;
    }

    public String getURL() {
        final Request lRequest = this.getRequest();
        return lRequest == null ? null : lRequest.getUrl();
    }

    /**
     * Tries to get a full URL out of string
     * 
     * @throws BrowserException
     */
    public String getURL(String string) {
        if (string == null) {
            string = this.getRedirectLocation();
        }
        if (string == null) {
            throw new NullPointerException("url is null");
        }
        try {
            /* this checks if string contains a full/correct URL */
            new URL(string);
        } catch (final Exception e) {
            final Request lRequest = this.getRequest();
            if (lRequest == null || lRequest.getHttpConnection() == null) {
                return string;
            }
            final String base = this.getBaseURL();
            if (string.startsWith("/") || string.startsWith("\\") || string.startsWith("?")) {
                try {
                    final String currentURL = this.getURL();
                    if (string.startsWith("?") && currentURL != null) {
                        /* TODO: this needs to be rechecked!! */
                        // '?' requests are amendments from current browser URL, base shouldn't be determined by browser html or the code
                        // below.
                        string = currentURL + string;
                    } else {
                        final URL bUrl = new URL(base);
                        String proto = "http://";
                        if (base.startsWith("https")) {
                            proto = "https://";
                        }
                        String portUse = "";
                        if (bUrl.getDefaultPort() > 0 && bUrl.getPort() > 0 && bUrl.getDefaultPort() != bUrl.getPort()) {
                            portUse = ":" + bUrl.getPort();
                        }
                        string = proto + bUrl.getHost() + portUse + string;
                    }
                } catch (final MalformedURLException e1) {
                    e1.printStackTrace();
                }
            } else {
                string = base + string;
            }
        }
        string = Browser.correctURL(Encoding.urlEncode_light(string));
        return string;
    }

    public boolean isCookiesExclusive() {
        return this.cookiesExclusive;
    }

    public boolean isDebug() {
        return this.debug || this.isVerbose();
    }

    public boolean isFollowingRedirects() {
        return this.doRedirects;
    }

    public boolean isKeepResponseContentBytes() {
        return this.keepResponseContentBytes;
    }

    public boolean isVerbose() {
        return Browser.VERBOSE || this.verbose;
    }

    /**
     * Reads the content behind a con and returns them. Note: if con==null, the current request is read. This is useful for redirects. Note
     * #2: if a connection is loaded, data is not stored in the browser instance.
     * 
     * @param con
     * @return
     * @throws IOException
     */
    public Request loadConnection(final URLConnectionAdapter connection) throws IOException {
        final Request requ;
        if (connection == null) {
            requ = this.getRequest();
        } else if (connection.getRequest() != null) {
            requ = connection.getRequest();
        } else {
            requ = new Request(connection) {

                @Override
                public long postRequest() throws IOException {
                    return 0;
                }

                @Override
                public void preRequest() throws IOException {
                }
            };
        }
        try {
            this.checkContentLengthLimit(requ);
            requ.read(this.isKeepResponseContentBytes());
            if (this.isVerbose() && requ != null) {
                final Logger llogger = this.getLogger();
                if (llogger != null) {
                    llogger.finest("\r\n" + requ.getHTMLSource() + "\r\n");
                }
            }
            return requ;
        } catch (final BrowserException e) {
            throw e;
        } catch (final IOException e) {
            throw new BrowserException(e, requ);
        } finally {
            if (requ != null) {
                requ.disconnect();
            }
        }
    }

    private void mergeHeaders(final Request request) {
        final RequestHeader lHeaders = this.headers;
        if (request != null && lHeaders != null) {
            if (lHeaders.isDominant()) {
                request.getHeaders().clear();
            }
            final RequestHeader requestHeaders = request.getHeaders();
            for (final HTTPHeader header : lHeaders) {
                requestHeaders.put(header);
            }
        }
    }

    /**
     * Opens a new connection based on a Form
     * 
     * @param form
     * @return
     * @throws Exception
     */
    public URLConnectionAdapter openFormConnection(final Form form) throws Exception {
        return this.openRequestConnection(this.createFormRequest(form));
    }

    public URLConnectionAdapter openFormConnection(final int i) throws Exception {
        return this.openFormConnection(this.getForm(i));
    }

    /**
     * Opens a new get connection
     * 
     * @param string
     * @return
     * @throws IOException
     */
    public URLConnectionAdapter openGetConnection(final String string) throws IOException {
        return this.openRequestConnection(this.createGetRequest(string));
    }

    /**
     * @since JD2
     **/
    public URLConnectionAdapter openHeadConnection(final String string) throws IOException {
        return this.openRequestConnection(this.createHeadRequest(string));
    }

    /**
     * Opens a Post COnnection based on a variable HashMap
     */
    public URLConnectionAdapter openPostConnection(final String url, final LinkedHashMap<String, String> post) throws IOException {
        return this.openRequestConnection(this.createPostRequest(url, post));
    }

    /**
     * OPens a new POst connection based on a query string
     */
    public URLConnectionAdapter openPostConnection(final String url, final String post) throws IOException {
        return this.openPostConnection(url, Request.parseQuery(post));
    }

    private void setRequestProperties(Request request) {
        if (request != null) {
            this.forwardCookies(request);
            request.setCustomCharset(this.customCharset);
            request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT_LANGUAGE, this.getAcceptLanguage());
            request.setConnectTimeout(this.getConnectTimeout());
            request.setReadTimeout(this.getReadTimeout());
            if (this.currentURL != null && this.getRedirectLocation() == null) {
                // without this.getRedirectLocation() it messes with download core setting originalUrl code!
                // @see jd.plugins.BrowserAdapter.openDownload(Browser br, Downloadable downloadable, Request request, boolean resume, int
                // chunks)
                // if (originalUrl != null) {
                // request.getHeaders().put("Referer", originalUrl);
                // }
                request.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, this.currentURL);
            }
            this.mergeHeaders(request);
        }
    }

    public URLConnectionAdapter openRequestConnection(final Request request) throws IOException {
        return this.openRequestConnection(request, this.doRedirects);
    }

    public URLConnectionAdapter openRequestConnection(Request request, final boolean followRedirects) throws IOException {
        int redirectLoopPrevention = 0;
        final Request originalRequest = request;
        while (true) {
            this.setRequestProperties(request);
            int proxyRetryCounter = 0;
            while (true) {
                try {
                    // connect may throw ProxyAuthException for https or direct connection method requests
                    try {
                        Browser.waitForPageAccess(this, request);
                    } catch (final InterruptedException e) {
                        throw new IOException("requestIntervalTime Exception");
                    }
                    final URLConnectionAdapter connection;
                    try {
                        if (request.getProxy() == null) {
                            final List<HTTPProxy> proxies = this.selectProxies(request.getUrl());
                            // choose first one
                            request.setProxy(proxies.get(0));
                        }
                        if (request.isSSLTrustALLSet() == null) {
                            request.setSSLTrustALL(this.getDefaultSSLTrustALL());
                        }
                        connection = request.connect().getHttpConnection();
                    } finally {
                        if (this.isDebug()) {
                            final Logger llogger = this.getLogger();
                            if (llogger != null) {
                                try {
                                    llogger.finest("\r\n" + request.printHeaders());
                                } catch (final Throwable e) {
                                    LogSource.exception(llogger, e);
                                }
                            }
                        }
                    }
                    this.setRequest(request);
                    if (connection != null) {
                        connection.setAllowedResponseCodes(this.getAllowedResponseCodes());
                        if (connection.getResponseCode() == 407) {
                            throw new ProxyAuthException(request.getProxy());
                        }
                    } else {
                        throw new BrowserException("connection is null", request);
                    }
                    break;
                } catch (BrowserException e) {
                    throw e;
                } catch (IOException e) {
                    request.disconnect();
                    final Logger llogger = this.getLogger();
                    if (llogger != null) {
                        LogSource.exception(llogger, e);
                    }
                    proxyRetryCounter++;
                    if (this.reportConnectException(proxyRetryCounter, e, request) || e instanceof ProxyAuthException && this.updateProxy(proxyRetryCounter, request)) {
                        // reset proxy
                        request.setProxy(null);
                        continue;
                    } else {
                        throw new BrowserException(e, request);
                    }
                }
            }
            final String redirect = request.getLocation();
            if (followRedirects && redirect != null) {
                request.disconnect();
                if (redirectLoopPrevention++ > 20) {
                    throw new BrowserException("Too many redirects!", originalRequest);
                }
                final Request redirectRequest = this.createRedirectFollowingRequest(request);
                if (redirectRequest == null) {
                    return request.getHttpConnection();
                } else {
                    request = redirectRequest;
                }
            } else {
                return request.getHttpConnection();
            }
        }
    }

    private boolean reportConnectException(final int proxyRetryCounter, final IOException e, final Request request) {
        final ProxySelectorInterface selector;
        if (this.proxy != null) {
            selector = this.proxy;
        } else {
            selector = Browser.GLOBAL_PROXY;
        }
        return selector != null && selector.reportConnectException(request, proxyRetryCounter, e);
    }

    /**
     * loads a new page (post)
     */
    public String postPage(final String url, final LinkedHashMap<String, String> post) throws IOException {
        return this.loadConnection(this.openPostConnection(url, post)).getHTMLSource();
    }

    /**
     * loads a new page (POST)
     */
    public String postPage(final String url, final String post) throws IOException {
        return this.postPage(url, Request.parseQuery(post));
    }

    public String postPageRaw(final String url, final byte[] post) throws IOException {
        final PostRequest request = (PostRequest) this.createPostRequest(url, new ArrayList<RequestVariable>(), null);
        request.setPostBytes(post);
        return this.loadConnection(this.openRequestConnection(request)).getHTMLSource();
    }

    /**
     * loads a new page (post) the postdata is given by the poststring. It will be sent as is
     */
    public String postPageRaw(final String url, final String post) throws IOException {
        final PostRequest request = (PostRequest) this.createPostRequest(url, new ArrayList<RequestVariable>(), null);
        request.setPostDataString(post);
        return this.loadConnection(this.openRequestConnection(request)).getHTMLSource();
    }

    protected List<HTTPProxy> selectProxies(final String url) throws IOException {
        final ProxySelectorInterface selector;
        if (this.proxy != null) {
            selector = this.proxy;
        } else {
            selector = Browser.GLOBAL_PROXY;
        }
        if (selector == null) {
            final ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
            ret.add(HTTPProxy.NONE);
            return ret;
        }
        final List<HTTPProxy> list;
        try {
            list = selector.getProxiesByUrl(url);
        } catch (Throwable e) {
            throw new NoGateWayException(selector, e);
        }
        if (list == null || list.size() == 0) {
            throw new NoGateWayException(selector, "No Gateway or Proxy Found");
        }
        return list;

    }

    public void setAcceptLanguage(final String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
    }

    /**
     * @param allowedResponseCodes
     *            the allowedResponseCodes to set
     * @since JD2
     */
    public void setAllowedResponseCodes(final int... allowedResponseCodes) {
        this.allowedResponseCodes = allowedResponseCodes;
    }

    /**
     * Adds input to existing response codes. This solves the issue were setAllowedResponseCodes(int...) destroys old with new.
     * 
     * @param input
     * @author raztoki
     * @since JD2
     */
    public void addAllowedResponseCodes(final int... input) {
        final int[] original = this.getAllowedResponseCodes();
        final HashSet<Integer> dupe = new HashSet<Integer>();
        for (final int a : original) {
            dupe.add(a);
        }
        for (final int a : input) {
            dupe.add(a);
        }
        final int[] outcome = new int[dupe.size()];
        int index = 0;
        for (final Integer i : dupe) {
            outcome[index++] = i;
        }
        this.setAllowedResponseCodes(outcome);
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setCookie(final String url, final String key, final String value) {
        final String host = Browser.getHost(url);
        Cookies cookies;
        if (!this.getCookies().containsKey(host) || (cookies = this.getCookies().get(host)) == null) {
            cookies = new Cookies();
            this.getCookies().put(host, cookies);
        }
        cookies.add(new Cookie(host, key, value));
    }

    public void setCookiesExclusive(final boolean b) {
        if (this.cookiesExclusive == b) {
            return;
        }
        this.cookiesExclusive = b;
        if (b) {
            this.cookies.clear();
            for (final Entry<String, Cookies> next : Browser.COOKIES.entrySet()) {
                Cookies tmp;
                this.cookies.put(next.getKey(), tmp = new Cookies());
                tmp.add(next.getValue());
            }
        } else {
            this.cookies.clear();
        }
    }

    /**
     * sets current URL, if null we don't send referer!
     * 
     * @param string
     * @since JD2
     * */
    public void setCurrentURL(final String url) throws MalformedURLException {
        if (StringUtils.isEmpty(url)) {
            this.currentURL = null;
        } else {
            this.currentURL = url;
        }
    }

    public void setCustomCharset(final String charset) {
        this.customCharset = charset;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public void setFollowRedirects(final boolean b) {
        this.doRedirects = b;
    }

    /**
     * do not below revision 10000
     * 
     * @since JD2
     * */
    public void setHeader(final String field, final String value) {
        this.getHeaders().put(field, value);
    }

    public void setHeaders(final RequestHeader h) {
        this.headers = h;
    }

    public void setKeepResponseContentBytes(final boolean keepResponseContentBytes) {
        this.keepResponseContentBytes = keepResponseContentBytes;
    }

    /**
     * Sets Browser upper page load limit Byte value.
     * 
     * @since JD2
     * @param i
     */
    public void setLoadLimit(final int i) {
        this.limit = Math.max(0, i);
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    @Deprecated
    /**
     * @deprecated
     * @param proxy2
     */
    public void setProxy(final HTTPProxy proxy2) {
        this.setProxySelector(new StaticProxySelector(proxy2));
    }

    @Deprecated
    /**
     * for usage in plugins for stable compatibility only
     * @param threadProxy
     */
    public void setProxy(final ProxySelectorInterface threadProxy) {
        this.setProxySelector(threadProxy);
    }

    public void setProxySelector(ProxySelectorInterface proxy) {
        final ProxySelectorInterface wished = proxy;
        if (proxy == null) {
            proxy = this.getThreadProxy();
        }
        if (proxy == this.proxy) {
            return;
        }
        this.proxy = proxy;
        if (this.debug) {
            final Logger llogger = this.getLogger();
            if (llogger != null) {
                llogger.info("Use local proxy: " + proxy + " wished: " + wished);
            }
        }
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setRequest(final Request request) {
        if (request == null) {
            this.currentURL = null;
        } else {
            this.currentURL = request.getUrl();
        }
        this.updateCookies(request);
        this.request = request;
    }

    public void setRequestIntervalLimit(final String host, final int i) {
        final String domain = Browser.getHost(host);
        if (domain == null) {
            return;
        }
        if (this.requestIntervalLimitMap == null) {
            this.requestTimeMap = new HashMap<String, Long>();
            this.requestIntervalLimitMap = new HashMap<String, Integer>();
        }
        this.requestIntervalLimitMap.put(domain, i);

    }

    public void setVerbose(final boolean b) {
        this.verbose = b;
    }

    public String submitForm(final Form form) throws Exception {
        return this.loadConnection(this.openFormConnection(form)).getHTMLSource();
    }

    @Override
    public String toString() {
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            return "Browser. no request yet";
        }
        return lRequest.getHTMLSource();
    }

    public void updateCookies(final Request request) {
        if (request != null) {
            final String host = Browser.getHost(request.getUrl());
            Cookies cookies = this.getCookies().get(host);
            if (cookies == null) {
                cookies = new Cookies();
                this.getCookies().put(host, cookies);
            }
            cookies.add(request.getCookies());
        }
    }

    /**
     * can update the connection information - for example ask for proxy auth.
     * 
     * @param request
     * @param proxyRetryCounter
     * 
     * @return true if a failed request should be done again.
     */
    protected boolean updateProxy(final int proxyRetryCounter, final Request request) {
        final ProxySelectorInterface selector;
        if (this.proxy != null) {
            selector = this.proxy;
        } else {
            selector = Browser.GLOBAL_PROXY;
        }
        return selector != null && selector.updateProxy(request, proxyRetryCounter);
    }

    /**
     * @return the defaultSSLTrustALL
     */
    public Boolean getDefaultSSLTrustALL() {
        return this.defaultSSLTrustALL;
    }

    /**
     * @param defaultSSLTrustALL
     *            the defaultSSLTrustALL to set
     */
    public void setDefaultSSLTrustALL(Boolean defaultSSLTrustALL) {
        this.defaultSSLTrustALL = defaultSSLTrustALL;
    }
}

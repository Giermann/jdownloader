//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/*
 * The idea behind this is to speed up linkchecking for host providers that go permanently offline. URLs tend to stay cached/archived on the intrawebs longer than host provider.
 * By providing the original plugin regular expression(s) we do not have to rely on directhttp plugin for linkchecking, or surrounding issues with 'silent errors' within the linkgrabber if the file extension isn't matched against directhttp.
 * 
 * - raztoki
 */

/* Set interfaceVersion to 3 to avoid old Stable trying to load this Plugin */

@HostPlugin(revision = "$Revision$", interfaceVersion = 3,

names = { "ezzfile.com", "skylo.me", "filefolks.com", "videoslasher.com", "filebigz.com", "filezy.net", "vidx.to", "uploadstation.com", "wooupload.com", "ifile.ws", "filesbb.com", "ihostia.com", "youload.me", "ok2upload.com", "bitupload.com", "cobrashare.sk", "enjoybox.in", "share4files.com", "up.msrem.com", "megaload.it", "nirafile.com", "terafiles.net", "vozupload.com", "freestorage.ro", "filekai.com", "divxforevertr.com", "filekeeping.de", "livefile.org", "iranfilm16.com", "isharemybitch.com", "shufuni.com", "belgeler.com", "loadhero.net", "ngsfile.com", "1-clickshare.com", "fastsonic.net", "brutalsha.re", "moviesnxs.com", "hotfile.com", "sharedbit.net", "ufox.com", "comload.net", "6ybh-upload.com", "cloudnes.com", "fileprohost.com", "cyberlocker.ch", "filebox.com", "x7files.com", "videozer.com", "megabitshare.com", "filestay.com", "uplly.com", "asixfiles.com", "zefile.com",
        "kingsupload.com", "fileking.co", "sharevid.co", "4fastfile.com", "1-upload.com", "dump.ro", "dippic.com", "uploking.com", "zshare.ma", "book-mark.net", "ginbig.com", "ddl.mn", "syfiles.com", "iuploadfiles.com", "thexyz.net", "zakachali.com", "indianpornvid.com", "hotfiles.ws", "wizzupload.com", "banashare.com", "downupload.com", "putshare.com", "vidbox.net", "filetube.to", "nowveo.com", "uploadic.com", "flashdrive.it", "flashdrive.uk.com", "filewinds.com", "wrzucaj.com", "yourfilestorage.com", "toucansharing.com", "uploaddot.com", "zooupload.com", "uploadcore.com", "spaceha.com", "tubethumbs.com", "peeje.com", "datacloud.to", "xxxmsncam.com", "uploadboxs.com", "rapidvideo.com", "247upload.com", "fileshare.in.ua", "upload.tc", "filesmall.com", "fileuplo.de", "quakefile.com", "wupfile.com", "vdoreel.com", "flazhshare.com", "upmorefiles.com", "cloudyload.com", "dotsemper.com",
        "icyfiles.com", "vidpe.com", "clouds.to", "zuzufile.com", "hostfil.es", "onlinedisk.ru", "fileduct.com", "frogup.com", "filejumbo.com", "dump.ru", "fileshawk.com", "vidstream.us", "filezpro.com", "fileupper.com", "speedy-share.net", "files.ge", "gbitfiles.com", "xtilourbano.info", "allbox4.com", "arab-box.com", "farmupload.com", "filedefend.com", "filesega.com", "kupload.org", "multishare.org", "98file.com", "wantload.com", "esnips.com", "uload.to", "share76.com", "filemates.com", "stahnu.to", "filestock.ru", "uploader.pl", "mach2upload.com", "megaunload.net", "bonpoo.com", "modovideo.com", "bitoman.ru", "maknyos.com", "upgrand.com", "pigsonic.com", "filevelocity.com", "filegaze.com", "ddldrive.com", "fileforth.com", "files-save.com", "media-4.me", "backupload.net", "upafacil.com", "filedownloads.org", "filesector.cc", "netuploaded.com", "squillion.com", "sharebees.com",
        "filetobox.com", "fufox.net", "mojedata.sk", "grupload.com", "stickam.com", "gimmedatnewjoint.com", "dup.co.il", "eazyupload.net", "depoindir.com", "own3d.tv", "drop.st", "favupload.com", "anonstream.com", "odsiebie.pl", "shareupload.com", "filebeer.info", "uploadfloor.com", "venusfile.com", "welload.com", "upaj.pl", "shareupload.net", "tsarfile.com", "omegave.org", "fsx.hu", "kiwiload.com", "gbmeister.com", "filesharing88.net", "fileza.net", "filecloud.ws", "filesome.com", "filehost.ws", "filemade.com", "bloonga.com", "zettaupload.com", "aavg.net", "freeporn.com", "bitbonus.com", "sharebeats.com", "vidhost.me", "filetechnology.com", "badongo.com", "uptorch.com", "videoveeb.com", "fileupped.com", "repofile.com", "filemsg.com", "dopeshare.com", "filefat.com", "fileplayground.com", "fileor.com", "aieshare.com", "q4share.com", "share-now.net", "1hostclick.com", "mummyfile.com",
        "hsupload.com", "upthe.net", "ufile.eu", "bitroad.net", "coolshare.cz", "speedfile.cz", "your-filehosting.com", "brontofile.com", "filestrum.com", "filedove.com", "sharpfile.com", "filerose.com", "filereactor.com", "boltsharing.com", "turboupload.com", "glumbouploads.com", "terabit.to", "buckshare.com", "zeusupload.com", "filedino.com", "filedude.com", "uptal.org", "uptal.net", "file-bit.net", "xtshare.com", "cosaupload.org", "sharing-online.com", "filestrack.com", "shareator.net", "azushare.net", "filecosy.com", "monsteruploads.eu", "vidhuge.com", "doneshare.com", "cixup.com", "animegoon.com", "supermov.com", "ufliq.com", "vidreel.com", "deditv.com", "supershare.net", "shareshared.com", "uploadville.com", "fileserver.cc", "bebasupload.com", "savefile.ro", "ovfile.com", "divxbase.com", "gptfile.com", "dudupload.com", "eyvx.com", "farshare.to", "azsharing.com",
        "freefilessharing.com", "elitedisk.com", "freakmov.com", "cloudnator.com", "filesavr.com", "saveufile.in.th", "migahost.com", "fastfreefilehosting.com", "files2k.eu", "shafiles.me", "jalurcepat.com", "divload.org", "refile.net", "oron.com", "wupload.com", "filesonic.com", "xxlupload.com", "cumfox.com", "pyramidfiles.com", "nahraj.cz", "jsharer.com", "annonhost.net", "filekeeper.org", "dynyoo.com", "163pan.com", "imagehost.org", "4us.to", "yabadaba.ru", "madshare.com", "diglo.com", "tubeload.to", "tunabox.net", "yourfilehost.com", "uploadegg.com", "brsbox.com", "amateurboobtube.com", "good.net", "freeload.to", "netporn.nl", "przeklej.pl", "alldrives.ge", "allshares.ge", "holderfile.com", "megashare.vnn.vn", "link.ge", "up.jeje.ge", "up-4.com", "cloudcache", "ddlanime.com", "mountfile.com", "platinshare.com", "megavideo.com", "megaupload.com", "megaporn.com", "zshare.net",
        "uploading4u.com", "megafree.kz", "batubia.com", "upload24.net", "files.namba.kz", "datumbit.com", "fik1.com", "fileape.com", "filezzz.com", "imagewaste.com", "fyels.com", "gotupload.com", "sharehub.com", "sharehut.com", "filesurf.ru", "openfile.ru", "letitfile.ru", "tab.net.ua", "uploadbox.com", "supashare.net", "usershare.net", "skipfile.com", "10upload.com", "x7.to", "uploadking.com", "uploadhere.com", "fileshaker.com", "vistaupload.com", "groovefile.com", "enterupload.com", "xshareware.com", "xun6.com", "yourupload.de", "youshare.eu", "mafiaupload.com", "addat.hu", "archiv.to", "bigupload.com", "biggerupload.com", "bitload.com", "bufiles.com", "cash-file.net", "combozip.com", "duckload.com", "exoshare.com", "file2upload.net", "filebase.to", "filebling.com", "filecrown.com", "filefrog.to", "filefront.com", "filehook.com", "filestage.to", "filezup.com", "fullshare.net",
        "gaiafile.com", "keepfile.com", "kewlshare.com", "lizshare.net", "loaded.it", "loadfiles.in", "megarapid.eu", "megashare.vn", "metahyper.com", "missupload.com", "netstorer.com", "nextgenvidz.com", "piggyshare.com", "profitupload.com", "quickload.to", "quickyshare.com", "share.cx", "sharehoster.de", "shareua.com", "speedload.to", "upfile.in", "ugotfile.com", "upload.ge", "uploadmachine.com", "uploady.to", "uploadstore.net", "vspace.cc", "web-share.net", "yvh.cc", "x-files.kz", "oteupload.com" },

urls = { "https?://(www\\.)?ezzfile\\.(com|it|co\\.nz)/((vid)?embed\\-)?[a-z0-9]{12}", "http://(www\\.)?skylo\\.me/[a-z]{2}\\.php\\?Id=[0-9a-f]+", "https?://(www\\.)?filefolks\\.com/[a-z0-9]{12}", "http://(www\\.)?videoslasher\\.com/video/[A-Z0-9]+", "https?://(www\\.)?filebigz\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?filezy\\.net/((vid)?embed\\-)?[a-z0-9]{12}", "https?://(www\\.)?vidx\\.to/((vid)?embed-)?[a-z0-9]{12}", "http://(www\\.)?uploadstation\\.com/file/[A-Za-z0-9]+", "https?://(www\\.)?wooupload\\.com/((vid)?embed-)?[a-z0-9]{12}", "https?://(www\\.)?ifile\\.ws/[a-z0-9]{12}", "https?://(www\\.)?filesbb\\.com/[a-z0-9]{12}", "https?://(www\\.)?ihostia\\.com/[a-z0-9]{12}", "https?://(www\\.)?youload\\.me/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?ok2upload\\.com/[a-z0-9]{12}",
        "https?://(www\\.)?bitupload\\.com/((file/p/id|hosting/download/index/p/id)/[a-z0-9]+|[a-z0-9]{12})", "http://[\\w\\.]*?cobrashare\\.sk(/downloadFile\\.php\\?id=.+|:[0-9]+/CobraShare\\-v\\.0\\.9/download/.+id=.+)", "https?://(www\\.)?enjoybox\\.in/((vid)?embed-)?[a-z0-9]{12}", "http://(www\\.)?share4files\\.com/files/[A-Za-z0-9]+\\.html", "https?://(www\\.)?up\\.msrem\\.com/((vid)?embed-)?[a-z0-9]{12}", "https?://(www\\.)?megaload\\.it/[a-z0-9]{12}", "https?://(www\\.)?nirafile\\.com/((vid)?embed-)?[a-z0-9]{12}", "http://(www\\.)?terafiles\\.net/v-\\d+\\.html", "https?://(www\\.)?vozupload\\.com/((vid)?embed-)?[a-z0-9]{12}", "https?://(www\\.)?freestorage\\.ro/[a-z0-9]{12}", "https?://(www\\.)?filekai\\.com/((vid)?embed-)?[a-z0-9]{12}", "http://(www\\.)?divxforevertr\\.com/index\\.php\\?act=subz\\&CODE=03\\&id=\\d+",
        "http://[\\w\\.]*?filekeeping\\.de/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?livefile\\.org/get/[A-Za-z0-9]+", "http://(www\\.)?iranfilm16\\.com/forum/dl\\.php\\?serverid=\\d+\\&file=[^<>\"]+", "http://(www\\.)?isharemybitch\\.com/(galleries/\\d+/[a-z0-9\\-]+|videos/\\d+/.*?)\\.html", "http://(www\\.)?shufuni\\.com/(?!Flash/)(VideoLP\\.aspx\\?videoId=\\d+|[\\w\\-\\+]+\\d+|handlers/FLVStreamingv2\\.ashx\\?videoCode=[A-Z0-9\\-]+)", "http://(www\\.)?belgeler\\.com/blg/[a-z0-9]+/", "http://(www\\.)?loadhero\\.net/[A-Za-z0-9]+", "https?://(www\\.)?ngsfile\\.com/[a-z0-9]{12}", "http://(www\\.)?1\\-clickshare\\.com/(\\d+|download2\\.php\\?a=\\d+)", "https?://(www\\.)?fastsonic\\.net/((vid)?embed-)?[a-z0-9]{12}", "https?://(www\\.)?brutalsha\\.re/(vidembed\\-)?[a-z0-9]{12}",
        "http://(www\\.)?moviesnxs\\.com/web/gallery/mnxs\\.php\\?id=\\d+", "https?://(www\\.)?hotfile\\.com/dl/\\d+/[0-9a-zA-Z]+/(.*?/|.+)?", "https?://(www\\.)?sharedbit\\.net/(file/\\d+|[a-z0-9]+)", "https?://(www\\.)?ufox\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?comload\\.net/[a-z0-9]{12}", "http://(www\\.)?6ybh-upload\\.com/[a-z0-9]{12}", "https?://(www\\.)?cloudnes\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?fileprohost\\.com/[a-z0-9]{12}", "https?://(www\\.)?cyberlocker\\.ch/((vid)?embed-)?[a-z0-9]{12}", "https?://(www\\.)?filebox\\.com/((vid)?embed-)?[a-z0-9]{12}", "https?://(www\\.)?x7files\\.com/(vidembed\\-)?[a-z0-9]{12}", "http://(www\\.)?videozer\\.com/(video|embed)/[A-Za-z0-9]+", "https?://(www\\.)?megabitshare\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?filestay\\.com/[a-z0-9]{12}", "https?://(www\\.)?uplly\\.com/[a-z0-9]{12}",
        "http://(www\\.)?asixfiles\\.com/[a-z0-9]{12}", "https?://(www\\.)?zefile\\.com/[a-z0-9]{12}", "https?://(www\\.)?kingsupload\\.com/[a-z0-9]{12}", "https?://(www\\.)?fileking\\.co/[a-z0-9]{12}", "https?://(www\\.)?sharevid\\.co/((vid)?embed-)?[a-z0-9]{12}", "http://(www\\.)?4fastfile\\.com(/[a-z]+)?/abv\\-fs/[\\d\\-]+/.+", "http://(www\\.)?1\\-upload\\.com(/[a-z]+)?/abv\\-fs/\\d+.+", "http://(www\\.)?dump\\.ro/[0-9A-Za-z/\\-\\.\\?\\=\\&]+", "https?://(www\\.)?dippic\\.com/[a-z0-9]{12}", "http://(www\\.)?uploking\\.com/file/[A-Za-z0-9_\\-]+/", "http://(www\\.)?(www\\d+\\.)?zshare(\\.net)?\\.ma/[a-z0-9]{12}", "http://(www\\.)?book\\-mark\\.net/videos/\\d+/.*?\\.html", "https?://(www\\.)?ginbig\\.com/[a-z0-9]{12}", "https?://(www\\.)?ddl\\.mn/[a-z0-9]{12}", "https?://(www\\.)?syfiles\\.com/[a-z0-9]{12}", "https?://(www\\.)?iuploadfiles\\.com/[a-z0-9]{12}",
        "https?://(www\\.)?thexyz\\.net/[a-z0-9]{12}", "http://(www\\.)?dl\\d+\\.zakachali\\.(net|com)/file/\\d+", "http://(www\\.)?indianpornvid\\.com/[A-Za-z0-9\\-_]+videos\\d+\\.html", "https?://(www\\.)?hotfiles\\.ws/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?wizzupload\\.com/[a-z0-9]{12}", "https?://(www\\.)?banashare\\.com/((vid)?embed\\-)?[a-z0-9]{12}", "http://(www\\.)?downupload\\.com/[a-z0-9]{12}", "http://(www\\.)?putshare\\.com/[a-z0-9]{12}", "http://(www\\.)?vidbox\\.net/file\\.php\\?fd=[0-9a-f]+", "http://(www\\.)?filetube\\.to/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?nowveo\\.com/file/\\d+/", "http://(www\\.)?uploadic\\.com/[a-z0-9]{12}", "https?://(www\\.)?flashdrive\\.it/(upload/)?[a-z0-9]{12}", "https?://(www\\.)?flashdrive\\.(uk\\.com|it)(/upload)?/[a-z0-9]{12}",
        "https?://(www\\.)?filewinds\\.com/(vidembed\\-)?[a-z0-9]{12}", "http://(www\\.)?wrzucaj\\.com/\\d+", "http://(www\\.)?yourfilestore\\.com/download/\\d+/[^<>\"]+\\.html", "https?://(www\\.)?toucansharing\\.com/[a-z0-9]{12}", "http://(www\\.)?uploaddot\\.com/[a-z0-9]{12}", "https?://(www\\.)?zooupload\\.com/(embed\\-)?[a-z0-9]{12}", "https?://(www\\.)?uploadcore\\.com/[a-z0-9]{12}", "https?://(www\\.)?spaceha\\.com/[a-z0-9]{12}", "http://(www\\.)?tubethumbs\\.com/galleries/\\d+/\\d+/.*?\\.php", "https?://(www\\.)?peeje(share)?\\.com/files/\\d+/[^<>\"\\'/]+", "http://(www\\.)?(datacloud|mediacloud)\\.to/download/[a-z0-9]+/.{1}", "http://(www\\.)?xxxmsncam\\.com/\\d+{4}/\\d+{2}/\\d+{2}/[^<>\"\\'/]+/", "https?://(www\\.)?uploadboxs\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?rapidvideo\\.com/view/[a-z0-9]{8}", "https?://(www\\.)?247upload\\.com/[a-z0-9]{12}",
        "http://(www\\.)?fileshare\\.in\\.ua/[0-9]+", "http://[\\w\\.]*?upload\\.tc/download/\\d+/.*?\\.html", "http://(www\\.)?filesmall\\.com/[A-Za-z0-9]+/download\\.html", "https?://(www\\.)?fileuplo\\.de/[a-z0-9]{12}", "https?://(www\\.)?quakefile\\.com/((vid)?embed\\-)?[a-z0-9]{12}", "https?://(www\\.)?wupfile\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?vdoreel\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?flazhshare\\.com/[a-z0-9]{12}", "https?://(www\\.)?upmorefiles\\.com/[a-z0-9]{12}", "http://(www\\.)?cloudyload\\.com/[A-Za-z0-9]+(~f)?", "https?://(www\\.)?dotsemper\\.com/(embed\\-)?[a-z0-9]{12}", "http://(www\\.)?icyfiles\\.com/[a-z0-9]+", "https?://(www\\.)?vidpe\\.com/[a-z0-9]{12}", "https?://(www\\.)?clouds\\.to/[a-z0-9]{12}", "https?://(www\\.)?zuzufile\\.com/[a-z0-9]{12}",
        "http://(www\\.)?hostfil\\.es/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?onlinedisk\\.ru/(file|view)/[0-9]+", "https?://(www\\.)?fileduct\\.com/[a-z0-9]{12}", "http://(www\\.)?frogup\\.com/plik/pokaz/[^<>\"/]*?/[0-9]+", "http://(www\\.)?filejumbo\\.com/Download/[A-Z0-9]+", "http://(www\\.)?dump\\.ru/file/[0-9]+", "http://(www\\.)?fileshawk\\.com/files/[A-Za-z0-9]+\\.html", "https?://(www\\.)?vidstream\\.us/[a-z0-9]{12}", "https?://(www\\.)?filezpro\\.com/[a-z0-9]{12}", "http://(www\\.)?fileupper\\.com/[a-z0-9]{12}", "http://(www\\.)?speedy\\-share\\.net/(?!faq|register|login|terms|report_file|plugins)[a-z0-9]+", "http://[\\w\\.]*?files\\.ge/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?gbitfiles\\.com/[a-z0-9]{12}", "https?://(www\\.)?xtilourbano\\.info/[a-z0-9]{12}",
        "https?://(www\\.)?allbox4\\.com/(\\-embed)?[a-z0-9]{12}", "https?://(www\\.)?arab\\-box\\.com/[a-z0-9]{12}", "https?://(www\\.)?farmupload\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?filedefend\\.com/[a-z0-9]{12}", "https?://(www\\.)?filesega\\.com/[a-z0-9]{12}", "https?://(www\\.)?kupload\\.org/[a-z0-9]{12}", "https?://(www\\.)?multishare\\.org/file/[a-z0-9]{12}/.*?\\.html", "https?://(www\\.)?98file\\.com/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?wantload\\.com/[a-z0-9]{12}", "http://(www\\.)?esnips\\.com/((ns)?doc/[a-z0-9\\-]+|displayimage\\.php\\?[\\w\\&\\=]+)", "https?://(www\\.)?uload\\.to/(vidembed\\-)?[a-z0-9]{12}", "https?://(www\\.)?share76\\.com/[a-z0-9]{12}(/[^<>\"/]+)?", "https?://(www\\.)?filemates\\.com/(vidembed\\-)?[a-z0-9]{12}", "http://(www\\.)?stahnu\\.to/files/get/[A-Za-z0-9]+/[A-Za-z0-9_\\-% ]+",
        "http://(www\\.)?filestock\\.ru/[a-z0-9]{12}", "http://[\\w\\.]*?uploader.pl/([a-z]{2}/)?file/\\d+/", "http://[\\w\\.]*?mach2upload\\.com/files/[A-Za-z0-9]+\\.html", "http://(www\\.)?megaunload\\.net/index\\.php/files/get/[A-Za-z0-9_\\-]+", "http://(www\\.)?app04\\.bonpoo\\.com/cgi\\-bin/download\\?fid=[A-Z0-9]+", "http://(www\\.)?modovideo\\.com/video(\\.php)?\\?v=[a-z0-9]+", "http://(www\\.)?bitoman\\.ru/download/\\d+\\.html", "https?://(www\\.)?maknyos\\.com/[a-z0-9]{12}", "https?://(www\\.)?upgrand\\.com/[a-z0-9]{12}", "https?://(www\\.)?pigsonic\\.com/[a-z0-9]{12}", "https?://(www\\.)?filevelocity\\.com/[a-z0-9]{12}", "https?://(www\\.)?filegaze\\.com/[a-z0-9]{12}", "https?://(www\\.)?ddldrive\\.com/[a-z0-9]{12}", "https?://(www\\.)?fileforth\\.com/[a-z0-9]{12}", "http://(www\\.)?files\\-save\\.com/(fr|en)/download\\-[a-z0-9]{32}\\.html",
        "http://(www\\.)?media\\-4\\.me/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?backupload\\.net/[a-z0-9]{12}", "http://(www\\.)?upafacil\\.com/(file/\\d+/|\\?d=)[\\w\\-\\.]+", "https?://(www\\.)?filedownloads\\.org/[a-z0-9]{12}", "http://(www\\.)?filesector\\.cc/f/[A-Z0-9]+/", "http://(www\\.)?netuploaded\\.com/[a-z0-9]{12}", "https?://(www\\.)?squillion\\.com/[a-z0-9]{12}", "https?://(www\\.)?sharebees\\.com/(vidembed\\-)?[a-z0-9]{12}", "http://(www\\.)?filetobox\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?fufox\\.(com|net)/\\?d=[A-Za-z0-9]+", "http://(www\\.)?mojedata\\.sk/[A-Za-z0-9_]+", "https?://(www\\.)?grupload\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?stickam\\.com/viewMedia\\.do\\?mId=\\d+", "http://(www\\.)?gimmedatnewjoint\\.com/play/\\d+/",
        "http://(www\\.)?dup\\.co\\.il/(file\\.php\\?akey=|v/)[\\w]{14}", "http://(www\\.)?eazyupload\\.net/(download/)?[A-Za-z0-9]+", "http://(www\\.)?depoindir\\.com/files/get/[A-Za-z0-9]+", "http://[\\w\\.]*?own3d\\.tv/(.*?/)?(video|watch)/\\d+", "http://(www\\.)?drop\\.st/[A-Za-z0-9]+", "http://(www\\.)?favupload\\.com/(video|file|audio)/\\d+/", "http://(www\\.)?anonstream\\.com/get_[A-Za-z0-9]+", "https?://(www\\.)?odsiebie\\.pl/[a-z0-9]{12}", "https?://(www\\.)?shareupload\\.com/[a-z0-9]{12}", "http://(www\\.)?filebeer\\.info/(\\d+~f|[A-Za-z0-9]+)", "http://[\\w\\.]*?uploadfloor\\.com/[a-z0-9]{12}", "https?://(www\\.)?venusfile\\.com/[a-z0-9]{12}", "https?://(www\\.)?welload\\.com/[a-z0-9]{12}", "https?://(www\\.)?upaj\\.pl/[a-z0-9]{12}", "http://(www\\.)?shareupload\\.net/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es|de)/)?file/[0-9]+/)",
        "https?://(www\\.)?tsarfile\\.com/[a-z0-9]{12}", "http://(www\\.)?omegave\\.org/[a-z0-9]{12}", "http://((www\\.)?(fsx|mfs\\.hu/download\\.php\\?s=\\d+\\&d=[^<>\"]+\\&h=[a-z0-9]+|s.*?\\.(fsx|mfs)\\.hu/.+/.+))", "http://(www\\.)?kiwiload\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es|de)/)?file/[0-9]+/)", "https?://(www\\.)?gbmeister\\.com/[a-z0-9]{12}", "http://(www\\.)?filesharing88\\.net/public/(?!(faq|register|login|terms|report_file|aboutus|contact|forgot|help|inthenews|privacy|upgrade)\\.html)[a-z0-9]+(?!~|%7Ef)", "https?://(www\\.)?fileza\\.net/[a-z0-9]{12}", "https?://(www\\.)?filecloud\\.ws/[a-z0-9]{12}", "https?://(www\\.)?filesome\\.com/[a-z0-9]{12}", "https?://(www\\.)?filehost\\.ws/[a-z0-9]{12}", "https?://(www\\.)?filemade\\.com?/[a-z0-9]{12}", "https?://(www\\.)?bloonga\\.com/[a-z0-9]{12}",
        "http://(www\\.)?zettaupload\\.com/(?!file_category|latest_file|wklej|terms|report_file|partners|contact|faq|login|register)[a-z0-9]+", "https?://(www\\.)?(aa\\.vg|aavg\\.net)/[a-z0-9]{12}", "http://(www\\.)?freeporn\\.com/(video/\\d+/|[a-z0-9\\-_]+\\-\\d+)", "http://(www\\.)?bitbonus\\.com/download/[A-Za-z0-9\\-]+", "https?://(www\\.)?sharebeats\\.com/[a-z0-9]{12}", "http://(www\\.)?vidhost\\.me/\\?dw?=[A-Z0-9]+", "https?://(www\\.)?filetechnology\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?badongo\\.com/.*(file|vid|cvid|audio|pic)/[0-9]+(/[0-9]+)?", "https?://(www\\.)?uptorch\\.com/\\?d=[A-Z0-9]{9}", "http://(www\\.)?videoveeb\\.(com|net)/\\?dw?=[A-Z0-9]+", "http://(www\\.)?fileupped\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?repofile\\.com/[a-z0-9]{12}", "https?://(www\\.)?filemsg\\.com/[a-z0-9]{12}",
        "https?://(www\\.)?dopeshare\\.com/[a-z0-9]{12}", "https?://(www\\.)?filefat\\.com/[a-z0-9]{12}", "http://(www\\.)?fileplayground\\.com/[a-z0-9]{12}", "https?://(www\\.)?fileor\\.com/[a-z0-9]{12}", "https?://(www\\.)?aieshare\\.com/[a-z0-9]{12}", "http://(www\\.)?q4share\\.com/[a-z0-9]{12}", "http://(www\\.)??share\\-now\\.net/{1,}files/\\d+\\-.*?\\.html", "https?://(www\\.)?1hostclick\\.com/[a-z0-9]{12}", "https?://(www\\.)?mummyfile\\.com/[a-z0-9]{12}", "http://(www\\.)?hsupload\\.(com|net)/\\?dw?=[A-Z0-9]+", "https?://(www\\.)?upthe\\.net/[a-z0-9]{12}", "https?://(www\\.)?ufile\\.eu/[a-z0-9]{12}", "http://(www\\.)?(bitroad\\.net|filemashine\\.com|friendlyfiles\\.net|vip4sms\\.com|manuls\\.ru)/download/[A-Fa-f0-9]+", "http://(www\\.)?coolshare\\.cz/stahnout/\\d+", "http://(www\\.)?speedfile\\.cz/((cs|en|de)/)?\\d+/[a-z0-9\\-]+",
        "https?://(www\\.)?your\\-filehosting\\.com/[a-z0-9]{12}", "http://(www\\.)?brontofile\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?filestrum\\.com/[a-z0-9]{12}", "https?://(www\\.)?filedove\\.com/[a-z0-9]{12}", "https?://(www\\.)?sharpfile\\.com/(?!folder/|banners|contact|images|register|privacy|premium|affiliates|language|resellers)[a-z0-9]{6,12}", "https?://(www\\.)?filerose\\.com/[a-z0-9]{12}", "https?://(www\\.)?filereactor\\.com/[a-z0-9]{12}", "https?://(www\\.)?boltsharing\\.com/[a-z0-9]{12}", "http://(www\\.)?turboupload\\.com/[a-z0-9]+", "http://(www\\.)?(uploads\\.glumbo|glumbouploads)\\.com/[a-z0-9]{12}", "https?://(www\\.)?terabit\\.to/[a-z0-9]{12}", "http://(www\\.)?buckshare\\.com/[a-z0-9]{12}", "https?://(www\\.)?zeusupload\\.com/[a-z0-9]{12}", "https?://(www\\.)?filedino\\.com/[a-z0-9]{12}",
        "http://(www\\.)?(appscene\\.org|filedude\\.com)/(download/[0-9a-zA-Z]+|download\\.php\\?id=\\d+)", "http://(www\\.)?(new\\.)?uptal\\.(com|org)/\\?d=[A-Fa-f0-9]+", "http://(www\\.)?uptal\\.net/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?file-bit\\.net/[a-z0-9]{12}", "http://(www\\.)?xtshare\\.com/toshare\\.php\\?Id=\\d+(\\&view=[0-9a-f]+)?", "http://(www\\.)?cosaupload\\.org/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es|de)/)?file/[0-9]+/)", "http://(www\\.)?sharing\\-online\\.com/[a-z0-9]{12}", "http://(www\\.)?filestrack\\.com/[a-z0-9]{12}", "http://(www\\.)?shareator\\.net/[0-9a-z]+", "http://(www\\.)?azushare\\.net/[A-Za-z0-9]+/", "https?://(www\\.)?filecosy\\.com/[a-z0-9]{12}", "https?://(www\\.)?monsteruploads\\.eu/[a-z0-9]{12}(/[^<>\"/]*?)?\\.html", "https?://(www\\.)?vidhuge\\.com/[a-z0-9]{12}",
        "http://(www\\.)?doneshare\\.com/files/details/[a-z0-9\\-]+\\.html", "http://(www\\.)?cixup\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?animegoon\\.com/[a-z0-9]{12}", "http://(www\\.)?supermov\\.com/(rc|video)/[0-9]+", "https?://(www\\.)?ufliq\\.com/[a-z0-9]{12}", "http://(www\\.)?vidreel\\.com/video/[0-9a-zA-Z]+/", "http://(www\\.)?deditv\\.com/(play|gate\\-way)\\.php\\?v=[0-9a-f]+", "http://(www\\.)?(hotshare|supershare)\\.net/(.+/)?(file|audio|video)/.+", "https?://(www\\.)?shareshared\\.com/[a-z0-9]{12}", "http://(www\\.)?uploadville\\.com/[a-z0-9]{12}", "http://(www\\.)?fileserver\\.cc/[a-z0-9]{12}", "http://(www\\.)?bebasupload\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?savefile\\.ro/[\\w]+/?", "http://(www\\.)?ovfile\\.com/[a-z0-9]{12}", "https?://(www\\.)?divxbase\\.com/[a-z0-9]{12}",
        "https?://(www\\.)?gptfile\\.com/[a-z0-9]{12}", "https?://(www\\.)?dudupload\\.com/[a-z0-9]{12}", "http://(www\\.)?eyvx\\.com/[a-z0-9]{12}", "http://(www\\.)?farshare\\.to/[a-z0-9]{12}", "http://(www\\.)?azsharing\\.com/[a-z0-9]{12}/", "http://(www\\.)?freefilessharing\\.com/[a-z0-9]{12}", "https?://(www\\.)?elitedisk\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?freakmov\\.com/(rc|video)/[0-9]+", "http://[\\w\\.]*?(shragle|cloudnator)\\.(com|de)/files/[\\w]+/.*", "http://[\\w\\.]*?filesavr\\.com/[A-Za-z0-9]+(_\\d+)?", "http://(www\\.)?saveufile\\.(in\\.th|com)/car\\.php\\?file=[a-z0-9]+", "https?://(www\\.)?migahost\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?fastfreefilehosting\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?files2k\\.eu/[a-z0-9]{12}", "https?://(www\\.)?shafiles\\.me/[a-z0-9]{12}",
        "http://(www\\.)?jalurcepat\\.com/[a-z0-9]{12}", "https?://(www\\.)?(divload|divupload)\\.org/(embed\\-)?[a-z0-9]{12}", "http://(www\\.)?refile\\.net/(d|f)/\\?[\\w]+", "http://[\\w\\.]*?oron\\.com/[a-z0-9]{12}", "http://(www\\.)?wupload\\.[a-z]{1,5}/file/([0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?)", "http://[\\w\\.]*?(sharingmatrix|filesonic)\\..*?/.*?file/([a-zA-Z0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?|[0-9]+(/.+)?)", "https?://(www\\.)?xxlupload\\.com/[a-z0-9]{12}", "http://(www\\.)?cumfox\\.com/videos/.*?-\\d+\\.html", "http://(www\\.)?pyramidfiles\\.com/[a-z0-9]{12}", "http://(www\\.)?nahraj\\.cz/content/(view|download)/[a-z0-9]+\\-[a-z0-9]+\\-[a-z0-9]+\\-[a-z0-9]+\\-[a-z0-9]+", "http://(www\\.)?jsharer\\.com/download/[a-z0-9]+\\.htm", "https?://(www\\.)?annonhost\\.net/[a-z0-9]{12}",
        "http://(www\\.)?filekeeper\\.org/download/[0-9a-zA-Z]+/([\\(\\)0-9A-Za-z\\.\\-_% ]+|[/]+/[\\(\\)0-9A-Za-z\\.\\-_% ])", "http://(www\\.)?dynyoo\\.com/\\?goto=dl\\&id=[a-z0-9]{32}", "http://[\\w\\.]*?163pan\\.com/files/[a-z0-9]+\\.html", "http://[\\w\\.]*?imagehost\\.org/(download/[0-9]+/.+|[0-9]+/.+)", "http://[\\w\\.]*?4us\\.to/download\\.php\\?id=[A-Z0-9]+", "http://[\\w\\.]*?yabadaba\\.ru/files/[0-9]+", "http://(www\\.)?madshare\\.com/(en/)?download/[a-zA-Z0-9]+/", "http://(www\\.)?diglo\\.com/download/[a-z0-9]+", "http://(www\\.)?tubeload\\.to/file(\\d+)?\\-.+", "http://(www\\.)?tunabox\\.net/files/[A-Za-z0-9]+\\.html", "http://[\\w\\.]*?yourfilehost\\.com/media\\.php\\?cat=.*?\\&file=.+", "https?://(www\\.)?uploadegg\\.com/[a-z0-9]{12}", "http://(www\\.)?brsbox\\.com/filebox/down/fc/[a-z0-9]{32}", "http://(www\\.)?amateurboobtube\\.com/videos/\\d+/.*?\\.html",
        "http://(www\\.)?good\\.net/.+", "http://(www\\.)*?(freeload|mcload)\\.to/(divx\\.php\\?file_id=|\\?Mod=Divx\\&Hash=)[a-z0-9]+", "http://(www\\.)?netporn\\.nl/watch/[a-z0-9]+/.{1}", "http://(www\\.)?przeklej\\.pl/(d/\\w+/|\\d+|plik/)[^\\s]+", "http://(www\\.)?alldrives\\.ge/main/linkform\\.php\\?f=[a-z0-9]+", "http://(www\\.)?allshares\\.ge/(\\?d|download\\.php\\?id)=[A-Z0-9]+", "https?://(www\\.)?holderfile\\.com/[a-z0-9]{12}", "http://(www\\.)?megashare\\.vnn\\.vn/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?link\\.ge/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?up\\.jeje\\.ge//((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?up\\-4\\.com/(\\?d|download\\.php\\?id)=[A-Z0-9]+", "https?://(www\\.)?cloudcache\\.cc/[a-z0-9]{12}",
        "https?://(www\\.)?(ddlanime\\.com|ddlani\\.me)/[a-z0-9]{12}", "http://(www\\.)?mountfile\\.com/file/[a-z0-9]+/[a-z0-9]+", "http://(www\\.)?platinshare\\.com/files/[A-Za-z0-9]+", "http://(www\\.)?megavideo\\.com/(.*?(v|d)=|v/)[a-zA-Z0-9]+", "http://(www\\.)?megaupload\\.com/.*?(\\?|&)d=[0-9A-Za-z]+", "http://(www\\.)?(megaporn|megarotic|sexuploader)\\.com/(.*?v=|v/)[a-zA-Z0-9]+", "http://(www\\.)?zshare\\.net/(download|video|image|audio|flash)/.*", "http://(www\\.)?uploading4u\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?megafree\\.kz/file\\d+", "http://(www\\.)?batubia\\.com/[a-z0-9]{12}", "http://(www\\.)?upload24\\.net/[a-z0-9]+\\.[a-z0-9]+", "http://(www\\.)?download\\.files\\.namba\\.kz/files/\\d+", "http://(www\\.)?datumbit\\.com/file/.*?/", "http://(www\\.)?fik1\\.com/[a-z0-9]{12}",
        "http://(www\\.)?fileape\\.com/(index\\.php\\?act=download\\&id=|dl/)\\w+", "http://(www\\.)?filezzz\\.com/download/[0-9]+/", "http://(www\\.)?imagewaste\\.com/pictures/\\d+/.{1}", "http://(www\\.)?fyels\\.com/[A-Za-z0-9]+", "http://(www\\.)?gotupload\\.com/[a-z0-9]{12}", "http://(go.sharehub.com|sharehub.me|follow.to|kgt.com|krt.com)/.*", "http://(www\\.)?sharehut\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?(filesurf|4ppl|files\\.youmama)\\.ru/[0-9]+", "http://[\\w\\.]*?openfile\\.ru/[0-9]+", "http://[\\w\\.]*?letitfile\\.(ru|com)/download/id\\d+", "http://[\\w\\.]*?tab\\.net\\.ua/sites/files/site_name\\..*?/id\\.\\d+/", "http://[\\w\\.]*?uploadbox\\.com/.*?files/[0-9a-zA-Z]+", "http://(www\\.)?supashare\\.net/[a-z0-9]{12}", "https?://(www\\.)?usershare\\.net/[a-z0-9]{12}", "http://(www\\.)?skipfile\\.com/[a-z0-9]{12}", "http://(www\\.)?10upload\\.com/[a-z0-9]{12}",
        "http://[\\w\\.]*?x7\\.to/(?!list)[a-zA-Z0-9]+(/(?!inList)[^/\r\n]+)?", "http://(www\\.)?uploadking\\.com/[A-Z0-9]+", "http://(www\\.)?uploadhere\\.com/[A-Z0-9]+", "http://[\\w\\.]*?fileshaker\\.com/.+", "http://(www\\.)?vistaupload\\.com/[a-z0-9]{12}", "https?://(www\\.)?groovefile\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?enterupload\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?xshareware\\.com/[\\w]+/.*", "http://[\\w\\.]*?xun6\\.(com|net)/file/[a-z0-9]+", "http://(www\\.)?yourupload\\.de/[a-z0-9]{12}", "http://(www\\.)?youshare\\.eu/[a-z0-9]{12}", "http://(www\\.)?mafiaupload\\.com/do\\.php\\?id=\\d+", "http://[\\w\\.]*?addat.hu/.+/.+", "http://(www\\.)?archiv\\.to/((\\?Module\\=Details\\&HashID\\=|GET/)FILE[A-Z0-9]+|view/divx/[a-z0-9]+)", "http://[\\w\\.]*?bigupload\\.com/(d=|files/)[A-Z0-9]+", "http://(www\\.)?biggerupload\\.com/[a-z0-9]{12}",
        "http://(www\\.)?(bitload\\.com/(f|d)/\\d+/[a-z0-9]+|mystream\\.to/file-\\d+-[a-z0-9]+)", "https?://(www\\.)?bufiles\\.com/[a-z0-9]{12}", "http://(www\\.)?cash-file\\.(com|net)/[a-z0-9]{12}", "http://[\\w\\.]*?combozip\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/(download/[a-z0-9]+|(divx|play)/[A-Z0-9\\.-]+|[a-zA-Z0-9\\.]+)", "http://(www\\.)?exoshare\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?file2upload\\.(net|com)/download/[0-9]+/", "http://[\\w\\.]*?filebase\\.to/(files|download)/\\d{1,}/.*", "http://[\\w\\.]*?filebling\\.com/[a-z0-9]{12}", "http://(www\\.)?filecrown\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?filefrog\\.to/download/\\d+/[a-zA-Z0-9]+", "http://[\\w\\.]*?filefront\\.com/[0-9]+", "http://(www\\.)?filehook\\.com/[a-z0-9]{12}", "http://(www\\.)?filestage\\.to/watch/[a-z0-9]+/", "http://(www\\.)?(filezup|divxupfile)\\.com/[a-z0-9]{12}",
        "http://[\\w\\.]*?fullshare\\.net/show/[a-z0-9]+/.+", "http://(www\\.)?gaiafile\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?keepfile\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?kewlshare\\.com/dl/[\\w]+/", "http://[\\w\\.]*?lizshare\\.net/[a-z0-9]{12}", "http://(www\\.)?loaded\\.it/(show/[a-z0-9]+/[A-Za-z0-9_\\-% \\.]+|(flash|divx)/[a-z0-9]+/)", "http://[\\w\\.]*?loadfiles\\.in/[a-z0-9]{12}", "(http://[\\w\\.]*?megarapid\\.eu/files/\\d+/.+)|(http://[\\w\\.]*?megarapid\\.eu/\\?e=403\\&m=captcha\\&file=\\d+/.+)", "http://[\\w\\.]*?(megashare\\.vn/(download\\.php\\?uid=[0-9]+\\&id=[0-9]+|dl\\.php/\\d+)|share\\.megaplus\\.vn/dl\\.php/\\d+)", "http://(www\\.)?metahyper\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?missupload\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?netstorer\\.com/[a-zA-Z0-9]+/.+", "http://[\\w\\.]*?nextgenvidz\\.com/view/\\d+", "http://(www\\.)?piggyshare\\.com/file/[a-z0-9]+",
        "http://(www\\.)?profitupload\\.com/files/[A-Za-z0-9]+\\.html", "http://[\\w\\.]*?quickload\\.to/\\?Go=Player\\&HashID=FILE[A-Z0-9]+", "http://[\\w\\.]*?quickyshare\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?share\\.cx/(files/)?\\d+", "http://[\\w\\.]*?sharehoster\\.(de|com|net)/(dl|wait|vid)/[a-z0-9]+", "http://[\\w\\.]*?shareua.com/get_file/.*?/\\d+", "http://[\\w\\.]*?speedload\\.to/FILE[A-Z0-9]+", "http://(www\\.)?upfile\\.in/[a-z0-9]{12}", "http://[\\w\\.]*?ugotfile.com/file/\\d+/.+", "http://[\\w\\.]*?upload\\.ge/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://[\\w\\.]*?uploadmachine\\.com/(download\\.php\\?id=[0-9]+&type=[0-9]{1}|file/[0-9]+/)", "http://[\\w\\.]*?uploady\\.to/dl/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?uploadstore\\.net/[a-z0-9]{12}",
        "http://[\\w\\.]*?vspace\\.cc/file/[A-Z0-9]+\\.html", "http://[\\w\\.]*?web-share\\.net/download/file/item/.*?_[0-9]+", "http://(www\\.)?yvh\\.cc/video\\.php\\?file=[a-z0-9_]+", "http://[\\w\\.]*?x-files\\.kz/[a-z0-9]+", "https?://(www\\.)?oteupload\\.com/((vid)?embed\\-)?[a-z0-9]{12}" }, flags = { 0 })
public class Offline extends PluginForHost {

    /**
     * fullbuild help comment
     * 
     * @param wrapper
     */
    public Offline(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls != null) {
            for (DownloadLink link : urls) {
                link.setAvailable(false);
            }
        }
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        ai.setStatus("Permanently Offline: Host provider no longer exists!");
        account.setValid(false);
        return ai;
    }

    public Boolean rewriteHost(Account acc) {
        /* please also update this for alternative hosts */
        if ("filemade.com".equals(getHost())) {
            if (acc != null && "filemade.co".equals(acc.getHoster())) {
                acc.setHoster("filemade.co");
                return true;
            }
            return false;
        }
        return null;
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permanently Offline: Host provider no longer exists");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permanently Offline: Host provider no longer exists");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}
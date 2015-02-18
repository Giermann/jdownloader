//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

package org.jdownloader.extensions.eventscripter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.AddonPanel;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Script;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.RemoteAPIInternalEventListener;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerControllerListener;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.eventscripter.sandboxobjects.CrawlerJobSandbox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.DownloadLinkSandBox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.EventSandbox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.FilePackageSandBox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.PackagizerLinkSandbox;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class EventScripterExtension extends AbstractExtension<EventScripterConfig, EventScripterTranslation> implements MenuExtenderHandler, DownloadWatchdogListener, GenericConfigEventListener<Object>, RemoteAPIInternalEventListener, FileCreationListener, LinkCollectorListener, PackagizerControllerListener {

    private Object                   lock = new Object();
    private EventScripterConfigPanel configPanel;
    private List<ScriptEntry>        entries;

    @Override
    public boolean isHeadlessRunnable() {
        return true;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public EventScripterExtension() throws StartException {
        setTitle(T._.title());

    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_EVENT;
    }

    @Override
    protected void stop() throws StopException {
        PackagizerController.getInstance().getEventSender().removeListener(this);
        DownloadWatchDog.getInstance().getEventSender().removeListener(this);
        RemoteAPIController.getInstance().getEventSender().removeListener(this);
        CFG_EVENT_CALLER.SCRIPTS.getEventSender().removeListener(this);
        FileCreationManager.getInstance().getEventSender().removeListener(this);
        LinkCollector.getInstance().getEventsender().removeListener(this);
    }

    @Override
    protected void start() throws StartException {
        PackagizerController.getInstance().getEventSender().addListener(this);
        LinkCollector.getInstance().getEventsender().addListener(this);
        FileCreationManager.getInstance().getEventSender().addListener(this);
        DownloadWatchDog.getInstance().getEventSender().addListener(this);
        RemoteAPIController.getInstance().getEventSender().addListener(this);
        CFG_EVENT_CALLER.SCRIPTS.getEventSender().addListener(this);
        entries = getSettings().getScripts();
        if (!Application.isHeadless()) {
            configPanel = new EventScripterConfigPanel(this);
        }
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                if (entries != null) {
                    for (ScriptEntry script : entries) {
                        if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_JDOWNLOADER_STARTED == script.getEventTrigger()) {
                            HashMap<String, Object> props = new HashMap<String, Object>();
                            runScript(script, props);

                        }
                    }
                }
            }
        });
    }

    @Override
    public String getDescription() {
        return _.description();
    }

    @Override
    public AddonPanel<EventScripterExtension> getGUI() {
        // if you want an own t
        return null;
    }

    @Override
    protected void initExtension() throws StartException {

    }

    @Override
    public boolean isQuickToggleEnabled() {
        return true;
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {

        return null;
    }

    @Override
    public ExtensionConfigPanel<?> getConfigPanel() {
        return configPanel;
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {

        if (entries == null) {
            return;
        }
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_DOWNLOAD_CONTROLLER_START == script.getEventTrigger()) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("link", new DownloadLinkSandBox(downloadController.getDownloadLink()));
                    props.put("package", new FilePackageSandBox(downloadController.getDownloadLink().getParentNode()));

                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }

    }

    public void runScript(ScriptEntry script, HashMap<String, Object> props) {
        new ScriptThread(script, props, getLogger()).start();

    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {

        if (entries == null) {
            return;
        }
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_DOWNLOAD_CONTROLLER_STOPPED == script.getEventTrigger()) {
                HashMap<String, Object> props = new HashMap<String, Object>();
                props.put("link", new DownloadLinkSandBox(downloadController.getDownloadLink()));
                props.put("package", new FilePackageSandBox(downloadController.getDownloadLink().getParentNode()));
                runScript(script, props);

            }
        }

    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        entries = getSettings().getScripts();
    }

    public void save(final List<ScriptEntry> tableData) {
        new Thread() {
            @Override
            public void run() {
                getSettings().setScripts(new ArrayList<ScriptEntry>(tableData));
            }
        }.start();

    }

    public void runTest(final EventTrigger eventTrigger, final String name, final String scriptSource) {
        new Thread("TestRun") {
            @Override
            public void run() {
                ScriptEntry script = new ScriptEntry();
                script.setEventTrigger(eventTrigger);
                script.setScript(scriptSource);
                script.setName(name);
                runScript(script, eventTrigger.getTestProperties());
            }
        }.start();

    }

    public void runTestCompile(EventTrigger eventTrigger, String script) {
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1);

        cx.setLanguageVersion(Context.VERSION_1_5);

        Global scope = new Global();
        scope.init(cx);

        try {
            // cx.compileString(source, sourceName, lineno, securityDomain)
            Script fcn = cx.compileString(script, "", 1, null);

            Dialog.getInstance().showMessageDialog(_GUI._.lit_successfull());
        } catch (Throwable e1) {
            Dialog.getInstance().showExceptionDialog(T._.syntax_error(), e1.getMessage(), e1);
        } finally {
            Context.exit();
        }
    }

    public void addScriptEntry(ScriptEntry newScript) {
        ArrayList<ScriptEntry> newEntries = entries == null ? new ArrayList<ScriptEntry>() : new ArrayList<ScriptEntry>(entries);
        newEntries.add(newScript);
        save(newEntries);
    }

    public void removeScriptEntries(List<ScriptEntry> entries2) {
        ArrayList<ScriptEntry> newEntries = entries == null ? new ArrayList<ScriptEntry>() : new ArrayList<ScriptEntry>(entries);
        newEntries.removeAll(entries2);
        save(newEntries);
    }

    @Override
    public void onRemoteAPIEvent(EventObject event) {
        if (entries == null) {
            return;
        }

        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_OUTGOING_REMOTE_API_EVENT == script.getEventTrigger()) {
                HashMap<String, Object> props = new HashMap<String, Object>();

                props.put("event", new EventSandbox(event));

                // props.put("package", getPackageInfo(downloadController.getDownloadLink().getParentNode()));
                runScript(script, props);

            }
        }
    }

    @Override
    public void onNewFile(Object caller, File[] fileList) {

        if (entries == null) {
            return;
        }

        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_NEW_FILE == script.getEventTrigger()) {
                HashMap<String, Object> props = new HashMap<String, Object>();
                String[] pathes = new String[fileList.length];
                for (int i = 0; i < fileList.length; i++) {
                    pathes[i] = fileList[i].getAbsolutePath();
                }
                props.put("files", pathes);
                props.put("caller", caller == null ? null : caller.getClass().getName());

                runScript(script, props);

            }
        }
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink link) {
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink link) {
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {

        if (entries == null) {
            return;
        }

        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_NEW_CRAWLER_JOB == script.getEventTrigger()) {
                HashMap<String, Object> props = new HashMap<String, Object>();

                props.put("job", new CrawlerJobSandbox(job));

                runScript(script, props);

            }
        }
    }

    @Override
    public void onPackagizerUpdate() {
    }

    @Override
    public void onPackagizerRunBeforeLinkcheck(CrawledLink link) {
        if (entries == null) {
            return;
        }

        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_PACKAGIZER == script.getEventTrigger()) {
                HashMap<String, Object> props = new HashMap<String, Object>();
                props.put("linkcheckDone", false);

                props.put("link", new PackagizerLinkSandbox(link));

                runScript(script, props);

            }
        }
    }

    @Override
    public void onPackagizerRunAfterLinkcheck(CrawledLink link) {
        if (entries == null) {
            return;
        }

        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_PACKAGIZER == script.getEventTrigger()) {
                HashMap<String, Object> props = new HashMap<String, Object>();
                props.put("linkcheckDone", true);

                props.put("link", new PackagizerLinkSandbox(link));

                runScript(script, props);

            }
        }
    }
}
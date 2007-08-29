package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import jd.Configuration;
import jd.JDUtilities;
import jd.controlling.JDAction;
import jd.event.ControlEvent;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginEvent;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class SimpleGUI implements UIInterface, ActionListener{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;
    /**
     * Das Hauptfenster
     */
    private JFrame frame;
    /**
     * Die Menüleiste
     */
    private JMenuBar menuBar;
    /**
     * Toolleiste für Knöpfe
     */
    private JToolBar toolBar;
    /**
     * Komponente, die alle Downloads anzeigt
     */
    private TabDownloadLinks  tabDownloadTable;;
    /**
     * Komponente, die den Fortschritt aller Plugins anzeigt
     */
    private TabPluginActivity tabPluginActivity;
    /**
     * TabbedPane
     */
    private JTabbedPane tabbedPane;
    /**
     * Die Statusleiste für Meldungen
     */
    private StatusBar statusBar;
    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener> uiListener = null;
    
    /**
     * Ein Togglebutton zum Starten / Stoppen der Downloads
     */
    private JToggleButton btnStartStop;
    private JDAction actionStartStopDownload;
    private JDAction actionAdd;
    private JDAction actionDelete;
    private JDAction actionLoadLinks;
    private JDAction actionSaveLinks;
    private JDAction actionExit;
    private JDAction actionLog;
    private JDAction actionConfig;
    
    private LogDialog logDialog;
    private Logger logger = Plugin.getLogger();

    private JCheckBoxMenuItem menViewLog;

    /**
     * Das Hauptfenster wird erstellt
     */
    public SimpleGUI(){
        super();
        try {
            UIManager.setLookAndFeel(new WindowsLookAndFeel());
        }
        catch (UnsupportedLookAndFeelException e) {}
        
        uiListener = new Vector<UIListener>();
        frame      = new JFrame();
        tabbedPane = new JTabbedPane();
        menuBar    = new JMenuBar();
        toolBar    = new JToolBar();
        frame.setIconImage(JDUtilities.getImage("mind"));
        frame.setTitle(JDUtilities.JD_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initActions();
        initMenuBar();
        buildUI();

        frame.pack();
        frame.setLocation(JDUtilities.getCenterOfComponent(null, frame));
        frame.setVisible(true);
    }
    /**
     * Die Aktionen werden initialisiert
     */
    public void initActions(){
        actionStartStopDownload = new JDAction(this,        "start", "action.start",         JDAction.APP_START_STOP_DOWNLOADS);
        actionAdd               = new JDAction(this,          "add", "action.add",           JDAction.ITEMS_ADD);
        actionDelete            = new JDAction(this,        "delete", "action.delete",       JDAction.ITEMS_REMOVE);
        actionLoadLinks         = new JDAction(this,          "load", "action.load",         JDAction.APP_LOAD);
        actionSaveLinks         = new JDAction(this,          "save", "action.save",         JDAction.APP_SAVE);
        actionExit              = new JDAction(this,          "exit", "action.exit",         JDAction.APP_EXIT);
        actionLog               = new JDAction(this,           "log", "action.viewlog",      JDAction.APP_LOG);
        actionConfig            = new JDAction(this, "configuration", "action.configuration",JDAction.APP_CONFIGURATION);
    }
    /**
     * Das Menü wird hier initialisiert
     */
    public void initMenuBar(){
        // file menu
        JMenu menFile         = new JMenu(JDUtilities.getResourceString("menu.file"));
        menFile.setMnemonic(JDUtilities.getResourceChar("menu.file_mnem"));

        JMenuItem menFileLoad = createMenuItem(actionLoadLinks);
        JMenuItem menFileSave = createMenuItem(actionSaveLinks);
        JMenuItem menFileExit = createMenuItem(actionExit);
        
        
        // action menu
        JMenu menAction       = new JMenu(JDUtilities.getResourceString("menu.action"));
        menAction.setMnemonic(JDUtilities.getResourceChar("menu.action_mnem"));
        
        JMenuItem menDownload = createMenuItem(actionStartStopDownload);
        
        // extra
        JMenu menExtra       = new JMenu(JDUtilities.getResourceString("menu.extra"));
        menAction.setMnemonic(JDUtilities.getResourceChar("menu.extra_mnem"));

        menViewLog = new JCheckBoxMenuItem(actionLog);
        menViewLog.setIcon(null);
        if (actionLog.getAccelerator()!=null)
            menViewLog.setAccelerator(actionLog.getAccelerator());

        JMenuItem menConfig = createMenuItem(actionConfig);

        // add menus to parents
        menFile.add(menFileLoad);
        menFile.add(menFileSave);
        menFile.addSeparator();
        menFile.add(menFileExit);
        
        menExtra.add(menViewLog);
        menExtra.add(menConfig);
        
        menAction.add(menDownload);
        
        menuBar.add(menFile);
        menuBar.add(menAction);
        menuBar.add(menExtra);
        frame.setJMenuBar(menuBar);
    }
     
    /**
     * factory method for menu items
     * @param action action for the menu item
     * @return the new menu item
     */
    private static JMenuItem createMenuItem(JDAction action) {
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setIcon(null);
        if (action.getAccelerator()!=null)
           menuItem.setAccelerator(action.getAccelerator());
        return menuItem;
    }
    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt
     */
    private void buildUI(){
        tabbedPane        = new JTabbedPane();
        tabDownloadTable  = new TabDownloadLinks(this);
        tabPluginActivity = new TabPluginActivity();
        statusBar         = new StatusBar();
        tabbedPane.addTab(JDUtilities.getResourceString("label.tab.download"),        tabDownloadTable);
        tabbedPane.addTab(JDUtilities.getResourceString("label.tab.plugin_activity"), tabPluginActivity);

        btnStartStop  = new JToggleButton(actionStartStopDownload);
        btnStartStop.setSelectedIcon(new ImageIcon(JDUtilities.getImage("stop")));
        btnStartStop.setFocusPainted(false);
        btnStartStop.setBorderPainted(false);
        btnStartStop.setText(null);


        JButton btnAdd    = new JButton(actionAdd);
        btnAdd.setFocusPainted(false);
        btnAdd.setBorderPainted(false);
        btnAdd.setText(null);

        JButton btnDelete = new JButton(actionDelete);
        btnDelete.setFocusPainted(false);
        btnDelete.setBorderPainted(false);
        btnDelete.setText(null);

        toolBar.setFloatable(false);
        toolBar.add(btnStartStop);
        toolBar.add(btnAdd);
        toolBar.add(btnDelete);

        frame.setLayout(new GridBagLayout());
        JDUtilities.addToGridBag(frame, toolBar,     0, 0, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH);
        JDUtilities.addToGridBag(frame, tabbedPane,  0, 1, 1, 1, 1, 1, null, GridBagConstraints.BOTH,       GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(frame, statusBar,   0, 2, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
        
        // Einbindung des Log Dialogs
        logDialog = new LogDialog(frame, logger);
        logDialog.addWindowListener(new LogDialogWindowAdapter());
    }
    
    /**
     * Hier werden die Aktionen ausgewertet und weitergeleitet
     *
     * @param e Die erwünschte Aktion
     */
    public void actionPerformed(ActionEvent e){
        switch(e.getID()){
            case JDAction.ITEMS_MOVE_UP:
            case JDAction.ITEMS_MOVE_DOWN:
            case JDAction.ITEMS_MOVE_TOP:
            case JDAction.ITEMS_MOVE_BOTTOM:
                if(tabbedPane.getSelectedComponent() == tabDownloadTable){
                    tabDownloadTable.moveItems(e.getID());
                }
                break;
            case JDAction.APP_START_STOP_DOWNLOADS:
                if(btnStartStop.isSelected())
                    fireUIEvent(new UIEvent(this,UIEvent.UI_START_DOWNLOADS));
                else
                    fireUIEvent(new UIEvent(this,UIEvent.UI_STOP_DOWNLOADS));
                break;
            case JDAction.APP_SAVE:
                fireUIEvent(new UIEvent(this,UIEvent.UI_SAVE_LINKS));
                break;
            case JDAction.APP_LOAD:
                fireUIEvent(new UIEvent(this,UIEvent.UI_LOAD_LINKS));
                break;
            case JDAction.APP_LOG:
                logDialog.setVisible(!logDialog.isVisible());
                break;
            case JDAction.ITEMS_ADD:
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String data;
                try {
                    data = (String)clipboard.getData(DataFlavor.stringFlavor);
                    fireUIEvent(new UIEvent(this,UIEvent.UI_LINKS_TO_PROCESS,data));
                }
                catch (UnsupportedFlavorException e1) {}
                catch (IOException e1)                {}
                break;
            case JDAction.APP_CONFIGURATION:
                boolean configChanged = ConfigurationDialog.showConfig(frame);
                if (configChanged)
                    fireUIEvent(new UIEvent(this,UIEvent.UI_SAVE_CONFIG));
                break;
        }
    }
    public void pluginEvent(PluginEvent event) {
        if(event.getSource() instanceof PluginForHost)
            tabDownloadTable.pluginEvent(event);
        if(event.getSource() instanceof PluginForDecrypt)
            tabPluginActivity.pluginEvent(event);
    }
    public void controlEvent(ControlEvent event){
        switch(event.getID()){
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE:
                setPluginActive((PluginForDecrypt)event.getParameter(), true);
                break;
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE:
                setPluginActive((PluginForDecrypt)event.getParameter(), false);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE:
                setPluginActive((PluginForHost)event.getParameter(), true);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE:
                setPluginActive((PluginForHost)event.getParameter(), false);
                break;
            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                btnStartStop.setSelected(false);
                break;
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED:
                tabDownloadTable.fireTableChanged();
        }
    }
    public Vector<DownloadLink> getDownloadLinks() {
        if(tabDownloadTable != null)
            return tabDownloadTable.getLinks();
        return null;
    }
    public void setDownloadLinks(Vector<DownloadLink> links) {
        if (tabDownloadTable != null)
            tabDownloadTable.setDownloadLinks(links);
        
    }
    public String getCaptchaCodeFromUser(Plugin plugin, String captchaAddress) {
        CaptchaDialog captchaDialog = new CaptchaDialog(frame,plugin,captchaAddress);
        frame.toFront();
        captchaDialog.setVisible(true);
        return captchaDialog.getCaptchaText();
    }
    public Configuration getConfiguration() {
        return null;
    }
    public void setConfiguration(Configuration configuration) {    }
    public void setLogger(Logger logger) {    }
    public void setPluginActive(Plugin plugin, boolean isActive) {   
        if(plugin instanceof PluginForDecrypt){
            statusBar.setPluginForDecryptActive(isActive);
        }
        else{
            statusBar.setPluginForHostActive(isActive);
        }
    }
    public void addUIListener(UIListener listener) {    
        synchronized (uiListener) {
            uiListener.add(listener);
        }
    }
    public void removeUIListener(UIListener listener) {    
        synchronized (uiListener) {
            uiListener.remove(listener);
        }
    }
    public void fireUIEvent(UIEvent uiEvent) {    
        synchronized (uiListener) {
            Iterator<UIListener> recIt = uiListener.iterator();

            while (recIt.hasNext()) {
                ((UIListener) recIt.next()).uiEvent(uiEvent);
            }
        }
    }
    /**
     * Toggled das MenuItem fuer die Ansicht des Log Fensters
     * 
     * @author Tom
     */
    private final class LogDialogWindowAdapter extends WindowAdapter {
       @Override
       public void windowOpened(WindowEvent e) {
          menViewLog.setSelected(true);
       }

       @Override
       public void windowClosed(WindowEvent e) {
          menViewLog.setSelected(false);
       }
    }
    /**
     * Diese Klasse realisiert eine StatusBar
     *
     * @author astaldo
     */
    private class StatusBar extends JPanel{
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 3676496738341246846L;
        private JLabel lblMessage;
        private JLabel lblSpeed;
        private JLabel lblPluginHostActive;
        private JLabel lblPluginDecryptActive;
        private ImageIcon imgActive;
        private ImageIcon imgInactive;

        public StatusBar(){
            imgActive   = new ImageIcon(JDUtilities.getImage("led_green"));
            imgInactive = new ImageIcon(JDUtilities.getImage("led_empty"));

            setLayout(new GridBagLayout());
            lblMessage             = new JLabel(JDUtilities.getResourceString("label.status.welcome"));
            lblSpeed               = new JLabel("450 kb/s");
            lblPluginHostActive    = new JLabel(imgInactive);
            lblPluginDecryptActive = new JLabel(imgInactive);
            lblPluginDecryptActive.setToolTipText(JDUtilities.getResourceString("tooltip.status.plugin_decrypt"));
            lblPluginHostActive.setToolTipText(JDUtilities.getResourceString("tooltip.status.plugin_host"));

            JDUtilities.addToGridBag(this, lblMessage,             0, 0, 1, 1, 1, 1, new Insets(0,5,0,0), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblSpeed,               1, 0, 1, 1, 0, 0, new Insets(0,5,0,5), GridBagConstraints.NONE,       GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblPluginHostActive,    2, 0, 1, 1, 0, 0, new Insets(0,5,0,0), GridBagConstraints.NONE,       GridBagConstraints.EAST);
            JDUtilities.addToGridBag(this, lblPluginDecryptActive, 3, 0, 1, 1, 0, 0, new Insets(0,5,0,5), GridBagConstraints.NONE,       GridBagConstraints.EAST);
        }
        public void setText(String text){
            lblMessage.setText(text);
        }
        /**
         * Zeigt, ob die Plugins zum Downloaden von einem Anbieter arbeiten
         *
         * @param active wahr, wenn Downloads aktiv sind
         */
        public void setPluginForHostActive(boolean active)    { setPluginActive(lblPluginHostActive,    active); }
        /**
         * Zeigt an, ob die Plugins zum Entschlüsseln von Links arbeiten
         *
         * @param active wahr, wenn soeben Links entschlüsselt werden
         */
        public void setPluginForDecryptActive(boolean active) { setPluginActive(lblPluginDecryptActive, active); }
        /**
         * Ändert das genutzte Bild eines Labels, um In/Aktivität anzuzeigen
         *
         * @param lbl Das zu ändernde Label
         * @param active soll es als aktiv oder inaktiv gekennzeichnet werden
         */
        private void setPluginActive(JLabel lbl,boolean active){
            if(active)
                lbl.setIcon(imgActive);
            else
                lbl.setIcon(imgInactive);
        }
    }
}

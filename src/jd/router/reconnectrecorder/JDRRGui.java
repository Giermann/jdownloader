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

package jd.router.reconnectrecorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectMethod;
import jd.gui.UserIO;
import jd.gui.skins.simple.components.JLinkButton;
import jd.nutils.JDFlags;
import jd.nutils.Screen;
import jd.parser.Regex;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class JDRRGui extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JButton btnCancel;

    private JButton btnStart;

    private JTextField routerip;
    private JCheckBox rawmode;
    public boolean saved = false;
    private String ip_before;
    private String ip_after;
    public String RouterIP = null;
    private JButton btnStop;
    public String methode = null, user = null, pass = null;
    private static long check_intervall = 3000;
    private static long reconnect_duration = 0;

    public JDRRGui(JFrame frame, String ip) {
        super(frame);

        RouterIP = ip;

        routerip = new JTextField(RouterIP);

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);

        btnStart = new JButton(JDLocale.L("gui.btn_start", "Start"));
        btnStart.addActionListener(this);

        rawmode = new JCheckBox("RawMode?");
        rawmode.setSelected(false);

        JTextPane infolable = new JTextPane();
        infolable.setEditable(false);
        infolable.setContentType("text/html");
        infolable.addHyperlinkListener(JLinkButton.getHyperlinkListener());
        infolable.setText(JDLocale.L("gui.config.jdrr.infolable", "<span color=\"#4682B4\">Überprüfe die IP-Adresse des Routers und drück auf Start,<br>ein Browserfenster mit der Startseite des Routers öffnet sich,<br>nach dem Reconnect drückst du auf Stop und speicherst.<br>Mehr Informationen gibt es </span><a href=\"http://wiki.jdownloader.org/index.php?title=Recorder\">hier</a>"));

        this.setTitle(JDLocale.L("gui.config.jdrr.title", "Reconnect Recorder"));
        this.setLayout(new MigLayout("wrap 1", "[center]"));
        this.add(new JLabel(JDLocale.L("gui.fengshuiconfig.routerip", "RouterIP") + ":"), "split 3");
        this.add(routerip, "growx");
        this.add(rawmode);
        this.add(infolable, "growx");
        this.add(btnStart, "split 2");
        this.add(btnCancel);
        this.pack();
        this.setLocation(Screen.getCenterOfComponent(null, this));
    }

    private void save() {
        int ret = UserIO.getInstance().requestConfirmDialog(0, JDLocale.L("gui.config.jdrr.success", "Success!"), JDLocale.L("gui.config.jdrr.savereconnect", "Der Reconnect war erfolgreich möchten sie jetzt speichern?"), UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), JDLocale.L("gui.btn_yes", "Ja"), JDLocale.L("gui.btn_no", "Nein"));
        if (JDFlags.hasSomeFlags(ret, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {

            Configuration configuration = JDUtilities.getConfiguration();

            StringBuilder b = new StringBuilder();
            for (String element : JDRR.steps) {
                b.append(element + System.getProperty("line.separator"));
            }
            methode = b.toString().trim();

            if (JDRR.auth != null) {
                user = new Regex(JDRR.auth, "(.+?):").getMatch(0);
                pass = new Regex(JDRR.auth, ".+?:(.+)").getMatch(0);
                configuration.setProperty(Configuration.PARAM_HTTPSEND_USER, user);
                configuration.setProperty(Configuration.PARAM_HTTPSEND_PASS, pass);
            }
            btnCancel.setText(JDLocale.L("gui.config.jdrr.close", "Schließen"));
            configuration.setProperty(Configuration.PARAM_HTTPSEND_IP, routerip.getText().trim());
            configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, methode);
            configuration.setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, "Reconnect Recorder Methode");
            configuration.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER);
            if (reconnect_duration <= 2000) {
                reconnect_duration = 2000;
                /* minimum von 2 seks */
            }
            configuration.setProperty(ReconnectMethod.PARAM_WAITFORIPCHANGE, ((reconnect_duration / 1000) * 2) + 10);
            configuration.setProperty(ReconnectMethod.PARAM_IPCHECKWAITTIME, ((reconnect_duration / 1000) / 2) + 2);
            configuration.save();
            saved = true;
            dispose();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnStart && JDRR.running == false) {
            if (routerip.getText() != null && !routerip.getText().matches("\\s*")) {
                String host = routerip.getText().trim();
                boolean startwithhttps = false;
                if (host.contains("https")) startwithhttps = true;
                host = host.replaceAll("http://", "").replaceAll("https://", "");
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, host);

                ip_before = JDUtilities.getIPAddress(null);
                JDRR.startServer(host, rawmode.isSelected());

                try {
                    if (startwithhttps) {
                        JLinkButton.openURL("http://localhost:" + (SubConfiguration.getConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972) + 1));
                    } else {
                        JLinkButton.openURL("http://localhost:" + (SubConfiguration.getConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972)));
                    }
                } catch (Exception e1) {
                    JDLogger.exception(e1);
                }
                final JDRRInfoPopup popup = new JDRRInfoPopup();
                popup.startCheck();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        popup.setVisible(true);
                    }
                });
                return;
            }
        }
        dispose();
    }

    public class JDRRInfoPopup extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;
        private long reconnect_timer = 0;
        private RRStatus statusicon;

        public JDRRInfoPopup() {
            super();
            setModal(true);
            setLayout(new MigLayout("wrap 1", "[center, grow, fill]"));
            btnStop = new JButton(JDLocale.L("gui.btn_abort", "Abort"));
            btnStop.addActionListener(this);
            statusicon = new RRStatus();
            this.add(statusicon, "w 32!, h 32!");
            this.add(btnStop);
            setDefaultCloseOperation(JDRRInfoPopup.DO_NOTHING_ON_CLOSE);

            setResizable(false);
            setUndecorated(true);
            setTitle("RRStatus");
            setLocation(20, 20);
            setAlwaysOnTop(true);
            pack();
        }

        public void startCheck() {
            new Thread() {
                public void run() {
                    statusicon.setStatus(0);
                    this.setName(JDLocale.L("gui.config.jdrr.popup.title", "JDRRPopup"));
                    reconnect_timer = 0;
                    while (JDRR.running) {
                        try {
                            Thread.sleep(check_intervall);
                        } catch (Exception e) {
                        }
                        ip_after = JDUtilities.getIPAddress(null);
                        if (ip_after.contains("offline") && reconnect_timer == 0) {
                            reconnect_timer = System.currentTimeMillis();
                        }
                        if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                            statusicon.setStatus(1);
                            if (JDRR.running == true) closePopup();
                            return;
                        }
                    }
                }
            }.start();
        }

        public class RRStatus extends JLabel {

            private static final long serialVersionUID = -3280613281656283625L;

            private ImageIcon imageProgress;

            private ImageIcon imageBad;

            private ImageIcon imageGood;

            public RRStatus() {
                imageProgress = JDTheme.II("gui.images.reconnect", 32, 32);
                imageBad = JDTheme.II("gui.images.unselected", 32, 32);
                imageGood = JDTheme.II("gui.images.selected", 32, 32);
                setStatus(0);
            }

            public void setStatus(int state) {
                if (state == 0) {
                    this.setIcon(imageProgress);
                } else if (state == 1) {
                    this.setIcon(imageGood);
                } else {
                    this.setIcon(imageBad);
                }
            }
        }

        public void closePopup() {
            JDRR.stopServer();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    btnStop.setEnabled(false);

                    ip_after = JDUtilities.getIPAddress(null);
                    if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                        if (reconnect_timer == 0) {
                            /*
                             * Reconnect fand innerhalb des Check-Intervalls
                             * statt
                             */
                            reconnect_duration = check_intervall;
                        } else {
                            reconnect_duration = System.currentTimeMillis() - reconnect_timer;
                        }
                        JDLogger.getLogger().info("dauer: " + reconnect_duration + "");
                        statusicon.setStatus(1);
                    } else {
                        statusicon.setStatus(-1);
                    }
                    if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                        save();
                    } else {
                        // save(); /*zu debugzwecken*/
                        JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.jdrr.reconnectfaild", "Reconnect failed"));
                    }

                    dispose();
                }
            });
        }

        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getSource() == btnStop) {
                closePopup();
            }
        }
    }

}
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

package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdesktop.swingx.renderer.PainterAware;
import org.jdesktop.swingx.table.TableColumnExt;

public class TreeTableRenderer extends DefaultTableRenderer implements PainterAware {

    private static final long serialVersionUID = -3912572910439565199L;

    private DecimalFormat c = new DecimalFormat("0.00");

    private Component co;

    private DownloadLink dLink;

    private FilePackage fp;

    private JDProgressBar progress;

    private DownloadTreeTable table;

    private ImageIcon icon_fp_closed;

    private ImageIcon icon_fp_open;

    private ImageIcon icon_link;

    private String strPluginDisabled;

    private Painter painter;

    private String strFilePackageStatusFinished;

    private String strETA;

    private StringBuilder sb = new StringBuilder();

    private String strDownloadLinkActive;

    private String strPluginError;

    private String strSecondsAbrv;

    private String strParts;

    private TableColumnExt col;

    private String strFile;

    private Border leftGap;

    private static Color COL_PROGRESS_ERROR = new Color(0xCC3300);

    TreeTableRenderer(DownloadTreeTable downloadTreeTable) {

        icon_link = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.link")));

      
        icon_fp_open = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.package_closed")));

        icon_fp_closed = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.package_opened")));

  

        table = downloadTreeTable;
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        progress = new JDProgressBar();
 
        progress.setStringPainted(true);
        progress.setOpaque(true);
        initLocale();

    }

    private void initLocale() {
        this.strPluginDisabled = JDLocale.L("gui.downloadlink.plugindisabled", "[Plugin disabled]");
        strFilePackageStatusFinished = JDLocale.L("gui.filepackage.finished", "[finished]");
        this.strDownloadLinkActive = JDLocale.L("gui.treetable.packagestatus.links_active", "aktiv");
        this.strETA = JDLocale.L("gui.eta", "ETA");
        strPluginError = null;
        strSecondsAbrv = null;
        this.strParts = JDLocale.L("gui.treetable.parts", "Teil(e)");
        strFile = null;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getColumn(column).getModelIndex();
        if (value instanceof FilePackage) {
            return getFilePackageCell(table, value, isSelected, hasFocus, row, column);

        } else if (value instanceof DownloadLink) {
            return getDownloadLinkCell(table, value, isSelected, hasFocus, row, column);
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

    }

    private Component getDownloadLinkCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        dLink = (DownloadLink) value;
        switch (column) {
        case DownloadTreeTableModel.COL_PART:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            clearSB();
            ((JRendererLabel) co).setIcon(icon_link);
            ((JRendererLabel) co).setText(strFile);
            ((JRendererLabel) co).setBorder(leftGap);

            return co;
        case DownloadTreeTableModel.COL_HOSTER:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            clearSB();
            ((JRendererLabel) co).setBorder(null);
            sb.append(dLink.getPlugin().getHost());
            sb.append(dLink.getPlugin().getSessionInfo());
            ((JRendererLabel) co).setText(sb.toString());

            return co;

        case DownloadTreeTableModel.COL_FILE:
            value=dLink.getName();
            break;
        case DownloadTreeTableModel.COL_PROGRESS:

            if (dLink.getPlugin() == null) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginError);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (!dLink.getPlugin().getWrapper().usePlugin()) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginDisabled);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (dLink.getPluginProgress() != null) {
                col = this.table.getCols()[column];
                if (col.getWidth() < 40) {

                } else if (col.getWidth() < 150) {
                    progress.setString(dLink.getPluginProgress().getPercent() + "%");

                } else {
                    progress.setString(dLink.getPluginProgress().getPercent() + "%");
                }

                progress.setMaximum(dLink.getPluginProgress().getTotal());
                progress.setValue(dLink.getPluginProgress().getCurrent());
                return progress;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && dLink.getPlugin().getRemainingHosterWaittime() > 0) || (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && dLink.getLinkStatus().getRemainingWaittime() > 0)) {
                progress.setMaximum(dLink.getLinkStatus().getTotalWaitTime());
                progress.setForeground(COL_PROGRESS_ERROR);
                progress.setValue(dLink.getLinkStatus().getRemainingWaittime());
                this.clearSB();
                col = ((TableColumnExt) table.getColumnModel().getColumn(column));
                if (col.getWidth() < 60) {

                } else if (col.getWidth() < 150) {
                    sb.append(c.format(10000 * progress.getPercentComplete() / 100.0)).append('%');

                } else {
                    sb.append(c.format(10000 * progress.getPercentComplete() / 100.0)).append("% (").append(progress.getValue() / 1000).append('/').append(progress.getMaximum() / 1000).append(strSecondsAbrv).append(')');

                }
                progress.setString(sb.toString());

                return progress;
            } else if (dLink.getDownloadCurrent() > 0) {
                if (!dLink.getLinkStatus().isPluginActive()) {
                    if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        col = this.table.getCols()[column];
                        if (col.getWidth() < 40) {

                        } else if (col.getWidth() < 150) {
                            progress.setString("- 100% -");

                        } else {
                            progress.setString("- 100% -");
                        }

                    } else {
                        progress.setString("");
                    }
                } else {

                    if (dLink.getLinkStatus().hasStatus(LinkStatus.WAITING_USERIO)) {
                        progress.setString(SimpleGUI.WAITING_USER_IO);
                    } else {
                        this.clearSB();
                        col = this.table.getCols()[column];
                        if (col.getWidth() < 60) {

                        } else if (col.getWidth() < 150) {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append('%');

                        } else {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append("% (").append(JDUtilities.formatBytesToMB(dLink.getDownloadCurrent())).append('/').append(JDUtilities.formatBytesToMB(Math.max(1, dLink.getDownloadSize()))).append(')');

                        }
                        progress.setString(sb.toString());
                    }
                }
                progress.setMaximum(10000);

                progress.setValue(dLink.getPercent());
                return progress;
            }
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setIcon(null);
            ((JRendererLabel) co).setText("");
            ((JRendererLabel) co).setBorder(null);
            return co;

        case DownloadTreeTableModel.COL_STATUS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            ((JRendererLabel) co).setIcon(null);

            ((JRendererLabel) co).setText(dLink.getLinkStatus().getStatusString());
            ((JRendererLabel) co).setBorder(null);

            return co;

        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        return co;
    }

    private Component getFilePackageCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        fp = (FilePackage) value;
        switch (column) {
        case DownloadTreeTableModel.COL_PART:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName());
            ((JRendererLabel) co).setIcon(fp.getBooleanProperty(DownloadTreeTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);
            ((JRendererLabel) co).setBorder(null);
            return co;

        case DownloadTreeTableModel.COL_HOSTER:
            value = fp.getHoster();

            break;

        case DownloadTreeTableModel.COL_FILE:

            clearSB();
            sb.append(fp.getDownloadLinks().size()).append(' ').append(strParts);
            value = sb.toString();
            break;
        case DownloadTreeTableModel.COL_PROGRESS:

            if (fp.isFinished()) {
                progress.setMaximum(100);
                progress.setValue(100);

                col = this.table.getCols()[column];
                if (col.getWidth() < 40) {

                } else if (col.getWidth() < 150) {
                    progress.setString("- 100% -");

                } else {
                    progress.setString("- 100% -");
                }
            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setValue(fp.getTotalKBLoaded());
                clearSB();
                col = this.table.getCols()[column];
                if (col.getWidth() < 40) {

                } else if (col.getWidth() < 150) {
                    sb.append(c.format(fp.getPercent())).append('%');

                } else {
                    sb.append(c.format(fp.getPercent())).append("% (").append(JDUtilities.formatKbReadable(progress.getValue())).append('/').append(JDUtilities.formatKbReadable(Math.max(1, fp.getTotalEstimatedPackageSize()))).append(')');

                }
                progress.setString(sb.toString());
            }

            return progress;

        case DownloadTreeTableModel.COL_STATUS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (fp.isFinished()) {
                ((JRendererLabel) co).setText(strFilePackageStatusFinished);
            } else if (fp.getTotalDownloadSpeed() > 0) {
                clearSB();
                sb.append('[').append(fp.getLinksInProgress()).append('/').append(fp.size()).append("] ");
                sb.append(this.strETA).append(' ').append(JDUtilities.formatSeconds(fp.getETA())).append(" @ ").append(JDUtilities.formatKbReadable(fp.getTotalDownloadSpeed() / 1024)).append("/s");
                ((JRendererLabel) co).setText(sb.toString());
            } else if (fp.getLinksInProgress() > 0) {
                clearSB();
                sb.append(fp.getLinksInProgress()).append('/').append(fp.size()).append(strDownloadLinkActive);
                ((JRendererLabel) co).setText(sb.toString());
            } else {
                ((JRendererLabel) co).setText("");
            }
            ((JRendererLabel) co).setBorder(null);
            return co;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        return co;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity() - 1);

    }

    public Painter getPainter() {
        // TODO Auto-generated method stub
        return painter;
    }

    public void setPainter(Painter painter) {
        this.painter = painter;

    }

}
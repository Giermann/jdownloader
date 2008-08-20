//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelPluginForHost extends ConfigPanel implements ActionListener, MouseListener, DropTargetListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.config.plugin.host.column_id2", "ID");
            case 1:
                return JDLocale.L("gui.config.plugin.host.column_host2", "Host");
            case 2:
                return JDLocale.L("gui.config.plugin.host.column_version2", "Version");
            case 3:
                return JDLocale.L("gui.config.plugin.host.column_coder2", "Ersteller");
            case 4:
                return JDLocale.L("gui.config.plugin.host.column_agb2", "AGB");
            case 5:
                return JDLocale.L("gui.config.plugin.host.column_agbChecked2", "akzeptieren");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return pluginsForHost.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return pluginsForHost.elementAt(rowIndex).getPluginName();
            case 2:
                return pluginsForHost.elementAt(rowIndex).getVersion();
            case 3:
                return pluginsForHost.elementAt(rowIndex).getCoder();
            case 4:
                return new JLinkButton(JDLocale.L("gui.config.plugin.host.readAGB", "AGB"), pluginsForHost.elementAt(rowIndex).getAGBLink());
            case 5:
                return pluginsForHost.elementAt(rowIndex).isAGBChecked();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex > 2;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if ((Boolean) value) {
                String msg = String.format(JDLocale.L("gui.config.plugin.abg_confirm", "Ich habe die AGB/TOS/FAQ von %s gelesen und erkläre mich damit einverstanden!"), pluginsForHost.elementAt(row).getHost());
                if (JOptionPane.showConfirmDialog(ConfigurationDialog.DIALOG, msg) == JOptionPane.OK_OPTION) {
                    pluginsForHost.elementAt(row).setAGBChecked((Boolean) value);
                }
            } else {
                pluginsForHost.elementAt(row).setAGBChecked((Boolean) value);
            }
        }
    }

    private class JLinkButtonEditor implements TableCellEditor, ActionListener {

        private JLinkButton btn;

        private boolean stop = false;

        public void actionPerformed(ActionEvent e) {
            stop = true;
            table.tableChanged(new TableModelEvent(table.getModel()));
        }

        public void addCellEditorListener(CellEditorListener l) {
        }

        public void cancelCellEditing() {
        }

        public Object getCellEditorValue() {
            return null;
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            btn = (JLinkButton) value;
            btn.addActionListener(this);
            return btn;
        }

        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        public void removeCellEditorListener(CellEditorListener l) {
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return false;
        }

        public boolean stopCellEditing() {
            return stop;
        }

    }

    private class JLinkButtonRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (JLinkButton) value;
        }
    }

    private static final long serialVersionUID = -5219586497809869375L;

    private JButton btnEdit;

    private Configuration configuration;

    private PluginForHost draggedPlugin;

    private Vector<PluginForHost> pluginsForHost;

    private JTable table;

    public ConfigPanelPluginForHost(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        pluginsForHost = JDUtilities.getPluginsForHost();
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnEdit) {
            editEntry();
        }
    }

    public void dragEnter(DropTargetDragEvent e) {
        draggedPlugin = pluginsForHost.get(table.getSelectedRow());
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent e) {
        int id = table.rowAtPoint(e.getLocation());
        pluginsForHost.remove(draggedPlugin);
        pluginsForHost.add(id, draggedPlugin);
        table.tableChanged(new TableModelEvent(table.getModel()));
        table.getSelectionModel().setSelectionInterval(id, id);
    }

    public void drop(DropTargetDropEvent e) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    private void editEntry() {
        PluginForHost plugin = getSelectedPlugin();
        ConfigPanel config = new ConfigEntriesPanel(plugin.getConfig(), JDLocale.LF("gui.config.plugin.host.dialogname", "%s Configuration", plugin.getPluginName()));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JPanel(), BorderLayout.NORTH);
        panel.add(config, BorderLayout.CENTER);

        ConfigurationPopup pop = new ConfigurationPopup(JDUtilities.getParentFrame(this), config, panel, uiinterface, configuration);
        pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
        pop.setVisible(true);
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.plugin.host.name", "Host Plugins");
    }

    private PluginForHost getSelectedPlugin() {
        int index = table.getSelectedRow();
        if (index < 0) return null;
        return pluginsForHost.elementAt(index);
    }

    @Override
    public void initPanel() {
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(650, 350));

        table = new JTable();
        InternalTableModel internalTableModel = new InternalTableModel();
        table.setModel(internalTableModel);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                btnEdit.setEnabled(pluginsForHost.get(table.getSelectedRow()).getConfig().getEntries().size() != 0);
            }
        });
        table.setDragEnabled(true);
        new DropTarget(table, this);

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(30);
                column.setMaxWidth(30);
                column.setMinWidth(30);
                break;
            case 1:
                column.setPreferredWidth(250);
                break;
            case 2:
                column.setPreferredWidth(60);
                column.setMinWidth(60);
                break;
            case 3:
                column.setPreferredWidth(250);
                break;
            case 4:
                column.setPreferredWidth(70);
                column.setMaxWidth(70);
                column.setMinWidth(70);
                column.setCellRenderer(new JLinkButtonRenderer());
                column.setCellEditor(new JLinkButtonEditor());
                break;
            case 5:
                column.setPreferredWidth(90);
                column.setMaxWidth(90);
                column.setMinWidth(90);
                break;
            }
        }

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        btnEdit = new JButton(JDLocale.L("gui.config.plugin.host.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);

        JDUtilities.addToGridBag(panel, scrollpane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(panel, btnEdit, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        add(new JLabel("<html><body align=\"justify\" color=\"red\"><b>" + JDLocale.L("gui.config.plugin.host.desc", "ACHTUNG!! Das JD Team übernimmt keine Verantwortung für die Einhaltung der AGB \r\n der Hoster. Bitte lesen Sie die AGB aufmerksam und aktivieren Sie das Plugin nur,\r\nfalls Sie sich mit diesen Einverstanden erklären!\r\nDie Reihenfolge der Plugins bestimmt die Prioritäten der automatischen Mirrorauswahl\n\rBevorzugte Hoster sollten oben stehen!")), BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void load() {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1 && table.getSelectedRow() != -1 && pluginsForHost.get(table.getSelectedRow()).getConfig().getEntries().size() != 0) {
            editEntry();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void save() {
        Vector<String> priority = new Vector<String>();
        for (PluginForHost plg : pluginsForHost) {
            priority.add(plg.getHost());
            if (plg.getPluginConfig() != null) {
                configuration.setProperty("PluginConfig_" + plg.getPluginName(), plg.getPluginConfig());
            }
        }
        configuration.setProperty(Configuration.PARAM_HOST_PRIORITY, priority);
    }

}

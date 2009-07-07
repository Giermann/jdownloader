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

package jd.controlling.interaction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
import jd.utils.JDUtilities;
import jd.utils.Replacer;
import jd.utils.locale.JDL;

public class SimpleExecute extends Interaction implements Serializable, ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String PROPERTY_COMMAND = "PROPERTY_COMMAND";
    private static final String PROPERTY_EXECUTE_IN = "PROPERTY_EXECUTE_IN";
    private static final String PROPERTY_PARAMETER = "PROPERTY_PARAMETER";
    private static final String PROPERTY_USE_EXECUTE_IN = "PROPERTY_USE_EXECUTE_IN";
    private static final String PROPERTY_WAIT_TERMINATION = "PROPERTY_WAIT_TERMINATION";
    private static final String PROPERTY_WAITTIME = "PROPERTY_WAITTIME";

    public void actionPerformed(ActionEvent e) {
        getConfig().requestSave();
        doInteraction(null);
    }

    // @Override
    public boolean doInteraction(Object arg) {
        String command = Replacer.insertVariables(getStringProperty(PROPERTY_COMMAND));
        String parameter = Replacer.insertVariables(getStringProperty(PROPERTY_PARAMETER));

        logger.info(getStringProperty(PROPERTY_COMMAND));
        File path = new File(command);

        if (!path.exists()) {
            String[] params = Regex.getLines(parameter);
            if (params.length > 0) {
                path = new File(params[0]);
            }

        }
        String executeIn = path.getParent();
        if (getStringProperty(PROPERTY_EXECUTE_IN, null) != null && getStringProperty(PROPERTY_EXECUTE_IN, null).length() > 0 && getBooleanProperty("PROPERTY_USE_EXECUTE_IN", false)) {
            executeIn = getStringProperty(PROPERTY_EXECUTE_IN, null);
        }

        logger.finer("Execute Returns: " + JDUtilities.runCommand(command, Regex.getLines(parameter), executeIn, this.getBooleanProperty(PROPERTY_WAIT_TERMINATION, false) ? getIntegerProperty(PROPERTY_WAITTIME, 60) : 0));
        return true;
    }

    // @Override
    public String getInteractionName() {
        return JDL.L("interaction.simpleExecute.name", "Programm/Script ausführen");
    }

    // @Override
    public void initConfig() {
        ConfigEntry cfg;
        ConfigEntry conditionEntry;

        ConfigContainer extended = new ConfigContainer(JDL.L("interaction.simpleExecute.extended", "Erweiterte Einstellungen"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, extended));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDL.L("interaction.simpleExecute.test", "Jetzt ausführen"),JDL.L("interaction.simpleExecute.test.long", "Test program execution"),null));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, this, PROPERTY_COMMAND, JDL.L("interaction.simpleExecute.cmd", "Befehl")));

        extended.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, this, PROPERTY_PARAMETER, JDL.L("interaction.simpleExecute.parameter", "Parameter (1 Parameter pro Zeile)")));

        config.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, PROPERTY_WAIT_TERMINATION, JDL.L("interaction.simpleExecute.waitForTermination", "Warten bis Befehl beendet wurde")).setDefaultValue(false));
        extended.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PROPERTY_WAITTIME, JDL.L("interaction.simpleExecute.waittime", "Maximale Ausführzeit"), 0, 60 * 60 * 24).setDefaultValue(60));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);

        extended.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, PROPERTY_USE_EXECUTE_IN, JDL.L("interaction.simpleExecute.useexecutein", "Benutzerdefiniertes 'Ausführen in'")).setDefaultValue(false));

        extended.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, this, PROPERTY_EXECUTE_IN, JDL.L("interaction.simpleExecute.executein", "Ausführen in (Ordner)")));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);
    }

    // @Override
    public String toString() {
        return JDL.L("interaction.simpleExecute.name", "Programm/Script ausführen");
    }

}

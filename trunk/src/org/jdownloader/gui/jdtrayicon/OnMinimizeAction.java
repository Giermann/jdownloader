package org.jdownloader.gui.jdtrayicon;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public enum OnMinimizeAction {
    TO_TRAY(_TRAY.T.OnMinimizeAction_totray()),
    TO_TASKBAR(_TRAY.T.OnMinimizeAction_totaskbar());

    private String translation;

    public String getTranslation() {
        return translation;
    }

    private OnMinimizeAction(String translation) {
        this.translation = translation;
    }
}

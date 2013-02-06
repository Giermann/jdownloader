package org.jdownloader.gui.laf;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import de.javasoft.util.OS;

public class JDDefaultLookAndFeel extends SyntheticaLookAndFeel {

    private static final long serialVersionUID = -6728461590292436336L;

    // Construtor
    public JDDefaultLookAndFeel() throws java.text.ParseException {
        // load synth.xml from custom package
        super("/themes/standard/org/jdownloader/laf/org.jdownloader.gui.laf.JDDefaultLookAndFeel/synthetica/xml/synth.xml");
        OS.getCurrentOS();
    }

    // return an unique LAF id
    public String getID() {
        return "SyntheticaJDVisionLookAndFeel";
    }

    // return the LAF name - readable for humans
    public String getName() {
        return "SyntheticaJDVisionLookAndFeel Custom Look and Feel";
    }

}

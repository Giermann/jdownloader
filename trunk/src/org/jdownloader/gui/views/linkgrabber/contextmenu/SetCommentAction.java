package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SetCommentAction extends AppAction {

    private AbstractNode            contextObject;
    private ArrayList<AbstractNode> selection;

    public SetCommentAction(AbstractNode contextObject, ArrayList<AbstractNode> selection) {
        this.contextObject = contextObject;
        this.selection = selection;
        setName(_GUI._.SetCommentAction_SetCommentAction_object_());
        setIconKey("list");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String def = null;
        for (AbstractNode n : selection) {
            if (n instanceof DownloadLink) {
                def = ((DownloadLink) n).getComment();
            } else if (n instanceof CrawledLink) {
                def = ((CrawledLink) n).getDownloadLink().getComment();
            } else if (n instanceof FilePackage) {
                def = ((FilePackage) n).getComment();
            } else if (n instanceof CrawledPackage) {
                def = ((CrawledPackage) n).getComment();
            }
            if (!StringUtils.isEmpty(def)) break;
        }

        try {
            String comment = Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, _GUI._.SetCommentAction_actionPerformed_dialog_title_(), "", def, null, null, null);
            for (AbstractNode n : selection) {
                if (n instanceof DownloadLink) {
                    ((DownloadLink) n).setComment(comment);
                } else if (n instanceof CrawledLink) {
                    ((CrawledLink) n).getDownloadLink().setComment(comment);
                } else if (n instanceof FilePackage) {
                    ((FilePackage) n).setComment(comment);
                } else if (n instanceof CrawledPackage) {
                    ((CrawledPackage) n).setComment(comment);
                }
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

    }

}

package jd.utils;

/*
 * SwingHTMLPreview.java
 *
 * Created on December 29, 2001, 9:25 PM
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;


/**
 * SwingHTMLPreview
 * little tool for testing the Swing HTML rendering
 * @author  Gordon Mohr gojomo@bitzi.com gojomo@usa.net
 *
 */
public class SwingHTMLPreview extends javax.swing.JFrame implements DocumentListener {
    JTextArea top;
    JEditorPane bottom;
    
    /** Creates a new instance of SwingHTMLPreview */
    public SwingHTMLPreview() {
    }
    
    public static void main(String args[]) {
        SwingHTMLPreview instance = new SwingHTMLPreview();
        instance.init();
        instance.show();
    }

    public void init() {
        Container content = getContentPane();
        content.setBackground(Color.white);
        content.setLayout(new GridLayout(2,1)); 
        
        top = new JTextArea();
        top.setEditable(true);
        
        top.setLineWrap(true);
        top.setWrapStyleWord(true);
        top.getDocument().addDocumentListener(this);
        JScrollPane topScrollPane = new JScrollPane(top);
        topScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        topScrollPane.setBorder(BorderFactory.createTitledBorder("Raw HTML"));      
        
        bottom = new JEditorPane();
        bottom.setEditable(false);
        bottom.setContentType("text/html");
        JScrollPane bottomScrollPane = new JScrollPane(bottom);
        bottomScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        bottomScrollPane.setBorder(BorderFactory.createTitledBorder("Swing Rendered"));      
        
        content.add(topScrollPane);
        content.add(bottomScrollPane);
        setTitle("SwingHTMLPreview");
        setSize(400,400);
    }
    
    public void insertUpdate(DocumentEvent e) {
        copyContents(e);
    }
    public void removeUpdate(DocumentEvent e) {
        copyContents(e);
    }
    public void changedUpdate(DocumentEvent e) {
        copyContents(e);
    }
    private void copyContents(DocumentEvent e) {
        String backup = bottom.getText();
        // some bad HTML causes the JEditorPane to choke;
        // ignoring the error isn't fatal but causes visual
        // glitching until error is fixed
        // SO, catch it and give clue (via red insertion caret)
        // that something is amiss
        try {
            bottom.setText(top.getText());
            top.setCaretColor(Color.black);
        } catch (Exception ex) {
            bottom.setText(backup);
            top.setCaretColor(Color.red);
        }
    }


}

package com.offbynull.overlay.visualizer;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public final class VisualizeUtils {
    private VisualizeUtils() {
        // do nothing
    }
    
    public static JFrame displayInWindow(String title, VisualizeComponent component) {
        JFrame jframe = new JFrame("" + title);
        
        jframe.getContentPane().add(component);
        jframe.setSize(400, 400);
        jframe.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        jframe.setVisible(true);
        
        return jframe;
    }
}

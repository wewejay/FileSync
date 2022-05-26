package com.we1u.filesync.listeners;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.we1u.filesync.Main.mainInstance;

public class MMouseListener implements MouseListener {

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1){
            Component source = (Component) e.getSource();
            switch (mainInstance.actionMap.get(source)) {
                //Left File Chooser
                case "lfc":
                    mainInstance.chooseFile(0);
                    break;
                //Right File Chooser
                case "rfc":
                    mainInstance.chooseFile(1);
                    break;
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}

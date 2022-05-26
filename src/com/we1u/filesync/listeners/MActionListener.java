package com.we1u.filesync.listeners;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.we1u.filesync.Main.mainInstance;

public class MActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (mainInstance.actionMap.get((Component) e.getSource())) {
            //Compare button
            case "compare":
                mainInstance.updateTree();
                break;
            //Sync button
            case "sync":
                mainInstance.sync();
                break;
            //Settings button
            case "settings":
                mainInstance.openSettingsDialog();
                break;
            //Interrupt button
            case "interrupt":
                mainInstance.interrupt();
                break;
            //Save button in settings dialog
            case "save":
                mainInstance.saveSettings();
                break;
        }
    }


}

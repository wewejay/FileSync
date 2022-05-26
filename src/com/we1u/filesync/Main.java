package com.we1u.filesync;


import com.sun.nio.file.ExtendedCopyOption;
import com.we1u.filesync.configs.FileComparatorConfig;
import com.we1u.filesync.configs.SyncerConfig;
import com.we1u.filesync.listeners.MActionListener;
import com.we1u.filesync.listeners.MMouseListener;
import com.we1u.filesync.logging.LogSeverity;
import com.we1u.filesync.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    //Gui Components
    JFrame mainFrame;
    Container mainPane;
    JTextField tFFileChooser1;
    JTextField tFFileChooser2;
    JTree tree1;
    JTree tree2;
    DefaultMutableTreeNode root1;
    DefaultMutableTreeNode root2;
    JScrollPane sPTree1;
    JScrollPane sPTree2;
    JPanel pButtons;
    JButton bCompare;
    JButton bSync;
    JButton bSettings;
    JButton bInterrupt;
    JTextArea tALog;
    JScrollPane sPLog;
    JFileChooser fileChooser;

    JDialog settingsDialog;
    JTabbedPane dTabbedPane;
    Container dPane;
    JPanel dPCompareSettings;
    JPanel dPSyncSettings;
    JButton dSaveButton;
    JCheckBox dCBCheckSize;
    JCheckBox dCBCheckModTime;
    JCheckBox dCBCheckContent;
    JLabel dLExtensionFilter;
    JTextArea dTAExtensions;
    JScrollPane dSPTAExtensions;
    ButtonGroup dButtonGroup;
    JRadioButton dRBOnly;
    JRadioButton dRBExclude;
    JCheckBox dCBOneToTwo;
    JCheckBox dCBTwoToOne;
    
    //GUI Config
    int mainPanelInset = 10;
    Insets stdInsets;
    int fileChooserHeight = 10;
    int buttonHeight = 20;
    Dimension framePSize = new Dimension(600, 600);
    Dimension frameMinSize = new Dimension(600, 600);
    Font customFont = new Font("Monospaced", Font.PLAIN, 12);

    //Actions
    MActionListener actionListener = new MActionListener();
    MMouseListener mouseListener = new MMouseListener();
    public ConcurrentHashMap<Component, String> actionMap = new ConcurrentHashMap<>();

    //Main variables
    public static Main mainInstance;
    Path rootPath1;
    Path rootPath2;
    boolean selected1 = false;
    boolean selected2 = false;
    DirComparator dirComparator1;
    DirComparator dirComparator2;
    Logger logger;
    Thread workingThread;
    FileComparatorConfig comparatorConfig;
    SyncerConfig syncerConfig;

    public Main(){
        Main.mainInstance = this;
        initConfig();
        initFrame();
        initActions();
        initLogger();
    }

    //Check if working thread is active
    private boolean notWorking(){
        if (workingThread == null)
            return true;
        return !workingThread.isAlive();
    }

    //Open FileChooser and apply the choice
    public void chooseFile(int chooser){
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int rtnVal = fileChooser.showOpenDialog(mainFrame);
        if (rtnVal == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            Path path = file.toPath().toAbsolutePath();
            if (chooser == 0){
                tFFileChooser1.setText(path.toString());
                rootPath1 = path;
                selected1 = true;
            }
            else if (chooser == 1){
                tFFileChooser2.setText(path.toString());
                rootPath2 = path;
                selected2 = true;
            }
            if (selected1 && selected2){
                bCompare.setEnabled(true);
                bSync.setEnabled(true);
            }
        }
    }

    //Set default settings
    private void initConfig(){
        comparatorConfig = new FileComparatorConfig(
                true,
                false,
                true,
                new ArrayList<>(),
                false
        );
        syncerConfig = new SyncerConfig(
                true,
                false
        );
    }

    //Initiate Logger
    private void initLogger(){
        logger = new Logger(tALog, true, true, true);
        logger.log("GUI started.", LogSeverity.INFO);
    }

    //Compare and update JTree
    public void updateTree(){
        if (notWorking()){
            logger.log("Started Comparison...", LogSeverity.INFO);
            workingThread = new Thread(() -> {
                dirComparator1 = new DirComparator(rootPath2, rootPath1, comparatorConfig);
                dirComparator2 = new DirComparator(rootPath1, rootPath2, comparatorConfig);
                try{
                    Files.walkFileTree(rootPath1, dirComparator1);
                    Files.walkFileTree(rootPath2, dirComparator2);
                }
                catch (IOException e){
                    e.printStackTrace();
                }

                if (dirComparator1.terminated || dirComparator2.terminated){
                    logger.log("Comparison Unsuccessful.", LogSeverity.ERROR);
                } else{
                    root1 = new DefaultMutableTreeNode(rootPath1.getFileName().toString());
                    root2 = new DefaultMutableTreeNode(rootPath2.getFileName().toString());

                    model(root1, dirComparator1.dirPaths);
                    model(root1, dirComparator1.filePaths);
                    model(root2, dirComparator2.dirPaths);
                    model(root2, dirComparator2.filePaths);

                    tree1.setModel(new DefaultTreeModel(root1));
                    tree2.setModel(new DefaultTreeModel(root2));

                    logger.log("Comparison Successful.", LogSeverity.INFO);
                }
            });
            workingThread.start();
        }
    }

    //Create tree from list of paths
    private void model(DefaultMutableTreeNode rootNode, List<Path> paths){
        for (Path path : paths) {
            String[] list = path.toString().split(Pattern.quote(File.separator));
            List<DefaultMutableTreeNode> nodes = new ArrayList<>();
            nodes.add(rootNode);
            for (int j = 0; j < list.length; j++) {
                boolean exists = false;
                Enumeration<TreeNode> children = nodes.get(j).children();
                String newNode = list[j];
                while (children.hasMoreElements()) {
                    DefaultMutableTreeNode tmpNode = (DefaultMutableTreeNode) children.nextElement();
                    String tmpInfo = tmpNode.getUserObject().toString();
                    if (tmpInfo.equals(list[j])) {
                        nodes.add(tmpNode);
                        exists = true;
                    }
                }
                if (!exists) {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(newNode);
                    nodes.add(node);
                    nodes.get(j).add(node);
                }
            }
        }
    }

    //Initialize Main Frame
    private void initFrame(){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        mainFrame = new JFrame("FileSync");
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (notWorking()){
                    System.exit(0);
                }
                else{
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Operation still running, wait for the process to end or interrupt it.",
                            "Process Still Running",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        });
        mainPane = mainFrame.getContentPane();
        fileChooser = new JFileChooser();
        mainPane.setLayout(new GridBagLayout());
        stdInsets = new Insets(mainPanelInset, mainPanelInset, mainPanelInset, mainPanelInset);
        
        //Left File Chooser
        tFFileChooser1 = new JTextField("Choose Folder...");
        tFFileChooser1.setEditable(false);
        addComponent(tFFileChooser1, mainPane, 0, 0,
                1, 1,
                -1, fileChooserHeight,
                -1, GridBagConstraints.HORIZONTAL,
                1, 0, stdInsets);

        //Right File Chooser
        tFFileChooser2 = new JTextField("Choose Folder...");
        tFFileChooser2.setEditable(false);
        addComponent(tFFileChooser2, mainPane, 1, 0,
                1, 1,
                -1, fileChooserHeight,
                -1, GridBagConstraints.HORIZONTAL,
                1, 0, stdInsets);

        //Left Tree
        tree1 = new JTree();
        tree1.setModel(null);
        sPTree1 = new JScrollPane(tree1);
        addComponent(sPTree1, mainPane, 0, 1,
                1, 1,
                -1, -1,
                -1, GridBagConstraints.BOTH,
                1, 1, stdInsets);

        //Right Tree
        tree2 = new JTree();
        tree2.setModel(null);
        sPTree2 = new JScrollPane(tree2);
        addComponent(sPTree2, mainPane, 1, 1,
                1, 1,
                -1, -1,
                -1, GridBagConstraints.BOTH,
                1, 1, stdInsets);

        //Buttons Panel
        pButtons = new JPanel();
        pButtons.setLayout(new GridLayout());
        addComponent(pButtons, mainPane, 0, 2,
                2, 1,
                -1, buttonHeight,
                -1, GridBagConstraints.HORIZONTAL,
                1, 0, stdInsets);

        //Compare Button
        bCompare = new JButton("Compare");
        bCompare.setEnabled(false);
        pButtons.add(bCompare);

        //Sync Button
        bSync = new JButton("Sync");
        bSync.setEnabled(false);
        pButtons.add(bSync);

        //Settings Button
        bSettings = new JButton("Settings");
        bSettings.setEnabled(true);
        pButtons.add(bSettings);

        //Interrupt Button
        bInterrupt = new JButton("Interrupt");
        bInterrupt.setEnabled(true);
        pButtons.add(bInterrupt);

        //Log TextArea
        tALog = new JTextArea();
        tALog.setEditable(false);
        tALog.setFont(customFont);
        sPLog = new JScrollPane(tALog);
        addComponent(sPLog, mainPane, 0, 3,
                2, 1,
                -1, -1,
                -1, GridBagConstraints.BOTH,
                1, 1, stdInsets);

        mainFrame.setPreferredSize(framePSize);
        mainFrame.setMinimumSize(frameMinSize);

        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - mainFrame.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - mainFrame.getHeight()) / 2);
        mainFrame.setLocation(x, y);
        mainFrame.setVisible(true);
    }

    //Initialize GUI Actions
    private void initActions(){
        actionMap.put(tFFileChooser1, "lfc");
        actionMap.put(tFFileChooser2, "rfc");
        actionMap.put(bCompare, "compare");
        actionMap.put(bSync, "sync");
        actionMap.put(bSettings, "settings");
        actionMap.put(bInterrupt, "interrupt");

        tFFileChooser1.addMouseListener(mouseListener);
        tFFileChooser2.addMouseListener(mouseListener);
        bCompare.addActionListener(actionListener);
        bSync.addActionListener(actionListener);
        bSettings.addActionListener(actionListener);
        bInterrupt.addActionListener(actionListener);
    }

    //Copying
    private void copyFromTo(Path sourceRoot, Path dstRoot,
                            List<Path> dirPathList, List<Path> filePathList) throws IOException{
        for (Path dirPath : dirPathList){
            Path dPath = dstRoot.resolve(dirPath);
            if (!dPath.toFile().exists()){
                Files.createDirectories(dPath);
            }
        }
        for (Path filePath : filePathList){
            Files.copy(sourceRoot.resolve(filePath), dstRoot.resolve(filePath),
                    StandardCopyOption.REPLACE_EXISTING, ExtendedCopyOption.INTERRUPTIBLE);
        }
    }

    //Syncing
    public void sync(){
        if (notWorking()) {
            Object[] options = {"Yes", "No"};
            int rtnVal = JOptionPane.showOptionDialog(
                    mainFrame,
                    "Are you sure you want to start syncing?",
                    "Confirmation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]
            );
            if (rtnVal != 0){
                return;
            }
            workingThread = new Thread(() -> {
                logger.log("Started Syncing...", LogSeverity.INFO);
                try {
                    if (syncerConfig.oneToTwo){
                        copyFromTo(rootPath1, rootPath2, dirComparator1.dirPaths, dirComparator1.filePaths);
                    }
                    if (syncerConfig.twoToOne){
                        copyFromTo(rootPath2, rootPath1, dirComparator2.dirPaths, dirComparator2.filePaths);
                    }
                } catch (IOException e){
                    logger.log(e.toString(), LogSeverity.ERROR);
                    logger.log("Syncing Unsuccessful.", LogSeverity.ERROR);
                    return;
                }
                logger.log("Syncing Successful.", LogSeverity.INFO);
            });
            workingThread.start();
        }
    }

    //Interrupt the working thread
    public void interrupt(){
        if (!notWorking()){
            Object[] options = {"Yes", "No"};
            int rtnVal = JOptionPane.showOptionDialog(
                    mainFrame,
                    "Interrupt running operation?",
                    "Confirmation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]
            );
            if (rtnVal == 0){
                workingThread.interrupt();
                logger.log("Stopping Operation...", LogSeverity.INFO);
            }
        }
    }

    //Create and open the dialog
    public void openSettingsDialog(){
        if (notWorking()){
            settingsDialog = new JDialog(mainFrame, "Settings", Dialog.ModalityType.APPLICATION_MODAL);
            settingsDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            settingsDialog.setResizable(false);
            settingsDialog.setLocation(mainFrame.getLocation());

            dPane = settingsDialog.getContentPane();
            dPane.setLayout(new GridBagLayout());

            dTabbedPane = new JTabbedPane();
            addComponent(dTabbedPane, dPane, 0, 0,
                    1, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 1, stdInsets);

            dSaveButton = new JButton("Save");
            actionMap.put(dSaveButton, "save");
            dSaveButton.addActionListener(actionListener);
            addComponent(dSaveButton, dPane, 0, 1,
                    1, 1,
                    -1, -1,
                    GridBagConstraints.LAST_LINE_END, GridBagConstraints.NONE,
                    0, 0, stdInsets);

            dPCompareSettings = new JPanel();
            dPCompareSettings.setLayout(new GridBagLayout());

            dCBCheckSize = new JCheckBox("Size");
            addComponent(dCBCheckSize, dPCompareSettings, 0, 0,
                    1, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 1, null);

            dCBCheckModTime = new JCheckBox("Last Modification Time");
            addComponent(dCBCheckModTime, dPCompareSettings, 1, 0,
                    1, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 1, null);

            dCBCheckContent = new JCheckBox("Content");
            addComponent(dCBCheckContent, dPCompareSettings, 2, 0,
                    1, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 1, null);

            dLExtensionFilter = new JLabel("File Extension Filter:");
            dLExtensionFilter.setHorizontalAlignment(SwingConstants.LEFT);
            addComponent(dLExtensionFilter, dPCompareSettings, 0, 1,
                    3, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 0, null);

            dTAExtensions = new JTextArea();
            dTAExtensions.setLineWrap(true);
            dSPTAExtensions = new JScrollPane(dTAExtensions);
            dTAExtensions.setFont(customFont);
            addComponent(dSPTAExtensions, dPCompareSettings, 0, 2,
                    2, 2,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 1, null);

            dRBOnly = new JRadioButton("Only");
            addComponent(dRBOnly, dPCompareSettings, 2, 2,
                    1, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 1, null);

            dRBExclude = new JRadioButton("Exclude");
            addComponent(dRBExclude, dPCompareSettings, 2, 3,
                    1, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 1, null);

            dButtonGroup = new ButtonGroup();
            dButtonGroup.add(dRBOnly);
            dButtonGroup.add(dRBExclude);

            dPSyncSettings = new JPanel();
            dPSyncSettings.setLayout(new GridBagLayout());

            dCBOneToTwo = new JCheckBox("Left to Right");
            addComponent(dCBOneToTwo, dPSyncSettings, 0, 0,
                    1, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 0, null);

            dCBTwoToOne = new JCheckBox("Right to Left");
            addComponent(dCBTwoToOne, dPSyncSettings, 1, 0,
                    1, 1,
                    -1, -1,
                    -1, GridBagConstraints.BOTH,
                    1, 0, null);

            dTabbedPane.addTab("Compare", dPCompareSettings);
            dTabbedPane.addTab("Sync", dPSyncSettings);

            initSettingsDialog();

            settingsDialog.pack();
            settingsDialog.setVisible(true);

        }
    }

    //Showing the settings in the dialog
    public void initSettingsDialog(){
        dCBCheckSize.setSelected(comparatorConfig.checkSize);
        dCBCheckModTime.setSelected(comparatorConfig.checkModTime);
        dCBCheckContent.setSelected(comparatorConfig.checkContent);
        dTAExtensions.setText(String.join(",", comparatorConfig.extensions));
        if (comparatorConfig.excludeExtension){
            dRBExclude.setSelected(true);
        }
        else{
            dRBOnly.setSelected(true);
        }
        dCBOneToTwo.setSelected(syncerConfig.oneToTwo);
        dCBTwoToOne.setSelected(syncerConfig.twoToOne);
    }

    //Applying the settings configured in the dialog
    public void saveSettings(){
        comparatorConfig.checkSize = dCBCheckSize.isSelected();
        comparatorConfig.checkModTime = dCBCheckModTime.isSelected();
        comparatorConfig.checkContent = dCBCheckContent.isSelected();
        if (dTAExtensions.getText().trim().isEmpty())
            comparatorConfig.extensions = new ArrayList<>();
        else
            comparatorConfig.extensions = Arrays.stream(dTAExtensions.getText().trim().split(",")).collect(Collectors.toList());
        comparatorConfig.excludeExtension = dRBExclude.isSelected();
        syncerConfig.oneToTwo = dCBOneToTwo.isSelected();
        syncerConfig.twoToOne = dCBTwoToOne.isSelected();
        settingsDialog.dispose();
        logger.log("Settings applied.", LogSeverity.INFO);
    }

    //Add Component to GridBagLayout
    private void addComponent(Component component,
                              Container container,
                              int _gridx, int _gridy,
                              int _gridwidth, int _gridheight,
                              int _ipadx, int _ipady,
                              int _anchor, int _fill,
                              double _weightx, double _weighty, Insets _insets
                              ){
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = _gridx != -1 ? _gridx : c.gridx;
        c.gridy = _gridy != -1 ? _gridy : c.gridy;
        c.gridwidth = _gridwidth != -1 ? _gridwidth : c.gridwidth;
        c.gridheight = _gridheight != -1 ? _gridheight : c.gridheight;
        c.weightx = _weightx != -1 ? _weightx : c.weightx;
        c.weighty = _weighty != -1 ? _weighty : c.weighty;
        c.anchor = _anchor != -1 ? _anchor : c.anchor;
        c.fill = _fill != -1 ? _fill : c.fill;
        c.insets = _insets != null ? _insets : c.insets;
        c.ipadx = _ipadx != -1 ? _ipadx : c.ipadx;
        c.ipady = _ipady != -1 ? _ipady : c.ipady;
        container.add(component, c);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Main();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

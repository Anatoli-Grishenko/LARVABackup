/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package larvabackup;

import appboot.XUITTY;
import basher.Basher;
import static console.Console.black;
import data.OleConfig;
import data.OleDot;
import disk.FileRecord;
import disk.FileTable;
import java.awt.Color;
import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.WHITE;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import swing.OleApplication;
import swing.OleDialog;
import swing.OleDrawPane;
import swing.OleList;
import swing.OleScrollPane;
import swing.SwingTools;
import tools.TimeHandler;
import tools.emojis;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class LARVABackup {

    static OleConfig myConfig, myProject; //, myPreferrences;
    static OleApplication oMainFrame;
    static JPanel pSideTools, pStatus, pMain;
    static JTextArea taList, taUpdate, taNewer;
    static OleList olFolders;
    static JPanel folderPane, targetPane;
    static JLabel jlSourceFolders, jlSourceSize, jlSourceFiles,
            jlTargetFolders, jlTargetSize, jlTargetFiles, jlTimeBackup, jlUpdates, jlNewers;
    static OleDrawPane opDiagram;
    static OleScrollPane osDiagram;
    static String projectFile = null, outputfile = "./backup.out";
    static ArrayList<String> fileList = null;
    static FileTable ftSources, ftTargets, ftUpdate, ftNew;
    static boolean allsaved = true, allgood = true;
    static XUITTY xuitty;

    public static void main(String[] args) {
        myConfig = new OleConfig();
        myConfig.loadFile("config/LARVABackup.json");
        if (myConfig.isEmpty()) {
            SwingTools.Error("Configuration file not found");
            System.exit(1);
        }
        oMainFrame = new OleApplication(myConfig) {
            @Override
            public void myActionListener(ActionEvent e) {
                frameActionListener(e);
            }

            @Override
            public void myKeyListener(KeyEvent e) {
                frameKeyListener(e);
            }

            @Override
            public void Draw(Graphics2D g) {
                paintBackup(g);
            }
        };
        osDiagram = oMainFrame.getScollPane();
        opDiagram = oMainFrame.getDrawingPane();
        opDiagram.removeAll();
        opDiagram.setLayout(new FlowLayout(FlowLayout.LEFT));

        folderPane = new JPanel();
        folderPane.setLayout(new BoxLayout(folderPane, BoxLayout.Y_AXIS));
//        folderPane.setBackground(WHITE);
        folderPane.setPreferredSize(new Dimension(
                opDiagram.getWidth() / 2, opDiagram.getHeight() - 4));
        folderPane.setBounds(new Rectangle(opDiagram.getWidth() / 2, opDiagram.getHeight() - 4));
        targetPane = new JPanel();
        targetPane.setLayout(new BoxLayout(targetPane, BoxLayout.Y_AXIS));
        targetPane.setPreferredSize(new Dimension(
                opDiagram.getWidth() / 2 - 64, opDiagram.getHeight() - 4));
        targetPane.setBounds(new Rectangle(opDiagram.getWidth() / 2, opDiagram.getHeight() - 4));
        jlSourceSize = new JLabel(emojis.PACKAGE + " Backup size (0)");
        jlSourceFolders = new JLabel(emojis.FOLDER + " Folders (0)");
        jlSourceFiles = new JLabel(emojis.CLASS + " Files (0)");
        taList = new JTextArea(10, 15);
        taList.setEditable(false);
//        for (int i = 0; i < 500; i++) {
//            taList.append(i+"Luis\n");
//            taList.append(i+"Paqui\n");
//        }
        folderPane.add(new JLabel("TO BACKUP"));
        folderPane.add(new JLabel(emojis.CALENDAR+" "+TimeHandler.Now()));
        folderPane.add(jlSourceSize);
        folderPane.add(jlSourceFiles);
        folderPane.add(jlSourceFolders);

        JScrollPane jsPane = new JScrollPane(taList);
        jsPane.setPreferredSize(new Dimension(
                folderPane.getWidth() - 64, folderPane.getHeight() - 64));
        jsPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        folderPane.add(jsPane);
        folderPane.validate();

        jlTargetSize = new JLabel(emojis.PACKAGE + " Backup size (0)");
        jlTargetFolders = new JLabel(emojis.FOLDER + " Folders (0)");
        jlTargetFiles = new JLabel(emojis.CLASS + " Files (0)");
        jlTimeBackup = new JLabel(emojis.CALENDAR + " ");

        jlNewers = new JLabel(emojis.NEW + " New (0)");
        taNewer = new JTextArea(10, 7);
        taNewer.setEditable(false);

        jlUpdates = new JLabel(emojis.CLOCK + " Updatable (0)");
        taUpdate = new JTextArea(10, 7);
        taUpdate.setEditable(false);

        targetPane.add(new JLabel("ALREADY BACKUP"));
        targetPane.add(jlTimeBackup);
        targetPane.add(jlTargetSize);
        targetPane.add(jlTargetFiles);
        targetPane.add(jlTargetFolders);
        targetPane.add(jlUpdates);
        targetPane.add(taUpdate);        
        targetPane.add(jlNewers);
        targetPane.add(taNewer);        

        opDiagram.add(folderPane);
        opDiagram.add(targetPane);
        targetPane.add(taUpdate);
        opDiagram.validate();
    }

    protected static void scrollUp() {
        osDiagram.dragRelative(0, -640);

    }

    protected static void scrollDown() {
        osDiagram.dragRelative(0, 640);

    }

    protected static void Wheel(MouseWheelEvent e) {

        int steps = e.getWheelRotation();
        if (steps < 0) {
            scrollUp();
        }
        if (steps > 0) {
            scrollDown();
        }
    }

    public static void frameActionListener(ActionEvent e) {
        System.out.println(">>> " + e.getActionCommand());
        switch (e.getActionCommand()) {
            case "NewProject":
            case "New Project":
                newProject();
                break;
            case "Open Project":
            case "OpenProject":
                openProject();
                break;
            case "Save Project":
            case "SaveProject":
                saveProject();
                break;
            case "Configure Project":
            case "ConfigureProject":
                configureProject();
                break;
            case "Simmulate":
                Simmulate();
                break;
            case "Backup":
                doBackup();
                break;
            case "Help":
                break;
            case "About":
                oMainFrame.Info("<html><center><h1>Incremental Backup</h1><br><b>Programmed by</b><br><i>Anatoli Grishenko</i></html>");
                break;
            case "Exit":
                if (oMainFrame.Confirm("Please confirm exit")) {
                    if (!allsaved) {
                        if (oMainFrame.Confirm("Save current project?")) {
                            saveProject();
                        }
                    }
                    oMainFrame.dispose();
                }
                break;
        }
    }

    public static void openProject() {
        if (!allsaved) {
            if (oMainFrame.Confirm("Save current project?")) {
                saveProject();
            }
        }
        projectFile = OleDialog.doSelectFile("./config", "backup");
        if (projectFile != null) {
            myProject = new OleConfig();
            myProject.loadFile(projectFile);
            configureProject();
        }

    }

    public static void saveProject() {
        if (myProject != null) {
            projectFile = OleDialog.doSelectFile("./config", "backup");
            if (projectFile != null) {
                myProject.saveAsFile("./", projectFile, true);
                allsaved = true;
            }
        }
    }

    public static void newProject() {
        projectFile = null;
        myProject = new OleConfig();
        myProject.loadFile("./config/EmptyConfig.backup");
        configureProject();
        allsaved = false;
    }

    public static void configureProject() {
        if (myProject != null) {
            OleDialog odg;
            odg = new OleDialog(oMainFrame, "Edit project");
            if (odg.run(myProject)) {
                allsaved = false;
                myProject = odg.getResult();
                Simmulate();
            }
        }
    }

    public static void frameKeyListener(KeyEvent e) {
    }

    public static void paintBackup(Graphics2D myg) {
    }

    public static void Simmulate() {
        if (myProject != null) {
            try {
                String command;
                ArrayList<String> lcommand = new ArrayList();
                String line;
                fileList = new ArrayList();
                ftSources = new FileTable();
                ftTargets = new FileTable();
                taList.setText("");
                ArrayList<String> inputs = new ArrayList<String>(myProject.getOptions().getOle("Sources").getArray("Input folders")),
                        excludes = new ArrayList<String>(myProject.getOptions().getOle("Sources").getArray("Ignore"));
                oMainFrame.showProgress("Backup", 0, inputs.size());
                for (String s : inputs) {
                    lcommand = new ArrayList();
//                    lcommand.add("find");
                    lcommand.add("rsync");
                    lcommand.add("--list-only");
                    lcommand.add("-r");
                    lcommand.add("-u");
                    lcommand.add("-p");
                    for (String sex : excludes) {
                        lcommand.add("--exclude=" + sex);
                    }
                    lcommand.add(s);
                    System.out.println(">>>>" + s);
                    oMainFrame.showProgress(s);
                    ProcessBuilder pb = new ProcessBuilder(lcommand);
                    pb.redirectOutput(new File(outputfile));
                    Process p = pb.start();
                    p.waitFor();
                    Basher blines = new Basher(outputfile);
                    for (String sl : blines.getList()) {
                        FileRecord fr = new FileRecord(sl);
                        fr.setName(s + fr.getName());
                        ftSources.add(fr);
                    }
                }
                oMainFrame.closeProgress("");
                lcommand = new ArrayList();
                lcommand.add("rsync");
                lcommand.add("--list-only");
                lcommand.add("-r");
                lcommand.add("-u");
                lcommand.add("-p");
                lcommand.add(myProject.getOptions().getOle("Sources").getString("Output folder", ""));
                ProcessBuilder pb = new ProcessBuilder(lcommand);
                pb.redirectOutput(new File(outputfile));
                Process p = pb.start();
                p.waitFor();
                Basher blines = new Basher(outputfile);
                for (String sl : blines.getList()) {
                    FileRecord fr = new FileRecord(sl);
                    fr.setName(myProject.getOptions().getOle("Sources").getString("Output folder", "") + fr.getName());
                    ftTargets.add(fr);
                }
                ftUpdate = ftSources.getUpdatableRecords(ftTargets);
                ftNew = ftSources.getNewerRecords(ftTargets);
                showSummary();
            } catch (Exception ex) {
                oMainFrame.Warning("Error executing command");
            }
        } else {
            oMainFrame.Warning("There is no active project");
        }
    }

    public static void showSummary() {
        long nd = 0, size = 0;
        nd = ftSources.getDirectories().size();
        size = ftSources.memory();
        oMainFrame.cleanStatus();
        if (allgood) {
            oMainFrame.addStatus("\t" + emojis.GREENCIRCLE + "\t");
        } else {
            oMainFrame.addStatus("\t" + emojis.BLACKCIRCLE + "\t");
        }
        oMainFrame.addStatus(emojis.CLASS + " " + ftSources.size() + " Files \t"
                + emojis.FOLDER + " " + nd + " Folders\t" + emojis.PACKAGE + " "
                + FileRecord.getMemory(size, FileRecord.Sizes.Bytes));
        FileTable ftd = ftSources.getDirectories();
        int y = 1;
        for (FileRecord fr : ftd.ToArrayList()) {
            taList.append(fr.getDepth()+"  "+fr.getName() + "\n");
        }
        jlSourceFolders.setText(jlSourceFolders.getText().replaceAll("\\(.*\\)", "(" + ftd.size() + ")"));
        jlSourceFiles.setText(jlSourceFiles.getText().replaceAll("\\(.*\\)", "(" + ftSources.size() + ")"));
        jlSourceSize.setText(jlSourceSize.getText().replaceAll("\\(.*\\)", "("
                + FileRecord.getMemory(ftSources.memory(), FileRecord.Sizes.Bytes) + ")"));
        jlTargetFolders.setText(jlTargetFolders.getText().replaceAll("\\(.*\\)", "(" + ftTargets.getDirectories().size() + ")"));
        jlTargetFiles.setText(jlTargetFiles.getText().replaceAll("\\(.*\\)", "(" + ftTargets.size() + ")"));
        jlTargetSize.setText(jlTargetSize.getText().replaceAll("\\(.*\\)", "("
                + FileRecord.getMemory(ftTargets.memory(), FileRecord.Sizes.Bytes) + ")"));
        jlUpdates.setText(jlUpdates.getText().replaceAll("\\(.*\\)", "("
                + ftUpdate.size()+")"));
        jlNewers.setText(jlNewers.getText().replaceAll("\\(.*\\)", "("
                + ftNew.size()+")"));
        jlTimeBackup.setText(emojis.CALENDAR+" "+myProject.getOptions().getOle("Sources").getString("Last backup",""));
    }

    public static void doBackup() {

    }
}

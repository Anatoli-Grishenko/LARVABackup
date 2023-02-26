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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import swing.OleApplication;
import swing.OleDialog;
import swing.OleDrawPane;
import swing.OleScrollPane;
import swing.SwingTools;
import tools.emojis;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class LARVABackup {

    static OleConfig myConfig, myProject; //, myPreferrences;
    static OleApplication oMainFrame;
    static JPanel pSideTools, pStatus, pMain;
    static JTextArea taList;
    static OleDrawPane opDiagram;
    static OleScrollPane osDiagram;
    static String projectFile = null, outputfile = "./backup.out";
    static ArrayList<String> fileList = null;
    static FileTable ftSources, ftTargets, ftTocopy;
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
        opDiagram = oMainFrame.getDrawingPane();
        osDiagram = oMainFrame.getScollPane();
        taList = new JTextArea();
        taList.setEditable(false);
        taList.setWrapStyleWord(true);
        xuitty = new XUITTY();
        xuitty.init(opDiagram, 200, 200, 10);
        xuitty.setFont(new Font("Monospaced", Font.PLAIN, 12));
        xuitty.setBackground(BLACK);
        xuitty.setForeground(WHITE);
        xuitty.clearScreen();
        xuitty.render();
        osDiagram.setHandlerWheel((e) -> Wheel(e));
//        osDiagram.add(taList);
//        osDiagram.add(new JButton("hllo"));
//        osDiagram.validate();
//        osDiagram.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
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
            case "Edit Project":
            case "EditProject":
                editProject();
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
            editProject();
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
        editProject();
        allsaved = false;
    }

    public static void editProject() {
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
//                command = "find ";
//                ArrayList<String> inputs = new ArrayList<String>(myProject.getOptions().getOle("Sources").getArray("Input folders"));
//                for (String s : inputs) {
//                    command += (" " + s + " ");
//                }
//                command = "rsync --list-only -r -u -p ";
//                ArrayList<String> inputs = new ArrayList<String>(myProject.getOptions().getOle("Sources").getArray("Input folders"));
//                for (String s : inputs) {
//                    command += (" " + s + " ");
//                }
                ArrayList<String> inputs = new ArrayList<String>(myProject.getOptions().getOle("Sources").getArray("Input folders"));
                oMainFrame.showProgress("Backup", 0, inputs.size());
                for (String s : inputs) {
                    lcommand = new ArrayList();
//                    lcommand.add("find");
                    lcommand.add("rsync");
                    lcommand.add("--list-only");
                    lcommand.add("-r");
                    lcommand.add("-u");
                    lcommand.add("-p");
                    lcommand.add(s);
                    System.out.println(">>>>" + s);
                    oMainFrame.showProgress(s);
//                Process p;
//                p = Runtime.getRuntime().exec(command);
//                p.waitFor();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//                line = reader.readLine();
//                while (line != null) {
//                    System.out.println(line);
//                    fileList.add(line);
//                    line = reader.readLine();
//                }
                    ProcessBuilder pb = new ProcessBuilder(lcommand);
                    pb.redirectOutput(new File(outputfile));
                    Process p = pb.start();
                    p.waitFor();
                    Basher blines = new Basher(outputfile);
                    for (String sl : blines.getList()) {
                        System.out.println(">>>>" + sl);
                        FileRecord fr = new FileRecord(sl);
                        fr.setName(s+fr.getName());
                        ftSources.add(fr);
                    }
                }
                oMainFrame.closeProgress("");
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
        xuitty.clearScreen();
        xuitty.setForeground(Color.WHITE);
        FileTable ftd = ftSources.getDirectories();
        xuitty.doFrameTitle(" FOLDERS TO BACKUP ", 0,0,  75, ftd.size()+2);
        xuitty.setForeground(Color.GREEN);
        int y=1;
        for (FileRecord fr : ftd.ToArrayList()) {
            xuitty.setCursorXY(1,y++);
            xuitty.println(fr.getName());
        }
        xuitty.render();
        xuitty.setCaretPosition(0);
    }

    public static void doBackup() {

    }
}

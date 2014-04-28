package com.ardoq.javadoc;

import com.ardoq.ArdoqClient;
import com.ardoq.model.*;
import com.ardoq.util.SyncUtil;
import com.sun.javadoc.*;
import jdepend.framework.JavaPackage;
import retrofit.RestAdapter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;

/**
 * Contains all the functionality for running ArdoqDoclet standalone or via
 *
 * ```javadoc -doclet```
 *
 */
public class ArdoqDoclet {
    private static String ardoqUsername = null; //System.getenv("ardoqUsername");
    private static String host = "https://app.ardoq.com"; //System.getenv("ardoqHost"); //"http://localhost:8080"; //
    private static String ardoqPassword = null; //System.getenv("ardoqPassword");
    private static String workspaceName = "Javadoc ArdoqDoclet";
    private static String token;
    private static String workspaceDir = "";
    private static String sourceControl= null;
    private static String srcDirectory = "";
    private static String targetDirectory = null;
    private final ReferenceManager referenceManager;
    private final ComponentManager componentManager;

    private jdepend.framework.JDepend analyzer;

    private final ArdoqClient client;
    private final SyncUtil ardoqSync;
    private final Workspace workspace;

    /**
     * Constructs a new ArdoqDoclet that communicates with the given ArdoqClient.
     * @param client The ArdoqClient to use
     * @param root The JavaDoc RootDoc to document.
     */
    public ArdoqDoclet(ArdoqClient client, RootDoc root) throws IOException {
        this.ardoqSync = new SyncUtil(client, workspaceName, "JavaDoc");
        this.client = client;
        this.client.setLogLevel(RestAdapter.LogLevel.NONE);


        this.workspace = this.ardoqSync.updateWorkspaceIfDifferent(new Workspace(workspaceName, this.ardoqSync.getModel().getId(), getWorkspaceDescription()));

        this.componentManager = new ComponentManager(workspace, ardoqSync, root);
        this.referenceManager = new ReferenceManager(componentManager, ardoqSync);

        if (null != sourceControl)
        {
            this.componentManager.setSourceControlUrl(sourceControl);
        }
        if (null!= this.targetDirectory)
        {
            analyzer = new jdepend.framework.JDepend();
            analyzer.addDirectory(targetDirectory);
            this.jDepend();
            this.referenceManager.addJDepend(analyzer);
            this.componentManager.addJDepend(analyzer);
        }

        this.componentManager.documentPackagesAndComponents();

        referenceManager.addReferences();


        this.ardoqSync.deleteNotSyncedItems();
        this.ardoqSync.syncTags();

        System.out.println(this.ardoqSync.getReport());
    }

    String getWorkspaceDescription() {
        String description = "";
        try {
            description = readFile(ArdoqDoclet.workspaceDir + "README.md");
        } catch (IOException e) {
            System.out.println("Couldn't read README.md in root, no workspace description.");
        }
        return description;
    }

    String readFile(String path)
            throws IOException {
        System.out.println("Loading description README.md: " + Paths.get(path).toAbsolutePath().normalize());
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, Charset.forName("UTF-8"));
    }

    private void jDepend() {
        Collection<JavaPackage> packages = analyzer.analyze();
        for (JavaPackage jp : packages){
            System.out.println(jp.getClasses());
            System.out.println(jp.getName());
        }
    }





    public static boolean validOptions(String[][] options, DocErrorReporter reporter){

        for (String option[] : options){
            if (option.length > 1) {
                System.out.println(option[0] + "=" + option[1]);
                if (option[0].equalsIgnoreCase("-targetClasses")){
                    targetDirectory = option[1];
                }
                else if (option[0].equalsIgnoreCase("-sourcepath")){
                    srcDirectory = option[1];
                }
                else if (option[0].equalsIgnoreCase("-sourceControl")){
                    sourceControl = option[1];
                }
                else if (option[0].equalsIgnoreCase("-projectDir")){
                    workspaceDir = option[1];
                    System.out.println("Setting workspace dir to "+workspaceDir);
                }
                else if (option[0].equalsIgnoreCase("-ardoqUsername")) {
                    ardoqUsername = option[1];
                } else if (option[0].equalsIgnoreCase("-ardoqHost")) {
                    System.out.println("Setting host: "+option[1]);
                    host = option[1];
                } else if (option[0].equalsIgnoreCase("-ardoqPassword")) {
                    ardoqPassword = option[1];
                } else if (option[0].equalsIgnoreCase("-workspaceName")) {
                    workspaceName = option[1];
                }
                else if (option[0].equalsIgnoreCase("-ardoqToken")){
                    token = option[1];
                }
            }

        }
        return true;
    }

    public static boolean start(RootDoc root) {
        System.out.println("Connecting to Ardoq: "+host);
        ArdoqClient client = null;
        if (token != null)
        {
            client = new ArdoqClient(host, token);
        }
        else
        {
            client = new ArdoqClient(host, ardoqUsername, ardoqPassword);
        }

        try {
            new ArdoqDoclet(client, root);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        com.sun.tools.javadoc.Main.execute("Ardoq doc:", ArdoqDoclet.class.getName(), args);
    }

    /**
     * Used by javadoc to identify number of args for a given option
     *
     * @param option the option as a string
     * @return the number of expected args for the option.
     */
    public static int optionLength(String option) {
        System.out.println("Options: " + option);
        return 2;
    }

}
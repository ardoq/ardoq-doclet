package com.ardoq.javadoc;

import com.ardoq.ArdoqClient;
import com.ardoq.model.*;
import com.ardoq.util.CacheManager;
import com.ardoq.util.SyncUtil;
import com.sun.javadoc.*;
import jdepend.framework.JavaPackage;
import retrofit.RestAdapter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;

/**
 * Contains all the functionality for running ArdoqDoclet standalone or via JavaDoc, or maven.
 *
 * ```
 * export docletLibs="$JAVA_HOME/lib/javafx-doclet.jar:$JAVA_HOME/lib/tools.jar:$M2_HOME/repository/com/ardoq/api/client/0.8.1/client-0.8.1.jar:$M2_HOME/repository/commons-codec/commons-codec/1.9/commons-codec-1.9.jar:$M2_HOMErepository/com/squareup/retrofit/retrofit/1.5.0/retrofit-1.5.0.jar:$M2_HOME/repository/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar:$M2_HOME/repository/jdepend/jdepend/2.9.1/jdepend-2.9.1.jar"
 * java -Dfile.encoding=UTF-8 -classpath $docletLibs com.ardoq.javadoc.ArdoqDoclet -ardoqToken 19a563c2083a48aa87e6928d269b8ab1 -ardoqHost http://localhost:8080 -workspaceName javadoc client -d ./target -targetClasses ./target/classes -sourcepath ./src/main/java -exclude java.net:java.lang -sourceControl https://github.com/ardoq/ardoq-doclet/tree/master/src/main/java -subpackages com.ardoq
 * ```
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
    private static String organization;
    private static String cacheDirectory = System.getProperty("java.io.tmpdir");
    private static boolean clearCache;
    private static boolean ignoreMethods = false;
    private final ReferenceManager referenceManager;
    private final ComponentManager componentManager;
    private final CacheManager cacheManager;

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
        this.cacheManager = new CacheManager(cacheDirectory, clearCache);
        this.componentManager = new ComponentManager(workspace, ardoqSync, root, cacheManager);
        this.componentManager.setIgnoreMethods(ignoreMethods);
        this.referenceManager = new ReferenceManager(componentManager, ardoqSync, cacheManager);

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
        this.cacheManager.saveCache();
        System.out.println(this.ardoqSync.getReport());
        System.out.println("\n\nSee result: "+host+"/app/view/workspace/"+this.ardoqSync.getWorkspace().getId()+"\n\n");
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
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String data = "";
        try
        {
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                    data += line+"\n";
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            reader.close();
        }
        return data;
    }

    private void jDepend() {
        Collection<JavaPackage> packages = analyzer.analyze();
    }





    public static boolean validOptions(String[][] options, DocErrorReporter reporter){
        for (String option[] : options){
            if (option.length > 1) {
                System.out.println(option[0] + "=" + option[1]);
                if (option[0].equalsIgnoreCase("-ardoqOrganization")){
                    organization = option[1];
                }
                else if (option[0].equalsIgnoreCase("-targetClasses")){
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
                else if (option[0].equalsIgnoreCase("-clearCache")){
                    clearCache = Boolean.parseBoolean(option[1]);
                }
                else if (option[0].equalsIgnoreCase("-cacheDirectory")){
                    cacheDirectory = option[1];
                }
                else if (option[0].equalsIgnoreCase("-ignoreMethods")){
                    ignoreMethods = true;
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

        if (null != organization){
            client.setOrganization(organization);
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
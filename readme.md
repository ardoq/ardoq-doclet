ardoq-doclet
----

The doclet that let's you automatically add all your JavaDoc and Java Application to Ardoq.

It will try to synchronize existing workspace and only add or remove packages/components/methods/references that has changed.

**Caution** Work in progress.


Execution
------------

### Parameters
| Parameter | Description |
| -ardoqToken | Your token for authenticating with Ardoq (or -ardoqUsername yourUserName / -ardoqPassword yourPass) |
|-ardoqHost| Ardoq Server (default https://app.ardoq.com)|
|-workspaceName | The name of the workspace you wish to document to|
|-targetClasses| If you specify this folder where your compiled classes are, we will run JDepend analysis on your packages.|
|-sourceControl| An URL to the location of your Java Source files, e.g. https://github.com/ardoq/ardoq-doclet/tree/master/src/main/java|
|-subpackages| Which packages to include|
|-d|  The location of your libs e.g. (./target) |
|-sourcepath| Java source path (e.g. ./src/main/java)|
|-exclude| Which packages to ignore; e.g. ```java.net:java.lang```|


###Java command line
```
export docletLibs="$JAVA_HOME/lib/javafx-doclet.jar:$JAVA_HOME/lib/tools.jar:$M2_HOME/repository/com/ardoq/api/client/0.8.1/client-0.8.1.jar:$M2_HOME/repository/commons-codec/commons-codec/1.9/commons-codec-1.9.jar:$M2_HOMErepository/com/squareup/retrofit/retrofit/1.5.0/retrofit-1.5.0.jar:$M2_HOME/repository/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar:$M2_HOME/repository/jdepend/jdepend/2.9.1/jdepend-2.9.1.jar"
java -Dfile.encoding=UTF-8 -classpath $docletLibs com.ardoq.javadoc.ArdoqDoclet -ardoqToken 19a563c2083a48aa87e6928d269b8ab1 -ardoqHost http://localhost:8080 -workspaceName javadoc client -d ./target -targetClasses ./target/classes -sourcepath ./src/main/java -exclude java.net:java.lang -sourceControl https://github.com/ardoq/ardoq-doclet/tree/master/src/main/java -subpackages com.ardoq
```

### In your Maven pom.xml


```
<reporting>
	<plugins>
		<plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>2.9.1</version>
                <reportSets>
                    <reportSet>
                        <id>ardoq</id>
                        <configuration>                           
                        <doclet>com.ardoq.javadoc.ArdoqDoclet</doclet>
                            <docletArtifact>
                                <groupId>com.ardoq.javadoc</groupId>
                                <artifactId>ardoq-doclet</artifactId>
                                <version>0.5.1</version>
                            </docletArtifact>
                            <additionalparam>-ardoqToken 19a563c2083a48aa87e6928d269b8ab1 -ardoqHost http://localhost:8080 -workspaceName "Ardoq  Java Client JavaDoc"</additionalparam>
                            <useStandardDocletOptions>false</useStandardDocletOptions>
                        </configuration>
                    <reports>
                       <report>javadoc</report>
                    </reports>
                    </reportSet>
                </reportSets>
            </plugin>
  </plugins>
</reporting>
```


###License

Copyright © 2014 Ardoq AS

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
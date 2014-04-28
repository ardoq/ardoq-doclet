package com.ardoq.javadoc;

import com.ardoq.model.Component;
import com.ardoq.model.Model;
import com.ardoq.model.Workspace;
import com.ardoq.util.SimpleMarkdownUtil;
import com.ardoq.util.SyncUtil;
import com.sun.javadoc.*;
import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;

import java.util.HashMap;
import java.util.Map;

public class ComponentManager {
    final Model model;
    private final SyncUtil ardoqSync;
    private final Workspace workspace;

    private final RootDoc root;

    private JDepend analyzer;

    HashMap<String, Component> componentMap = new HashMap<String, Component>();
    HashMap<String, ProgramElementDoc> classMap = new HashMap<String, ProgramElementDoc>();
    private String sourceControlUrl;


    public ComponentManager(Workspace workspace , SyncUtil ardoqSync, RootDoc root ) {
        this.ardoqSync = ardoqSync;
        this.workspace = workspace;
        this.root = root;
        this.model = ardoqSync.getModel();
    }

    public void addJDepend(JDepend analyzer)
    {
        this.analyzer = analyzer;
    }

    public void documentPackagesAndComponents(){
        PackageDoc[] packageDocs = root.specifiedPackages();
        for (PackageDoc p : packageDocs){
            System.out.println("Creating doc for package: "+p.name());
            Component packageComp = this.addPackage(p);
            this.addClasses(packageComp, p.allClasses(true));
        }

        this.addReturnTypes();
    }

    void addReturnTypes() {

        for (ProgramElementDoc doc : this.classMap.values()) {
            if (doc.isMethod()) {
                updateMethodDoc((MethodDoc) doc);
            }
        }
    }

    void updateMethodDoc(MethodDoc doc) {
        if (doc.returnType() != null && doc.returnType().qualifiedTypeName() != null) {
            Component c = ardoqSync.getComponentByPath(doc.qualifiedName());
            c.setDescription(SimpleMarkdownUtil.htmlToMarkdown(c.getDescription()) + "\n\nReturns " + this.getComponentLinkOrText(doc.returnType().qualifiedTypeName()));
            this.updateComponent(doc.qualifiedName(), c);

        }

    }

    public void updateComponent(String qualifiedName, Component c) {
        c = ardoqSync.addComponent(c);
        this.componentMap.put(qualifiedName, c);
    }

    Component addPackage(PackageDoc p) {
        Component packageComp = componentMap.get(p.name());
        if (packageComp == null) {
            packageComp = new Component(p.name(), this.workspace.getId(), p.commentText());
            addJDependInfo(packageComp);
            packageComp = this.ardoqSync.addComponent(packageComp);
            this.componentMap.put(p.name(), packageComp);
        }
        return packageComp;
    }

    void addJDependInfo(Component packageComp) {
        if (null != this.analyzer)
        {
            Map<String, Object> fields = packageComp.getFields();
            JavaPackage jp = this.analyzer.getPackage(packageComp.getName());
            fields.put("AbstractClassCount", jp.getAbstractClassCount());
            fields.put("Abstractness", jp.abstractness());
            fields.put("AfferentCoupling", jp.afferentCoupling());
            fields.put("ContainsCycle", jp.containsCycle());
            fields.put("Distance", jp.distance());
            fields.put("EfferentCoupling", jp.efferentCoupling());
            fields.put("ClassCount", jp.getClassCount());
            fields.put("ConcreteClassCount", jp.getConcreteClassCount());
            fields.put("Volatility", jp.getVolatility());
            fields.put("Instability", jp.instability());
            packageComp.setFields(fields);
        }

    }


    protected Component addClass(ProgramElementDoc classDoc) {
        Component packageComponent = this.addPackage(classDoc.containingPackage());
        return this.addClass(packageComponent, classDoc);
    }


    void addClasses(Component packageComp, ClassDoc[] classes) {
        for (ClassDoc i : classes) {
            addClass(packageComp, i);
        }
    }

    Component addClass(Component packageComp, ProgramElementDoc classDoc) {
        Component c = this.componentMap.get(classDoc.qualifiedName());

        if (c == null) {
            String type = this.getType(classDoc);
            c = new Component(classDoc.name(), this.workspace.getId(), classDoc.commentText(), model.getComponentTypeByName(type), packageComp.getId());
            c.setType(type);
            addParams(classDoc, c);

            this.classMap.put(classDoc.qualifiedName(), classDoc);

            ClassDoc cd = null;

            if (classDoc.isOrdinaryClass()) {
                cd = (ClassDoc) classDoc;
                c.setDescription(SimpleMarkdownUtil.htmlToMarkdown(c.getDescription()) + "\n\n###Constructors");
                for (ConstructorDoc constructor : cd.constructors()) {
                    c.setDescription(c.getDescription() + "\n\n####" + getParamsDocumentation(constructor));
                }
            } else if (classDoc.isMethod()) {
                MethodDoc mdr = (MethodDoc) classDoc;
                //Reset description, method documentation below.
                c.setDescription("");
                for (MethodDoc md : mdr.containingClass().methods()) {

                    if (md.qualifiedName().equals(mdr.qualifiedName())) {
                        c.setDescription(SimpleMarkdownUtil.htmlToMarkdown(c.getDescription()) + "\n\n###" + getParamsDocumentation(md));
                    }
                }
            }
            c = this.ardoqSync.addComponent(c);
            this.componentMap.put(classDoc.qualifiedName(), c);
            if (cd != null && classDoc.isOrdinaryClass()) {

                for (MethodDoc md : cd.methods()) {
                    this.addClass(c, md);
                }
            }
        }
        return c;
    }

    String getParamsDocumentation(ExecutableMemberDoc emd) {
        String paramsDoc = "";
        for (Parameter p : emd.parameters()) {
            paramsDoc += " ," + this.getComponentLinkOrText(p.typeName()) + " " + p.name();
        }
        paramsDoc = (paramsDoc.length() > 0) ? paramsDoc.substring(2) : "";
        String emdDoc = emd.name() + "(" + paramsDoc + ")\n\n" + SimpleMarkdownUtil.htmlToMarkdown(emd.commentText()) + "\n\n";

        String paramDescription = "";
        for (Tag pt : emd.paramTags()) {
            paramDescription += "|" + pt.text().replaceFirst(" ", "|") + "|\n";
        }
        if (paramDescription.length() > 0) {
            emdDoc += "|Parameter|Description|\n" + paramDescription;
        }

        return emdDoc;
    }

    protected String getComponentLinkOrText(String classId) {
        String linkOrText;
        if (this.componentMap.containsKey(classId)) {
            Component c = this.componentMap.get(classId);
            linkOrText = "[" + classId + "](comp://" + c.getId() + "/" + c.getName() + ")";
        } else {
            linkOrText = classId;
        }
        return linkOrText;
    }

    void addParams(ProgramElementDoc classDoc, Component c) {
        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put("public", classDoc.isPublic());
        fields.put("private", classDoc.isPrivate());
        fields.put("protected", classDoc.isProtected());
        fields.put("static", classDoc.isStatic());
        fields.put("package", classDoc.containingPackage().name());
        Boolean deprecated = false;
        for (Tag tag : classDoc.tags()) {
            String name = tag.kind().replace("@", "");
            if (name.equals("deprecated")) {
                deprecated = true;
            } else if (!name.equals("param") && !name.equals("throws") && !name.equals("see") && !name.equals("serial") && !name.equals("return")) {
                System.out.println("Name val: " + name + " - " + name + " = " + tag.text());
                fields.put(name, tag.text());
            }
        }
        if (null != this.sourceControlUrl){
            String classNameSource = (classDoc.isMethod()) ? classDoc.containingClass().qualifiedName() : classDoc.qualifiedName();
            classNameSource = classNameSource.replaceAll("\\.","/")+".java";
            fields.put("SourceControl", sourceControlUrl+"/"+classNameSource+"#L"+classDoc.position().line());
        }
        fields.put("deprecated", deprecated.toString());
        c.setFields(fields);
    }

    String getType(ProgramElementDoc ped) {

        if (ped.isMethod()) {
            return "Method";
        }

        ClassDoc classDoc = (ClassDoc) ped;

        if (classDoc.isException()) {
            return "Exception";
        } else if (classDoc.isInterface()) {
            System.out.println("Interface type found");
            return "Interface";
        } else if (classDoc.isEnum()) {
            return "Enum";
        } else if (classDoc.isAbstract() && !classDoc.isOrdinaryClass()) {
            return "Abstract class";
        }
        return "Class";
    }


    public Component getComponent(String qualifiedName) {
        return this.componentMap.get(qualifiedName);
    }

    public ProgramElementDoc getClassByQualifiedName(String name){
        return this.classMap.get(name);
    }

    public ProgramElementDoc[] getClasses(){
        return this.classMap.values().toArray(new ProgramElementDoc[this.classMap.size()]);
    }

    public void setSourceControlUrl(String sourceControlUrl) {
        this.sourceControlUrl = sourceControlUrl;
    }

    /*public Map<String, ProgramElementDoc> getClassMap(){
        return this.classMap;
    }*/


}
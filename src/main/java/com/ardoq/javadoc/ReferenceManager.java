package com.ardoq.javadoc;

import com.ardoq.model.*;
import com.ardoq.util.SimpleMarkdownUtil;
import com.ardoq.util.SyncUtil;
import com.sun.javadoc.*;
import com.sun.javadoc.Tag;
import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;

import java.util.Collection;
import java.util.HashMap;

public class ReferenceManager {
    private static final String TAG_PARAMETER = "parameter";
    private final SyncUtil ardoqSync;

    private final ComponentManager compManager;
    private final Workspace workspace;
    private final Model model;

    private JDepend analyzer;

    private HashMap<String, Boolean> addedRef = new HashMap<String, Boolean>();

    public ReferenceManager(ComponentManager compManager, SyncUtil ardoqSync) {
        this.compManager = compManager;
        this.ardoqSync = ardoqSync;
        this.workspace = ardoqSync.getWorkspace();
        this.model = ardoqSync.getModel();

    }

    public void addJDepend(JDepend analyzer)
    {
        this.analyzer = analyzer;
    }

    void addReferences() {
        ProgramElementDoc[] classes = compManager.getClasses();
        System.out.println("Adding references: " + classes.length);
        for (ProgramElementDoc source : classes) {
            System.out.println("Creating references for: " + source.name());
            for (Tag tag : source.seeTags()) {
                createReference(source, compManager.getClassByQualifiedName(tag.text().trim()), "Implicit");
            }

            if (source.isMethod()) {
                MethodDoc md = (MethodDoc) source;
                for (ClassDoc ex : md.thrownExceptions()) {
                    createReference(source, compManager.getClassByQualifiedName(ex.qualifiedTypeName()), "Throws");
                }
                for (Parameter p : md.parameters()) {
                    createReference(source, compManager.getClassByQualifiedName(p.type().qualifiedTypeName()), "Uses");
                }

                createReference(source, compManager.getClassByQualifiedName(md.returnType().qualifiedTypeName()), "Uses", "Returns");


            } else {
                ClassDoc sourceClass = (ClassDoc) source;
                for (ClassDoc target : sourceClass.interfaces()) {
                    createReference(source, target, "Implements");
                }

                for (ConstructorDoc constructor : sourceClass.constructors()) {
                    for (ClassDoc ex : constructor.thrownExceptions()) {
                        createReference(source, compManager.getClassByQualifiedName(ex.qualifiedTypeName()), "Throws");
                    }

                    for (Parameter parameter : constructor.parameters()) {
                        ProgramElementDoc d = compManager.getClassByQualifiedName(parameter.type().qualifiedTypeName());
                        if (d != null) {
                            createReference(sourceClass, d,"Uses");
                        }
                    }
                }
                createReference(sourceClass, sourceClass.superclass(), "Extends");

                for (FieldDoc field : sourceClass.fields(false)) {
                    System.out.println("Creating field: " + field.toString() + " - " + field.qualifiedName());
                    ProgramElementDoc d = compManager.getClassByQualifiedName(field.type().qualifiedTypeName());
                    if (d != null) {
                        createReference(sourceClass, d, "Uses");
                    }
                }
            }

        }

        this.addJDependAnalysis();

        

    }

    private void addJDependAnalysis() {
        if (null != this.analyzer ) {
            for (Object jpo : this.analyzer.getPackages()) {
                JavaPackage jp = (JavaPackage) jpo;
                Component src = this.compManager.getComponent(jp.getName());

                if (null != src) {
                    Collection coll = jp.getEfferents();
                    for (Object jpt : coll) {
                        JavaPackage packageTarget = (JavaPackage) jpt;
                        Component target = this.compManager.getComponent(packageTarget.getName());
                        addJDependRef(src, target);
                    }

                    coll = jp.getAfferents();
                    for (Object jpt : coll) {
                        JavaPackage packageTarget = (JavaPackage) jpt;
                        Component target = this.compManager.getComponent(packageTarget.getName());
                        addJDependRef(target, src);
                    }
                }
            }
        }
    }

    private void addJDependRef(Component src, Component target) {
        if (src != null && target != null && !addedRef.containsKey(src.getName()+target.getName())) {
            addedRef.put(src.getName()+target.getName(), true);
            Reference ref = ardoqSync.addReference(new Reference(workspace.getId(), "", src.getId(), target.getId(), this.model.getReferenceTypeByName("Uses")));
            tagRefAndComponent("jdepend", src, target, ref);
        }
    }

    private void tagRefAndComponent(String tagName, Component src, Component target, Reference ref) {
        com.ardoq.model.Tag tag = ardoqSync.getTagByName(tagName);
        tag.addReference(ref.getId());
        tag.addComponent(src.getId());
        tag.addComponent(target.getId());
        ardoqSync.updateTag(tag);
    }

    void createReference(ProgramElementDoc source, ProgramElementDoc target, String refType) {
        if (source != target)
        createReference(source, target, refType, "");
    }

    void createReference(ProgramElementDoc source, ProgramElementDoc target, String refTypeName, String description) {

        Component targetComp = (null != target) ? compManager.getComponent(target.qualifiedName()) : null;
        Component srcComp =  (null != source) ? compManager.getComponent(source.qualifiedName()) : null;

        if (target != null && srcComp != null && targetComp != null && !addedRef.containsKey(source.qualifiedName() + target.qualifiedName())) {
            addedRef.put(source.qualifiedName() + target.qualifiedName(), true);
            Reference ref;
            Integer refType = this.model.getReferenceTypeByName(refTypeName);
            String tagName = refTypeName;
            if (source.isMethod()) {
                ref = new Reference(workspace.getId(), "", srcComp.getId(), targetComp.getId(), refType);
                tagName = TAG_PARAMETER;
            } else {
                ref = new Reference(workspace.getId(), "", srcComp.getId(), targetComp.getId(), refType);
            }
            ref.setDescription(description);
            ref = this.ardoqSync.addReference(ref);
            tagRefAndComponent(tagName, srcComp, targetComp, ref);
        }
    }
}
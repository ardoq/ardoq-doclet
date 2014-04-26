package com.ardoq.javadoc;

import com.ardoq.model.Component;
import com.ardoq.model.Model;
import com.ardoq.model.Reference;
import com.ardoq.model.Workspace;
import com.ardoq.util.SimpleMarkdownUtil;
import com.ardoq.util.SyncUtil;
import com.sun.javadoc.*;
import jdepend.framework.JDepend;

import java.util.HashMap;

public class ReferenceManager {
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

                createReference(source, compManager.getClassByQualifiedName(tag.text().trim()), 2);
            }

            if (source.isMethod()) {
                MethodDoc md = (MethodDoc) source;
                for (ClassDoc ex : md.thrownExceptions()) {
                    createReference(source, compManager.getClassByQualifiedName(ex.qualifiedTypeName()), this.model.getReferenceTypeByName("Throws"));
                }
                for (Parameter p : md.parameters()) {
                    createReference(source, compManager.getClassByQualifiedName(p.type().qualifiedTypeName()), this.model.getReferenceTypeByName("Uses"));
                }

                createReference(source, compManager.getClassByQualifiedName(md.returnType().qualifiedTypeName()), this.model.getReferenceTypeByName("Uses"), "Returns");


            } else {
                ClassDoc sourceClass = (ClassDoc) source;
                for (ClassDoc target : sourceClass.interfaces()) {
                    Integer refType = this.model.getReferenceTypeByName("Implements");
                    createReference(source, target, refType);
                }

                for (ConstructorDoc constructor : sourceClass.constructors()) {
                    for (ClassDoc ex : constructor.thrownExceptions()) {
                        createReference(source, compManager.getClassByQualifiedName(ex.qualifiedTypeName()), this.model.getReferenceTypeByName("Throws"));
                    }

                    for (Parameter parameter : constructor.parameters()) {
                        ProgramElementDoc d = compManager.getClassByQualifiedName(parameter.type().qualifiedTypeName());
                        if (d != null) {
                            createReference(sourceClass, d, this.model.getReferenceTypeByName("Uses"));
                        }
                    }
                }

                Integer refType = this.model.getReferenceTypeByName("Extends");
                createReference(sourceClass, sourceClass.superclass(), refType);

                for (FieldDoc field : sourceClass.fields(false)) {
                    System.out.println("Creating field: " + field.toString() + " - " + field.qualifiedName());
                    ProgramElementDoc d = compManager.getClassByQualifiedName(field.type().qualifiedTypeName());
                    if (d != null) {
                        createReference(sourceClass, d, this.model.getReferenceTypeByName("Uses"));
                    }
                }
            }

        }

        

    }

    void createReference(ProgramElementDoc source, ProgramElementDoc target, Integer refType) {
        createReference(source, target, refType, "");
    }

    void createReference(ProgramElementDoc source, ProgramElementDoc target, Integer refType, String description) {

        if (target != null && compManager.getClassByQualifiedName(target.qualifiedName()) != null && !addedRef.containsKey(source.qualifiedName() + target.qualifiedName())) {
            addedRef.put(source.qualifiedName() + target.qualifiedName(), true);
            Reference ref;

            if (source.isMethod() && compManager.getComponent(source.qualifiedName()) != null) {
                ref = new Reference(workspace.getId(), "", compManager.addClass(source).getId(), compManager.getComponent(source.qualifiedName()).getId(), refType);
            } else {
                ref = new Reference(workspace.getId(), "", compManager.addClass(source).getId(), compManager.addClass(target).getId(), refType);
            }
            ref.setDescription(description);
            this.ardoqSync.addReference(ref);
        }
    }
}
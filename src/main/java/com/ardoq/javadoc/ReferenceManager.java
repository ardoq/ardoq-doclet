package com.ardoq.javadoc;

import com.ardoq.model.*;
import com.ardoq.util.CacheManager;
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
    private final CacheManager cacheManager;

    private JDepend analyzer;

    private HashMap<String, Boolean> addedRef = new HashMap<String, Boolean>();

    public ReferenceManager(ComponentManager compManager, SyncUtil ardoqSync, CacheManager cacheManager) {
        this.compManager = compManager;
        this.ardoqSync = ardoqSync;
        this.workspace = ardoqSync.getWorkspace();
        this.model = ardoqSync.getModel();
        this.cacheManager = cacheManager;

    }

    public void addJDepend(JDepend analyzer)
    {
        this.analyzer = analyzer;
    }

    void addReferences() {
        ProgramElementDoc[] classes = compManager.getClasses();
        for (ProgramElementDoc source : classes) {
            for (Tag tag : source.seeTags()) {
                createReference(source, tag.text().trim(), "Implicit");
            }

            if (source.isMethod()) {
                MethodDoc md = (MethodDoc) source;
                for (ClassDoc ex : md.thrownExceptions()) {
                    createReference(source, ex.qualifiedTypeName(), "Throws");
                }
                for (Parameter p : md.parameters()) {
                    createReference(source, p.type().qualifiedTypeName(), "Uses");
                }

                createReference(source, md.returnType().qualifiedTypeName(), "Uses", "Returns the target", "ReturnValue");


            } else {
                ClassDoc sourceClass = (ClassDoc) source;

                for (PackageDoc target : sourceClass.importedPackages()) {
                    if (target != null)
                    {
                        createReference(source, target.name(), "Uses", "", "ImportedPackage");
                    }
                }

                for (ClassDoc target : sourceClass.importedClasses()) {
                    if (target != null) {
                        createReference(source, target.qualifiedName(), "Uses");
                        createReference(source, target.name(), "Uses", "", "ImportedClass");
                    }
                }

                for (ClassDoc target : sourceClass.interfaces()) {
                    createReference(source, target.qualifiedName(), "Implements");
                }

                for (ConstructorDoc constructor : sourceClass.constructors()) {
                    for (ClassDoc ex : constructor.thrownExceptions()) {
                        createReference(source, ex.qualifiedTypeName(), "Throws");
                    }

                    for (Parameter parameter : constructor.parameters()) {
                         createReference(sourceClass, parameter.type().qualifiedTypeName() ,"Uses");
                    }
                }
                createReference(sourceClass, sourceClass.superclass().qualifiedName(), "Extends");

                for (FieldDoc field : sourceClass.fields(false)) {
                    createReference(sourceClass, field.type().qualifiedTypeName(), "Uses");
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
                        if (target == null){
                            target = this.getExternalCachedComponent(packageTarget.getName(), target);
                        }
                        addJDependRef(src, target);
                    }

                    coll = jp.getAfferents();
                    for (Object jpt : coll) {
                        JavaPackage packageTarget = (JavaPackage) jpt;
                        Component target = this.compManager.getComponent(packageTarget.getName());
                        if (target == null){
                            target = this.getExternalCachedComponent(packageTarget.getName(), target);
                        }
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

    Reference createReference(ProgramElementDoc source, String target, String refType) {
        if (!source.qualifiedName().equalsIgnoreCase(target))
        {
            return createReference(source, target, refType, "", null);
        }
        return null;
    }

    Reference createReference(ProgramElementDoc source, String target, String refTypeName, String description, String customTag) {

        Component targetComp = (null != target) ? compManager.getComponent(target) : null;
        Component srcComp =  (null != source) ? compManager.getComponent(source.qualifiedName()) : null;

        boolean isExternal = false;
        //Try to retrieve a cached version
        if (targetComp == null && target != null) {
            targetComp = getExternalCachedComponent(target, targetComp);
            isExternal = true;
        }
        if (target != null && srcComp != null && targetComp != null && !addedRef.containsKey(source.qualifiedName() + target)) {
            addedRef.put(source.qualifiedName() + target, true);
            Reference ref;
            Integer refType = this.model.getReferenceTypeByName(refTypeName);
            String tagName = refTypeName;
            if (source.isMethod()) {
                ref = new Reference(workspace.getId(), "", srcComp.getId(), targetComp.getId(), refType);
                ref.setTargetWorkspace(targetComp.getRootWorkspace());
                tagName = TAG_PARAMETER;
            } else {
                ref = new Reference(workspace.getId(), "", srcComp.getId(), targetComp.getId(), refType);
                ref.setTargetWorkspace(targetComp.getRootWorkspace());
            }
            ref.setDescription(description);
            ref = this.ardoqSync.addReference(ref);
            tagRefAndComponent(tagName, srcComp, targetComp, ref);
            if (isExternal){
                tagRefAndComponent("ExternalDependency", srcComp, targetComp, ref);
            }

            if (customTag != null){
                tagRefAndComponent(customTag, srcComp, targetComp, ref);
            }
            return ref;
        }

        return null;
    }

    private Component getExternalCachedComponent(String target, Component targetComp) {
        if (targetComp == null && target != null){
            String id = this.cacheManager.getComponentId(target);
            if (id != null){
                System.out.println("Found cached component: "+target);
                String targetWorkspace = this.cacheManager.getWorkspaceId(target);
                targetComp = new Component("", targetWorkspace, "", "", "");
                targetComp.setId(id);
            }
        }
        return targetComp;
    }
}
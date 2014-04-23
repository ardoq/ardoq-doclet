package com.ardoq.javadoc;

import com.ardoq.ArdoqClient;
import com.ardoq.model.*;
import com.ardoq.service.ComponentService;
import com.sun.javadoc.*;
import com.sun.javadoc.Tag;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.HashMap;

public class ArdoqDoclet {
    private static String ardoqUsername = null; //System.getenv("ardoqUsername");
    private static String host = null; //System.getenv("ardoqHost"); //"http://localhost:8080"; //
    private static String ardoqPassword = null; //System.getenv("ardoqPassword");
    private static String workspaceName = "Javadoc ArdoqDoclet";
    private static String token;
    private final Model model;
    private final Workspace workspace;
    private final ComponentService componentService;
    private final ArdoqClient client;
    private HashMap<String, Component> componentMap = new HashMap<String, Component>();
    private HashMap<String, ProgramElementDoc> classMap = new HashMap<String, ProgramElementDoc>();
    private HashMap<String, Boolean> addedRef = new HashMap<String, Boolean>();

    public ArdoqDoclet(ArdoqClient client, RootDoc root) {
        this.model = client.model().getModelByName("JavaDoc");

        this.workspace = client.workspace().createWorkspace(new Workspace(workspaceName, model.getId(), this.getWorkspaceDescription()));
        this.componentService = client.component();
        this.client = client;
        PackageDoc[] packageDocs = root.specifiedPackages();
        for (PackageDoc p : packageDocs){
            System.out.println("Creating doc: "+p.name());

            Component packageComp = addPackage(p);
            this.addClasses(packageComp, p.allClasses(true));
        }

        this.addReferences();
        this.addReturnTypes();
        /*


        Component webShopCreateOrder = componentService.createComponent(new Component("createOrder", workspace.getId(), "Order from cart", webshop.getId()));

        Component erp = componentService.createComponent(new Component("ERP", workspace.getId(), ""));
        Component erpCreateOrder = componentService.createComponent(new Component("createOrder", workspace.getId(), "", erp.getId()));
        //Create a Synchronous integration between the Webshop:createOrder and ERP:createOrder services

        List<String> componentIds = Arrays.asList(webShopCreateOrder.getId(), erpCreateOrder.getId());
        List<String> referenceIds = Arrays.asList(reference.getId());
        client.tag().createTag(new Tag("Customer", workspace.getId(), "", componentIds, referenceIds));
        */
    }

    private void addReturnTypes() {

        for (ProgramElementDoc doc : this.classMap.values()){
            if (doc.isMethod())
            {
                this.updateMethodDoc((MethodDoc) doc);
            }
        }
    }

    private void updateMethodDoc(MethodDoc doc) {
        if (doc.returnType() != null && doc.returnType().qualifiedTypeName() != null)
        {
            Component c = this.componentMap.get(doc.qualifiedName());
            c.setDescription(c.getDescription()+ "\n\nReturns "+getComponentLinkOrText(doc.returnType().qualifiedTypeName()));
            /**
             * {"_id":"53558e48300407322b951c3f","name":"getCreatedBy","model":"534e7552300450cc9c5510a6","state":"new","created":"2014-04-21T23:31:52+02:00","created-by":"51efbb924728a34a9a10178e","last-updated":"2014-04-21T23:31:52.000+02:00","version":"0.0.1","_version":1,"rootWorkspace":"53558e46300407322b951bb1","parent":"53558e47300407322b951c2d","type":"Method","typeId":"p1397650658563","description":"\n\n###getCreatedBy()\n\n\n\n\n\nReturns java.lang.String","private":false,"public":true,"static":false,"isPublic":false,"protected":false}
             ---> END HTTP (536-byte body)
             <--- HTTP 400 http://localhost:8080/api/component/53558e48300407322b951c3f (15ms)
             : HTTP/1.1 400 Bad Request
             X-Api-Version: 1.6.9
             Vary: Accept
             Date: Mon, 21 Apr 2014 21:31:56 GMT
             Content-Length: 126
             Set-Cookie: organization=ardoq;Path=/
             Set-Cookie: ring-session=86e36481-ed1d-4b71-b01d-811ff68a46cd;Path=/
             Connection: keep-alive
             Content-Type: application/json;charset=UTF-8
             Server: nginx/1.4.3

             ["Value \"2014-04-21T23:31:52+02:00\", at path [:created], did not match predicate 'ardoq.utils.validation\/date-time-pred'."]
             <--- END HTTP (126-byte body)
             */
            c.setCreated(null);
            c = this.componentService.updateComponent(c.getId(), c);
            this.componentMap.put(doc.qualifiedName(), c);
        }

    }

    private String getWorkspaceDescription() {
        String description = "";
        try {
           description = readFile("../../../README.md");
        } catch (IOException e) {
            System.out.println("Couldn't read README.md in root, no workspace description.");
            e.printStackTrace();
        }
        return description;
    }

    private String readFile(String path)
            throws IOException
    {
        System.out.println("Loading description README.md: "+Paths.get(path).toAbsolutePath().normalize());
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, Charset.forName("UTF-8"));
    }

    private Component addPackage(PackageDoc p) {
        Component packageComp = componentMap.get(p.name());
        if (packageComp == null){
            packageComp = new Component(p.name(), this.workspace.getId(), p.commentText());
            packageComp = this.componentService.createComponent(packageComp);
            this.componentMap.put(p.name(), packageComp);
        }
        return packageComp;
    }

    private void addReferences(){
        System.out.println("Adding references: "+ this.classMap.values().size());
        for (ProgramElementDoc source : this.classMap.values().toArray(new ProgramElementDoc[this.classMap.size()])){
            System.out.println("Creating references for: "+source.name());
            for (Tag tag : source.seeTags()){

                createReference(source, this.classMap.get(tag.text().trim()), 2);
            }

            if (source.isMethod()){
                MethodDoc md = (MethodDoc) source;
                for (ClassDoc ex : md.thrownExceptions()){
                    createReference(source, this.classMap.get(ex.qualifiedTypeName()), model.getReferenceTypeByName("Throws"));
                }
                for (Parameter p : md.parameters()){
                    createReference(source, this.classMap.get(p.type().qualifiedTypeName()), model.getReferenceTypeByName("Uses"));
                }

                createReference(source, this.classMap.get(md.returnType().qualifiedTypeName()), model.getReferenceTypeByName("Uses"), "Returns");
            }
            else {
                ClassDoc sourceClass = (ClassDoc) source;
                for (ClassDoc target : sourceClass.interfaces()) {
                    Integer refType = model.getReferenceTypeByName("Implements");
                    createReference(source, target, refType);
                }

                for (ConstructorDoc constructor : sourceClass.constructors())
                {
                    for (ClassDoc ex : constructor.thrownExceptions()){
                        createReference(source, this.classMap.get(ex.qualifiedTypeName()), model.getReferenceTypeByName("Throws"));
                    }

                    for (Parameter parameter : constructor.parameters()) {
                        ProgramElementDoc d = this.classMap.get(parameter.type().qualifiedTypeName());
                        if (d != null) {
                            createReference(sourceClass, d, model.getReferenceTypeByName("Uses"));
                        }
                    }
                }

                Integer refType = model.getReferenceTypeByName("Extends");
                createReference(sourceClass, sourceClass.superclass(), refType);

                for (FieldDoc field : sourceClass.fields()) {
                    System.out.println("Creating field: " + field.toString() + " - " + field.qualifiedName());
                    ProgramElementDoc d = this.classMap.get(field.type().qualifiedTypeName());
                    if (d != null) {
                        createReference(sourceClass, d, model.getReferenceTypeByName("Uses"));
                    }
                }
            }

        }

    }



    private void createReference(ProgramElementDoc source, ProgramElementDoc target, Integer refType) {
        createReference(source,target,refType, "");
    }

    private void createReference(ProgramElementDoc source, ProgramElementDoc target, Integer refType, String description) {

        System.out.println("Possible reference from "+source.qualifiedName()+" to "+target);
        if (target != null && !this.addedRef.containsKey(source.qualifiedName()+target.qualifiedName()))
        {
            this.addedRef.put(source.qualifiedName() + target.qualifiedName(), true);
            Reference ref = null;

            if (source.isMethod() && this.componentMap.containsKey(source.qualifiedName()))
            {
                System.out.println("Creating method reference from: "+source.qualifiedName()+" to "+target.qualifiedName());
                ref = new Reference(workspace.getId(), "", this.addClass(source).getId(),this.componentMap.get(target.qualifiedName()) .getId(), refType);
            }
            else {
                System.out.println("Creating reference from "+source.qualifiedName()+" to "+target.qualifiedName());
                ref = new Reference(workspace.getId(), "", this.addClass(source).getId(), this.addClass(target).getId(), refType);
            }
            ref.setDescription(description);
            this.client.reference().createReference(ref);
        }
    }

    private Component addClass(ProgramElementDoc classDoc){
        Component packageComponent = this.addPackage(classDoc.containingPackage());
        return this.addClass(packageComponent, classDoc);
    }
    private void addClasses(Component packageComp, ClassDoc[] classes) {
        for (ClassDoc i : classes){
            addClass(packageComp, i);
        }
    }

    private Component addClass(Component packageComp, ProgramElementDoc classDoc) {
        Component c = this.componentMap.get(classDoc.qualifiedName());

        if (c == null) {
            String type = this.getType(classDoc);
            System.out.println("Parent: "+packageComp.toString());
            System.out.println("Component: "+classDoc.qualifiedName()+" is "+this.getType(classDoc));
            c = new Component(classDoc.name(), this.workspace.getId(), classDoc.commentText(), model.getComponentTypeByName(type), packageComp.getId());
            c.setType(type);
            addParams(classDoc, c);
            System.out.println("Creating "+c);
            this.classMap.put(classDoc.qualifiedName(), classDoc);

            ClassDoc cd = null;

            if (classDoc.isOrdinaryClass()){
                cd =  (ClassDoc) classDoc;
                c.setDescription(c.getDescription()+"\n\n###Constructors");
                for (ConstructorDoc constructor : cd.constructors()){
                    c.setDescription(c.getDescription() + "\n\n####"+getParamsDocumentation(constructor));
                }
            }
            else if (classDoc.isMethod()){
                MethodDoc mdr = (MethodDoc) classDoc;
                //Reset description, method documentation below.
                c.setDescription("");
                for (MethodDoc md : mdr.containingClass().methods())
                {

                    if (md.qualifiedName().equals(mdr.qualifiedName()))
                    {
                        c.setDescription(c.getDescription() + "\n\n###"+getParamsDocumentation(md));
                    }
                }
            }
            c = this.componentService.createComponent(c);
            this.componentMap.put(classDoc.qualifiedName(), c);
            if (classDoc.isOrdinaryClass()){

                for (MethodDoc md : cd.methods()){
                    this.addClass(c, md);
                }
            }
        }
        return c;
    }

    private String getParamsDocumentation(ExecutableMemberDoc emd) {
        String paramsDoc = "";
        for (Parameter p : emd.parameters()){
            paramsDoc += " ,"+this.getComponentLinkOrText(p.typeName())+ " "+p.name();
        }
        paramsDoc = (paramsDoc.length() > 0) ? paramsDoc.substring(2) : "";
        String emdDoc = emd.name()+"("+paramsDoc+")\n\n"+emd.commentText()+"\n\n";

        String paramDescription = "";
        for (Tag pt : emd.paramTags()){
            paramDescription += "|"+pt.text().replaceFirst(" ", "|")+"|\n";
        }
        if (paramDescription.length() > 0)
        {
            emdDoc += "|Parameter|Description|\n"+paramDescription;
        }

        return emdDoc;
    }

    private String getComponentLinkOrText(String classId) {
        String linkOrText = "";
        if (this.componentMap.containsKey(classId))
        {
            Component c = this.componentMap.get(classId);
            linkOrText =  "["+classId+"](comp://"+c.getId()+"/"+c.getName()+")";
        }
        else
        {
            linkOrText = classId;
        }
        return linkOrText;
    }

    private void addParams(ProgramElementDoc classDoc, Component c) {
        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put("public", classDoc.isPublic());
        fields.put("private", classDoc.isPrivate());
        fields.put("protected", classDoc.isProtected());
        fields.put("static", classDoc.isStatic());
        fields.put("package", classDoc.containingPackage().name());
        Boolean deprecated = false;
        for(Tag tag : classDoc.tags())
        {
            String name = tag.kind().replace("@", "");
             if (name.equals("deprecated"))
             {
                 deprecated = true;
             }
             else if (name != "param" && name != "throws" && name != "see" && name != "serial" && name != "return")
             {
                 System.out.println("Name val: "+name+" - "+name+" = "+tag.text());
                 fields.put(name, tag.text());
             }
        }

        fields.put("deprecated", deprecated.toString());
        c.setFields(fields);
    }


    private String getType(ProgramElementDoc ped) {

        if (ped.isMethod())
        {
            return "Method";
        }

        ClassDoc classDoc = (ClassDoc)ped;

        if (classDoc.isException()){
            return "Exception";
        }
        else if (classDoc.isInterface()){
            System.out.println("Interface type found");
            return "Interface";
        }
        else if (classDoc.isEnum()){
            return "Enum";
        }else if (classDoc.isAbstract() && !classDoc.isOrdinaryClass()){
            return "Abstract class";
        }
        return "Class";
    }


    public static int optionLength(String option){
        System.out.println("Options: "+option);
        return 2;
    }

    public static boolean validOptions(String[][] options, DocErrorReporter reporter){

        for (String option[] : options){
            if (option.length > 1) {
                System.out.println(option[0] + "=" + option[1]);

                if (option[0].equalsIgnoreCase("-ardoqUsername")) {
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

        new ArdoqDoclet(client, root);

        return true;
    }

}
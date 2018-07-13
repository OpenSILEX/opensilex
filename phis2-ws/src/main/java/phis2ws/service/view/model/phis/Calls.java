//**********************************************************************************************
//                                       Calls.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: January 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  February, 2017
// Subject: Represents the experiment 
//***********************************************************************************************

package phis2ws.service.view.model.phis;

import java.util.ArrayList;

public class Calls {
    private String call;
    private ArrayList<String> datatypes = new ArrayList<>();
    private ArrayList<String> methods = new ArrayList<>();
    private ArrayList<String> versions = new ArrayList<>();

    //Default constructor
    public Calls() {
        
    }    

    /*Constructor with param
    public Calls(String callname) {
        this.callname = callname;
    }
    */

    //Constructor with param
    public Calls(String call, ArrayList<String> datatypes, ArrayList<String> methods, ArrayList<String> versions) {
        this.call = call;
        this.datatypes = datatypes;
        this.methods = methods;
        this.versions = versions;
    }
//
//    
//
//    //Accessor
//    public ArrayList<String> getString() {
//        return this.datatypes;
//    }
//
//    //Mutator
//    public void setString(ArrayList<String> datatypes) {
//        this.datatypes = datatypes;
//    }

//    //Accessor
//    public ArrayList<String> getString() {
//        return this.methods;
//    }
//
//    //Mutator  
//    public void setString(ArrayList<String> methods) {
//        this.methods = methods;
//    }
//
//    //Accessor
//    public ArrayList<String> getString() {
//        return this.versions;
//    }
//
//    //Mutator
//    public void setString(ArrayList<String> versions) {
//        this.versions = versions;
//    }
    

    public String getCall() {
        return call;
    }

    public void setCall(String call) {
        this.call = call;
    }

    public ArrayList<String> getDatatypes() {
        return datatypes;
    }

    public void setDatatypes(ArrayList<String> datatypes) {
        this.datatypes = datatypes;
    }

    public ArrayList<String> getMethods() {
        return methods;
    }

    public void setMethods(ArrayList<String> methods) {
        this.methods = methods;
    }

    public ArrayList<String> getVersions() {
        return versions;
    }

    public void setVersions(ArrayList<String> versions) {
        this.versions = versions;
    }
}

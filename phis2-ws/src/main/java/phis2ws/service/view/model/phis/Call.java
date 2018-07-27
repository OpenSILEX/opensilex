//**********************************************************************************************
//                                       Call.java 
//
// Author(s): Alice Boizet
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: July 2018
// Contact: alice.boizet@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  July 2018
// Subject: Represents the calls 
//***********************************************************************************************

package phis2ws.service.view.model.phis;

import java.util.ArrayList;

public class Call {
    private String call;
    private ArrayList<String> datatypes = new ArrayList<>();
    private ArrayList<String> methods = new ArrayList<>();
    private ArrayList<String> versions = new ArrayList<>();

    //Default constructor
    public Call() {
        
    }    

    //Constructor with param
    public Call(String call, ArrayList<String> datatypes, ArrayList<String> methods, ArrayList<String> versions) {
        this.call = call;
        this.datatypes = datatypes;
        this.methods = methods;
        this.versions = versions;
    }


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

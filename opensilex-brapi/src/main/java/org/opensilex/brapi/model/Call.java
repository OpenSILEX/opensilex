//******************************************************************************
//                          ObservationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.util.ArrayList;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
public class Call {
    private String call;
    private ArrayList<String> dataTypes = new ArrayList<>();
    private ArrayList<String> methods = new ArrayList<>();
    private ArrayList<String> versions = new ArrayList<>();

    //Default constructor
    public Call() {
    }    

    //Constructor with parameters
    public Call(String call, ArrayList<String> dataTypes, ArrayList<String> methods, ArrayList<String> versions) {
        this.call = call;
        this.dataTypes = dataTypes;
        this.methods = methods;
        this.versions = versions;
    }

    public String getCall() {
        return call;
    }

    public void setCall(String call) {
        this.call = call;
    }

    public ArrayList<String> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(ArrayList<String> dataTypes) {
        this.dataTypes = dataTypes;
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

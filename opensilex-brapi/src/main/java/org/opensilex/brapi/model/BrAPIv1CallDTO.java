//******************************************************************************
//                          BrAPIv1ObservationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.util.Set;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1CallDTO {
    private String call;
    private Set<String> dataTypes;
    private Set<String> methods;
    private Set<String> versions;

    //Constructor with parameters
    public BrAPIv1CallDTO(String call, Set<String> dataTypes, Set<String> methods, Set<String> versions) {
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

    public Set<String> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(Set<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public Set<String> getMethods() {
        return methods;
    }

    public void setMethods(Set<String> methods) {
        this.methods = methods;
    }

    public Set<String> getVersions() {
        return versions;
    }

    public void setVersions(Set<String> versions) {
        this.versions = versions;
    }
}

//******************************************************************************
//                          faidarev1ObservationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import java.util.Set;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1CallDTO {
    private String call;
    private Set<String> dataTypes;
    private Set<String> methods;
    private Set<String> versions;

    //Constructor with parameters
    public Faidarev1CallDTO(String call, Set<String> dataTypes, Set<String> methods, Set<String> versions) {
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

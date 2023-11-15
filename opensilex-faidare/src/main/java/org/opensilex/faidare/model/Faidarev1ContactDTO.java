//******************************************************************************
//                          Faidarev1ContactDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1ContactDTO {
    private String contactDbId;
    private String email;
    private String instituteName;
    private String name;
    private String orcid;
    private String type;

    public String getContactDbId() {
        return contactDbId;
    }

    public Faidarev1ContactDTO setContactDbId(String contactDbId) {
        this.contactDbId = contactDbId;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Faidarev1ContactDTO setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public Faidarev1ContactDTO setInstituteName(String instituteName) {
        this.instituteName = instituteName;
        return this;
    }

    public String getName() {
        return name;
    }

    public Faidarev1ContactDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getOrcid() {
        return orcid;
    }

    public Faidarev1ContactDTO setOrcid(String orcid) {
        this.orcid = orcid;
        return this;
    }

    public String getType() {
        return type;
    }

    public Faidarev1ContactDTO setType(String type) {
        this.type = type;
        return this;
    }
}

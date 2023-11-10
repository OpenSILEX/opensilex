//******************************************************************************
//                          Faidarev1ContactDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import org.opensilex.security.person.dal.PersonModel;

/**
 * @author Gabriel Besombes
 */
class Faidarev1ContactDTO {
    private String contactDbId;
    private String email;
    private String instituteName;
    private String name;
    private String orcid;
    private String type;

    public String getContactDbId() {
        return contactDbId;
    }

    public void setContactDbId(String contactDbId) {
        this.contactDbId = contactDbId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Faidarev1ContactDTO extractFromModel(PersonModel model, String role) {
        if (model.getEmail() != null && !model.getEmail().toString().isEmpty()) {
            this.setEmail(model.getEmail().toString());
        }
        this.setContactDbId(model.getUri().toString());
        this.setName(model.getLastName().toUpperCase() + model.getFirstName().substring(0,1).toUpperCase() + model.getFirstName().substring(1));
        if (model.getOrcid() != null && !model.getOrcid().toString().isEmpty()){
            this.setOrcid(model.getOrcid().toString());
        }
        this.setType(role);

        return this;
    }

    public static Faidarev1ContactDTO fromModel(PersonModel model, String role) {
        Faidarev1ContactDTO contact = new Faidarev1ContactDTO();
        return contact.extractFromModel(model, role);
    }
}

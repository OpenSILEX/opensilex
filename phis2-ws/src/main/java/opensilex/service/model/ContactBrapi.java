//******************************************************************************
//                              ContactBrapi.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 Aug. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * BrAPI contact model.
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class ContactBrapi {
    private String contactDbId;
    private String name;
    private String instituteName;
    private String email;
    private String type;
    private String orcid;

    public ContactBrapi() {
    }

    public String getContactDbId() {
        return contactDbId;
    }

    public void setContactDbId(String contactDbId) {
        this.contactDbId = contactDbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
}

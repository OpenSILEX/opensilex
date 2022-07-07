//******************************************************************************
//                          Contact.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.security.user.dal.UserModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
class Contact {
    private String contactDbId;
    private String email;
    private String institutionName;
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

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
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

    public static List<Contact> fromContact(List<UserModel> user){
        List<Contact> contactList = new ArrayList<>() ;

        for (UserModel userModel : user){
            Contact contact = new Contact();
            contact.setName(userModel.getFirstName()+" "+userModel.getLastName());
            contact.setEmail(userModel.getEmail().toString());
            contact.setContactDbId(userModel.getUri().toString());
            contact.setType(userModel.getType().toString());

            contactList.add(contact);
        }

        return contactList;
    }
    
    
}

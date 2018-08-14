//**********************************************************************************************
//                                       User.java 
//
// Author(s): Anne TIREAU, Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: may 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr, 
//         morgane.vidal@inra.fr
// Last modification date:  April, 2017
// Subject: Represents a user
//***********************************************************************************************
package phis2ws.service.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import phis2ws.service.view.model.phis.Group;

/**
 *
 * @author tireau
 * @update AC 07/16 Ajout des champ de classe user et implémentation du DAO USER
 * @update [Morgane Vidal] 04/26 ajout des attributs phone, affiliation, orcid.
 * suppression des attributs isAdmin, role, type a impacté la classe
 * UserDaoPhisBrapi. Et ajout de la liste des groupes auxquels l'utilisateur
 * appartient (utilisé pour la création d'un utilisateur)
 */
@ApiModel
public class User {

    private String email;
    private String password;
    private String firstName;
    private String familyName;
    private String address;
    private String phone;
    private String affiliation;
    private String uri; // uri of user eg. http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy
    private String orcid;
    private String admin;
    private String available;

    private ArrayList<Group> groups = new ArrayList<>();

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @ApiModelProperty(hidden = true)
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    @ApiModelProperty(hidden = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @ApiModelProperty(hidden = true)
    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    @ApiModelProperty(hidden = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ApiModelProperty(hidden = true)
    public String getAvailable() {
        return available;
    }

    public void setAvailable(String available) {
        this.available = available;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public ArrayList<Group> getGroups() {
        return this.groups;
    }

    public void addGroup(Group group) {
        this.groups.add(group);
    }

    public void setGroupList(ArrayList<Group> groups) {
        this.groups = groups;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}

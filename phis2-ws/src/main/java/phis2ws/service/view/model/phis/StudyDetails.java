//******************************************************************************
//                                       StudyDetails.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents the study format for the brapi/studies service response
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudyDetails {
    private String studyDbId;
    private String studyName;
    private String studyTypeDbId;
    private String studyTypeName;
    private String studyDescription;
    private ArrayList<String> seasons;
    private String commonCropName;
    private String trialDbId;
    private String trialName;
    private String startDate;
    private String endDate;
    private Boolean active; 
    private String license;
    private String location; //TODO class Location
    private ArrayList<ContactBrapi> contacts = new ArrayList<ContactBrapi>();
    private ArrayList<String> dataLinks; //TODO class Datalink
    private ArrayList<String> lastUpdate; //TODO 
    private HashMap<String,String> additionalInfo; //TODO
    private String documentationURL;

    public StudyDetails() {
    }

    public StudyDetails(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public String getStudyType() {
        return studyTypeName;
    }

    public void setStudyType(String studyTypeName) {
        this.studyTypeName = studyTypeName;
    }

    public String getStudyDescription() {
        return studyDescription;
    }

    public void setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
    }

    public ArrayList<String> getSeasons() {
        return seasons;
    }

    public void setSeasons(ArrayList<String> seasons) {
        this.seasons = seasons;
    }

    public String getTrialDbId() {
        return trialDbId;
    }

    public void setTrialDbId(String trialDbId) {
        this.trialDbId = trialDbId;
    }

    public String getTrialName() {
        return trialName;
    }

    public void setTrialName(String trialName) {
        this.trialName = trialName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLocation() {
        return location;
    }

    public void setSLocation(String location) {
        this.location = location;
    }

    public ArrayList<ContactBrapi> getContacts() {
        return contacts;
    }

    public void addContact(ContactBrapi contact) {
        this.contacts.add(contact);
    }

    public ArrayList<String> getDataLinks() {
        return dataLinks;
    }

    public void setDataLinks(ArrayList<String> dataLinks) {
        this.dataLinks = dataLinks;
    }

    public ArrayList<String> getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(ArrayList<String> lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public HashMap<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(HashMap<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }    

    public String getStudyTypeDbId() {
        return studyTypeDbId;
    }

    public void setStudyTypeDbId(String studyTypeDbId) {
        this.studyTypeDbId = studyTypeDbId;
    }

    public String getStudyTypeName() {
        return studyTypeName;
    }

    public void setStudyTypeName(String studyTypeName) {
        this.studyTypeName = studyTypeName;
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public void setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
    }
    
}


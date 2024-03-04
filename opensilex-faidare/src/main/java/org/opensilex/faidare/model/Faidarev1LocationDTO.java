//******************************************************************************
//                          Faidarev1LocationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;


import java.util.Map;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1LocationDTO {
    private String abbreviation;
    private String abreviation;
    private Map<String, String> additionalInfo;
    private Double altitude;
    private String countryCode;
    private String countryName;
    private String documentationURL;
    private String instituteAddress;
    private String instituteAdress;
    private String instituteName;
    private Double latitude;
    private String locationDbId;
    private String locationName;
    private String name;
    private String locationType;
    private Double longitude;

    public String getAbbreviation() {
        return abbreviation;
    }

    public Faidarev1LocationDTO setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
        return this;
    }

    public String getAbreviation() {
        return abreviation;
    }

    public Faidarev1LocationDTO setAbreviation(String abreviation) {
        this.abreviation = abreviation;
        return this;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public Faidarev1LocationDTO setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    public Double getAltitude() {
        return altitude;
    }

    public Faidarev1LocationDTO setAltitude(Double altitude) {
        this.altitude = altitude;
        return this;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Faidarev1LocationDTO setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public String getCountryName() {
        return countryName;
    }

    public Faidarev1LocationDTO setCountryName(String countryName) {
        this.countryName = countryName;
        return this;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public Faidarev1LocationDTO setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
        return this;
    }

    public String getInstituteAdress() {
        return instituteAdress;
    }

    public Faidarev1LocationDTO setInstituteAdress(String instituteAdress) {
        this.instituteAdress = instituteAdress;
        return this;
    }

    public String getInstituteAddress() {
        return instituteAddress;
    }

    public Faidarev1LocationDTO setInstituteAddress(String instituteAddress) {
        this.instituteAddress = instituteAddress;
        return this;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public Faidarev1LocationDTO setInstituteName(String instituteName) {
        this.instituteName = instituteName;
        return this;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Faidarev1LocationDTO setLatitude(Double latitude) {
        this.latitude = latitude;
        return this;
    }

    public String getLocationDbId() {
        return locationDbId;
    }

    public Faidarev1LocationDTO setLocationDbId(String locationDbId) {
        this.locationDbId = locationDbId;
        return this;
    }

    public String getLocationName() {
        return locationName;
    }

    public Faidarev1LocationDTO setLocationName(String locationName) {
        this.locationName = locationName;
        return this;
    }

    public String getName() {
        return name;
    }

    public Faidarev1LocationDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getLocationType() {
        return locationType;
    }

    public Faidarev1LocationDTO setLocationType(String locationType) {
        this.locationType = locationType;
        return this;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Faidarev1LocationDTO setLongitude(Double longitude) {
        this.longitude = longitude;
        return this;
    }
}

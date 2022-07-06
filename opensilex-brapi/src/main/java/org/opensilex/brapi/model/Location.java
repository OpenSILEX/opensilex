//******************************************************************************
//                          Location.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
public class Location {
    private String abbreviation;
    private Map additionalInfo;
    private double altitude;
    private String countryCode;
    private String countryName;
    private String documentationURL;
    private String instituteAdress;
    private String instituteName;
    private double latitude;
    private URI locationDbId;
    private String locationName;
    private String locationType;
    private double longitude;

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public Map getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryCode) {
        this.countryName = countryName;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
    }

    public String getInstituteAddress() {
        return instituteAdress;
    }

    public void setInstituteAddress(String instituteAdress) {
        this.instituteAdress = instituteAdress;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public URI getLocationDbId() {
        return locationDbId;
    }

    public void setLocationDbId(URI locationDbId) {
        this.locationDbId = locationDbId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public static List<Location> fromFacilities(List<InfrastructureFacilityModel> facilities){
        List<Location> location = new ArrayList<>() ;
        for (InfrastructureFacilityModel infrastructure : facilities){
            Location loc = new Location();
            loc.setLocationDbId(infrastructure.getUri());

            if(infrastructure.getAddress()!=null) {
                loc.setInstituteAddress(infrastructure.getAddress().getStreetAddress());
                loc.setCountryName(infrastructure.getAddress().getCountryName());
            }

            loc.setLocationName(infrastructure.getName());
            loc.setLocationType(infrastructure.getTypeLabel().getDefaultValue());

            location.add(loc);
        }
        return location;
    }
}

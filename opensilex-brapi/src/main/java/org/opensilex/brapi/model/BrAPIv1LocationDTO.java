//******************************************************************************
//                          BrAPIv1LocationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;


import com.mongodb.client.model.geojson.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.site.SiteAddressModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.rest.validation.model.OpenSilexLocale;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
class BrAPIv1LocationDTO {
    private String abbreviation;
    private Map<String, String> additionalInfo;
    private Double altitude;
    private String countryCode;
    private String countryName;
    private String documentationURL;
    private String instituteAddress;
    private String instituteName;
    private Double latitude;
    private String locationDbId;
    private String locationName;
    private String locationType;
    private Double longitude;

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
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

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
    }

    public String getInstituteAddress() {
        return instituteAddress;
    }

    public void setInstituteAddress(String instituteAddress) {
        this.instituteAddress = instituteAddress;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getLocationDbId() {
        return locationDbId;
    }

    public void setLocationDbId(String locationDbId) {
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

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public BrAPIv1LocationDTO extractFromModel(FacilityModel model, FacilityDAO facilityDAO, OrganizationDAO organizationDAO, AccountModel currentAccount) throws Exception {
        this.setLocationDbId(model.getUri().toString());

        this.setLocationName(model.getName());
        this.setLocationType(model.getType().toString());

        if (facilityDAO.getFacilityGeospatialModel(model.getUri()) != null){
            Geometry facilityGeometry = facilityDAO.getFacilityGeospatialModel(model.getUri()).getGeometry();
            org.locationtech.jts.geom.Geometry facilityJtsGeometry = new GeoJsonReader().read(facilityGeometry.toJson());

            if (!facilityJtsGeometry.isEmpty()){

                Point centroid = facilityJtsGeometry.getCentroid();
                this.setLongitude(centroid.getX());
                this.setLatitude(centroid.getY());
            }
        }

        // Try to get a Site from the hierarchy of Organisations
        // Initialize with the Organisations hosted by the facility
        Set<OrganizationModel> directParentOrganizations = new HashSet<>(organizationDAO.search(new OrganizationSearchFilter()
                .setFacilityURI(model.getUri())
                .setUser(currentAccount)));
        while (!directParentOrganizations.isEmpty()){
            List<OrganizationModel> parentsWithOneAddress = directParentOrganizations.stream().map(parentOrg -> {
                OrganizationModel parentModel;
                try {
                    parentModel = organizationDAO.get(parentOrg.getUri(), currentAccount);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (parentModel.getSites().size() == 1) {
                    return parentModel;
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            if (parentsWithOneAddress.size()==1) { // If exactly one organisation on this level with exactly one site
                OrganizationModel institute = organizationDAO.get(parentsWithOneAddress.get(0).getUri(), currentAccount);
                SiteAddressModel parentAddress = institute.getSites().get(0).getAddress();
                this.setInstituteAddress(parentAddress.toString());
                this.setInstituteName(institute.getName());
                String countryName = parentAddress.getCountryName();
                this.setCountryName(countryName);
                this.setCountryCode(new OpenSilexLocale(countryName).getISO3Country());
                directParentOrganizations.remove(parentsWithOneAddress.get(0));
            } else if (parentsWithOneAddress.size()>1) { // If more than one, go an Organisation level above
                Set<OrganizationModel> newParents = new HashSet<>();
                for (OrganizationModel childOrga : directParentOrganizations) {
                    newParents.addAll(organizationDAO.search(new OrganizationSearchFilter()
                            .setDirectChildURI(childOrga.getUri())
                            .setUser(currentAccount)));
                }
                directParentOrganizations = newParents;
            } else {
                directParentOrganizations = Collections.emptySet();
            }
        }

        if (model.getAddress() != null && !model.getAddress().toString().isEmpty()){
            String countryName = model.getAddress().getCountryName();
            this.setCountryName(countryName);
            this.setCountryCode(new OpenSilexLocale(countryName).getISO3Country());
        }

        return this;
    }

    public static BrAPIv1LocationDTO fromModel(FacilityModel model, FacilityDAO facilityDAO, OrganizationDAO organizationDAO, AccountModel currentAccount) throws Exception {
        BrAPIv1LocationDTO location = new BrAPIv1LocationDTO();
        return location.extractFromModel(model, facilityDAO, organizationDAO, currentAccount);
    }
}

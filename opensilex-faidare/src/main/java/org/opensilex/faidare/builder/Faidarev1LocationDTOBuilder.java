package org.opensilex.faidare.builder;

import com.mongodb.client.model.geojson.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.site.SiteAddressModel;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.util.*;
import java.util.stream.Collectors;

public class Faidarev1LocationDTOBuilder {
    private final FacilityDAO facilityDAO;
    private final OrganizationDAO organizationDAO;

    public Faidarev1LocationDTOBuilder(FacilityDAO facilityDAO, OrganizationDAO organizationDAO) {
        this.facilityDAO = facilityDAO;
        this.organizationDAO = organizationDAO;
    }

    public Faidarev1LocationDTO fromModel(FacilityModel model, AccountModel currentAccount) throws Exception {
        Faidarev1LocationDTO dto = new Faidarev1LocationDTO();
        dto.setLocationDbId(SPARQLDeserializers.getExpandedURI(model.getUri()))
                .setLocationName(model.getName())
                .setName(model.getName())
                .setLocationType(SPARQLDeserializers.getExpandedURI(model.getType()));


        if (facilityDAO.getFacilityGeospatialModel(model.getUri()) != null){
            Geometry facilityGeometry = facilityDAO.getFacilityGeospatialModel(model.getUri()).getGeometry();

            org.locationtech.jts.geom.Geometry facilityJtsGeometry = new GeoJsonReader().read(facilityGeometry.toJson());

            if (!facilityJtsGeometry.isEmpty()){

                Point centroid = facilityJtsGeometry.getCentroid();
                dto.setLongitude(centroid.getX())
                        .setLatitude(centroid.getY());
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
                if (parentModel.getSites().size() == 1 && parentModel.getSites().get(0).getAddress() != null) {
                    return parentModel;
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            if (parentsWithOneAddress.size()==1) { // If exactly one organisation on this level with exactly one site
                OrganizationModel institute = organizationDAO.get(parentsWithOneAddress.get(0).getUri(), currentAccount);
                SiteAddressModel parentAddress = institute.getSites().get(0).getAddress();
                dto.setInstituteAddress(parentAddress.toString())
                        .setInstituteAdress(parentAddress.toString())
                        .setInstituteName(institute.getName());
                String countryName = parentAddress.getCountryName();
                dto.setCountryName(countryName)
                        .setCountryCode(new Locale(countryName).getISO3Country());
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
            dto.setCountryName(countryName);
            dto.setCountryCode(new Locale(countryName).getISO3Country());
        }

        return dto;
    }
}

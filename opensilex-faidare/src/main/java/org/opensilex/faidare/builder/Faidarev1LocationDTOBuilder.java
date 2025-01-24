/*
 * *****************************************************************************
 *                         Faidarev1LocationDTOBuilder.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 15/07/2024 13:31
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare.builder;

import com.mongodb.client.model.geojson.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.site.SiteAddressModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.faidare.Countries;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Faidarev1LocationDTOBuilder {
    private final FacilityLogic facilityLogic;
    private final OrganizationDAO organizationDAO;
    private static final List<Countries.CountryMap> countriesList;

    static {
        try {
            countriesList = Countries.getCountriesList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Faidarev1LocationDTOBuilder(FacilityLogic facilityLogic, OrganizationDAO organizationDAO) {
        this.facilityLogic = facilityLogic;
        this.organizationDAO = organizationDAO;
    }

    public Faidarev1LocationDTO fromModel(FacilityModel model, AccountModel currentAccount) throws Exception {
        Faidarev1LocationDTO dto = new Faidarev1LocationDTO();
        dto.setLocationDbId(SPARQLDeserializers.getExpandedURI(model.getUri()))
                .setLocationName(model.getName())
                .setName(model.getName())
                .setLocationType(SPARQLDeserializers.getExpandedURI(model.getType()));


        if (facilityLogic.getLastFacilityLocationModel(model) != null){
            Geometry facilityGeometry = facilityLogic.getLastFacilityLocationModel(model).getLocation().getGeometry();

            org.locationtech.jts.geom.Geometry facilityJtsGeometry = new GeoJsonReader().read(facilityGeometry.toJson());

            if (!facilityJtsGeometry.isEmpty()){

                Point centroid = facilityJtsGeometry.getCentroid();
                dto.setLongitude(centroid.getX())
                        .setLatitude(centroid.getY());
            }
        }

        // If exactly one parent organization use it as institute
        Set<OrganizationModel> directParentOrganizations = new HashSet<>(organizationDAO.search(new OrganizationSearchFilter()
                .setFacilityURI(model.getUri())
                .setUser(currentAccount)));
        if (directParentOrganizations.size() == 1){
            OrganizationModel parentOrganization = new ArrayList<>(directParentOrganizations).get(0);
            dto.setInstituteName(parentOrganization.getName());
            List<SiteModel> sites = parentOrganization.getSites();
            if(sites.size() == 1){
                SiteAddressModel parentAddress = sites.get(0).getAddress();
                if (Objects.nonNull(parentAddress)){
                    dto.setInstituteAddress(parentAddress.toString())
                            .setInstituteAdress(parentAddress.toString());
                    String countryName = parentAddress.getCountryName();
                    setCountryCodeFromName(dto, countryName);
                }
            }
        }

        // If facility has an address use its country for country name and code instead of its parent organization's
        if (model.getAddress() != null && !model.getAddress().toString().isEmpty()){
            String countryName = model.getAddress().getCountryName();
            dto.setCountryName(countryName);
            setCountryCodeFromName(dto, countryName);
        }

        return dto;
    }

    private void setCountryCodeFromName(Faidarev1LocationDTO dto, String countryName) {
        List<Countries.CountryMap> matchingCountries = countriesList.stream()
                .filter(countryMap -> Objects.equals(countryMap.getEnCountryName(), countryName) || Objects.equals(countryMap.getFrCountryName(), countryName))
                .collect(Collectors.toList());
        if (matchingCountries.size() == 1){
            dto.setCountryCode(
                    matchingCountries.get(0).getAlpha3code()
            );
        }
    }
}

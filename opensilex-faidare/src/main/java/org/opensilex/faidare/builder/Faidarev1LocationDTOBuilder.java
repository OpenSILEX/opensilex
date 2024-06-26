/*
 * *****************************************************************************
 *                         Faidarev1LocationDTOBuilder.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/06/2024 10:32
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare.builder;

import com.mongodb.client.model.geojson.Geometry;
import io.github.castmart.jcountry.Country;
import io.github.castmart.jcountry.CountryDB;
import io.github.castmart.jcountry.JCountry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.site.SiteAddressModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.util.*;

public class Faidarev1LocationDTOBuilder {
    private final FacilityDAO facilityDAO;
    private final OrganizationDAO organizationDAO;
    private static final JCountry jcountry = JCountry.getInstance();
    private static final CountryDB countryDB = jcountry.getCountriesDB();
    private final Map<String, Country> countriesByName = countryDB.getCountriesMapByName();
    private static final ResourceBundle frenchCountriesTranslations = countryDB.getCountriesTranslations(Locale.FRENCH).get();

    private final static Map<String, String> countriesTranslations = new HashMap<>() {{
        for (Iterator<String> it = frenchCountriesTranslations.getKeys().asIterator(); it.hasNext(); ) {
            String countryNameEnglish = it.next();
            put(frenchCountriesTranslations.getObject(countryNameEnglish).toString(), countryNameEnglish);
        }
    }};

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
            setCountryCodeFromName(dto, countryName);
        }

        return dto;
    }

    private void setCountryCodeFromName(Faidarev1LocationDTO dto, String countryName) {
        dto.setCountryName(countryName);
        if (countriesByName.containsKey(countryName)){
            dto.setCountryCode(
                    countriesByName.get(countryName).getAlpha3()
            );
        } else {

            if (countriesTranslations.containsKey(countryName) && countriesByName.containsKey(countriesTranslations.get(countryName))) {
                dto.setCountryCode(
                        countriesByName.get(countriesTranslations.get(countryName)).getAlpha3()
                );
            }

        }
    }
}

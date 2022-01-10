package org.opensilex.core.address.dal;

import org.apache.jena.vocabulary.VCARD4;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.util.UUID;

/**
 * An address corresponding to the VCARD specification.
 *
 * @author Valentin RIGOLLE
 */
@SPARQLResource(
        ontology = VCARD4.class,
        resource = "Address"
)
public class AddressModel extends SPARQLResourceModel implements ClassURIGenerator<AddressModel> {
    @SPARQLProperty(
            ontology = VCARD4.class,
            property = "country_name"
    )
    protected String countryName;

    @SPARQLProperty(
            ontology = VCARD4.class,
            property = "locality"
    )
    protected String locality;

    @SPARQLProperty(
            ontology = VCARD4.class,
            property = "postal_code"
    )
    protected String postalCode;

    @SPARQLProperty(
            ontology = VCARD4.class,
            property = "region"
    )
    protected String region;

    @SPARQLProperty(
            ontology = VCARD4.class,
            property = "street_address"
    )
    protected String streetAddress;

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    @Override
    public String[] getInstancePathSegments(AddressModel instance) {
        return new String[] {
                "address",
                UUID.randomUUID().toString()
        };
    }
}

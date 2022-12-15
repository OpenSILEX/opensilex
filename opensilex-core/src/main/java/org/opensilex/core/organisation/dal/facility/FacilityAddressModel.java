package org.opensilex.core.organisation.dal.facility;

import org.apache.jena.vocabulary.VCARD4;
import org.opensilex.core.address.dal.AddressModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

/**
 * Address of a facility.
 *
 * @author Valentin RIGOLLE
 */
@SPARQLResource(
        ontology = VCARD4.class,
        resource = "Address",
        graph = OrganizationModel.GRAPH
)
public class FacilityAddressModel extends AddressModel {
    @SPARQLProperty(
            ontology = VCARD4.class,
            property = "hasAddress",
            inverse = true
    )
    protected FacilityModel facility;

    public FacilityModel getFacility() {
        return facility;
    }

    public void setFacility(FacilityModel facility) {
        this.facility = facility;
    }
}

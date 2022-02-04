package org.opensilex.core.organisation.dal;

import org.apache.jena.vocabulary.VCARD4;
import org.opensilex.core.address.dal.AddressModel;
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
        graph = InfrastructureModel.GRAPH
)
public class FacilityAddressModel extends AddressModel {
    @SPARQLProperty(
            ontology = VCARD4.class,
            property = "hasAddress",
            inverse = true
    )
    protected InfrastructureFacilityModel facility;

    public InfrastructureFacilityModel getFacility() {
        return facility;
    }

    public void setFacility(InfrastructureFacilityModel facility) {
        this.facility = facility;
    }
}

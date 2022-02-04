package org.opensilex.core.organisation.dal;

import org.apache.jena.vocabulary.ORG;
import org.apache.jena.vocabulary.VCARD4;
import org.opensilex.core.address.dal.AddressModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

/**
 * Address of a site.
 *
 * @author Valentin RIGOLLE
 */
@SPARQLResource(
        ontology = VCARD4.class,
        resource = "Address",
        graph = InfrastructureModel.GRAPH
)
public class SiteAddressModel extends AddressModel {
    @SPARQLProperty(
            ontology = ORG.class,
            property = "siteAddress",
            inverse = true
    )
    protected SiteModel site;
    public static final String SITE_FIELD = "site";

    public SiteModel getSite() {
        return site;
    }

    public void setSite(SiteModel site) {
        this.site = site;
    }
}

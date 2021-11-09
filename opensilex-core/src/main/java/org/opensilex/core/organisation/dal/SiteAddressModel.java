package org.opensilex.core.organisation.dal;

import org.apache.jena.vocabulary.ORG;
import org.opensilex.core.address.dal.AddressModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Address",
        graph = "set/infrastructures"
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


package org.opensilex.core.experiment.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

/**
 * @author efernandez
 A simple model which define an instance of the FundingModel class
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Funding"
)
public class FundingModel extends SPARQLNamedResourceModel<FundingModel> {

    public static final String GRAPH = "funding";

}
//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.germplasmGroup.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.util.List;

/**
 * @author Maximilian HART
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "GermplasmGroup",
        graph = GermplasmGroupModel.GRAPH
)
public class GermplasmGroupModel extends SPARQLNamedResourceModel<GermplasmGroupModel> {

    public static final String GRAPH = "germplasmGroup";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "description";

    
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "member"
    )
    private List<GermplasmModel> germplasmList;
    public static final String GERMPLASM_LIST_FIELD = "germplasmList";
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<GermplasmModel> getGermplasmList() {
        return germplasmList;
    }
    
    public void setGermplasmList(List<GermplasmModel> germplasm) {
        this.germplasmList = germplasm;
    }
}

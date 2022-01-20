//******************************************************************************
//                          SpeciesDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.species.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Renaud COLIN
 */
public class SpeciesDAO {

    protected final SPARQLService sparql;

    public SpeciesDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     *
     * @param lang the {@link SpeciesModel} {@link org.apache.jena.vocabulary.RDFS#label} lang
     * @return the list of all {@link SpeciesModel} in the given lang
     */
    public List<SpeciesModel> getAll(String lang) throws Exception {

        List<OrderBy> orderByList = new ArrayList();
        orderByList.add(new OrderBy("label=asc"));

        return sparql.search(
                SpeciesModel.class,
                lang,
                (SelectBuilder select) -> {
                    select.addFilter(SPARQLQueryHelper.eq(SPARQLResourceModel.TYPE_FIELD, new URI(Oeso.Species.toString())));
                },
                orderByList
        );
    }

    public List<SpeciesModel> getByExperiment(URI xpUri, String lang) throws Exception {
        List<OrderBy> orderByList = new ArrayList();
        orderByList.add(new OrderBy("label=asc"));

        return sparql.search(
                SpeciesModel.class,
                lang,
                (SelectBuilder select) -> {
                    select.addFilter(SPARQLQueryHelper.eq(SPARQLResourceModel.TYPE_FIELD, new URI(Oeso.Species.toString())));
                    select.addWhere(SPARQLDeserializers.nodeURI(xpUri), Oeso.hasSpecies, makeVar(SpeciesModel.URI_FIELD));
                },
                orderByList
        );
    }

    /**
     * Get all species indirectly associated with the experiment, i.e. the species associated with the germplasms of the
     * scientific objects in the experiment.
     *
     * @param experimentUri
     * @return
     * @throws Exception
     */
    public List<SpeciesModel> getFromExperimentScientificObjects(URI experimentUri) throws Exception {
        // For the sake of performance, all the species will be retrieved in one query. This query has the following
        // form :
        //
        // select distinct ?species {
        //    graph <__experimentUri__> {
        //        ?scientificObject a ?rdfType.
        //    }
        //    ?rdfType rdfs:subClassOf* vocabulary:ScientificObject.
        //    ?scientificObject vocabulary:hasGermplasm ?germplasm.
        //    {
        //        ?germplasm a/rdfs:subClassOf* vocabulary:Species.
        //        bind(?germplasm as ?species)
        //    } union {
        //        ?germplasm vocabulary:fromSpecies ?species.
        //    }
        // }
        //
        // A union is used to handle both possible cases : either the germplasm itself is a species (first case), or
        // the germplasm derives from a species (second case).

        // Select and var creation
        SelectBuilder select = new SelectBuilder();
        Var scientificObjectVar = makeVar("scientificObject");
        Var germplasmVar = makeVar("germplasm");
        Var rdfTypeVar = makeVar("rdfType");
        Var speciesVar = makeVar("species");

        // Useful paths to reuse
        Path subClassOf = new P_ZeroOrMore1(new P_Link(RDFS.subClassOf.asNode()));
        Path aSubClassOf = new P_Seq(new P_Link(RDF.type.asNode()), subClassOf);

        // distinct ?species
        select.setDistinct(true);
        select.addVar(speciesVar);

        // selection of the scientific object and its germplasm
        select.addGraph(SPARQLDeserializers.nodeURI(experimentUri),
                scientificObjectVar, RDF.type.asNode(), rdfTypeVar);
        select.addWhere(
                rdfTypeVar, subClassOf, Oeso.ScientificObject.asNode());
        select.addWhere(scientificObjectVar, Oeso.hasGermplasm.asNode(), germplasmVar);

        // The two cases for the species
        WhereBuilder whereIsSpecies = new WhereBuilder();
        WhereBuilder whereFromSpecies = new WhereBuilder();

        // First case : the germplasm is a species
        whereIsSpecies.addWhere(germplasmVar, aSubClassOf, Oeso.Species.asNode());
        whereIsSpecies.addBind(new ExprVar(germplasmVar), speciesVar);

        // Second case : the germplasm is derived from a species
        whereFromSpecies.addWhere(germplasmVar, Oeso.fromSpecies.asNode(), speciesVar);

        // Union
        select.addWhere(
                whereIsSpecies.addUnion(whereFromSpecies)
        );

        // Transform the result into SpeciesModel
        List<SPARQLResult> results = sparql.executeSelectQuery(select);
        List<SpeciesModel> speciesList = new ArrayList<>();
        for (SPARQLResult result : results) {
            URI speciesUri = new URI(result.getStringValue(speciesVar.getVarName()));
            SpeciesModel species = new SpeciesModel();
            species.setUri(speciesUri);
            speciesList.add(species);
        }

        return speciesList;
    }
}

/*
 * *****************************************************************************
 *                         FactorCategoryDeleteVerificationAskQueryBuilder.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 29/05/2026 10:30
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.experiment.factor.dal;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.ontology.dal.ClassSpecificDeleteVerificationAskQueryProvider;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class FactorCategoryDeleteVerificationAskQueryBuilder implements ClassSpecificDeleteVerificationAskQueryProvider {

    /**
     * @return a AskBuilder with a query returning true if the class is a subtype of vocabulary:FactorCategory and is linked with at least one factor
     * @implNote query generated (replacing classURI by the real uri) :
     * ASK {
     *    classURI rdf:type owl:Class .
     *    classURI rdfs:subClassOf* vocabulary:FactorCategory .
     *    ?linkedCategory vocabulary:hasCategory classURI .
     * }
     */
    @Override
    public AskBuilder getFactorCategoryDeleteVerificationAskQuery(URI factorCategoryURI) {

        Var linkedCategory = makeVar("linkedCategory");
        Node factorCategoryUriNode = SPARQLDeserializers.nodeURI(factorCategoryURI);

        return new AskBuilder()
                .addWhere(factorCategoryUriNode, RDF.type, OWL.Class)
                .addWhere(factorCategoryUriNode, Ontology.subClassAny, Oeso.FactorCategory).
                addWhere(linkedCategory, Oeso.hasCategory, factorCategoryUriNode);
    }

    @Override
    public String getErrorTranslationKey() {
        return "component.ontology.class.exception.delete.factor-category";
    }
}

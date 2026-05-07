/*
 * *****************************************************************************
 *                         ChangeTypeParametersUri.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 06/03/2026 11:27
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.migration.one_point_five_ALL;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.vueOwlExtension.VueOwlExtension;
import org.opensilex.core.ontology.vueOwlExtension.dal.VueOwlExtensionDAO;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.opensilex.uri.generation.URIGenerator;

import java.time.OffsetDateTime;
import java.util.Objects;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;


/**
 * This migration aims to change uri of type parameters in order to separate them from their associated type.
 * What we call parameter is the subject of type vocabulary:owl-vue-extension#ClassExtension
 * Before, the dynamic type has the same uri as its parameters. After the parameter has as uri [type uri]/owl-vue-extension
 * @implNote example with a border type with uri vocabulary:border
 * <pre>
 * before :
 * vocabulary:border rdf:type owl:Class
 * vocabulary:border rdf:type  http://prefix/vocabulary/owl-vue-extension#ClassExtension
 * vocabulary:border dc:publisher prefix:user/account.myaccount
 * vocabulary:border http://prefix/vocabulary/owl-vue-extension#hasIcon "myIcon"
 * vocabulary:border http://prefix/vocabulary/owl-vue-extension#isAbstractClass true
 *
 * _________________________________________________________________________________________
 *
 * after :
 * vocabulary:border rdf:type owl:Class
 * vocabulary:border dc:publisher prefix:user/account.myaccount
 * vocabulary:border http://prefix/vocabulary/owl-vue-extension#hasVueExtensionClassMode vocabulary:border/owl-vue-extension
 *
 * vocabulary:border/owl-vue-extension rdf:type  http://prefix/vocabulary/owl-vue-extension#ClassExtension
 * vocabulary:border/owl-vue-extension http://prefix/vocabulary/owl-vue-extension#hasIcon "myIcon"
 * vocabulary:border/owl-vue-extension http://prefix/vocabulary/owl-vue-extension#isAbstractClass true
 * </pre>
 */
public class ChangeTypeParametersUri implements OpenSilexModuleUpdate {

    public final static String DESCRIPTION = "This migration change the uri of type parameters in order to separate them from their associated type (when manually created a type with ontologie API or interface).";
    private SPARQLService sparql;

    public void setSparql(SPARQLService sparql) {
        this.sparql = sparql;
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        if (Objects.isNull(sparql)) {
            SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
            this.sparql = factory.provide();
        }
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        try {
            Node graph = NodeFactory.createURI(
                    sparql.getBaseURI().toString()
                            .concat(SPARQLClassObjectMapper.DEFAULT_GRAPH_KEYWORD)
                            .concat(URIGenerator.URI_SEPARATOR)
                            .concat(OntologyDAO.CUSTOM_TYPES_AND_PROPERTIES_GRAPH)
            );

            sparql.executeUpdateQuery(generateUpdateRequestForTypeParameters(graph));
        } catch (Exception e){
            throw new OpensilexModuleUpdateException("an error occurred while migrating type parameters uris during ChangeTypeParametersUri migration ", e);
        }
    }

    /**
     * this request update the uri of every types and type parameters by appending "/owl-vue-extension".
     * return the UpdateBuilder with delete and insert request
     * @implNote  generated query example : (the filter clause appears only if excludedPredicates is not empty).
     * The last filter clause prevent from updating already updated parameters.
     * <pre>
     * WITH http://prefix/set/properties
     * DELETE {
     *      ?uriToDelete rdf:type  http://prefix/vocabulary/owl-vue-extension#ClassExtension.
     *      ?uriToDelete  http://prefix/vocabulary/owl-vue-extension#hasIcon ?icon.
     *      ?uriToDelete http://prefix/vocabulary/owl-vue-extension#isAbstractClass ?isAbstract.
     *      ?fromOwlClass http://prefix/vocabulary/owl-vue-extension#fromOwlClass ?uriToDelete.
     * }
     * INSERT {
     *     ?uriToCreate rdf:type  http://prefix/vocabulary/owl-vue-extension#ClassExtension.
     *     ?uriToCreate  http://prefix/vocabulary/owl-vue-extension#hasIcon ?icon.
     *     ?uriToCreate http://prefix/vocabulary/owl-vue-extension#isAbstractClass ?isAbstract.
     *     ?fromOwlClass http://prefix/vocabulary/owl-vue-extension#fromOwlClass ?uriToInsert.
     *     ?uriToDelete http://prefix/vocabulary/owl-vue-extension#hasVueExtensionClassMode ?uriToCreate
     * }
     * WHERE {
     *      ?uriToDelete rdf:type http://prefix/vocabulary/owl-vue-extension#ClassExtension .
     *      OPTIONAL { ?uriToDelete  http://prefix/vocabulary/owl-vue-extension#hasIcon ?icon. }
     *      OPTIONAL { ?uriToDelete http://prefix/vocabulary/owl-vue-extension#isAbstractClass ?isAbstract. }
     *      OPTIONAL { ?fromOwlClass http://prefix/vocabulary/owl-vue-extension#fromOwlClass ?uriToDelete. }
     *      BIND (URI(CONCAT(STR(?uriToDelete), "/owl-vue-extension"))AS ?uriToCreate)
     *      FILTER (!STRENDS(STR(?uriToDelete), "/owl-vue-extension"))
     * }
     * </pre>
     **/
    private UpdateBuilder generateUpdateRequestForTypeParameters(Node graph){
        Var uriToDeleteVar = makeVar("uriToDelete");
        Var uriToInsertVar = makeVar("uriToInsert");
        Var iconVar = makeVar("icon");
        Var isAbstractVar = makeVar("isAbstract");
        Var fromOwlClassVar = makeVar("fromOwlClass");

        Node classExtensionType = VueOwlExtension.ClassExtension.asNode();
        Node rdfTypeUri = RDF.type.asNode();
        Node hasIconUri = VueOwlExtension.hasIcon.asNode();
        Node isAbstractClassUri = VueOwlExtension.isAbstractClass.asNode();
        Node fromOwlClassUri = VueOwlExtension.fromOwlClass.asNode();
        Node uriSuffixStrinfNode = NodeFactory.createLiteralString(VueOwlExtensionDAO.URI_SUFFIX);
        Node hasVueExtensionClassNode = VueOwlExtension.hasVueExtensionClassModel.asNode();

        ExprFactory factory = new ExprFactory();
        Expr strUriExpr = factory.str(uriToDeleteVar);
        Expr concatUriExpr = factory.concat(strUriExpr, uriSuffixStrinfNode);
        Expr uriBindExpr = factory.iri(concatUriExpr);

        Expr strEndsExpr = factory.strends(strUriExpr, uriSuffixStrinfNode);
        Expr filterUriIsNotAlreadyUpdatedExpr = factory.not(strEndsExpr);

        return new UpdateBuilder()
                .with(graph)
                .addDelete(uriToDeleteVar, rdfTypeUri, classExtensionType)
                .addDelete(uriToDeleteVar, hasIconUri, iconVar)
                .addDelete(uriToDeleteVar, isAbstractClassUri, isAbstractVar)
                .addDelete(fromOwlClassVar, fromOwlClassUri, uriToDeleteVar)

                .addInsert(uriToInsertVar, rdfTypeUri, classExtensionType)
                .addInsert(uriToInsertVar, hasIconUri, iconVar)
                .addInsert(uriToInsertVar, isAbstractClassUri, isAbstractVar)
                .addInsert(fromOwlClassVar, fromOwlClassUri, uriToInsertVar)
                .addInsert(uriToDeleteVar, hasVueExtensionClassNode, uriToInsertVar)

                .addWhere(uriToDeleteVar, rdfTypeUri, classExtensionType)
                .addOptional(uriToDeleteVar, hasIconUri, iconVar)
                .addOptional(uriToDeleteVar, isAbstractClassUri, isAbstractVar)
                .addOptional(fromOwlClassVar, fromOwlClassUri, uriToDeleteVar)
                .addBind(uriBindExpr, uriToInsertVar)
                .addFilter(filterUriIsNotAlreadyUpdatedExpr);
    }
}

package org.opensilex.migration;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

/**
 * @author rcolin
 */
public class ScientificObjectNameIntegerConvertMigration extends DatabaseMigrationModuleUpdate {

    private static final Node URI_VAR = NodeFactory.createVariable("uri");
    private static final Node TYPE_VAR = NodeFactory.createVariable("type");
    private static final Node NAME_VAR = NodeFactory.createVariable("name");
    private static final Node NAME_STRING_VAR = NodeFactory.createVariable("nameString");

    Node scientificObjectClass = NodeFactory.createURI(Oeso.ScientificObject.getURI());
    @Override
    public String getDescription() {
        return "[Scientific Object] Change datatype of rdfs:label in case of name composed only of digit (In global context)";
    }

    private WhereBuilder buildWhere(SPARQLService sparql) throws SPARQLException {

        Node defaultOsGraph = sparql.getDefaultGraph(ScientificObjectModel.class);
        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

        // perform an ASK query in order to  that the migration must apply on database
        return new WhereBuilder()
                .addWhere(TYPE_VAR, Ontology.subClassAny, scientificObjectClass)
                .addGraph(defaultOsGraph, new WhereBuilder()
                        .addWhere(URI_VAR, RDF.type, TYPE_VAR)
                        .addWhere(URI_VAR, RDFS.label, NAME_VAR)
                        .addFilter(exprFactory.not(
                                exprFactory.eq(exprFactory.datatype(NAME_VAR), XSD.xstring.asNode())
                        ))
                        .addBind(exprFactory.str(NAME_VAR), NAME_STRING_VAR)
                );
    }

    /**
     *
     * <pre>
     * {@code
     *
     * SELECT ?uri ?name ?nameString WHERE {
     *   ?type rdfs:subClassOf* vocabulary:ScientificObject
     *
     *   GRAPH <http://www.phenome-fppn.fr/set/scientific-object> {
     *     ?uri a ?type ;
     *         rdfs:label ?name .
     *     FILTER( !( DATATYPE(?name) = xsd:string))
     *     BIND(STR(?name) AS ?nameString)
     *   }
     * }
     * </pre>
     */
    @Override
    protected boolean applyOnSparql(SPARQLService sparql, SPARQLConfig sparqlConfig) throws SPARQLException {
        return sparql.executeAskQuery(
                new AskBuilder().addWhere(buildWhere(sparql))
        );
    }


    @Override
    protected void sparqlOperation(SPARQLService sparql, SPARQLConfig sparqlConfig) throws SPARQLException {

        Node defaultOsGraph = sparql.getDefaultGraph(ScientificObjectModel.class);

        UpdateBuilder updateQuery = new UpdateBuilder()
                .addDelete(defaultOsGraph, URI_VAR, RDFS.label, NAME_VAR)
                .addInsert(defaultOsGraph, URI_VAR, RDFS.label, NAME_STRING_VAR)
                .addWhere(buildWhere(sparql));

        sparql.executeUpdateQuery(updateQuery);
    }
}

package org.opensilex.core.organisation.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class OrganizationSPARQLHelper {
    protected final SPARQLService sparql;

    public OrganizationSPARQLHelper(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     * Creates a clause associating an organization to all users belonging to its groups,
     * or the groups of their ascendants in the hierarchy.
     *
     * <pre><code>
     * {
     *     ?parent (vocabulary:hasPart)* ?orgUri
     *     ?parent os-sec:hasGroup/os-sec:hasUserProfile/os-sec:hasUser ?userUri
     * }
     * </code></pre>
     *
     * The parameters orgUri and userUri can either be variables or resource uris.
     *
     * @param orgUri A var or an URI representing the organization
     * @param userUri A var or an URI representing the user.
     * @return
     */
    public WhereBuilder buildOrganizationGroupUserClause(Object orgUri, Object userUri) {
        WhereBuilder where = new WhereBuilder();

        Var orgParent = makeVar(InfrastructureModel.PARENTS_FIELD + "_");
        where.addWhere(orgParent, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), orgUri);
        where.addWhere(orgParent, PathFactory.pathSeq(
                PathFactory.pathLink(SecurityOntology.hasGroup.asNode()),
                PathFactory.pathSeq(
                        PathFactory.pathLink(SecurityOntology.hasUserProfile.asNode()),
                        PathFactory.pathLink(SecurityOntology.hasUser.asNode())
                )
        ), userUri);

        return where;
    }

    /**
     * Creates a clause associating the organization with its creator.
     *
     * <pre>
     *     <code>
     *         {
     *             ?orgUri dc:creator ?creatorUri
     *         }
     *     </code>
     * </pre>
     *
     * @param orgUri A var or an URI representing the organization.
     * @param creatorUri A var or an URI representing the creator.
     * @return
     */
    public WhereBuilder buildOrganizationCreatorClause(Object orgUri, Object creatorUri) {
        WhereBuilder where = new WhereBuilder();

        where.addWhere(orgUri, DCTerms.creator, creatorUri);

        return where;
    }

    /**
     * Constructs a select clause to retrieve the URI of organizations with zero associated
     * groups.
     *
     * <pre>
     *     <code>
     * select ?uri where {
     *     ?rdfType (rdfs:subClassOf)* foaf:Organization
     *     graph (organization graph) {
     *         ?uri a ?rdfType
     *         optional {
     *             ?parent (vocabulary:hasPart)* ?uri;
     *                     os-sec:hasGroup ?groups
     *         }
     *     }
     *     group by ?uri
     *     having (count(?groups) = 0)
     * }
     *     </code>
     * </pre>
     *
     * @param orgVar The var representing the organization URI retrieved by the select.
     * @param orgUri An optional value for the organization URI. If this URI is not null, a
     *               VALUES clause will be added to restrict the results.
     * @return
     * @throws Exception
     */
    public SelectBuilder buildNoGroupOrganizationSelect(Var orgVar, URI orgUri) throws Exception {
        SelectBuilder select = new SelectBuilder();

        Var orgParent = makeVar(InfrastructureModel.PARENTS_FIELD + "_");
        Var rdfType = makeVar(InfrastructureModel.TYPE_FIELD + "_");
        Var group = makeVar(InfrastructureModel.GROUP_FIELD + "_");

        select.setDistinct(true);
        select.addVar(orgVar);

        select.addWhere(rdfType, Ontology.subClassAny, FOAF.Organization);
        select.addGraph(sparql.getDefaultGraph(InfrastructureModel.class),
                orgVar, RDF.type, rdfType);

        // Where clause for the groups
        WhereBuilder noGroupWhere = new WhereBuilder();
        noGroupWhere.addWhere(orgParent, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), orgVar);
        noGroupWhere.addWhere(orgParent, SecurityOntology.hasGroup, group);
        select.addOptional(noGroupWhere);

        if (orgUri != null) {
            select.addWhereValueVar(orgVar);
            select.addWhereValueRow(SPARQLDeserializers.nodeURI(orgUri));
        }

        select.addGroupBy(orgVar);
        select.addHaving("COUNT(" + group + ") = 0");

        return select;
    }
}

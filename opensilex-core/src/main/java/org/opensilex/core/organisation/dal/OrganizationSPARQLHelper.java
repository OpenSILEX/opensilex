package org.opensilex.core.organisation.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.ORG;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.site.SiteDAO;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.organisation.dal.site.SiteSearchFilter;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.util.Collection;
import java.util.List;

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
     *     ?parent (vocabulary:hasPart)* ?orgURIVar
     *     ?parent os-sec:hasGroup/os-sec:hasUserProfile/os-sec:hasUser ?userURIVar
     * }
     * </code></pre>
     *
     * The parameters orgURIVar and userURIVar can either be variables or resource uris.
     *
     * @param orgURIVar A var or an URI representing the organization
     * @param userURIVar A var or an URI representing the user.
     * @return
     */
    private WhereBuilder buildOrganizationGroupUserClause(Var orgURIVar, Var userURIVar) {
        WhereBuilder where = new WhereBuilder();

        Var orgParent = makeVar(OrganizationModel.PARENTS_FIELD + "_");
        where.addWhere(orgParent, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), orgURIVar);
        where.addWhere(orgParent, PathFactory.pathSeq(
                PathFactory.pathLink(SecurityOntology.hasGroup.asNode()),
                PathFactory.pathSeq(
                        PathFactory.pathLink(SecurityOntology.hasUserProfile.asNode()),
                        PathFactory.pathLink(SecurityOntology.hasUser.asNode())
                )
        ), userURIVar);

        return where;
    }

    /**
     * Creates a clause associating the organization with its publisher.
     *
     * <pre>
     *     <code>
     *         {
     *             ?orgURIVar dc:publisher ?publisherUriVar
     *         }
     *     </code>
     * </pre>
     *
     * @param orgURIVar A var or an URI representing the organization.
     * @param publisherUriVar A var or an URI representing the publisher.
     * @return
     */
    private WhereBuilder buildOrganizationPublisherClause(Var orgURIVar, Object publisherUriVar) {
        WhereBuilder where = new WhereBuilder();

        where.addWhere(orgURIVar, DCTerms.publisher, publisherUriVar);

        return where;
    }

    /**
     * Constructs a select clause to retrieve the URI of organizations with zero associated
     * groups.
     *
     * <pre>
     *     <code>
     * select distinct ?uri where {
     *     ?rdfType_ (rdfs:subClassOf)* foaf:Organization
     *     graph (organization_graph) {
     *         ?uri a ?rdfType_
     *         optional {
     *             ?parents_ (vocabulary:hasPart)* ?uri .
     *             ?parents_ os-sec:hasGroup ?groups
     *         }
     *     }
     *     group by ?uri
     *     having (count(?groups) = 0)
     * }
     *     </code>
     * </pre>
     *
     * @param orgURIVar The var representing the organization URI retrieved by the select.
     *
     * @return
     * @throws Exception
     */
    private SelectBuilder buildNoGroupOrganizationSelect(Var orgURIVar) throws Exception {
        SelectBuilder select = new SelectBuilder();

        Var orgParent = makeVar(OrganizationModel.PARENTS_FIELD + "_");
        Var rdfType = makeVar(OrganizationModel.TYPE_FIELD + "_");
        Var group = makeVar(OrganizationModel.GROUP_FIELD + "_");

        select.setDistinct(true);
        select.addVar(orgURIVar);

        select.addWhere(rdfType, Ontology.subClassAny, FOAF.Organization);
        select.addGraph(sparql.getDefaultGraph(OrganizationModel.class),
                orgURIVar, RDF.type, rdfType);

        // Where clause for the groups
        WhereBuilder noGroupWhere = new WhereBuilder();
        noGroupWhere.addWhere(orgParent, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), orgURIVar);
        noGroupWhere.addWhere(orgParent, SecurityOntology.hasGroup, group);
        select.addOptional(noGroupWhere);

        select.addGroupBy(orgURIVar);
        select.addHaving(SPARQLQueryHelper.countEqExpr(group, true, 0));

        return select;
    }

    /**
     * Adds an access clause for an organization in a query. An organization can be accessed by a non-admin user
     * iff at least one of the following conditions is true :
     *
     * <ul>
         * <li>The user is a member of a group of the organization</li>
         * <li>The user is a member of a group of a parent organization</li>
         * <li>The user is the creator of the organization</li>
         * <li>Neither the organization nor its parents have groups</li>
     * </ul>
     *
     * To do this, the following clause are added to the query :
     *
     * <pre>
     *     <code>
     * {
     *     OPTIONAL {
     *         ?parents_ (vocabulary:hasPart)* ?uri .
     *         ?parents_ os-sec:hasGroup/(os-sec:hasUserProfile/os-sec:hasUser) ?_userURI
     *     } OPTIONAL {
     *         ?uri  dc:creator  ?creator
     *     }
     *     FILTER ( ( bound(?_userURI) && ( ?_userURI = <USER_URI> ) ) || ( bound(?creator) && ( ?creator = <USER_URI> ) ) )
     * } UNION {
     *     SELECT DISTINCT  ?uri
     *     WHERE {
     *         ?rdfType_ (rdfs:subClassOf)* foaf:Organization
     *         GRAPH <ORGANIZATION_GRAPH> {
     *             ?uri  a  ?rdfType_
     *         }
     *         OPTIONAL {
     *             ?parents_ (vocabulary:hasPart)* ?uri .
     *             ?parents_  os-sec:hasGroup  ?groups_
     *         }
     *         GROUP BY ?uri
     *         HAVING ( COUNT(DISTINCT ?groups_) = 0 )
     *     }
     * }
     *     </code>
     * </pre>
     *
     * @param where The where clause (can be added to a SELECT or ADD query for example)
     * @param orgURIVar The Var representing the URI of the organization
     * @param userURI The URI of the user
     */
    public void addOrganizationAccessClause(WhereClause<?> where, Var orgURIVar, URI userURI) throws Exception {
        Var userURIVar = makeVar("_userURI");

        WhereBuilder organizationAccessWhere = new WhereBuilder();

        // This first where clause is used to retrieve all users in the associated groups of the organizations, or the
        // associated groups of the ascendant organizations in the hierarchy.
        WhereBuilder userProfileGroup = buildOrganizationGroupUserClause(orgURIVar, userURIVar);
        organizationAccessWhere.addOptional(userProfileGroup);
        Expr isInGroup = SPARQLQueryHelper.and(
                SPARQLQueryHelper.bound(userURIVar),
                SPARQLQueryHelper.eq(userURIVar, SPARQLDeserializers.nodeURI(userURI)));

        // Retrieve the publisher of the organizations
        Var publisherVar = makeVar(ExperimentModel.PUBLISHER_FIELD);
        organizationAccessWhere.addOptional(buildOrganizationPublisherClause(orgURIVar, publisherVar));
        Expr isPublisher = SPARQLQueryHelper.and(
                SPARQLQueryHelper.bound(publisherVar),
                SPARQLQueryHelper.eq(publisherVar, userURI));

        // The filter represents the OR condition
        organizationAccessWhere.addFilter(SPARQLQueryHelper.or(isInGroup, isPublisher));

        // Here we create the nested select clause
        SelectBuilder noGroupSelect = buildNoGroupOrganizationSelect(orgURIVar);

        // Create the union
        organizationAccessWhere.addUnion(noGroupSelect);

        where.addWhere(organizationAccessWhere);
    }

    /**
     * Adds an access clause for a site to a query. A site can be accessed by a non-admin user iff at least on of the
     * following conditions is true :
     *
     * <ul>
     *     <li>The site belongs to an organization the user has access to</li>
     *     <li>The site has a group the user belongs to</li>
     * </ul>
     *
     * Please note that you will need to fetch the organizations the user has access to first. The recommended way of doing
     * that is by calling {@link OrganizationDAO#search(String, List, AccountModel)}.
     *
     * @param where The where clause (can be added to a SELECT or ADD query for example)
     * @param siteURIVar The Var representing the URI of the site
     * @param userOrganizations The list of organizations the user has access to. Must be retrieved by calling {@link OrganizationDAO#search(String, List, AccountModel)} first.
     * @param userURI The URI of the user
     */
    public void addSiteAccessClause(WhereClause<?> where, Var siteURIVar, Collection<URI> userOrganizations, URI userURI) {
        Var organizationVar = makeVar("_organization");
        Var groupVar = makeVar(SiteModel.GROUP_FIELD);
        Var profileVar = makeVar(GroupModel.USER_PROFILES_FIELD);
        Node userUriNode = SPARQLDeserializers.nodeURI(userURI);

        WhereBuilder siteInOrganization = new WhereBuilder();
        siteInOrganization.addWhere(organizationVar, ORG.hasSite, siteURIVar);

        WhereBuilder userInSiteGroup = new WhereBuilder();
        userInSiteGroup.addWhere(siteURIVar, SecurityOntology.hasGroup, groupVar);
        userInSiteGroup.addWhere(groupVar, SecurityOntology.hasUserProfile, profileVar);
        userInSiteGroup.addWhere(profileVar, SecurityOntology.hasUser, userUriNode);

        WhereBuilder userInOrgOrInSiteGroup = new WhereBuilder();
        userInOrgOrInSiteGroup.addOptional(siteInOrganization);
        userInOrgOrInSiteGroup.addOptional(userInSiteGroup);

        where.addWhere(userInOrgOrInSiteGroup);
        where.addFilter(SPARQLQueryHelper.or(
                SPARQLQueryHelper.inURIFilter(organizationVar, userOrganizations),
                SPARQLQueryHelper.bound(groupVar)));
    }

    /**
     * Adds an access clause for a facility to a query. A facility can be accessed by a non-admin user iff at least one
     * of the following conditions is true :
     *
     * <ul>
     *     <li>The facility hosts an organization the user has access to</li>
     *     <li>The facility is located within a site the user has access to</li>
     *     <li>The user is the publisher of the facility</li>
     * </ul>
     *
     * Please note that you will need to fetch the organizations and sites the user has access to first. The recommended
     * way of doing that is by calling {@link OrganizationDAO#search(String, List, AccountModel)} and {@link SiteDAO#search(SiteSearchFilter)}.
     *
     * @param where The where clause (can be added to a SELECT or ADD query for example)
     * @param facilityURIVar The Var representing the URI of the facility
     * @param userOrganizations The list of organizations the user has access to. Must be retrieved by calling {@link OrganizationDAO#search(String, List, AccountModel)} first.
     * @param userSites The list of sites the user has access to. Must be retrieved by calling {@link SiteDAO#search(SiteSearchFilter)} first.
     * @param userURI The URI of the user
     */
    public void addFacilityAccessClause(WhereClause<?> where, Var facilityURIVar, Collection<URI> userOrganizations, Collection<URI> userSites, URI userURI) throws Exception {
        Var organizationVar = makeVar("_organization");
        Var siteVar = makeVar("_site");
        Var publisherVar = makeVar("_publisher");
        Node userURINode = SPARQLDeserializers.nodeURI(userURI);

        WhereBuilder facilityOrganization = new WhereBuilder();
        facilityOrganization.addWhere(organizationVar, Oeso.isHosted, facilityURIVar);

        WhereBuilder facilitySite = new WhereBuilder();
        facilitySite.addWhere(facilityURIVar, Oeso.withinSite, siteVar);

        WhereBuilder facilityPublisher= new WhereBuilder();
        facilityPublisher.addWhere(facilityURIVar, DCTerms.publisher, publisherVar);

        WhereBuilder unionWhere = new WhereBuilder();
        unionWhere.addOptional(facilityOrganization);
        unionWhere.addOptional(facilitySite);
        unionWhere.addOptional(facilityPublisher);

        where.addWhere(unionWhere);
        where.addFilter(SPARQLQueryHelper.or(
                SPARQLQueryHelper.inURIFilter(organizationVar, userOrganizations),
                SPARQLQueryHelper.inURIFilter(siteVar, userSites),
                SPARQLQueryHelper.eq(publisherVar, userURINode)
        ));
    }
}

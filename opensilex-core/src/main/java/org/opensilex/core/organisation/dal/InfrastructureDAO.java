//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.organisation.dal;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.external.geocoding.GeocodingService;
import org.opensilex.core.external.geocoding.OpenStreetMapGeocodingService;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.organisation.api.facitity.FacilityAddressDTO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.exceptions.SPARQLInvalidModelException;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vidalmor
 */
public class InfrastructureDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final GeocodingService geocodingService;

    protected final GeospatialDAO geospatialDAO;

    public InfrastructureDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;

        this.geospatialDAO = new GeospatialDAO(nosql);
        this.geocodingService = new OpenStreetMapGeocodingService();
    }

    public List<InfrastructureModel> search(String pattern, List<URI> organizationsRestriction, UserModel user) throws Exception {
        Set<URI> organizations = getUserInfrastructures(user);

        // Filter by organizations
        if (organizationsRestriction != null && !organizationsRestriction.isEmpty()) {
            if (organizations != null) {
                organizations.retainAll(organizationsRestriction);
            } else {
                organizations = new HashSet<>(organizationsRestriction);
            }
        }

        if (organizations != null && organizations.isEmpty()) {
            return new ArrayList<>();
        }

        final Set<URI> finalOrganizations = organizations;

        return sparql.search(InfrastructureModel.class, user.getLanguage(), (SelectBuilder select) -> {
            if (pattern != null && !pattern.isEmpty()) {
                select.addFilter(SPARQLQueryHelper.regexFilter(InfrastructureModel.NAME_FIELD, pattern));
            }

            if (finalOrganizations != null) {
                SPARQLQueryHelper.inURI(select, InfrastructureModel.URI_FIELD, finalOrganizations);
            }
        });
    }

    public void validateInfrastructureAccess(URI infrastructureURI, UserModel user) throws Exception {
        if (!sparql.uriExists(InfrastructureModel.class, infrastructureURI)) {
            throw new NotFoundURIException(infrastructureURI);
        }

        if (user.isAdmin()) {
            return;
        }

        AskBuilder ask = sparql.getUriExistsQuery(InfrastructureModel.class, infrastructureURI);
        addAskInfrastructureAccess(ask, null, SPARQLDeserializers.nodeURI(infrastructureURI), user);

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(infrastructureURI);
        }
    }

    public void validateInfrastructureFacilityAccess(List<URI> facilityUris, UserModel user) throws Exception {

        if (user.isAdmin()) {
            return;
        }

        SelectBuilder askFacilityAccess = sparql.getUriListExistQuery(InfrastructureFacilityModel.class, facilityUris);
        addAskInfrastructureAccess(askFacilityAccess, null, makeVar(InfrastructureFacilityModel.INFRASTRUCTURE_FIELD), user);

        for (SPARQLResult result : sparql.executeSelectQuery(askFacilityAccess)) {
            boolean value = Boolean.parseBoolean(result.getStringValue(SPARQLService.EXISTING_VAR));
            if (!value) {
                URI uri = new URI(result.getStringValue(SPARQLResourceModel.URI_FIELD));
                throw new ForbiddenURIAccessException(uri);
            }
        }

    }


    public void validateInfrastructureFacilityAccess(URI infrastructureFacilityURI, UserModel user) throws Exception {
        if (!sparql.uriExists(InfrastructureFacilityModel.class, infrastructureFacilityURI)) {
            throw new NotFoundURIException(infrastructureFacilityURI);
        }

        if (user == null || user.isAdmin()) {
            return;
        }

        AskBuilder ask = sparql.getUriExistsQuery(InfrastructureFacilityModel.class, infrastructureFacilityURI);
        addAskInfrastructureAccess(ask, makeVar(InfrastructureFacilityModel.INFRASTRUCTURE_FIELD), null, user);

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(infrastructureFacilityURI);
        }
    }

    /**
     * MUST BE CALLED BEFORE AN UPDATE
     *
     * Validate the organization hierarchy, i.e. fails if modifying the parents of the organization results in a
     * cycle in the hierarchy graph (for example, adding a child organization as a parent creates a cycle).
     *
     * @param model the organization to update
     * @throws SPARQLInvalidModelException if a cycle is found
     */
    protected void validateOrganizationHierarchy(InfrastructureModel model) throws Exception {
        AskBuilder ask = sparql.getUriExistsQuery(InfrastructureModel.class, model.getUri());

        Node nodeUri = SPARQLDeserializers.nodeURI(model.getUri());
        Var childrenVar = makeVar(InfrastructureModel.CHILDREN_FIELD);

        ask.addWhere(nodeUri, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), childrenVar);

        // We must check if any of the parents of the organization is a child of itself
        // In that case this would create a cycle, thus we forbid it
        List<URI> parentUriList = model.getParents()
                .stream().map(InfrastructureModel::getUri)
                .collect(Collectors.toList());

        if (parentUriList.isEmpty()) {
            // No parents to check, so no need to do a query
            return;
        }

        SPARQLQueryHelper.addWhereUriValues(ask, childrenVar.getVarName(), parentUriList);

        if (sparql.executeAskQuery(ask)) {
            throw new SPARQLInvalidModelException("Invalid organization model : parents cannot be children of the organization");
        }
    }

    /**
     * Add an organization access clause to the given query. An organization is considered accessible to a user if ONE
     * of the following statements is true :
     *
     * <ul>
     *     <li>The user is admin (thus they have access to all the organizations)</li>
     *     <li>The organization has no security group</li>
     *     <li>The user belongs to one of the groups of the organizations</li>
     *     <li>The user is the creator of the organization</li>
     * </ul>
     *
     * Please note that the groups are inherited by the descendant organizations. E.g. if a parent organization has a group,
     * this group is taken in account for all descendant organizations in the hierarchy.
     *
     * @param where
     * @param uriVar
     * @param user
     * @throws Exception
     */
    protected void addAskInfrastructureAccess(WhereClause<?> where, Var uriVar, Object orgUri, UserModel user) throws Exception {
        Var userProfileVar = makeVar("_userProfile");
        Var userVar = makeVar("_userURI");
        Var parentVar = makeVar("_parent");

        /*
        We'll build a quite complicated request. It will look like this :

        {
            optional {
                ?_parent (vocabulary:hasPart)* ?uri ;
                         os-sec:hasGroup ?groups.
                ?groups os-sec:hasUserProfile ?_userProfile.
                ?_userProfile os-sec:hasUser ?_userURI
            }
            optional {
                ?uri dc:creator ?uri
            }
            filter ((bound(?_userURI) && (?_userURI = <the uri of the user to check>)
                  || bound(?creator) && (?creator = <the uri of the user to check>))
        } union {
            select distinct ?uri {
                ?rdfType (rdfs:subClassOf)* foaf:Organization
                graph <the organizations graph> {
                    ?uri a ?rdfType
                }
                optional {
                    ?_parent (vocabulary:hasPart)* ?uri;
                             os-sec:hasGroup ?groups
                }
            }
            group by ?uri
            having (count(?groups) = 0)
        }

        While this could be written way more efficiently, this is the only way I found to make to work for both rdf4j and
        GraphDB (which was causing a lot of trouble with the `filter` and `or` statements). Feel free to rewrite it if you
        manage to find a better way.
         */

        WhereBuilder userInGroupOrCreator = new WhereBuilder();

        // This first where clause is used to retrieve all users in the associated groups of the organizations, or the
        // associated groups of the ascendant organizations in the hierarchy.
        WhereBuilder userProfileGroup = new WhereBuilder();
        userProfileGroup.addWhere(parentVar, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), uriVar != null ? uriVar : orgUri);
        userProfileGroup.addWhere(parentVar, SecurityOntology.hasGroup.asNode(), makeVar(InfrastructureModel.GROUP_FIELD));
        userProfileGroup.addWhere(makeVar(InfrastructureModel.GROUP_FIELD), SecurityOntology.hasUserProfile.asNode(), userProfileVar);
        userProfileGroup.addWhere(userProfileVar, SecurityOntology.hasUser.asNode(), userVar);
        userInGroupOrCreator.addOptional(userProfileGroup);
        Expr isInGroup = SPARQLQueryHelper.and(
                SPARQLQueryHelper.bound(userVar),
                SPARQLQueryHelper.eq(userVar, SPARQLDeserializers.nodeURI(user.getUri())));

        // Retrieve the creator of the organizations
        Var creatorVar = makeVar(ExperimentModel.CREATOR_FIELD);
        userInGroupOrCreator.addOptional(uriVar != null ? uriVar : orgUri, DCTerms.creator.asNode(), creatorVar);
        Expr isCreator = SPARQLQueryHelper.and(
                SPARQLQueryHelper.bound(creatorVar),
                SPARQLQueryHelper.eq(creatorVar, user.getUri()));

        // The filter represents the OR condition
        userInGroupOrCreator.addFilter(SPARQLQueryHelper.or(isInGroup, isCreator));

        // Here we create the nested select clause
        Var nestedParentVar = makeVar("_parent");
        Var nestedGroupVar = makeVar("_group");
        Var nestedTypeFieldVar = makeVar("_rdfType");
        Var nestedUriVar = uriVar != null
                ? uriVar
                : makeVar(InfrastructureModel.URI_FIELD);
        SelectBuilder noGroupSelect2 = new SelectBuilder();
        noGroupSelect2.setDistinct(true);
        noGroupSelect2.addVar(nestedUriVar);

        // The type clause for Organizations
        noGroupSelect2.addWhere(nestedTypeFieldVar, Ontology.subClassAny, FOAF.Organization);
        noGroupSelect2.addGraph(sparql.getDefaultGraph(InfrastructureModel.class),
                nestedUriVar, RDF.type, nestedTypeFieldVar);

        // The where clause to retrieve the groups of the organization
        WhereBuilder noGroupWhere = new WhereBuilder();
        noGroupWhere.addWhere(nestedParentVar, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), nestedUriVar);
        noGroupWhere.addWhere(nestedParentVar, SecurityOntology.hasGroup.asNode(), nestedGroupVar);
        noGroupSelect2.addOptional(noGroupWhere);

        if (orgUri != null) {
            noGroupSelect2.addWhereValueVar(nestedUriVar);
            noGroupSelect2.addWhereValueRow(orgUri);
        }
        // At the end of the nested select, we only keep those who don't have any group
        noGroupSelect2.addGroupBy(nestedUriVar);

        // append HAVING(COUNT(?_group) = 0) Expr
        Expr noGroupEqExpr = SPARQLQueryHelper.countEqExpr(nestedGroupVar,false,0);
        noGroupSelect2.addHaving(noGroupEqExpr);

        // Create the union
        userInGroupOrCreator.addUnion(noGroupSelect2);

        where.addWhere(userInGroupOrCreator);
    }

    /**
     * Get all organizations accessible by a giver user. See {@link #addAskInfrastructureAccess(WhereClause, Var, Object, UserModel)}
     * for further information on the conditions for an organization to be considered accessible.
     *
     * @param user
     * @return
     * @throws Exception
     */
    public Set<URI> getUserInfrastructures(UserModel user) throws Exception {
        if (user == null || user.isAdmin()) {
            return null;
        }

        String lang = user.getLanguage();

        List<URI> infras = sparql.searchURIs(InfrastructureModel.class, lang, (SelectBuilder select) -> {
            Var uriVar = makeVar(InfrastructureModel.URI_FIELD);

            addAskInfrastructureAccess(select, uriVar, null, user);
        });

        return new HashSet<>(infras);
    }

    public InfrastructureModel create(InfrastructureModel instance) throws Exception {
        sparql.create(instance);

        return instance;
    }

    public InfrastructureModel get(URI uri, UserModel user) throws Exception {
        validateInfrastructureAccess(uri, user);
        return sparql.getByURI(InfrastructureModel.class, uri, user.getLanguage());
    }

    public void delete(URI uri, UserModel user) throws Exception {
        validateInfrastructureAccess(uri, user);
        sparql.delete(InfrastructureModel.class, uri);
    }

    public InfrastructureModel update(InfrastructureModel instance, UserModel user) throws Exception {
        validateInfrastructureAccess(instance.getUri(), user);
        validateOrganizationHierarchy(instance);
        sparql.update(instance);
        return instance;
    }

    public InfrastructureFacilityModel createFacility(InfrastructureFacilityModel instance, UserModel user) throws Exception {
        String lang = null;
        if (user != null) {
            lang = user.getLanguage();
        }
        List<InfrastructureModel> infrastructureModels = sparql.getListByURIs(InfrastructureModel.class, instance.getInfrastructureUris(), lang);
        instance.setInfrastructures(infrastructureModels);
        sparql.create(instance);

        createFacilityGeospatialModel(instance, sparql.getDefaultGraphURI(InfrastructureModel.class));

        return instance;
    }

    public InfrastructureFacilityModel getFacility(URI uri, UserModel user) throws Exception {
        validateInfrastructureFacilityAccess(uri, user);
        return sparql.getByURI(InfrastructureFacilityModel.class, uri, user.getLanguage());
    }

    public List<InfrastructureFacilityModel> getFacilitiesByURI(UserModel user,List<URI> uris) throws Exception {
        validateInfrastructureFacilityAccess(uris, user);
        return sparql.getListByURIs(InfrastructureFacilityModel.class,uris,user.getLanguage());
    }

    public ListWithPagination<InfrastructureFacilityModel> searchFacilities(UserModel user, String pattern, List<URI> organizations, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Set<URI> infras = getUserInfrastructures(user);

        // Filter by organizations
        if (organizations != null && !organizations.isEmpty()) {
            if (infras != null) {
                infras.retainAll(organizations);
            } else {
                infras = new HashSet<>(organizations);
            }
        }

        if (infras != null && infras.size() == 0) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        final Set<URI> finalOrganizations = infras;

        return sparql.searchWithPagination(
                InfrastructureFacilityModel.class,
                user.getLanguage(),
                (select -> {
                    if (finalOrganizations != null) {
                        select.addWhere(makeVar(InfrastructureFacilityModel.INFRASTRUCTURE_FIELD), Oeso.isHosted, makeVar(InfrastructureFacilityModel.URI_FIELD));
                        SPARQLQueryHelper.inURI(select, InfrastructureFacilityModel.INFRASTRUCTURE_FIELD, finalOrganizations);
                    }

                    // append REGEX filter on name and uri
                    if (!StringUtils.isEmpty(pattern)) {

                        ExprFactory exprFactory = select.getExprFactory();
                        Expr uriStrRegex = exprFactory.str(exprFactory.asVar(InfrastructureFacilityModel.URI_FIELD));
                        select.addFilter(SPARQLQueryHelper.regexFilter(uriStrRegex, pattern, null));

                        select.addFilter(SPARQLQueryHelper.regexFilter(InfrastructureFacilityModel.NAME_FIELD, pattern));
                    }
                }),
                orderByList,
                page,
                pageSize
        );

    }

    public List<InfrastructureFacilityModel> getAllFacilities(UserModel user) throws Exception {
        Set<URI> infras = getUserInfrastructures(user);

        if (infras != null && infras.size() == 0) {
            return new ArrayList<>();
        }

        return sparql.search(InfrastructureFacilityModel.class, user.getLanguage(), (select) -> {
            if (infras != null) {
                SPARQLQueryHelper.inURI(select, InfrastructureFacilityModel.INFRASTRUCTURE_FIELD, infras);
            }
        });
    }

    public void deleteFacility(URI uri, UserModel user) throws Exception {
        validateInfrastructureFacilityAccess(uri, user);

        InfrastructureFacilityModel model = sparql.getByURI(InfrastructureFacilityModel.class, uri, user.getLanguage());
        URI graphUri = sparql.getDefaultGraphURI(InfrastructureModel.class);

        // Must delete the associated address if there is one
        if (model.getAddress() != null) {
            if (this.geospatialDAO.getGeometryByURI(uri, graphUri) != null) {
                this.geospatialDAO.delete(uri, graphUri);
            }

            sparql.delete(FacilityAddressModel.class, model.getAddress().getUri());
        }

        sparql.delete(InfrastructureFacilityModel.class, uri);
    }

    public InfrastructureFacilityModel updateFacility(InfrastructureFacilityModel instance, UserModel user) throws Exception {
        validateInfrastructureFacilityAccess(instance.getUri(), user);

        List<InfrastructureModel> infrastructureModels = sparql.getListByURIs(InfrastructureModel.class, instance.getInfrastructureUris(), user.getLanguage());
        instance.setInfrastructures(infrastructureModels);

        URI graphUri = sparql.getDefaultGraphURI(InfrastructureModel.class);

        InfrastructureFacilityModel existingModel = sparql.getByURI(InfrastructureFacilityModel.class, instance.getUri(), user.getLanguage());

        if (existingModel.getAddress() != null) {
            if (this.geospatialDAO.getGeometryByURI(instance.getUri(), graphUri) != null) {
                this.geospatialDAO.delete(instance.getUri(), graphUri);
            }

            sparql.delete(FacilityAddressModel.class, existingModel.getAddress().getUri());
        }

        createFacilityGeospatialModel(instance, graphUri);

        sparql.deleteByURI(sparql.getDefaultGraph(InfrastructureFacilityModel.class), instance.getUri());
        sparql.create(instance);
        return instance;
    }

    private void createFacilityGeospatialModel(InfrastructureFacilityModel facility, URI graphUri) {
        if (facility.getAddress() == null) {
            return;
        }

        FacilityAddressDTO addressDto = new FacilityAddressDTO();
        addressDto.fromModel(facility.getAddress());

        Geometry geom = geocodingService.getPointFromAddress(addressDto.toReadableAddress());

        if (geom == null) {
            return;
        }

        GeospatialModel geospatialModel = new GeospatialModel();
        geospatialModel.setUri(facility.getUri());
        geospatialModel.setName(facility.getName());
        geospatialModel.setRdfType(facility.getType());
        geospatialModel.setGraph(graphUri);
        geospatialModel.setGeometry(geom);

        this.geospatialDAO.create(geospatialModel);
    }
}

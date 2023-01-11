//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.organisation.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vidalmor
 */
public class OrganizationDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final OrganizationSPARQLHelper organizationSPARQLHelper;

    public OrganizationDAO(SPARQLService sparql, MongoDBService nosql) throws Exception {
        this.sparql = sparql;
        this.nosql = nosql;

        this.organizationSPARQLHelper = new OrganizationSPARQLHelper(sparql);
    }

    /**
     * Validates that the user has access to an organization. Throws an exception if that is not the case. The
     * organization must exist, and the user/organization must satisfy at least one of the following conditions :
     *
     * <ul>
     *     <li>The user is admin</li>
     *     <li>The user is a member of a group associated with the organization</li>
     *     <li>The user is a member of a group associated with a parent organization</li>
     *     <li>The user is the creator of the organization</li>
     * </ul>
     *
     * @param organizationURI The organization URI to check
     * @param user The user
     * @throws IllegalArgumentException If the userModel is null
     */
    public void validateOrganizationAccess(URI organizationURI, AccountModel user) throws Exception {
        if (Objects.isNull(user)) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!sparql.uriExists(OrganizationModel.class, organizationURI)) {
            throw new NotFoundURIException(organizationURI);
        }

        if (user.isAdmin()) {
            return;
        }

        Var uriVar = makeVar(OrganizationModel.URI_FIELD);

        AskBuilder ask = new AskBuilder();
        ask.addWhere(uriVar, Ontology.typeSubClassAny, sparql.getMapperIndex().getForClass(OrganizationModel.class).getRDFType());
        ask.addFilter(SPARQLQueryHelper.eq(uriVar, SPARQLDeserializers.nodeURI(organizationURI)));
        organizationSPARQLHelper.addOrganizationAccessClause(ask, uriVar, user.getUri());

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(organizationURI);
        }
    }

    /**
     * MUST BE CALLED BEFORE AN UPDATE
     * <p>
     * Validate the organization hierarchy, i.e. fails if modifying the parents of the organization results in a
     * cycle in the hierarchy graph (for example, adding a child organization as a parent creates a cycle).
     * </p>
     *
     * @param model the organization to update
     * @throws BadRequestException if a cycle is found
     */
    protected void validateOrganizationHierarchy(OrganizationModel model) throws Exception {
        AskBuilder ask = sparql.getUriExistsQuery(OrganizationModel.class, model.getUri());

        Node nodeUri = SPARQLDeserializers.nodeURI(model.getUri());
        Var childrenVar = makeVar(OrganizationModel.CHILDREN_FIELD);

        ask.addWhere(nodeUri, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), childrenVar);

        // We must check if any of the parents of the organization is a child of itself
        // In that case this would create a cycle, thus we forbid it
        List<URI> parentUriList = model.getParents()
                .stream().map(OrganizationModel::getUri)
                .collect(Collectors.toList());

        if (parentUriList.isEmpty()) {
            // No parents to check, so no need to do a query
            return;
        }

        SPARQLQueryHelper.addWhereUriValues(ask, childrenVar.getVarName(), parentUriList);

        if (sparql.executeAskQuery(ask)) {
            throw new BadRequestException("Invalid organization model : parents cannot be children of the organization");
        }
    }

    /**
     * Get all organizations accessible by a giver user. See {@link OrganizationSPARQLHelper#addOrganizationAccessClause(WhereClause, Var, URI)}
     * for further information on the conditions for an organization to be considered accessible.
     *
     * @param user The current user
     * @return The accessible organizations
     */
    public List<OrganizationModel> search(String nameFilter, List<URI> restrictedOrganizations, AccountModel user) throws Exception {
        if (Objects.isNull(user)) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.isAdmin()) {
            return sparql.search(OrganizationModel.class, sparql.getDefaultLang(), selectBuilder -> {
                if (StringUtils.isNotEmpty(nameFilter)) {
                    selectBuilder.addFilter(SPARQLQueryHelper.regexFilter(OrganizationModel.NAME_FIELD, nameFilter));
                }

                if (restrictedOrganizations != null) {
                    SPARQLQueryHelper.inURI(selectBuilder, OrganizationModel.URI_FIELD, restrictedOrganizations);
                }
            });
        }

        String lang = user.getLanguage();

        return sparql.search(OrganizationModel.class, lang, selectBuilder -> {
            if (StringUtils.isNotEmpty(nameFilter)) {
                selectBuilder.addFilter(SPARQLQueryHelper.regexFilter(OrganizationModel.NAME_FIELD, nameFilter));
            }

            Var orgURIVar = makeVar(OrganizationModel.URI_FIELD);

            organizationSPARQLHelper.addOrganizationAccessClause(selectBuilder, orgURIVar, user.getUri());

            if (restrictedOrganizations != null) {
                SPARQLQueryHelper.inURI(selectBuilder, OrganizationModel.URI_FIELD, restrictedOrganizations);
            }
        });
    }

    public OrganizationModel create(OrganizationModel instance) throws Exception {
        sparql.create(instance);

        return instance;
    }

    /**
     * Gets an organization by its URI. Throws an exception if the current user does not have access to the organization.
     * See {@link #validateOrganizationAccess(URI, AccountModel)} for further information on access validation.
     *
     * @param uri The organization URI
     * @param user The current user
     * @return The organization
     * @throws Exception If the validation fails, or any other problem occurs
     */
    public OrganizationModel get(URI uri, AccountModel user) throws Exception {
        validateOrganizationAccess(uri, user);

        return sparql.getByURI(OrganizationModel.class, uri, user.getLanguage());
    }

    /**
     * Deletes an organization. Throws an exception if the current user does not have access to the organization.
     * See {@link #validateOrganizationAccess(URI, AccountModel)} for further information on access validation.
     *
     * @param uri The organization URI
     * @param user The current user
     * @throws Exception If the validation fails, or any other problem occurs
     */
    public void delete(URI uri, AccountModel user) throws Exception {
        validateOrganizationAccess(uri, user);

        sparql.delete(OrganizationModel.class, uri);
    }

    /**
     * Updates an organization. Throws an exception if the current user does not have access to the organization.
     * See {@link #validateOrganizationAccess(URI, AccountModel)} for further information on access validation. Also throws
     * an exception if the organization hierarchy is invalid (see {@link #validateOrganizationHierarchy(OrganizationModel)}).
     *
     * @param instance The organization to update
     * @param user The user
     * @return The updated organization
     * @throws Exception If the validation fails, or any other problem occurs
     */
    public OrganizationModel update(OrganizationModel instance, AccountModel user) throws Exception {
        validateOrganizationAccess(instance.getUri(), user);
        validateOrganizationHierarchy(instance);

        sparql.update(instance);
        return instance;
    }
}

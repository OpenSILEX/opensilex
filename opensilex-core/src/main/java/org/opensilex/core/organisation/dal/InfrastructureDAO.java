//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.organisation.dal;

import java.net.URI;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.api.InfrastructureGetDTO;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLDagModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vidalmor
 */
public class InfrastructureDAO {

    protected final SPARQLService sparql;

    public InfrastructureDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public List<InfrastructureModel> search(String pattern, List<URI> organizationsRestriction, UserModel user) throws Exception {
        Set<URI> organizations = getUserInfrastructures(user);

        if (organizations != null && organizations.isEmpty()) {
            return new ArrayList<>();
        }

        if (organizations == null) {
            organizations = new HashSet<>();
        }

        if (organizationsRestriction != null && !organizationsRestriction.isEmpty()) {
            organizationsRestriction.retainAll(organizations);
            organizations.clear();
            organizations.addAll(organizationsRestriction);
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
        addAskInfrastructureAccess(ask, SPARQLDeserializers.nodeURI(infrastructureURI), user);

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(infrastructureURI);
        }
    }

    public void validateInfrastructureFacilityAccess(List<URI> facilityUris, UserModel user) throws Exception {

        if (user.isAdmin()) {
            return;
        }

        SelectBuilder askFacilityAccess = sparql.getUriListExistQuery(InfrastructureFacilityModel.class, facilityUris);
        addAskInfrastructureAccess(askFacilityAccess, makeVar(InfrastructureFacilityModel.INFRASTRUCTURE_FIELD), user);

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
        addAskInfrastructureAccess(ask, makeVar(InfrastructureFacilityModel.INFRASTRUCTURE_FIELD), user);

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(infrastructureFacilityURI);
        }
    }

    public void addAskInfrastructureAccess(WhereClause<?> ask, Object infraURIVar, UserModel user) {
        Var userProfileVar = makeVar("_userProfile");
        Var groupVar = makeVar(InfrastructureModel.GROUP_FIELD);
        ask.addWhere(infraURIVar, SecurityOntology.hasGroup, groupVar);
        ask.addWhere(groupVar, SecurityOntology.hasUserProfile, userProfileVar);
        ask.addWhere(userProfileVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(user.getUri()));
    }

    public Set<URI> getUserInfrastructures(UserModel user) throws Exception {
        if (user == null || user.isAdmin()) {
            return null;
        }

        String lang = user.getLanguage();
        Set<URI> userInfras = new HashSet<>();

        List<URI> infras = sparql.searchURIs(InfrastructureModel.class, lang, (SelectBuilder select) -> {
            Var userProfileVar = makeVar("_userProfile");
            Var userVar = makeVar("_userURI");
            Var uriVar = makeVar(InfrastructureModel.URI_FIELD);

            WhereHandler userProfileGroup = new WhereHandler();
            userProfileGroup.addWhere(select.makeTriplePath(uriVar, SecurityOntology.hasGroup, makeVar(InfrastructureModel.GROUP_FIELD)));
            userProfileGroup.addWhere(select.makeTriplePath(makeVar(InfrastructureModel.GROUP_FIELD), SecurityOntology.hasUserProfile, userProfileVar));
            userProfileGroup.addWhere(select.makeTriplePath(userProfileVar, SecurityOntology.hasUser, userVar));
            select.getWhereHandler().addOptional(userProfileGroup);

            Expr isInGroup = SPARQLQueryHelper.eq(userVar, SPARQLDeserializers.nodeURI(user.getUri()));

            Var creatorVar = makeVar(ExperimentModel.CREATOR_FIELD);
            select.addOptional(new Triple(uriVar, DCTerms.creator.asNode(), creatorVar));
            Expr isCreator = SPARQLQueryHelper.eq(creatorVar, user.getUri());

            select.addFilter(SPARQLQueryHelper.or(isInGroup, isCreator));
        });

        userInfras.addAll(infras);

        return userInfras;
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

    public ListWithPagination<InfrastructureFacilityModel> searchFacilities(UserModel user, String pattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Set<URI> infras = getUserInfrastructures(user);

        if (infras != null && infras.size() == 0) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        return sparql.searchWithPagination(
                InfrastructureFacilityModel.class,
                user.getLanguage(),
                (select -> {
                    if (infras != null) {
                        SPARQLQueryHelper.inURI(select, InfrastructureFacilityModel.INFRASTRUCTURE_FIELD, infras);
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
        sparql.delete(InfrastructureFacilityModel.class, uri);
    }

    public InfrastructureFacilityModel updateFacility(InfrastructureFacilityModel instance, UserModel user) throws Exception {
        validateInfrastructureFacilityAccess(instance.getUri(), user);
        List<InfrastructureModel> infrastructureModels = sparql.getListByURIs(InfrastructureModel.class, instance.getInfrastructureUris(), user.getLanguage());
        instance.setInfrastructures(infrastructureModels);
        sparql.deleteByURI(sparql.getDefaultGraph(InfrastructureFacilityModel.class), instance.getUri());
        sparql.create(instance);
        return instance;
    }
}

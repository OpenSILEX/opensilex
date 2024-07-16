//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.profile.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vincent
 */
public final class ProfileDAO {

    private final SPARQLService sparql;

    public ProfileDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ProfileModel create(
            URI uri,
            String name,
            List<String> credentials,
            URI publisher
    ) throws Exception {
        ProfileModel profile = new ProfileModel();
        profile.setUri(uri);
        profile.setName(name);
        profile.setCredentials(credentials);
        if (Objects.nonNull(publisher)) {
            profile.setPublisher(publisher);
        }

        sparql.create(profile);

        return profile;
    }

    public ProfileModel get(URI uri) throws Exception {
        return sparql.getByURI(ProfileModel.class, uri, null);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(ProfileModel.class, instanceURI);
    }

    public ProfileModel update(
            URI uri,
            String name,
            List<String> credentials
    ) throws Exception {
        ProfileModel profile = new ProfileModel();
        profile.setUri(uri);
        profile.setName(name);
        profile.setCredentials(credentials);

        sparql.update(profile);

        return profile;
    }

    public ListWithPagination<ProfileModel> search(String namePattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr nameFilter = SPARQLQueryHelper.regexFilter(ProfileModel.NAME_FIELD, namePattern);

        return sparql.searchWithPagination(
                ProfileModel.class,
                null,
                (SelectBuilder select) -> {
                    if (nameFilter != null) {
                        select.addFilter(nameFilter);
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }

    /**
     *
     * @param uriFilter uris filter
     * @param lang user lang
     * @return All profile models with minimal amount of field loaded
     * @throws Exception
     */
    public ListWithPagination<ProfileModel> noProxySearch(List<URI> uriFilter, String lang) throws Exception {
        SparqlNoProxyFetcher<ProfileModel> profileFetcher = new SparqlNoProxyFetcher<>(ProfileModel.class, sparql);

        return sparql.searchWithPagination(
                sparql.getDefaultGraph(ProfileModel.class),
                ProfileModel.class,
                lang,
                (SelectBuilder select) -> {
                    if(!CollectionUtils.isEmpty(uriFilter)){
                        select.addFilter(SPARQLQueryHelper.inURIFilter(GroupUserProfileModel.URI_FIELD, uriFilter));
                    }
                },
                Collections.emptyMap(),
                (SPARQLResult result) -> {
                    //Set to short uri so that they can be easily used in Front-end for Profile selectors
                    ProfileModel nextModel = profileFetcher.getInstance(result, lang);
                    nextModel.setUri(new URI(SPARQLDeserializers.getShortURI(nextModel.getUri())));
                    return nextModel;
                },
                Collections.emptyList(),
                0,
                0
        );
    }

    public List<ProfileModel> getAll(List<OrderBy> orderByList) throws Exception {
        return sparql.search(
                ProfileModel.class,
                null,
                orderByList
        );
    }

    public List<ProfileModel> getByUserURI(URI uri) throws Exception {
        return sparql.search(
                ProfileModel.class,
                null,
                (SelectBuilder select) -> {
                    Var profileURIVar = sparql.getURIFieldVar(ProfileModel.class);
                    Var groupURIVar = makeVar("__userProfileURI");
                    WhereHandler whereHandler = new WhereHandler();
                    whereHandler.addWhere(select.makeTriplePath(groupURIVar, SecurityOntology.hasProfile, profileURIVar));
                    whereHandler.addWhere(select.makeTriplePath(groupURIVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(uri)));

                    ElementNamedGraph elementNamedGraph = new ElementNamedGraph(sparql.getDefaultGraph(GroupUserProfileModel.class), whereHandler.getElement());
                    select.getWhereHandler().getClause().addElement(elementNamedGraph);
                }
        );
    }

    public boolean profileNameExists(String name) throws Exception {
        return sparql.existsByUniquePropertyValue(
                ProfileModel.class,
                RDFS.label,
                name
        );
    }
}

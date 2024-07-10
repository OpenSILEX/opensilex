/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.dal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.group.api.GroupDTO;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vidalmor
 */
public final class GroupDAO {

    private final SPARQLService sparql;

    public GroupDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public GroupModel create(GroupModel group) throws Exception {
        sparql.create(group);
        return group;
    }

    public GroupModel get(URI uri) throws Exception {
        return sparql.getByURI(GroupModel.class, uri, null);
    }

    public void delete(URI groupURI) throws Exception {
        sparql.delete(GroupModel.class, groupURI);
    }

    public GroupModel update(GroupModel group) throws Exception {
        sparql.update(group);
        return group;
    }

    public List<URI> getGroupUriList(AccountModel user) throws Exception {

        return sparql.searchURIs(GroupModel.class, null, (SelectBuilder select) -> {
            Var groupUriVar = sparql.getURIFieldVar(GroupModel.class);
            Var groupUserProfileVar = makeVar("gup");
            select.addWhere(groupUriVar, SecurityOntology.hasUserProfile, groupUserProfileVar);
            select.addWhere(groupUserProfileVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(user.getUri()));
        });

    }



    public ListWithPagination<GroupModel> search(String namePattern, String lang, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr nameFilter = SPARQLQueryHelper.regexFilter(GroupModel.NAME_FIELD, namePattern);

        SparqlNoProxyFetcher<GroupModel> customFetcher = new SparqlNoProxyFetcher<>(GroupModel.class, sparql);

        ListWithPagination<GroupModel> models = sparql.searchWithPagination(
                sparql.getDefaultGraph(GroupModel.class),
                GroupModel.class,
                lang,
                (SelectBuilder select) -> {
                    if (nameFilter != null) {
                        select.addFilter(nameFilter);
                    }
                },
                Collections.emptyMap(),
                (SPARQLResult result) -> customFetcher.getInstance(result, lang),
                orderByList,
                page,
                pageSize
        );

        //Load the GroupUserProfileModels associated to groups
        if(!CollectionUtils.isEmpty(models.getList())){
            loadGroupUserProfileModelsIntoGroups(models.getList(), lang);
        }

        return models;
    }

    /**
     *
     * @param groupModels to modify with loaded GroupUserProfileModels
     * @param lang user language
     * @throws Exception
     */
    private void loadGroupUserProfileModelsIntoGroups(List<GroupModel> groupModels, String lang) throws Exception {
        //Fetch GroupUserProfileModel uris
        SPARQLListFetcher<GroupModel> listFetcher = new SPARQLListFetcher<>(
                sparql,
                GroupModel.class,
                sparql.getDefaultGraph(GroupModel.class),
                Collections.singleton(GroupModel.USER_PROFILES_FIELD),
                groupModels
        );
        listFetcher.updateModels();

        //Maps to have fast access to GroupUserProfileModel's to update and which groups contain which UserProfiles
        HashMap<String, List<String>> userProfileUrisPerGroup = new HashMap<>();
        HashSet<String> encounteredUserProfileUris = new HashSet<>();

        for(GroupModel groupModel : groupModels) {
            String groupUri = SPARQLDeserializers.getExpandedURI(groupModel.getUri());
            if(groupModel.getUserProfiles() != null){
                List<String> userProfileUris = groupModel.getUserProfiles().stream().map(e -> SPARQLDeserializers.getExpandedURI(e.getUri())).collect(Collectors.toList());
                userProfileUrisPerGroup.put(groupUri, userProfileUris);
                encounteredUserProfileUris.addAll(userProfileUris);
            }else{
                userProfileUrisPerGroup.put(groupUri, Collections.emptyList());

            }
        }

        //Complete user profile models basic fields
        SparqlNoProxyFetcher<GroupUserProfileModel> userProfileFetcher = new SparqlNoProxyFetcher<>(GroupUserProfileModel.class, sparql);

        List<URI> encounteredUserProfileUrisAsUris = new ArrayList<>();
        for(String stringUri : encounteredUserProfileUris){
            encounteredUserProfileUrisAsUris.add(new URI(stringUri));
        }
        ListWithPagination<GroupUserProfileModel> userProfileModels = sparql.searchWithPagination(
                sparql.getDefaultGraph(GroupUserProfileModel.class),
                GroupUserProfileModel.class,
                lang,
                (SelectBuilder select) -> {
                    select.addFilter(SPARQLQueryHelper.inURIFilter(GroupUserProfileModel.URI_FIELD, encounteredUserProfileUrisAsUris));
                },
                Collections.emptyMap(),
                (SPARQLResult result) -> userProfileFetcher.getInstance(result, lang),
                Collections.emptyList(),
                0,
                0
        );
        //After search generate uri-GroupUserProfileModel map
        Map<String, GroupUserProfileModel> groupUserProfileModelMap = new HashMap<>();
        userProfileModels.forEach(e->groupUserProfileModelMap.put(SPARQLDeserializers.getExpandedURI(e.getUri()), e));

        //Load Profiles and Accounts into GroupUserProfileModels
        if(!CollectionUtils.isEmpty(userProfileModels.getList())){
            loadProfileAndAccountModelsIntoGroupUserProfileModels(userProfileModels.getList(), lang);
        }

        //Set Groups userProfileModel lists
        for(GroupModel group : groupModels){
            if(userProfileUrisPerGroup.get(SPARQLDeserializers.getExpandedURI(group.getUri())) != null){
                List<GroupUserProfileModel> newGropUserProfileModels = userProfileUrisPerGroup.get(
                        SPARQLDeserializers.getExpandedURI(group.getUri()))
                        .stream()
                        .map(groupUserProfileModelMap::get)
                        .collect(Collectors.toList());

                group.setUserProfiles(newGropUserProfileModels);
            }
        }
    }

    /**
     *
     * @param groupUserProfileModels to update
     * @param lang
     * @throws Exception
     */
    private void loadProfileAndAccountModelsIntoGroupUserProfileModels(List<GroupUserProfileModel> groupUserProfileModels, String lang) throws Exception {

        //Get embedded fields all at once : ProfileModels and AccountModels
        //Sets to save only distinct uris we need to fetch
        HashSet<String> encounteredProfileUris = new HashSet<>();
        HashSet<String> encounteredAccountUris = new HashSet<>();

        //Generate Uri to ProfileModels and AccountModels maps for fast setting of GroupUserProfileModels fields
        Map<String, ProfileModel> profileMap = new HashMap<>();
        Map<String, AccountModel> accountMap = new HashMap<>();

        groupUserProfileModels.forEach(userProfileModel -> {
            if(userProfileModel.getProfile() != null){
                encounteredProfileUris.add(SPARQLDeserializers.getExpandedURI(userProfileModel.getProfile().getUri()));
            }

            if(userProfileModel.getUser() != null){
                encounteredAccountUris.add(SPARQLDeserializers.getExpandedURI(userProfileModel.getUser().getUri()));
            }

        });

        //Load Profile Models
        if(!CollectionUtils.isEmpty(encounteredProfileUris)){
            List<URI> encounteredProfileUrisAsUris = new ArrayList<>();
            for(String stringUri : encounteredProfileUris){
                encounteredProfileUrisAsUris.add(new URI(stringUri));
            }
            //TODO dont invoke profile dao here (put this code in a future logic layer)
            ListWithPagination<ProfileModel> profileModels = new ProfileDAO(sparql).noProxySearch(encounteredProfileUrisAsUris, lang);
            profileModels.forEach(e->profileMap.put(SPARQLDeserializers.getExpandedURI(e.getUri()), e));
        }

        //Load AccountModels
        if(!CollectionUtils.isEmpty(encounteredAccountUris)){
            List<URI> encounteredAccountUrisAsUris = new ArrayList<>();
            for(String stringUri : encounteredAccountUris){
                encounteredAccountUrisAsUris.add(new URI(stringUri));
            }
            //TODO dont invoke group dao here (put this code in a future logic layer)
            ListWithPagination<AccountModel> accountModels = new AccountDAO(sparql).searchWithNoGroupUserProfiles(
                    null,
                    encounteredAccountUrisAsUris,
                    lang,
                    false,
                    false,
                    Collections.emptyList(),
                    0,
                    0);

            accountModels.forEach(e->accountMap.put(SPARQLDeserializers.getExpandedURI(e.getUri()), e));
        }

        //Set GroupUserProfileModel's Accounts and Profiles with what was just fetched
        for(GroupUserProfileModel groupUserProfileModel : groupUserProfileModels){
            groupUserProfileModel.setProfile(profileMap.get(SPARQLDeserializers.getExpandedURI(groupUserProfileModel.getProfile().getUri())));
            groupUserProfileModel.setUser(accountMap.get(SPARQLDeserializers.getExpandedURI(groupUserProfileModel.getUser().getUri())));
        }
    }

    public List<GroupModel> getUserGroups(URI userURI) throws Exception {

        Var userProfilesVar = makeVar(GroupModel.USER_PROFILES_FIELD);
        SPARQLClassObjectMapper<GroupModel> mapper = sparql.getForClass(GroupModel.class);
        return sparql.search(
                GroupModel.class,
                null,
                (SelectBuilder select) -> {
                    select.addWhere(mapper.getURIFieldVar(), SecurityOntology.hasUserProfile, userProfilesVar);
                    select.addWhere(userProfilesVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(userURI));
                }
        );
    }

    public boolean groupNameExists(String name) throws Exception {
        return sparql.existsByUniquePropertyValue(
                GroupModel.class,
                RDFS.label,
                name
        );
    }

    public List<GroupModel> getList(List<URI> uri) throws Exception {
        return sparql.getListByURIs(GroupModel.class, uri, null);
    }
}

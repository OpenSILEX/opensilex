/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.authentication.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.credentials.ExtraCredentialService;
import org.opensilex.security.credentials.config.Credential;
import org.opensilex.security.credentials.config.CredentialConfig;
import org.opensilex.security.credentials.config.CredentialGroup;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 *
 * @author vince
 */
public final class AuthenticationDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationDAO.class);

    private final SPARQLService sparql;

    private final ExtraCredentialService extraCredentialService;

    public AuthenticationDAO(SPARQLService sparql) {
        this.sparql = sparql;
        this.extraCredentialService = new ExtraCredentialService();
    }

    public static String getCredentialIdFromMethod(Method method) {
        ApiCredential credentialAnnotation = method.getAnnotation(ApiCredential.class);
        return (credentialAnnotation != null && !credentialAnnotation.hide()) ? credentialAnnotation.credentialId() : null;
    }

    private static TreeMap<String, Map<String, String>> credentialsGroups;

    private static Map<String, String> credentialsGroupLabels;

    private static Set<String> credentialsIdList;

    private void buildCredentials() {
        if (credentialsGroups == null || credentialsGroupLabels == null || credentialsIdList == null) {
            credentialsGroups = new TreeMap<>();
            credentialsGroupLabels = new HashMap<>();
            credentialsIdList = new HashSet<>();
            Set<Method> methods = sparql.getOpenSilex().getMethodsAnnotatedWith(ApiCredential.class);
            methods.forEach((method) -> {
                ApiCredential apiCredential = method.getAnnotation(ApiCredential.class);

                if (apiCredential != null && !apiCredential.hide()) {
                    String groupId = apiCredential.groupId();
                    String groupLabelKey = apiCredential.groupLabelKey();
                    if (groupId.isEmpty() || groupLabelKey.isEmpty()) {
                        ApiCredentialGroup apiCredentialGroup = method.getDeclaringClass().getAnnotation(ApiCredentialGroup.class);
                        if (apiCredentialGroup != null) {
                            groupId = apiCredentialGroup.groupId();
                            groupLabelKey = apiCredentialGroup.groupLabelKey();
                        } else {
                            groupId = null;
                        }
                    }

                    if (groupId != null && !groupId.isEmpty()) {

                        if (groupLabelKey.isEmpty()) {
                            groupLabelKey = groupId;
                        }
                        
                        if (!credentialsGroups.containsKey(groupId)) {
                            credentialsGroups.put(groupId, new HashMap<>());
                            credentialsGroupLabels.put(groupId, groupLabelKey);
                        }

                        Map<String, String> groupMap = credentialsGroups.get(groupId);

                        LOGGER.debug("Register credential: " + groupId + " - " + apiCredential.credentialId() + " (" + apiCredential.credentialLabelKey() + ")");
                        groupMap.put(apiCredential.credentialId(), apiCredential.credentialLabelKey());
                        credentialsIdList.add(apiCredential.credentialId());
                    }
                }
            });

            // Register extra credentials from config file
            CredentialConfig extraCredentialConfig = extraCredentialService.getCredentialConfig();
            for (CredentialGroup credentialGroup : extraCredentialConfig.credentialGroups()) {
                String groupId = credentialGroup.id();
                String groupLabelKey = credentialGroup.label();

                if (!StringUtils.isEmpty(groupId)) {
                    if (groupLabelKey.isEmpty()) {
                        groupLabelKey = groupId;
                    }

                    if (!credentialsGroups.containsKey(groupId)) {
                        credentialsGroups.put(groupId, new HashMap<>());
                        credentialsGroupLabels.put(groupId, groupLabelKey);
                    }

                    Map<String, String> groupMap = credentialsGroups.get(groupId);

                    for (Credential credential : credentialGroup.credentials()) {
                        LOGGER.debug("Register extra credential: " + groupId + " - " + credential.id() + " (" + credential.label() + ")");
                        groupMap.put(credential.id(), credential.label());
                        credentialsIdList.add(credential.id());
                    }
                }
            }
        }
    }

    public Set<String> getCredentialsIdList() {
        buildCredentials();
        return credentialsIdList;
    }

    public Map<String, String> getCredentialsGroupLabels() {
        buildCredentials();
        return credentialsGroupLabels;
    }

    public TreeMap<String, Map<String, String>> getCredentialsGroups() {
        buildCredentials();
        return credentialsGroups;
    }

    public boolean checkUserAccess(UserModel user, String accessId) throws SPARQLException {
        Node nodeUri = SPARQLDeserializers.nodeURI(user.getUri());
        Var groupVar = makeVar("__group");
        Var profileVar = makeVar("__profile");

        AskBuilder query = sparql.getUriExistsQuery(UserModel.class, user.getUri())
                .addWhere(groupVar, SecurityOntology.hasUser, nodeUri)
                .addWhere(groupVar, SecurityOntology.hasProfile, profileVar)
                .addWhere(profileVar, SecurityOntology.hasAccess, accessId);

        return sparql.executeAskQuery(query);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.security.dal;

import org.opensilex.rest.user.dal.UserModel;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class SecurityAccessDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityAccessDAO.class);

    private final SPARQLService sparql;

    public SecurityAccessDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public static String getCredentialIdFromMethod(Method method) {
        ApiCredential credentialAnnotation = method.getAnnotation(ApiCredential.class);
        return credentialAnnotation.credentialId();
    }

    private static TreeMap<String, Map<String, String>> credentialsGroups;

    private static Map<String, String> credentialsGroupLabels;
    
    private static List<String> credentialsIdList;

    private void buildCredentials() {
        if (credentialsGroups == null || credentialsGroupLabels == null || credentialsIdList == null) {
            credentialsGroups = new TreeMap<>();
            credentialsGroupLabels = new HashMap<>();
            credentialsIdList = new ArrayList<>();
            Set<Method> methods = ClassUtils.getAnnotatedMethods(ApiCredential.class);
            methods.forEach((method) -> {
                ApiCredential apiCredential = method.getAnnotation(ApiCredential.class);

                if (apiCredential != null) {
                    String groupId = apiCredential.groupId();
                    if (!credentialsGroups.containsKey(groupId)) {
                        credentialsGroups.put(groupId, new HashMap<>());
                        credentialsGroupLabels.put(groupId, apiCredential.groupLabelKey());
                    }

                    Map<String, String> groupMap = credentialsGroups.get(groupId);

                    LOGGER.debug("Register credential: " + groupId + " - " + apiCredential.credentialId() + " (" + apiCredential.credentialLabelKey() + ")");
                    groupMap.put(apiCredential.credentialId(), apiCredential.credentialLabelKey());
                    credentialsIdList.add(apiCredential.credentialId());
                }
            });
        }
    }

    public List<String> getCredentialsIdList() {
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.security.dal;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.Path;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.opensilex.utils.SwaggerAPIGenerator;
import org.opensilex.server.security.ApiProtected;
import org.opensilex.server.security.SecurityOntology;
import org.opensilex.server.security.api.CredentialsGroupDTO;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class SecurityAccessDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityAccessDAO.class);

    private final static String PATH_METHOD_SEPARATOR = "|";

    private final SPARQLService sparql;
    
    public SecurityAccessDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    public static String getCredentialIdFromMethod(Method method, String httpMethodString) {
        Path classPath = method.getDeclaringClass().getAnnotation(Path.class);
        Path methodPath = method.getAnnotation(Path.class);
        
        String accessId = "";
        if (classPath != null) {
            accessId = classPath.value();
        }
        
        if (methodPath != null) {
            accessId += "/" + methodPath.value();
        }
        
        accessId = accessId.replaceAll("/+", "/");
        
        accessId += PATH_METHOD_SEPARATOR + HttpMethod.valueOf(httpMethodString.toUpperCase());
        
        return accessId;
    }
    
    public TreeMap<String, Map<String, String>> getCredentialsGroups() {
        Swagger api = SwaggerAPIGenerator.getFullApi();
        TreeMap<String, Map<String, String>> credentialsMap = new TreeMap<>();
        if (api != null) {

            api.getPaths().forEach((String pathUri, io.swagger.models.Path path) -> {
                LOGGER.debug("Analyse API path: " + pathUri);
                path.getOperationMap().forEach((HttpMethod method, Operation operation) -> {
                    addOperationAccess(pathUri, operation, method, credentialsMap);
                });
            });
        }
        
        return credentialsMap;
    }

    private void addOperationAccess(String pathUri, Operation operation, HttpMethod method, Map<String, Map<String, String>> accessMap) {
        if (operation != null) {
            AtomicBoolean isProtected = new AtomicBoolean(false);
            operation.getParameters().forEach((Parameter param) -> {
                if ((param instanceof HeaderParameter) && param.getName().equals(ApiProtected.HEADER_NAME)) {
                    isProtected.set(true);
                }
            });

            if (isProtected.get()) {
                String operationTag = "";
                if (operation.getTags().size() > 0) {
                    operationTag = operation.getTags().get(0);
                    if (operation.getTags().size() > 1) {
                        LOGGER.warn("API has multiple tags for path: " + pathUri);
                        LOGGER.warn("First tag is used by default: " + operationTag);
                    }
                }

                LOGGER.debug("Register webservice in access list: " + method.name() + " " + pathUri);
                if (!accessMap.containsKey(operationTag)) {
                    accessMap.put(operationTag, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
                }

                String accessId = pathUri + PATH_METHOD_SEPARATOR + method.name();
                accessMap.get(operationTag).put(accessId, operation.getSummary());
            } else {
                LOGGER.debug("Ignore webservice from access list (no security header): " + method.name() + " " + pathUri);
            }
        }
    }

    public boolean checkUserAccess(UserModel user, String accessId) throws SPARQLException {
        Node nodeUri = Ontology.nodeURI(user.getUri());
        Var groupVar = makeVar("__group");
        Var profileVar = makeVar("__profile");
        
        AskBuilder query = sparql.getUriExistsQuery(UserModel.class, user.getUri())
                .addWhere(groupVar, SecurityOntology.hasUser, nodeUri)
                .addWhere(groupVar, SecurityOntology.hasProfile, profileVar)
                .addWhere(profileVar, SecurityOntology.hasAccess, accessId);
        
        return sparql.executeAskQuery(query);
    }
}

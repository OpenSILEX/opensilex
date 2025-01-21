package org.opensilex.sparql.service.schemaQuery;

import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.util.List;

/**
 * This class provides an alternative for sparql search to the proxy models. Instead of semi-loading fields we pass a
 * simple schema to follow for the fetching of nested models. This will most of the time be faster than the proxy search
 * as we will only perform 1 search per type per node.
 * A node represents a field on a given model, it has some information about the field in question and a list of child nodes.
 *
 * Here is a simple example of a schema to fetch user Groups, inside these models we want to load the 'userProfiles' field
 * that contains GroupUserProfileModels. Then inside them we want to load the 'user' and 'profile' fields that contain
 * AccountModels and ProfileModels respectively.
 *
 * <pre>
 *     {@code
 *     SparqlSchemaNode<ProfileModel> profileNode = new SparqlSchemaNode<>(
 *                 ProfileModel.class,
 *                 GroupUserProfileModel.PROFILE_FIELD,
 *                 new ArrayList<>(),
 *                 false
 *         );
 *
 *         SparqlSchemaNode<AccountModel> accountNode = new SparqlSchemaNode<>(
 *                 AccountModel.class,
 *                 GroupUserProfileModel.USER_FIELD,
 *                 new ArrayList<>(),
 *                 false
 *         );
 *
 *         SparqlSchemaNode<GroupUserProfileModel> groupUserProfileNode = new SparqlSchemaNode<>(
 *                 GroupUserProfileModel.class,
 *                 GroupModel.USER_PROFILES_FIELD,
 *                 List.of(profileNode, accountNode),
 *                 true
 *         );
 *
 *         SparqlSchemaNode<GroupModel> rootNode = new SparqlSchemaNode<>(
 *                 GroupModel.class,
 *                 null,
 *                 Collections.singletonList(groupUserProfileNode),
 *                 false
 *         );
 *     }
 * </pre>
 *
 * @author mhart
 * @param <T> the SPARQL model class
 */
public class SparqlSchema<T extends SPARQLResourceModel> {

    private final SparqlSchemaNode<T> root;

    public SparqlSchema(SparqlSchemaNode<T> root) {
        this.root = root;
    }

    public List<T> resolveSchema(SPARQLService sparql, List<T> initialSearchResult, String lang) throws Exception {
        root.completeNodeModels(sparql, initialSearchResult, lang);
        return initialSearchResult;
    }

}

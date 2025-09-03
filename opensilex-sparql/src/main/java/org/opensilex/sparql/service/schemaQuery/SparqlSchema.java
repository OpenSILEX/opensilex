/*
 * *****************************************************************************
 *                         SparqlSchema.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *****************************************************************************
 */
package org.opensilex.sparql.service.schemaQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * This class provides an alternative for sparql search to the proxy models. Instead of semi-loading fields we pass a
 * simple schema to follow for the fetching of nested models. This will most of the time be faster than the proxy search
 * as we will only perform 1 search per type per node.
 * A node represents a field on a given model, it has some information about the field in question and a list of child nodes.
 * We can load the dynamic relations on the models of a particular field by setting the node's fetchDynamicRelations boolean attribute.
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
 *                 false,
 *                 false
 *         );
 *
 *         SparqlSchemaNode<AccountModel> accountNode = new SparqlSchemaNode<>(
 *                 AccountModel.class,
 *                 GroupUserProfileModel.USER_FIELD,
 *                 new ArrayList<>(),
 *                 false,
 *                 false
 *         );
 *
 *         SparqlSchemaNode<GroupUserProfileModel> groupUserProfileNode = new SparqlSchemaNode<>(
 *                 GroupUserProfileModel.class,
 *                 GroupModel.USER_PROFILES_FIELD,
 *                 List.of(profileNode, accountNode),
 *                 true,
 *                 false
 *         );
 *
 *         SparqlSchemaRootNode<GroupModel> rootNode = new SparqlSchemaRootNode<>(
 *                 GroupModel.class,
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

    private final SparqlSchemaRootNode<T> root;

    public SparqlSchema(SparqlSchemaRootNode<T> root) {
        this.root = root;
    }

    public List<T> resolveSchema(SPARQLService sparql, List<T> initialSearchResult, String lang) throws Exception {
        if(CollectionUtils.isEmpty(initialSearchResult)) {
            return initialSearchResult;
        }

        //If we want to handle relation in root node then handle them here as normally each node fetches relations for its children
        //(Did it that way so we can fetch relations all at once for a same type across multiple fields)
        if(root.isFetchDynamicRelations()){
            HashMap<String, List<SPARQLModelRelation>> relationsPerUri = SparqlSchemaNode.getRelationsAndCreateUriRelationsMap(
                    root,
                    new HashSet<>(initialSearchResult.stream().map(e-> SPARQLDeserializers.getShortURI(e.getUri())).toList()),
                    sparql
            );

            if(!MapUtils.isEmpty(relationsPerUri)){
                for(T model: initialSearchResult){
                    List<SPARQLModelRelation> relationsForUri = relationsPerUri.get(SPARQLDeserializers.getShortURI(model.getUri()));
                    if(relationsForUri == null){
                        //If null set to an empty list as the dtos expect an empty list, null pointer exception otherwise
                        relationsForUri = new ArrayList<>();
                    }
                    model.setRelations(relationsForUri);
                }
            }
        }

        //Call the recursive function to load the tree of children
        root.completeNodeModels(initialSearchResult, sparql, lang);

        return initialSearchResult;
    }

}

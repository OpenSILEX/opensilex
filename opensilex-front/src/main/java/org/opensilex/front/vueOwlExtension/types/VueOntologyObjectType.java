/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types;

import java.net.URI;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;

/**
 * A specialization of {@link VueOntologyType} used to handle object/resource in VueJs
 * @author vmigot
 */
public interface VueOntologyObjectType extends VueOntologyType {

    default Map<String, String> getInputComponentsByProperty() {
        return new HashMap<>();
    }

    default Map<String, String> getInputComponentsMap() {
        Map<String, String> map = getInputComponentsByProperty();
        Map<String, String> shortMap = new HashMap<>();
        map.forEach((key, value) -> {
            shortMap.put(SPARQLDeserializers.getShortURI(key), value);
        });
        map.putAll(shortMap);
        return map;
    }

    /**
     *
     * @apiNote The default implementation use the {@link org.opensilex.sparql.ontology.store.OntologyStore} to retrieve all aliases from {@link #getTypeUri()}
     */
    @Override
    default List<String> getTypeUriAliases() {

        String type = getTypeUri();
        if(StringUtils.isEmpty(type)){
            return Collections.emptyList();
        }

        try{
            // use OntologyStore in order to retrieve all subClasses
            SPARQLTreeListModel<ClassModel> descendants = SPARQLModule.getOntologyStoreInstance().searchSubClasses(URI.create(type),null,null,true);

            // return all descendant URI
            List<String> types = new ArrayList<>();
            descendants.traverse(classModel -> types.add(classModel.getUri().toString()) );
            return types;
        }catch (SPARQLException e){
            throw new RuntimeException(e);
        }

    }
}

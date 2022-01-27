//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.security.user.dal.UserModel;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vidalmor
 * @author rcolin
 */
public class VariableDAO extends BaseVariableDAO<VariableModel> {

    protected final DataDAO dataDAO;

    public VariableDAO(SPARQLService sparql, MongoDBService nosql, FileStorageService fs) throws URISyntaxException {
        super(VariableModel.class, sparql);
        this.dataDAO = new DataDAO(nosql, sparql, fs);
    }

    @Override
    public void delete(URI varUri) throws Exception {
        int linkedDataNb = getLinkedDataNb(varUri);
        if (linkedDataNb > 0) {
            throw new ForbiddenURIAccessException(varUri, "Variable can't be deleted. " + linkedDataNb + " linked data");
        }
        super.delete(varUri);
    }

    protected int getLinkedDataNb(URI uri) throws Exception {
        return dataDAO.count(null, null, null, Collections.singletonList(uri), null, null, null, null, null, null, null);
    }

    @Override
    public VariableModel update(VariableModel instance) throws Exception {

        VariableModel oldInstance = sparql.loadByURI(VariableModel.class, instance.getUri(), null, null);
        if (oldInstance == null) {
            throw new SPARQLInvalidURIException(instance.getUri());
        }

        // if the datatype has changed, check that they are no linked data
        if (!SPARQLDeserializers.compareURIs(oldInstance.getDataType(), instance.getDataType())) {
            int linkedDataNb = getLinkedDataNb(instance.getUri());
            if (linkedDataNb > 0) {
                throw new ForbiddenURIAccessException(instance.getUri(), "Variable datatype can't be updated. " + linkedDataNb + " linked data");
            }
        }
        return super.update(instance);
    }

    /**
     *
     * @apiNote
     * The following SPARQL variables are used  : <br>
     * <ul>
     *     <li>_entity_name : the name of the variable entity</li>
     *     <li>_characteristic_name : the name of the variable characteristic</li>
     *     <li>_method_name : the name of the variable method</li>
     *     <li>_unit_name : the name of the variable unit</li>
     * </ul>
     * You can use them into the orderByList
     */
    public ListWithPagination<VariableModel> search(
            String name,
            URI entity,
            URI interestEntity,
            URI characteristic,
            URI method,
            URI unit,
            URI group,
            URI experiment,
            List<OrderBy> orderByList,
            int page,
            int pageSize,
            UserModel user) throws Exception {
                
        Set<URI> variableUriList = experiment != null ? dataDAO.getUsedVariablesByExperiments(user, Collections.singletonList(experiment)) : null;

        return sparql.searchWithPagination(
                VariableModel.class,
                null,
            
                (SelectBuilder select) -> {
                    if (!StringUtils.isEmpty(name)) {

                        // append name filter on name or alternative name
                        select.addFilter(SPARQLQueryHelper.or(
                                SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, name),
                                SPARQLQueryHelper.regexFilter(VariableModel.ALTERNATIVE_NAME_FIELD_NAME, name)
                        ));
                    }

                    if (entity != null) {
                        select.addFilter(SPARQLQueryHelper.eq(VariableModel.ENTITY_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(entity.toString()))));
                    }

                    if (interestEntity != null) {
                        select.addFilter(SPARQLQueryHelper.eq(VariableModel.ENTITY_OF_INTEREST_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(interestEntity.toString()))));
                    }

                    if (characteristic != null) {
                        select.addFilter(SPARQLQueryHelper.eq(VariableModel.CHARACTERISTIC_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(characteristic.toString()))));
                    }

                    if (method != null) {
                        select.addFilter(SPARQLQueryHelper.eq(VariableModel.METHOD_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(method.toString()))));
                    }

                    if (unit != null) {
                        select.addFilter(SPARQLQueryHelper.eq(VariableModel.UNIT_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(unit.toString()))));
                    }

                    if (group != null) {
                        select.addWhere(SPARQLDeserializers.nodeURI(group), RDFS.member, makeVar(SPARQLResourceModel.URI_FIELD));
                    }
                    
                    if (!CollectionUtils.isEmpty(variableUriList)) {
                        SPARQLQueryHelper.addWhereUriValues(select, SPARQLResourceModel.URI_FIELD, variableUriList);
                    }
                    
                },
                orderByList,
                page,
                pageSize
        );
    }

    public List<VariableModel> getList(List<URI> uris) throws Exception {
        return sparql.getListByURIs(VariableModel.class, uris, null);
    }
}


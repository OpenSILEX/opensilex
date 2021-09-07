//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import java.net.URI;

import com.google.common.base.CaseFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URISyntaxException;
import java.util.*;

/**
 * @author vidalmor
 * @author rcolin
 */
public class VariableDAO extends BaseVariableDAO<VariableModel> {

    static Var entityLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.ENTITY_FIELD_NAME));
    static Var characteristicLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.CHARACTERISTIC_FIELD_NAME));
    static Var methodLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.METHOD_FIELD_NAME));
    static Var unitLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.UNIT_FIELD_NAME));

    /**
     * Contains the pre-computed list of SPARQL variables which could be used in order to filter or
     * entity, characteristic, method or unit name.
     * <p>
     * This {@link Map} is indexed by the variables names
     */
    static Map<String, Var> varsByVarName;
    static {
        varsByVarName = new HashMap<>();
        varsByVarName.put(entityLabelVar.getVarName(), entityLabelVar);
        varsByVarName.put(characteristicLabelVar.getVarName(), characteristicLabelVar);
        varsByVarName.put(methodLabelVar.getVarName(), methodLabelVar);
        varsByVarName.put(unitLabelVar.getVarName(), unitLabelVar);
    }

    protected final DataDAO dataDAO;

    public VariableDAO(SPARQLService sparql,MongoDBService nosql, FileStorageService fs) throws URISyntaxException {
        super(VariableModel.class, sparql);
        this.dataDAO = new DataDAO(nosql,sparql,fs);
    }

    @Override
    public void delete(URI varUri) throws Exception{
        int linkedDataNb = getLinkedDataNb(varUri);
        if(linkedDataNb > 0){
            throw new ForbiddenURIAccessException(varUri,"Variable can't be deleted. "+linkedDataNb+" linked data");
        }
        super.delete(varUri);
    }

    protected int getLinkedDataNb(URI uri) throws Exception {
        return dataDAO.count(null,null,null, Collections.singletonList(uri),null,null,null,null,null,null,null);
    }

    @Override
    public VariableModel update(VariableModel instance) throws Exception {

        VariableModel oldInstance = sparql.loadByURI(VariableModel.class,instance.getUri(),null,null);
        if (oldInstance == null) {
            throw new SPARQLInvalidURIException(instance.getUri());
        }

        // if the datatype has changed, check that they are no linked data
        if(!SPARQLDeserializers.compareURIs(oldInstance.getDataType(),instance.getDataType())){
            int linkedDataNb = getLinkedDataNb(instance.getUri());
            if(linkedDataNb > 0){
                throw new ForbiddenURIAccessException(instance.getUri(),"Variable datatype can't be updated. "+linkedDataNb+" linked data");
            }
        }
        return super.update(instance);
    }

    /**
     * Read each orderBy of orderByList and then :
     * <pre>
     *     - Append an ORDER BY {@link Expr} into the orderByExprList if the given orderBy field name
     *     is one of {@link #varsByVarName}
     *     - Else append the given orderBy into the unmatchingOrderByList
     * </pre>
     *
     * @param orderByList           the initial {@link OrderBy} list to read
     * @param orderByExprMap        the new list of ORDER BY {@link Expr}.
     *                              An {@link Expr} is created for each {@link OrderBy} which have a field name present in {@link #varsByVarName}
     * @param unmatchingOrderByList the new  list of {@link OrderBy} which doesn't have a field name present in {@link #varsByVarName}
     * @see OrderBy#getFieldName()
     */
    private void appendSpecificOrderBy(List<OrderBy> orderByList, Map<Expr, Order> orderByExprMap, List<OrderBy> unmatchingOrderByList) {

        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

        for (OrderBy orderBy : orderByList) {

            /* We need to map field fieldName to _field_name : which is the variable name returned by SPARQLClassObjectMapper.getObjectNameVarName.
               Else because of OrderBY snake_case to camelCase transformation, the direct mapping between orderBy field and SPARQLClassObjectMapper.getObjectNameVarName() result is broken
               #TODO : append a cleaner way to do it. Here the solution works only because we known how SPARQLClassObjectMapper.getObjectNameVarName() works */

            String correspondingVarName = "_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, orderBy.getFieldName());
            Var correspondingVar = varsByVarName.get(correspondingVarName);

            if (correspondingVar == null) {
                unmatchingOrderByList.add(orderBy);
            } else {
                Expr orderByExpr = exprFactory.lcase(exprFactory.str(correspondingVar));
                orderByExprMap.put(orderByExpr, orderBy.getOrder());
            }
        }

    }

    /**
     * Search all variables with a name, a long name, an entity name or a characteristic name
     * corresponding with the given stringPattern.
     *
     * <br></br>
     * <br> The following SPARQL variables are used  : </br>
     * <pre>
     *     _entity_name : the name of the variable entity
     *     _characteristic_name : the name of the variable characteristic
     *     _method_name : the name of the variable method
     *     _unit_name : the name of the variable unit
     * </pre>
     * <p>
     * You can use them into the orderByList
     *
     * @param stringPattern the string pattern to search
     * @param orderByList   the {@link List} of {@link OrderBy} to apply on query
     * @param page          the current page
     * @param pageSize      the maximum page size
     * @return the list of {@link VariableModel} founds
     * @see VariableModel#getName()
     * @see VariableModel#getAlternativeName()
     * @see EntityModel#getName()
     * @see CharacteristicModel#getName()
     */
    public ListWithPagination<VariableModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        boolean hasStringFilter = !StringUtils.isEmpty(stringPattern);

        if (!hasStringFilter && CollectionUtils.isEmpty(orderByList)) {
            return sparql.searchWithPagination(
                VariableModel.class,
                null,
                (SelectBuilder select) -> {
                },
                orderByList,
                page,
                pageSize
            );
        }


        Map<Expr, Order> orderByExprMap = new HashMap<>();
        List<OrderBy> newOrderByList = new LinkedList<>();
        appendSpecificOrderBy(orderByList, orderByExprMap, newOrderByList);

        return sparql.searchWithPagination(
            VariableModel.class,
            null,
            (SelectBuilder select) -> {

                ExprFactory exprFactory = select.getExprFactory();
                Expr uriStrRegex = exprFactory.str(exprFactory.asVar(VariableModel.URI_FIELD));

                if (hasStringFilter) {

                    // append string regex matching on entity, characteristic, method and unit name
                    Expr[] regexExprArray = varsByVarName.values().stream()
                        .map(var -> SPARQLQueryHelper.regexFilter(var.getVarName(), stringPattern))
                        .toArray(Expr[]::new);

                    // set the string regex filter on entity, characteristic, method, unit  name,long name and URI
                    select.addFilter(
                        SPARQLQueryHelper.or(
                            regexExprArray,
                            SPARQLQueryHelper.regexFilter(VariableModel.NAME_FIELD, stringPattern),
                            SPARQLQueryHelper.regexFilter(VariableModel.ALTERNATIVE_NAME_FIELD_NAME, stringPattern),
                            SPARQLQueryHelper.regexFilter(uriStrRegex, stringPattern, null)
                        ));
                }
                // append specific(s) ORDER BY based on entity, characteristic, method and unit
                orderByExprMap.forEach(select::addOrderBy);
            },
            newOrderByList,
            page,
            pageSize
        );
    }

     public List<VariableModel> getList(List<URI> uris) throws Exception {
        return sparql.getListByURIs(VariableModel.class, uris, null);
    }
}


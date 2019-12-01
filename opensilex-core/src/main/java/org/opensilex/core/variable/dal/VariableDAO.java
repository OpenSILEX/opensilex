//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;


/**
 *
 * @author vidalmor
 */
public class VariableDAO extends BaseVariableDAO<VariableModel> {

    public VariableDAO(SPARQLService sparql) {
        super(VariableModel.class, sparql);
    }

    public ListWithPagination<VariableModel> find(
            String labelPattern,
            String commentPattern,
            URI entity,
            URI quality,
            URI method,
            URI unit,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize
    ) throws Exception {
        SPARQLClassObjectMapper<VariableModel> mapper = SPARQLClassObjectMapper.getForClass(objectClass);

        Expr labelFilter = mapper.getRegexFilter(BaseVariableModel.LABEL_FIELD_NAME, labelPattern);
        Expr commentFilter = mapper.getRegexFilter(BaseVariableModel.COMMENT_FIELD_NAME, commentPattern);

        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);

        return sparql.searchWithPagination(
                objectClass,
                (SelectBuilder select) -> {
                    if (labelFilter != null) {
                        select.addFilter(labelFilter);
                    }
                    if (commentFilter != null) {
                        select.addFilter(commentFilter);
                    }
                    if (entity != null) {
                        select.addWhereValueVar(VariableModel.ENTITY_FIELD_NAME, uriDeserializer.getNode(entity));
                    }
                    if (quality != null) {
                        select.addWhereValueVar(VariableModel.QUALITY_FIELD_NAME, uriDeserializer.getNode(quality));
                    }
                    if (method != null) {
                        select.addWhereValueVar(VariableModel.METHOD_FIELD_NAME, uriDeserializer.getNode(method));
                    }
                    if (unit != null) {
                        select.addWhereValueVar(VariableModel.UNIT_FIELD_NAME, uriDeserializer.getNode(unit));
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }
}

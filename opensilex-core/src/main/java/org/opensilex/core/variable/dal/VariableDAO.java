//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
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

    public ListWithPagination<VariableModel> search(
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
        Expr labelFilter = SPARQLQueryHelper.regexFilter(BaseVariableModel.LABEL_FIELD, labelPattern);
        Expr commentFilter = SPARQLQueryHelper.regexFilter(BaseVariableModel.COMMENT_FIELD, commentPattern);

        SPARQLDeserializer<URI> sparqlURI = SPARQLDeserializers.getForClass(URI.class);
        
        return sparql.searchWithPagination(
                VariableModel.class,
                null,
                (SelectBuilder select) -> {
                    if (labelFilter != null) {
                        select.addFilter(labelFilter);
                    }
                    if (commentFilter != null) {
                        select.addFilter(commentFilter);
                    }
                    if (entity != null) {
                        select.addWhereValueVar(VariableModel.ENTITY_FIELD_NAME, sparqlURI.getNode(entity));
                    }
                    if (quality != null) {
                        select.addWhereValueVar(VariableModel.QUALITY_FIELD_NAME, sparqlURI.getNode(quality));
                    }
                    if (method != null) {
                        select.addWhereValueVar(VariableModel.METHOD_FIELD_NAME, sparqlURI.getNode(method));
                    }
                    if (unit != null) {
                        select.addWhereValueVar(VariableModel.UNIT_FIELD_NAME, sparqlURI.getNode(unit));
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.opensilex.sparql.utils.OrderBy;
import java.net.*;
import java.util.*;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.sparql.expr.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.mapping.*;
import org.opensilex.utils.*;

/**
 *
 * @author vidalmor
 */
public class BaseVariableDAO<T extends BaseVariableModel> {
    
    protected final SPARQLService sparql;
    
    protected final Class<T> objectClass;
    
    public BaseVariableDAO(Class<T> objectClass, SPARQLService sparql) {
        this.sparql = sparql;
        this.objectClass = objectClass;
    }
    
    public T create(T instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public T update(T instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(objectClass, instanceURI);
    }

    public T get(URI instanceURI) throws Exception {
        return sparql.getByURI(objectClass, instanceURI);
    }

    public ListWithPagination<T> find(String labelPattern, String commentPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        SPARQLClassObjectMapper<T> mapper = SPARQLClassObjectMapper.getForClass(objectClass);
        
        Expr labelFilter = mapper.getRegexFilter(BaseVariableModel.LABEL_FIELD_NAME, labelPattern);
        Expr commentFilter = mapper.getRegexFilter(BaseVariableModel.COMMENT_FIELD_NAME, commentPattern);

        return sparql.searchWithPagination(
                objectClass,
                (SelectBuilder select) -> {
                    if (labelFilter != null) {
                        select.addFilter(labelFilter);
                    }
                    if (commentFilter != null) {
                        select.addFilter(commentFilter);
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }
}

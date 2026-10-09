package org.opensilex.core.widget.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.List;

/**
 * Persistence of widgets. A DAO is a plain object created where it is needed ({@code new WidgetDAO(sparql)}); it never
 * imports API classes or DTOs, so it stays usable from migrations, CLI commands and other modules.
 */
public class WidgetDAO {

    private final SPARQLService sparql;

    public WidgetDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public WidgetModel create(WidgetModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public WidgetModel get(URI uri, AccountModel user) throws Exception {
        return sparql.getByURI(WidgetModel.class, uri, user.getLanguage());
    }

    public List<WidgetModel> getList(List<URI> uris, AccountModel user) throws Exception {
        return sparql.getListByURIs(WidgetModel.class, uris, user.getLanguage());
    }

    /**
     * Rewrites the resource from the given model: a {@code null} field removes the stored value unless the property
     * is declared with {@code ignoreUpdateIfNull = true}. Build the model from a DTO that carries every field.
     */
    public WidgetModel update(WidgetModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(WidgetModel.class, uri);
    }

    public ListWithPagination<WidgetModel> search(WidgetSearchFilter filter) throws Exception {
        Expr nameFilter = SPARQLQueryHelper.regexFilter(WidgetModel.NAME_FIELD, filter.getNamePattern());

        return sparql.searchWithPagination(
                WidgetModel.class,
                filter.getLang(),
                (SelectBuilder select) -> {
                    if (nameFilter != null) {
                        select.addFilter(nameFilter);
                    }
                },
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize()
        );
    }

    public boolean nameExists(String name) throws Exception {
        return sparql.existsByUniquePropertyValue(WidgetModel.class, RDFS.label, name);
    }
}

package org.opensilex.core.widget.bll;

import org.opensilex.core.widget.dal.WidgetDAO;
import org.opensilex.core.widget.dal.WidgetModel;
import org.opensilex.core.widget.dal.WidgetSearchFilter;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;

/**
 * Business rules of the widget concept.
 * <p>
 * Add a {@code bll} class when a rule spans several DAOs or stores (SPARQL + MongoDB), needs a transaction, or must be
 * reused by the API, a CLI command and a migration. Plain CRUD can stay in the API + DAO. Expected business failures
 * are typed exceptions of {@code org.opensilex.server.exceptions}, translated to HTTP statuses by the global mapper:
 * the API layer needs no try/catch for them.
 */
public class WidgetLogic {

    private final WidgetDAO dao;

    public WidgetLogic(SPARQLService sparql) {
        this.dao = new WidgetDAO(sparql);
    }

    public WidgetModel create(WidgetModel instance, AccountModel user) throws Exception {
        if (dao.nameExists(instance.getName())) {
            throw new ConflictException("Widget already exists: " + instance.getName());
        }
        instance.setPublisher(user.getUri());
        return dao.create(instance);
    }

    public WidgetModel get(URI uri, AccountModel user) throws Exception {
        WidgetModel model = dao.get(uri, user);
        if (model == null) {
            throw new NotFoundURIException(uri);
        }
        return model;
    }

    public ListWithPagination<WidgetModel> search(WidgetSearchFilter filter) throws Exception {
        return dao.search(filter);
    }
}

package org.opensilex.sparql.service;

import org.opensilex.utils.OrderBy;

import java.util.List;

/**
 * @author rcolin
 */
public abstract class SparqlSearchFilter {

    protected List<OrderBy> orderByList;

    protected Integer page;

    protected Integer pageSize;

    protected String lang;

    public List<OrderBy> getOrderByList() {
        return orderByList;
    }

    public SparqlSearchFilter setOrderByList(List<OrderBy> orderByList) {
        this.orderByList = orderByList;
        return this;
    }

    public Integer getPage() {
        return page;
    }

    public SparqlSearchFilter setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public SparqlSearchFilter setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public String getLang() {
        return lang;
    }

    public SparqlSearchFilter setLang(String lang) {
        this.lang = lang;
        return this;
    }
}

package org.opensilex.sparql.service;

import io.swagger.annotations.ApiModelProperty;
import net.minidev.json.annotate.JsonIgnore;
import org.opensilex.OpenSilex;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author rcolin
 */
public abstract class SparqlSearchFilter {

    protected Collection<URI> includedUris;

    protected List<OrderBy> orderByList;
    protected Integer page;
    protected Integer pageSize;

    @JsonIgnore
    protected String lang;

    protected SparqlSearchFilter() {
        this.lang = OpenSilex.DEFAULT_LANGUAGE;
        this.page = 0;
        this.pageSize = 20;
        this.orderByList = Collections.emptyList();
        this.includedUris = Collections.emptyList();
    }

    public Collection<URI> getIncludedUris() {
        return includedUris;
    }

    public SparqlSearchFilter setIncludedUris(Collection<URI> includedUris) {
        this.includedUris = includedUris;
        return this;
    }

    @ApiModelProperty(name = "order_by", value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc")
    public List<OrderBy> getOrderByList() {
        return orderByList;
    }

    public SparqlSearchFilter setOrderByList(List<OrderBy> orderByList) {
        this.orderByList = orderByList;
        return this;
    }

    @ApiModelProperty(name = "page", value = "Page number")
    public Integer getPage() {
        return page;
    }

    public SparqlSearchFilter setPage(Integer page) {
        this.page = page;
        return this;
    }

    @ApiModelProperty(name = "page_size", value = "Page size")
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

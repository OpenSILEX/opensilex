package org.opensilex.sparql.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import net.minidev.json.annotate.JsonIgnore;
import org.opensilex.OpenSilex;
import org.opensilex.utils.OrderBy;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generic search filter for database request
 * @author rcolin
 */
public abstract class SearchFilter {

    public static final int DEFAULT_PAGE_SIZE = 20;

    protected Collection<URI> includedUris;
    protected Collection<URI> rdfTypes;
    protected List<OrderBy> orderByList;
    protected int page;
    protected int pageSize;

    @JsonIgnore
    protected String lang;

    protected SearchFilter() {
        this.lang = OpenSilex.DEFAULT_LANGUAGE;
        this.page = 0;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.orderByList = Collections.emptyList();
        this.includedUris = Collections.emptyList();
    }

    public Collection<URI> getIncludedUris() {
        return includedUris;
    }

    public SearchFilter setIncludedUris(Collection<URI> includedUris) {
        this.includedUris = includedUris;
        return this;
    }

    @ApiModelProperty(name = "order_by", value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc")
    @JsonProperty("order_by")
    public List<OrderBy> getOrderByList() {
        return orderByList;
    }

    public SearchFilter setOrderByList(List<OrderBy> orderByList) {
        this.orderByList = orderByList;
        return this;
    }

    @ApiModelProperty(name = "page", value = "Page number")
    public int getPage() {
        return page;
    }

    public SearchFilter setPage(int page) {
        if(page < 0){
            throw new IllegalArgumentException("pageSize must be >= to 0");
        }
        this.page = page;
        return this;
    }

    @ApiModelProperty(name = "page_size", value = "Page size")
    @JsonProperty("page_size")
    public int getPageSize() {
        return pageSize;
    }

    public SearchFilter setPageSize(int pageSize) {
        if(pageSize < 0){
            throw new IllegalArgumentException("pageSize must be >= to 0");
        }
        this.pageSize = pageSize;
        return this;
    }

    public String getLang() {
        return lang;
    }

    public SearchFilter setLang(String lang) {
        this.lang = lang;
        return this;
    }

    public Collection<URI> getRdfTypes() {
        return rdfTypes;
    }

    public void setRdfTypes(Collection<URI> rdfTypes) {
        this.rdfTypes = rdfTypes;
    }

    /**
     * Validate the constraints for the filter. At the moment, the only validated constraint is that all getters annotated
     * with the {@link NotNull} annotations actually return an initialized value.
     * <p>
     *     You can override this method to add custom validation, but make sure to call <code>super.validate();</code>
     *     to validate the basic constraints.
     * </p>
     *
     * @throws IllegalArgumentException If a constraint is not valid
     * @throws InvocationTargetException If the annotated method throws an exception
     * @throws IllegalAccessException If the method annotated with "NotNull" could not be accessed from SparqlSearchFilter.
     * Remember that {@link NotNull} can only be specified on <strong>public getters</strong>.
     */
    public void validate() throws IllegalArgumentException, InvocationTargetException, IllegalAccessException {
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(NotNull.class) && Objects.isNull(method.invoke(this))) {
                throw new IllegalArgumentException(method.getName() + " cannot be null");
            }
        }
    }
}

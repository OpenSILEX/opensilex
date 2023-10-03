package org.opensilex.query;

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
 * @author rcolin
 */
public abstract class SearchFilter {

    protected Collection<URI> includedUris;
    protected Collection<URI> rdfTypes;


    protected List<OrderBy> orderByList;
    protected Integer page;
    protected Integer pageSize;

    @JsonIgnore
    protected String lang;

    @JsonIgnore
    private URI accountURI;

    protected SearchFilter() {
        this.lang = OpenSilex.DEFAULT_LANGUAGE;
        this.page = 0;
        this.pageSize = 0;
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
    public Integer getPage() {
        return page;
    }

    public SearchFilter setPage(Integer page) {
        this.page = page;
        return this;
    }

    @ApiModelProperty(name = "page_size", value = "Page size")
    @JsonProperty("page_size")
    public Integer getPageSize() {
        return pageSize;
    }

    public SearchFilter setPageSize(Integer pageSize) {
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

    public URI getAccountURI() {
        return accountURI;
    }

    public SearchFilter setAccountURI(URI accountURI) {
        this.accountURI = accountURI;
        return this;
    }

    public Collection<URI> getRdfTypes() {
        return rdfTypes;
    }

    public SearchFilter setRdfTypes(Collection<URI> rdfTypes) {
        this.rdfTypes = rdfTypes;
        return this;
    }
}

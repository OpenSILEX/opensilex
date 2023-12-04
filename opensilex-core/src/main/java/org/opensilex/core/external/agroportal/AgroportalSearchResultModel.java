/*******************************************************************************
 *                         AgroPortalSearchResultModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 04/12/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.external.agroportal;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author Valentin Rigolle
 */
public class AgroportalSearchResultModel {
    private int page;
    private int pageCount;
    private int totalCount;
    private Integer prevPage;
    private Integer nextPage;
    private Map<String, URI> links;
    private List<AgroportalTermModel> collection;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPrevPage() {
        return prevPage;
    }

    public void setPrevPage(Integer prevPage) {
        this.prevPage = prevPage;
    }

    public Integer getNextPage() {
        return nextPage;
    }

    public void setNextPage(Integer nextPage) {
        this.nextPage = nextPage;
    }

    public Map<String, URI> getLinks() {
        return links;
    }

    public void setLinks(Map<String, URI> links) {
        this.links = links;
    }

    public List<AgroportalTermModel> getCollection() {
        return collection;
    }

    public void setCollection(List<AgroportalTermModel> collection) {
        this.collection = collection;
    }
}

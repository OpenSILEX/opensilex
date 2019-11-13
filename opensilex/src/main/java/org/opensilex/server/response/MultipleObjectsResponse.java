/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.response;

import java.util.List;

/**
 *
 * @author vincent
 */
public class MultipleObjectsResponse<T> extends JsonResponse<List<T>> {

    public MultipleObjectsResponse(List<T> result, long currentPage, long totalCount) {
        super(javax.ws.rs.core.Response.Status.OK);
        this.metadata = new Metadata(new Pagination(result.size(), currentPage, totalCount));
        this.result = result;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.response;

/**
 *
 * @author vincent
 */
public class SingleObjectResponse<T> extends JsonResponse<T> {

    public SingleObjectResponse(T result) {
        super(javax.ws.rs.core.Response.Status.OK);
        this.metadata = new Metadata(new Pagination());
        this.result = result;
    }
}

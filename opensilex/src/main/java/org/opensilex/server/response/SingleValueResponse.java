/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.response;

import javax.ws.rs.core.Response.Status;

/**
 *
 * @author Vincent Migot
 */
public class SingleValueResponse extends JsonResponse {

    private String value;
    
    public SingleValueResponse(Object value) {
        super(Status.OK);
        this.value = value.toString();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    
}

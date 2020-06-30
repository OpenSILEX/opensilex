/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.response;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.server.response.JsonResponse;

/**
 *
 * @author vmigot
 */
public class PartialResourceTreeResponse extends JsonResponse<List<PartialResourceTreeDTO>> {

    public PartialResourceTreeResponse() {
        this(new ArrayList<>());
    }

    public PartialResourceTreeResponse(List<PartialResourceTreeDTO> result) {
        super();
        this.result = result;
    }

}
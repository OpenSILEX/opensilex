/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Alice Boizet
 */
public class DataFileUpdateDTO extends DataFilePathCreationDTO{
    URI uri;
    
    @NotNull
    @Override
    public URI getUri() {
        return uri;
    }
    
}

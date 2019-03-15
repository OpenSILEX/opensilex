//******************************************************************************
//                           VerifiedClassInterface.java
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: 25 Jun, 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package phis2ws.service.resources.dto.manager;

/**
 * Interface implemented by DTO class which will be verified
 * during a POST Request
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 * @param <T> Object returned by the class
 */
public interface VerifiedClassInterface<T> {
    
    public T createObjectFromDTO() throws Exception;
}

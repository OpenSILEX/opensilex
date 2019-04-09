//******************************************************************************
//                           VerifiedClassInterface.java
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: 25 Jun. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
<<<<<<< HEAD:phis2-ws/src/main/java/opensilex/service/resources/dto/manager/VerifiedClassInterface.java
package opensilex.service.resources.dto.manager;
=======

package opensilex.service.resource.dto.manager;
>>>>>>> reorganize-packages:phis2-ws/src/main/java/opensilex/service/resource/dto/manager/VerifiedClassInterface.java

/**
 * Interface implemented by DTO classes which will be verified
 * during a POST Request
 * @param <T> Object returned by the class
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public interface VerifiedClassInterface<T> {
    
    public T createObjectFromDTO() throws Exception;
}

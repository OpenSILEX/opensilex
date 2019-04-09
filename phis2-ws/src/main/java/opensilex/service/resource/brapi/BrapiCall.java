//******************************************************************************
//                                       BrapiCall.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.brapi;

import java.util.ArrayList;
import opensilex.service.model.Call;

/**
 * Interface for dependency injection in order to get Calls attributes
 * @author Alice Boizet alice.boizet@inra.fr
 * @update [Alice Boizet] 24 September, 2018 : the input of the method callInfo is now a List 
 * because there can be several calls defined in the same class (see TraitsResourceService as an example)
 */
public interface BrapiCall {

    ArrayList<Call> callInfo();
}

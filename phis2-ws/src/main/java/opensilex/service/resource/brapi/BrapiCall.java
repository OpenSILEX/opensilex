//******************************************************************************
//                                 BrapiCall.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 24 Sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.brapi;


import java.util.ArrayList;

import opensilex.service.model.Call;
import org.jvnet.hk2.annotations.Contract;

/**
 * Interface for dependency injection in order to get Calls attributes.
 * @update [Alice Boizet] 24 Sept. 2018: the input of the method callInfo is now 
 * a List because there can be several calls defined in the same class (see 
 * TraitsResourceService as an example)
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
@Contract
public interface BrapiCall {

    ArrayList<Call> callInfo();
}

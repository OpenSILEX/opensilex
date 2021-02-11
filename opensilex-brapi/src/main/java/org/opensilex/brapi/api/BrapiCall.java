//******************************************************************************
//                                 BrapiCall.java 
// SILEX-PHIS
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;


import java.util.ArrayList;
import org.opensilex.brapi.model.Call;

/**
 * Interface for dependency injection in order to get Calls attributes. * 
 * @author Alice Boizet
 */
public interface BrapiCall {

    ArrayList<Call> callInfo();
}

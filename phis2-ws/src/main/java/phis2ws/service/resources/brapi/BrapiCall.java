//******************************************************************************
//                                       BrapiCall.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package phis2ws.service.resources.brapi;

import phis2ws.service.view.model.phis.Call;

/**
 * interface for dependency injection in order to get Calls attributes
 * @author Alice Boizet alice.boizet@inra.fr
 */
public interface BrapiCall {
    Call callInfo();
}

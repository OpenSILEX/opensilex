//******************************************************************************
//                                       BrapiCalls.java
//
// Author(s): Alice Boizet
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 26 juil. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  26 juil. 2018
// Subject: interface for dependency injection in order to get Calls attribute
//******************************************************************************
package phis2ws.service.resources;

import phis2ws.service.view.model.phis.Call;

public interface BrapiCall {
    Call callInfo();
}

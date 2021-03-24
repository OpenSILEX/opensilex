//******************************************************************************
//                      OpenIDException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication;

public class OpenIDException extends Exception {

    public OpenIDException(String message) {
        super(message);
    }

    public OpenIDException(String message, Exception ex) {
        super(message, ex);
    }
}

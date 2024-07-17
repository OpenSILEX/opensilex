//******************************************************************************
//                          BrapiDataResponsePart.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi;

/**
 *
 * @author Alice Boizet
 */
public class BrapiDataResponsePart<T> {
    private T data;

    public BrapiDataResponsePart(T data) {
        this.data = data;
    }

    public BrapiDataResponsePart() {
        this.data = null;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
    
}

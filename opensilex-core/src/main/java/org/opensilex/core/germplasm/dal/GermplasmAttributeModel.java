//******************************************************************************
//                          GermplasmAttributeModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import java.util.Map;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 *
 * @author Alice Boizet
 */
public class GermplasmAttributeModel extends MongoModel{
    
    Map<String, String> attribute;

    public Map<String, String> getAttribute() {
        return attribute;
    }

    public void setAttribute(Map<String, String> attribute) {
        this.attribute = attribute;
    }

}

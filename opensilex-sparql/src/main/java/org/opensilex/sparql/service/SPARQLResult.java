//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.service;

import java.util.function.BiConsumer;

/**
 *
 * @author Vincent Migot
 */
public interface SPARQLResult {

    public String getStringValue(String key);
    
    public void forEach(BiConsumer<? super String, ? super String> action);
}

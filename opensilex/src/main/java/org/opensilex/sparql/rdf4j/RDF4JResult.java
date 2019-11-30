//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import java.util.function.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.*;
import org.opensilex.sparql.*;

/**
 *
 * @author vincent
 */
public class RDF4JResult implements SPARQLResult {

    private BindingSet bindingSet;

    public RDF4JResult(BindingSet bindingSet) {
        this.bindingSet = bindingSet;
    }

    @Override
    public String getStringValue(String key) {
        Value binding = bindingSet.getValue(key);
        if (binding == null) {
            return null;
        } else {
            return bindingSet.getValue(key).stringValue();
        }
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super String> action) {
        bindingSet.forEach((Binding bind) -> {
            action.accept(bind.getName(), bind.getValue().stringValue());
        });
    }

}

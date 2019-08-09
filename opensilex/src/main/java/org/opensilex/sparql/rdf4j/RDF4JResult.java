/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import java.util.function.BiConsumer;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.opensilex.sparql.SPARQLResult;

/**
 *
 * @author vincent
 */
public class RDF4JResult implements SPARQLResult {

    private Statement statement;

    public RDF4JResult(Statement statement) {
        this.statement = statement;
    }

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

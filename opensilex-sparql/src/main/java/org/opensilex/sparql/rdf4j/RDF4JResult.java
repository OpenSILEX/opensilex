//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import java.net.URI;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.opensilex.sparql.service.SPARQLLiteral;
import org.opensilex.sparql.service.SPARQLResult;


/**
 *
 * @author vincent
 */
public class RDF4JResult implements SPARQLResult {

    private final BindingSet bindingSet;

    public RDF4JResult(BindingSet bindingSet) {
        this.bindingSet = bindingSet;
    }

    @Override
    public String getStringValue(String key) {
        return getValue(key)
                .map(Value::stringValue)
                .orElse(null);
    }

    @Override
    public Optional<SPARQLLiteral> getLiteralValue(String key) {
        var value = getValue(key);
        if (value.isEmpty() || !value.get().isLiteral()) {
            return Optional.empty();
        }

        var literal = (Literal) value.get();
        return Optional.of(new SPARQLLiteral(
                literal.stringValue(),
                literal.getLanguage().orElse(null),
                URI.create(literal.getDatatype().stringValue())));
    }

    @Override
    public boolean isURI(String key) {
        return getValue(key)
                .map(Value::isIRI)
                .orElse(false);
    }

    @Override
    public boolean isLiteral(String key) {
        return getValue(key)
                .map(Value::isLiteral)
                .orElse(false);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super String> action) {
        bindingSet.forEach((Binding bind) ->
                action.accept(bind.getName(), bind.getValue().stringValue()));
    }

    private Optional<Value> getValue(String key) {
        return Optional.ofNullable(bindingSet.getValue(key));
    }
}

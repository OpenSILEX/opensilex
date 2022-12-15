package org.opensilex.sparql.csv.validation;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.Objects;

/**
 * @author rcolin
 */
public class CustomCsvValidation<T extends SPARQLResourceModel> {

    private final String property;
    private final URI propertyURI;
    private final boolean applyDefaultValidation;
    private final CustomCsvValidationAction<T> validationAction;

    public CustomCsvValidation(String property, boolean applyDefaultValidation, CustomCsvValidationAction<T> validationAction) {
        if(StringUtils.isEmpty(property)){
            throw new IllegalArgumentException("Null or empty property");
        }
        this.property = property;
        this.propertyURI = URIDeserializer.formatURI(property);
        this.applyDefaultValidation = applyDefaultValidation;

        Objects.requireNonNull(validationAction);
        this.validationAction = validationAction;
    }

    public String getProperty() {
        return property;
    }

    public URI getPropertyURI() {
        return propertyURI;
    }

    public boolean applyDefaultValidation() {
        return applyDefaultValidation;
    }

    public CustomCsvValidationAction<T> getValidationAction() {
        return validationAction;
    }
}

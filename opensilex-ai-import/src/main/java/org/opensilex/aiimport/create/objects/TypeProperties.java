//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.objects;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.ontology.dal.AbstractPropertyModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.OntologyStore;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The properties a scientific-object type accepts, as the platform's own importer accepts them.
 * <p>
 * Read from the same place the importer reads them — the ontology store's restrictions on the
 * class, inherited from every ancestor up to {@code oeso:ScientificObject} — so the drop-down never
 * offers a property the importer would then refuse as unknown for the type.
 *
 * @author Arnaud Charleroy
 */
public class TypeProperties {

    /**
     * One property of the type.
     *
     * @param object whether its value is a resource, named in the file and found by its name
     * @param range  the class of the resources it points to, or the datatype of its values
     */
    public record TypeProperty(URI uri, String name, boolean object, URI range, boolean required,
                               boolean list) {
    }

    private static final URI SCIENTIFIC_OBJECT = URI.create(Oeso.ScientificObject.getURI());

    private final String lang;

    public TypeProperties(String lang) {
        this.lang = lang;
    }

    /**
     * @return whether the type is {@code oeso:ScientificObject} or one of its descendants
     * <p>
     * Decided by walking up the class's parents rather than by the store's {@code classExist}
     * with an ancestor, which answers yes for any class the store knows — a facility, a germplasm —
     * whatever its ancestry.
     */
    public boolean isObjectType(URI type) {
        if (type == null) {
            return false;
        }
        try {
            for (ClassModel model = store().getClassModel(type, SCIENTIFIC_OBJECT, lang); model != null;
                 model = model.getParent()) {
                if (SPARQLDeserializers.compareURIs(model.getUri(), SCIENTIFIC_OBJECT)) {
                    return true;
                }
            }
            return false;
        } catch (SPARQLException | RuntimeException e) {
            return false;
        }
    }

    /**
     * @return the properties of the type, by name
     * @throws IllegalArgumentException when the type is not a scientific-object type
     */
    public List<TypeProperty> of(URI type) throws SPARQLException {
        if (!isObjectType(type)) {
            throw new IllegalArgumentException("Not a scientific object type: " + type);
        }
        OntologyStore store = store();
        ClassModel model = store.getClassModel(type, SCIENTIFIC_OBJECT, lang);
        List<TypeProperty> properties = new ArrayList<>();
        for (OwlRestrictionModel restriction : model.getRestrictionsByProperties().values()) {
            URI property = URI.create(SPARQLDeserializers.getExpandedURI(restriction.getOnProperty()));
            boolean object = restriction.getOnClass() != null;
            URI range = object ? restriction.getOnClass() : restriction.getOnDataRange();
            properties.add(new TypeProperty(property, nameOf(store, property), object,
                    range == null ? null : URI.create(SPARQLDeserializers.getExpandedURI(range)),
                    restriction.isRequired(), restriction.isList()));
        }
        properties.sort(Comparator.comparing(TypeProperty::name, String.CASE_INSENSITIVE_ORDER));
        return properties;
    }

    /**
     * The property's label in the user's language, or the last segment of its URI when the store
     * has no label for it.
     */
    private String nameOf(OntologyStore store, URI property) {
        try {
            AbstractPropertyModel<?> model = store.getProperty(property, null, null, lang);
            if (model != null && model.getName() != null && !model.getName().isBlank()) {
                return model.getName();
            }
        } catch (SPARQLException | RuntimeException e) {
            // Named from its URI below: a label is a courtesy, not a condition.
        }
        String text = property.toString();
        return text.substring(Math.max(text.lastIndexOf('#'), text.lastIndexOf('/')) + 1);
    }

    private static OntologyStore store() {
        return SPARQLModule.getOntologyStoreInstance();
    }
}

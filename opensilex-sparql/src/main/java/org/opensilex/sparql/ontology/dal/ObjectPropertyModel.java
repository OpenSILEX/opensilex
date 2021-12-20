/*******************************************************************************
 *                         ObjectPropertyModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.dal;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeModel;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author vmigot
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "ObjectProperty",
        ignoreValidation = true
)
public class ObjectPropertyModel extends AbstractPropertyModel<ObjectPropertyModel> {

    @SPARQLIgnore()
    protected String name;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf",
            inverse = true
    )
    protected List<ObjectPropertyModel> children;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf"
    )
    protected ObjectPropertyModel parent;


    @SPARQLProperty(
            ontology = RDFS.class,
            property = "range"
    )
    protected ClassModel range;
    public static final String RANGE_FIELD = "range";

    protected Set<ObjectPropertyModel> parents;

    public ObjectPropertyModel() {
        children = new LinkedList<>();
        parents = new HashSet<>();
    }

    public ObjectPropertyModel(ObjectPropertyModel other) {
        this(other, true);
    }

    public ObjectPropertyModel(ObjectPropertyModel other, boolean readChildren) {

    }

    public ClassModel getRange() {
        return range;
    }

    public void setRange(ClassModel range) {
        this.range = range;
    }

    @Override
    public List<ObjectPropertyModel> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<ObjectPropertyModel> children) {
        this.children = children;
    }

    @Override
    public ObjectPropertyModel getParent() {
        return parent;
    }

    @Override
    public void setParent(ObjectPropertyModel parent) {
        this.parent = parent;
    }

    @Override
    public Set<ObjectPropertyModel> getParents() {
        return parents;
    }

    @Override
    public void setParents(Set<ObjectPropertyModel> parents) {
        this.parents = parents;
    }
}

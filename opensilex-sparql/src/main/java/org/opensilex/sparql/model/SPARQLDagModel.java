package org.opensilex.sparql.model;

import java.util.List;

public abstract class SPARQLDagModel<T extends SPARQLDagModel<T>> extends SPARQLNamedResourceModel<SPARQLDagModel<T>> {
    protected List<T> parents;
    public static final String PARENTS_FIELD = "parents";

    protected List<T> children;
    public static final String CHILDREN_FIELD = "children";

    public List<T> getParents() {
        return parents;
    }

    public void setParents(List<T> parents) {
        this.parents = parents;
    }

    public List<T> getChildren() {
        return children;
    }

    public void setChildren(List<T> children) {
        this.children = children;
    }
}

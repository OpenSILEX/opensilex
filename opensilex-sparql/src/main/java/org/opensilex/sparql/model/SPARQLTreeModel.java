/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.model;

import java.util.List;

/**
 *
 * @author vince
 */
public abstract class SPARQLTreeModel<T extends SPARQLResourceModel> extends SPARQLResourceModel {

    protected String name;
    public static final String NAME_FIELD = "name";

    protected T parent;
    public static final String PARENT_FIELD = "parent";

    protected List<T> children;
    public static final String CHILDREN_FIELD = "children";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getParent() {
        return parent;
    }

    public void setParent(T parent) {
        this.parent = parent;
    }

    public List<T> getChildren() {
        return children;
    }

    public void setChildren(List<T> children) {
        this.children = children;
    }
}

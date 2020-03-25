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
public abstract class SPARQLTreeModel extends SPARQLResourceModel {

    protected String name;

    protected SPARQLTreeModel parent;

    protected List<? extends SPARQLTreeModel> children;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SPARQLTreeModel getParent() {
        return parent;
    }

    public void setParent(SPARQLTreeModel parent) {
        this.parent = parent;
    }

    public List<? extends SPARQLTreeModel> getChildren() {
        return children;
    }

    public void setChildren(List<? extends SPARQLTreeModel> children) {
        this.children = children;
    }
}

package org.opensilex.sparql.response;

import org.opensilex.sparql.model.SPARQLDagModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceDagDTO<T extends SPARQLDagModel<?>> extends NamedResourceDTO<T> {
    private List<URI> parents;

    private List<URI> children;

    public List<URI> getParents() {
        return parents;
    }

    public void setParents(List<URI> parents) {
        this.parents = parents;
    }

    public List<URI> getChildren() {
        return children;
    }

    public void setChildren(List<URI> children) {
        this.children = children;
    }

    @Override
    public void fromModel(T model) {
        super.fromModel(model);

        List<T> parents = (List<T>) model.getParents();
        if (parents != null) {
            setParents(parents
                    .stream().map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList()));
        }

        List<T> children = (List<T>) model.getChildren();
        if (children != null) {
            setChildren(children
                    .stream().map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList()));
        }
    }
}

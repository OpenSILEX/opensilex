package org.opensilex.sparql.response;

import org.opensilex.sparql.model.SPARQLDagModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceDagDTO extends NamedResourceDTO<SPARQLDagModel<?>> {
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
    public void fromModel(SPARQLDagModel<?> model) {
        super.fromModel(model);

        setParents(model.getParents()
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList()));

        setChildren(model.getChildren()
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList()));
    }
}

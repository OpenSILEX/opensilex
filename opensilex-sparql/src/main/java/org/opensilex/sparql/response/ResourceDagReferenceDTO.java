package org.opensilex.sparql.response;

import org.opensilex.sparql.model.SPARQLDagModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Similar to {@link ResourceDagDTO<T>}, represents a dag node. However, it stores references to parents
 * and children nodes as named resources ({@link NamedResourceDTO<T>}, instead of plain URIs. This generates more
 * queries when requesting the object, but provides more information.
 *
 * @author Valentin RIGOLLE
 */
public class ResourceDagReferenceDTO<T extends SPARQLDagModel<T>> extends NamedResourceDTO<T> {
    protected List<NamedResourceDTO<T>> parents;

    protected List<NamedResourceDTO<T>> children;

    public List<NamedResourceDTO<T>> getParents() {
        return parents;
    }

    public void setParents(List<NamedResourceDTO<T>> parents) {
        this.parents = parents;
    }

    public List<NamedResourceDTO<T>> getChildren() {
        return children;
    }

    public void setChildren(List<NamedResourceDTO<T>> children) {
        this.children = children;
    }

    @Override
    public void toModel(T model) {
        super.toModel(model);

        if (getParents() != null) {
            model.setParents(getParents().stream()
                    .map(NamedResourceDTO::newModel)
                    .collect(Collectors.toList()));
        }

        if (getChildren() != null) {
            model.setChildren(getParents().stream()
                    .map(NamedResourceDTO::newModel)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public void fromModel(T model) {
        super.fromModel(model);

        List<T> parentModels = model.getParents();
        if (parentModels != null) {
            setParents(parentModels.stream()
                    .map(parentModel -> (NamedResourceDTO<T>) NamedResourceDTO.getDTOFromModel(parentModel))
                    .collect(Collectors.toList()));
        }

        List<T> childrenModels = model.getChildren();
        if (childrenModels != null) {
            setChildren(childrenModels.stream()
                    .map(childModel -> (NamedResourceDTO<T>) NamedResourceDTO.getDTOFromModel(childModel))
                    .collect(Collectors.toList()));
        }
    }
}

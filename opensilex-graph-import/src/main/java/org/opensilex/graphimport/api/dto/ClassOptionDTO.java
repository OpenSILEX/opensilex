//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.interpret.ClassCatalog;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class a sheet may hold, with its subclasses, to choose the class of a sheet from.
 *
 * @author Arnaud Charleroy
 */
public class ClassOptionDTO {

    @JsonProperty("uri")
    private URI uri;

    @JsonProperty("label")
    private String label;

    @JsonProperty("children")
    @ApiModelProperty(value = "Its direct subclasses")
    private List<ClassOptionDTO> children = new ArrayList<>();

    /** The classes of the catalog as trees, one per root, sorted by label. */
    public static List<ClassOptionDTO> treeOf(ClassCatalog catalog, String language) {
        return sorted(catalog.getRoots().stream()
                .map(root -> node(root, catalog, language, new HashSet<>()))
                .toList());
    }

    private static ClassOptionDTO node(URI classUri, ClassCatalog catalog, String language, Set<URI> path) {
        ClassOptionDTO dto = new ClassOptionDTO();
        dto.uri = classUri;
        dto.label = catalog.labelOf(classUri, language);
        path.add(classUri);
        List<ClassOptionDTO> children = new ArrayList<>();
        for (URI child : catalog.childrenOf(classUri)) {
            // A class seen higher on the path would loop forever on a cyclic hierarchy.
            if (!path.contains(child)) {
                children.add(node(child, catalog, language, new HashSet<>(path)));
            }
        }
        dto.children = sorted(children);
        return dto;
    }

    private static List<ClassOptionDTO> sorted(List<ClassOptionDTO> options) {
        List<ClassOptionDTO> copy = new ArrayList<>(options);
        copy.sort(Comparator.comparing(ClassOptionDTO::getLabel, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    public URI getUri() {
        return uri;
    }

    public String getLabel() {
        return label;
    }

    public List<ClassOptionDTO> getChildren() {
        return children;
    }
}

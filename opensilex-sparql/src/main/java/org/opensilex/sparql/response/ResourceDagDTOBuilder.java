/*
 * *****************************************************************************
 *                         ResourceDagDTOBuilder.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 04/09/2024 09:15
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.sparql.response;

import org.opensilex.sparql.model.SPARQLDagModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder design pattern for the ResourceDagDTOs. When querying a list of {@link SPARQLDagModel}, this pattern
 * is more efficient because it makes fewer requests (compared to querying the models one by one).
 *
 * @author Valentin RIGOLLE
 */
public class ResourceDagDTOBuilder<T extends SPARQLDagModel<T>> {
    private List<T> dagModelList;
    private final HashMap<URI, List<URI>> parentUriMap;

    public ResourceDagDTOBuilder(List<T> dagModelList) {
        this.dagModelList = dagModelList;
        this.parentUriMap = new HashMap<>();
    }

    public void setDagModelList(List<T> dagModelList) {
        this.dagModelList = dagModelList;
    }

    public List<ResourceDagDTO<T>> build() {
        List<ResourceDagDTO<T>> dtoList = dagModelList.stream().map(model -> {
            ResourceDagDTO<T> dto = instanciateDto();
            dto.fromModelChildren(model);
            dto.getChildren().forEach(childUri -> {
                if (!parentUriMap.containsKey(childUri)) {
                    parentUriMap.put(childUri, new ArrayList<>());
                }
                parentUriMap.get(childUri).add(dto.getUri());
            });
            return dto;
        }).collect(Collectors.toList());

        dtoList.forEach(dto -> {
            if (parentUriMap.containsKey(dto.getUri())) {
                dto.setParents(parentUriMap.get(dto.getUri()));
            } else {
                dto.setParents(new ArrayList<>());
            }
        });

        return dtoList;
    }

    protected ResourceDagDTO<T> instanciateDto(){
        return new ResourceDagDTO<>();
    }
}

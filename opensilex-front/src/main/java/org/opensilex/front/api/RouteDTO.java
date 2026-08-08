/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.front.config.Route;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 *
 * @author vidalmor
 */
@Schema
public class RouteDTO {

    public static RouteDTO fromModel(Route route) {
        if (route.path().isEmpty() || route.component().isEmpty()) {
            return null;
        }

        RouteDTO routeDTO = new RouteDTO();

        routeDTO.setPath(route.path());
        routeDTO.setComponent(route.component());
        routeDTO.setCredentials(route.credentials());
        routeDTO.setIcon(route.icon());
        routeDTO.setTitle(route.title());
        routeDTO.setDescription(route.description());
        routeDTO.setRdfType(route.rdfType());
        routeDTO.setName(route.name());

        return routeDTO;
    }

    @NotNull
    private String path;

    @NotNull
    private String component;

    private List<String> credentials;

    public String icon;

    public String title;

    public String description;

    public String rdfType;

    private String name;

    @Schema(description = "Route path", example = "/users")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Schema(description = "Route component", example = "opensilex.AccountList")
    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    @Schema(description = "Required credentials list for this route")
    public List<String> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<String> credentials) {
        this.credentials = credentials;
    }

///////////////////////////////////////////////////////////////

    @Schema(description = "Route icon", example = "ik#ik-target")
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Schema(description = "Route title", example = "component.menu.scientificObjects")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Schema(description = "Route description", example = "ScientificObjectList.description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Schema(description = "Route rdf type", example = "vocabulary:ScientificObject")
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    @Schema(description = "Route name", example = "testPage")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.front.config.Route;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class RouteDTO {

    public static RouteDTO fromModel(Route route) {
        //In vue3 with vue router child routes can have a length of 0, so we only need to check for null
        if (route.path() == null || route.component().isEmpty()) {
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
        routeDTO.setChildren(route.children().stream().map(RouteDTO::fromModel).toList());

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

    private List<RouteDTO> children;

    @ApiModelProperty(value = "Route path", example = "/users")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @ApiModelProperty(value = "Route component", example = "opensilex.AccountList")
    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    @ApiModelProperty(value = "Required credentials list for this route")
    public List<String> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<String> credentials) {
        this.credentials = credentials;
    }

///////////////////////////////////////////////////////////////

    @ApiModelProperty(value = "Route icon", example = "ik#ik-target")
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @ApiModelProperty(value = "Route title", example = "component.menu.scientificObjects")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ApiModelProperty(value = "Route description", example = "ScientificObjectList.description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Route rdf type", example = "vocabulary:ScientificObject")
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    @ApiModelProperty(value = "Route name", example = "testPage")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "Nested routes", example = "some child")
    public List<RouteDTO> getChildren() {
        return children;
    }

    public void setChildren(List<RouteDTO> children) {
        this.children = children;
    }
}

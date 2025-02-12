package org.opensilex.core.dataV2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@JsonPropertyOrder({"dimension", "value"})
public class DataValueModel {

    @NotNull
    @JsonProperty("dimension")
    URI dimension;

    @NotNull
    @JsonProperty("value")
    Object value;

    public DataValueModel() {
    }

    public DataValueModel(URI dimension, Object value) {
        this.dimension = dimension;
        this.value = value;
    }

    /**
     * It takes a list of LinkedHashMaps and returns a list of DataValueModels
     *
     * @param listHashMap The list of LinkedHashMap objects that you want to convert to a list of
     *                    DataValueModel objects.
     * @return A list of DataValueModel objects.
     */
    public static List<DataValueModel> fromLinkedHashMap(List<LinkedHashMap> listHashMap) {
        List<DataValueModel> list = new ArrayList<>();
        listHashMap.forEach(linkedHashMap -> {
            list.add(new DataValueModel(
                    URI.create(linkedHashMap.get("dimension").toString()),
                    linkedHashMap.get("value"))
            );
        });
        return list;
    }

    public URI getDimension() {
        return dimension;
    }

    public void setDimension(URI dimension) {
        this.dimension = dimension;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
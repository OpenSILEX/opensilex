package org.opensilex.core.dataV2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * It takes the list of LinkedHashMap objects and returns a list of DataValueModels
     *
     * @param data The list of LinkedHashMap objects that you want to convert to a list of DataValueModel objects.
     * @param target The target URI.
     *
     * @return A list of DataValueModel objects.
     */
    public static List<DataValueModel> fromLinkedHashMap(Object data, URI target) {
        List<DataValueModel> dataValueModelList = new ArrayList<>();

        if (Objects.isNull(data)) {
            return dataValueModelList;
        }

        if (!(data instanceof List<?> inputList)) {
            throw new IllegalArgumentException("Expected a List but got: " + data.getClass().getName());
        }

        for (Object item : inputList) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Item at target " + target.toString() + " is not a Map.");
            }

            Object dimensionObj = map.get("dimension");
            if (Objects.isNull(dimensionObj)) {
                throw new IllegalArgumentException("Missing 'dimension' key at target " + target.toString());
            }

            URI dimension;
            try {
                dimension = URI.create(dimensionObj.toString());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid URI for 'dimension' at target " + target.toString() + ": " + dimensionObj);
            }

            Object value = map.get("value");
            if (Objects.isNull(value)) {
                throw new IllegalArgumentException("Missing 'value' key at target " + target.toString());
            }
            dataValueModelList.add(new DataValueModel(dimension, value));
        }

        return dataValueModelList;
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
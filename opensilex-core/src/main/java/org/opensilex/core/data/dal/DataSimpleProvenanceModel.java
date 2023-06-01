//******************************************************************************
//                          DataProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.provenance.api.ProvenanceAPI;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Provenance model used in DataModel
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri"})
public class DataSimpleProvenanceModel {
    @NotNull
    @ApiModelProperty(value = "provenance uri", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI)
    String uri;


    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSimpleProvenanceModel that = (DataSimpleProvenanceModel) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
}

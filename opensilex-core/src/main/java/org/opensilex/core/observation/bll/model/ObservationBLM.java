/*******************************************************************************
 *                         ObservationBllModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.bll.model;

import org.opensilex.core.observation.dal.model.ObservationModel;
import org.opensilex.core.observation.proto_ext.datatype.ObservationDatatype;
import org.opensilex.core.observation.proto_ext.datatype.ObservationDatatypeRegistry;
import org.opensilex.core.observation.proto_ext.datatype.ObservationValue;

import java.net.URI;

/**
 * @author Valentin Rigolle
 */
public class ObservationBLM<V extends ObservationValue> {
    private URI uri;
    private V value;
    private ObservationDatatype<V> datatype;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public ObservationDatatype<V> getDatatype() {
        return datatype;
    }

    public void setDatatype(ObservationDatatype<V> datatype) {
        this.datatype = datatype;
    }

    public ObservationModel toModel() {
        var model = new ObservationModel();
        model.setUri(uri);
        model.setValue(datatype.toBson(value));
        model.setDatatype(datatype.getUri());
        return model;
    }

    public static ObservationBLM<?> fromModel(ObservationModel model) {
        var blm = new ObservationBLM<>();
        blm.setUri(model.getUri());
        blm.setDatatype(ObservationDatatypeRegistry.get(model.getDatatype()));
        blm.setValue(blm.getDatatype().fromBson(model.getValue()));
        return blm;
    }
}

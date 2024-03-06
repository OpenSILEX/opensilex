package org.opensilex.faidare.builder;

import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.faidare.model.Faidarev1ScaleDTO;

import java.net.URI;
import java.util.Objects;

public class Faidarev1ScaleDTOBuilder {

    public Faidarev1ScaleDTOBuilder() {
    }

    public Faidarev1ScaleDTO fromModel(UnitModel unitModel){
        Faidarev1ScaleDTO dto = new Faidarev1ScaleDTO();

        dto.setScaleDbId(unitModel.getUri().toString())
                .setName(unitModel.getName());

        return dto;
    }

    public Faidarev1ScaleDTO fromModel(UnitModel unitModel, URI dataTypeUri){
        Faidarev1ScaleDTO dto = this.fromModel(unitModel);

        if (Objects.equals(dataTypeUri, URI.create(XSD.decimal.getURI()))
                | Objects.equals(dataTypeUri, URI.create(XSD.integer.getURI()))){
            dto.setDataType("Numerical");
        }
        if (Objects.equals(dataTypeUri, URI.create(XSD.date.getURI()))
                | Objects.equals(dataTypeUri, URI.create(XSD.dateTime.getURI()))){
            dto.setDataType("Date");
        }
        if (Objects.equals(dataTypeUri, URI.create(XSD.xstring.getURI()))){
            dto.setDataType("Text");
        }
        if (Objects.equals(dataTypeUri, URI.create(XSD.xboolean.getURI()))){
            dto.setDataType("Nominal");
        }

        return dto;
    }
}

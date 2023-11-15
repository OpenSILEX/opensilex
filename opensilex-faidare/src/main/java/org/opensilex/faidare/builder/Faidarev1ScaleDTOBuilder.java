package org.opensilex.faidare.builder;

import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.faidare.model.Faidarev1ScaleDTO;

import java.util.Objects;

public class Faidarev1ScaleDTOBuilder {

    public Faidarev1ScaleDTOBuilder() {
    }

    public Faidarev1ScaleDTO fromModel(UnitModel unitModel){
        Faidarev1ScaleDTO dto = new Faidarev1ScaleDTO();

        dto.setScaleDbId(unitModel.getUri().toString())
                .setScaleName(unitModel.getName());

        return dto;
    }

    public Faidarev1ScaleDTO fromModel(UnitModel unitModel, String dataTypeUri){
        Faidarev1ScaleDTO dto = this.fromModel(unitModel);

        if (Objects.equals(dataTypeUri, "xsd:decimal") | Objects.equals(dataTypeUri, "xsd:integer")){
            dto.setDataType("Numerical");
        }
        if (Objects.equals(dataTypeUri, "xsd:date") | Objects.equals(dataTypeUri, "xsd:dateTime")){
            dto.setDataType("Date");
        }
        if (Objects.equals(dataTypeUri, "xsd:string")){
            dto.setDataType("Text");
        }
        if (Objects.equals(dataTypeUri, "xsd:boolean")){
            dto.setDataType("Nominal");
        }

        return dto;
    }
}

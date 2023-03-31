package org.opensilex.core.annotation.api;

import io.swagger.annotations.ApiModelProperty;

public class AnnotationExistsDTO {

    private byte[] targetExists;
    public AnnotationExistsDTO setTargetExists(byte[] targetExists) {
        this.targetExists = targetExists;
        return this;
    }

    @ApiModelProperty(dataType = "BYTE")
    public byte[] getTargetExists() {
        return targetExists;
    }





}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.utils;

import java.time.Instant;

/**
 *
 * @author charlero
 */
public class ParsedDateTimeMongo {

    private Instant instant;

    private String offset;
    
    private Boolean isDateTime;

    public ParsedDateTimeMongo(Instant localDateTime, String offset, Boolean isDateTime) {
        this.instant = localDateTime;
        this.offset = offset;
        this.isDateTime = isDateTime;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant localDateTime) {
        this.instant = localDateTime;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public Boolean getIsDateTime() {
        return isDateTime;
    }

    public void setIsDateTime(Boolean isDateTime) {
        this.isDateTime = isDateTime;
    }

}

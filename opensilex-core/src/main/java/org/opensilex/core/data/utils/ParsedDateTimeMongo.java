/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.utils;

import java.time.LocalDateTime;

/**
 *
 * @author charlero
 */
public class ParsedDateTimeMongo {

    private LocalDateTime localDateTime;

    private String offset;

    public ParsedDateTimeMongo(LocalDateTime localDateTime, String offset) {
        this.localDateTime = localDateTime;
        this.offset = offset;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

}

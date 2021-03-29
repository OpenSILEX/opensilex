/*
 * *******************************************************************************
 *                     PositionModel.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */

package org.opensilex.core.position.dal;

import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * @author Renaud COLIN
 */
public class PositionModel {

    private URI eventUri;
    private OffsetDateTime moveTime;

    private InfrastructureFacilityModel to;
    private InfrastructureFacilityModel from;
    private PositionNoSqlModel positionNoSqlModel;

    public InfrastructureFacilityModel getTo() {
        return to;
    }

    public void setTo(InfrastructureFacilityModel to) {
        this.to = to;
    }

    public InfrastructureFacilityModel getFrom() {
        return from;
    }

    public void setFrom(InfrastructureFacilityModel from) {
        this.from = from;
    }

    public URI getEventUri() {
        return eventUri;
    }

    public void setEventUri(URI eventUri) {
        this.eventUri = eventUri;
    }

    public OffsetDateTime getMoveTime() {
        return moveTime;
    }

    public void setMoveTime(OffsetDateTime moveTime) {
        this.moveTime = moveTime;
    }

    public PositionNoSqlModel getPositionNoSqlModel() {
        return positionNoSqlModel;
    }

    public void setPositionNoSqlModel(PositionNoSqlModel positionNoSqlModel) {
        this.positionNoSqlModel = positionNoSqlModel;
    }
}

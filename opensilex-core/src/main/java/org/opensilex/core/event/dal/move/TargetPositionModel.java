package org.opensilex.core.event.dal.move;

import java.net.URI;

public class TargetPositionModel {

    private URI target;
    private PositionModel position;

    public URI getTarget() {
        return target;
    }

    public void setTarget(URI target) {
        this.target = target;
    }

    public PositionModel getPosition() {
        return position;
    }

    public void setPosition(PositionModel position) {
        this.position = position;
    }
}

package org.opensilex.core.event.dal.move;

import java.net.URI;

public class ConcernedItemPositionModel {

    private URI concernedItem;
    private PositionModel position;

    public URI getConcernedItem() {
        return concernedItem;
    }

    public void setConcernedItem(URI concernedItem) {
        this.concernedItem = concernedItem;
    }

    public PositionModel getPosition() {
        return position;
    }

    public void setPosition(PositionModel position) {
        this.position = position;
    }
}

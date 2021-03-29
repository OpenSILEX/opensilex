package org.opensilex.core.position.dal;

import java.net.URI;

public class ConcernedItemPositionModel {

    private URI concernedItem;
    private PositionNoSqlModel position;

    public URI getConcernedItem() {
        return concernedItem;
    }

    public void setConcernedItem(URI concernedItem) {
        this.concernedItem = concernedItem;
    }

    public PositionNoSqlModel getPosition() {
        return position;
    }

    public void setPosition(PositionNoSqlModel position) {
        this.position = position;
    }
}

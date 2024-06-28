package org.opensilex.core.event.dal.move;

import org.opensilex.core.event.dal.EventSearchFilter;

import java.time.OffsetDateTime;

public class MoveSearchFilter extends EventSearchFilter {

    private OffsetDateTime afterEnd;

    /**
     * Used to know if we need to call MoveDao's appendTimeAfterFilter instead of Event dao's appendTimeFilter
     * @see MoveEventDAO#search(EventSearchFilter)
     */
    public OffsetDateTime getAfterEnd() {
        return afterEnd;
    }

    /**
     * Used to know if we need to call MoveDao's appendTimeAfterFilter instead of Event dao's appendTimeFilter
     * @see MoveEventDAO#search(EventSearchFilter)
     */
    public MoveSearchFilter setAfterEnd(OffsetDateTime afterEnd) {
        this.afterEnd = afterEnd;
        return this;
    }
}

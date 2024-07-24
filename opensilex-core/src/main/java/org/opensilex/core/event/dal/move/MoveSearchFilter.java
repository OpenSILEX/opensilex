package org.opensilex.core.event.dal.move;

import org.opensilex.core.event.dal.EventSearchFilter;

import java.time.OffsetDateTime;

public class MoveSearchFilter extends EventSearchFilter {

    private OffsetDateTime afterEnd;

    //Separate single target filter used only for move search, didn't use EventSearchFilter's targets field to make the move search function more readable
    /*private String target;*/

    /**
     * Used to know if we need to call MoveDao's appendTimeAfterFilter instead of Event dao's appendTimeFilter
     * @see MoveEventDAO#search(MoveSearchFilter)
     */
    public OffsetDateTime getAfterEnd() {
        return afterEnd;
    }

    /**
     * Used to know if we need to call MoveDao's appendTimeAfterFilter instead of Event dao's appendTimeFilter
     * @see MoveEventDAO#search(MoveSearchFilter)
     */
    public MoveSearchFilter setAfterEnd(OffsetDateTime afterEnd) {
        this.afterEnd = afterEnd;
        return this;
    }

    /*public String getTarget() {
        return target;
    }

    public MoveSearchFilter setTarget(String target) {
        this.target = target;
        return this;
    }*/
}

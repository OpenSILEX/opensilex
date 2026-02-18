package org.opensilex.migration.one_point_five_ALL;

import org.opensilex.OpenSilex;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;

import java.time.OffsetDateTime;

public class MigrateToOnePointFive implements OpenSilexModuleUpdate {

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        StringBuilder entireDescriptionBuilder = new StringBuilder();
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {

    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {

    }
}

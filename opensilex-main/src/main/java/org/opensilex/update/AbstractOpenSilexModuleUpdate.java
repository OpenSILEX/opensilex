package org.opensilex.update;

import org.opensilex.OpenSilex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public abstract class AbstractOpenSilexModuleUpdate implements OpenSilexModuleUpdate{

    protected final Logger logger;
    protected OpenSilex opensilex;

    protected AbstractOpenSilexModuleUpdate(){
        logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
       this.opensilex = opensilex;
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

}

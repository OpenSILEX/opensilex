//******************************************************************************
//                        OpenSilexModuleUpdate.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.update;

import org.opensilex.OpenSilex;

import java.time.OffsetDateTime;

/**
 * Interface for modules update classes.
 *
 * @author Vincent Migot
 */
public interface OpenSilexModuleUpdate {

    /**
     * Date of update creation for auto execution.
     *
     * @return Date of update creation
     */
    OffsetDateTime getDate();

    /**
     * Description of the update.
     *
     * @return Description
     */
    String getDescription();

    /**
     * Update logic to implement.
     * @throws org.opensilex.update.OpensilexModuleUpdateException
     */
     void execute() throws OpensilexModuleUpdateException;

    /**
     * @param opensilex the Opensilex instance
     */
     void setOpensilex(OpenSilex opensilex);


}

/*
 * *****************************************************************************
 *                         SpiderMutagenBuisinessLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 02/07/2026 14:42
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan.spidermutagen.bll;

import org.opensilex.yvan.spidermutagen.dal.SpiderMutagenDAO;
import org.opensilex.yvan.spidermutagen.dal.SpiderMutagenModel;

import javax.inject.Inject;
import java.util.Collection;

public class SpiderMutagenLogic {
    @Inject
    private SpiderMutagenDAO dao;

    public SpiderMutagenModel createSpiderMutagen(SpiderMutagenModel model){
        return null;
    }

    public SpiderMutagenModel updateSpiderMutagen(SpiderMutagenModel model){
        return null;
    }

    public void deleteSpiderMutagen(SpiderMutagenModel model){
        return;
    }

    public Collection<SpiderMutagenModel> searchSpiderMutagen(){
        return null;
    }
}

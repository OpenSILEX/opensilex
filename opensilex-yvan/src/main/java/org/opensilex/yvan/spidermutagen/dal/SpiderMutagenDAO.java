/*
 * *****************************************************************************
 *                         SpiderMutagenDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 02/07/2026 14:38
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan.spidermutagen.dal;

import org.jvnet.hk2.annotations.Service;
import org.opensilex.service.reflection.SelfBound;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;

@Service
@SelfBound
public class SpiderMutagenDAO {
    public static final String GRAPH = "spidermutagen";
    private final SPARQLService sparql;

    @Inject
    public SpiderMutagenDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
}

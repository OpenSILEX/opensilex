//******************************************************************************
//                                       ResultStudyDetails.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.StudyDetails;

/**
 *
 * @author boizetal
 */
public class ResultStudyDetails extends Resultat<StudyDetails> {
     /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param study to detail
     */
    public ResultStudyDetails(ArrayList<StudyDetails> study) {
        super(study);
    }
    
    /**
     * Contructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param study to detail
     * @param pagination Objet pagination permettant de trier la liste experimentList
     * @param paginate 
     */
    public ResultStudyDetails(ArrayList<StudyDetails> study, Pagination pagination, 
            boolean paginate) {
        super(study, pagination, paginate);
    }
}

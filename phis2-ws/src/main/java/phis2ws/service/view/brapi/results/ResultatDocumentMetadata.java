//**********************************************************************************************
//                                       ResultatDocumentMetadata.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: June 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  June, 2017
// Subject: extended from Resultat adapted to the documents metadata list
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Document;

public class ResultatDocumentMetadata extends Resultat<Document>{
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param documentsList liste des documents contenant un seul élément
     */
    public ResultatDocumentMetadata(ArrayList<Document> documentsList) {
        super(documentsList);
    }
    
    /**
     * Contructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param documentsList liste des documents
     * @param pagination Objet pagination permettant de trier la liste documentsList
     * @param paginate 
     */
    public ResultatDocumentMetadata(ArrayList<Document> documentsList, Pagination pagination, 
            boolean paginate) {
        super(documentsList, pagination, paginate);
    }
}

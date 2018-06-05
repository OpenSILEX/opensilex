//**********************************************************************************************
//                                       ResultForm.java 
//
// Author(s): Samuël Chérimont, Arnaud Charleroy 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: december 2015
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: A class which represente response form 
//***********************************************************************************************
package phis2ws.service.view.manager;

import java.util.List;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.Status;

/**
 * ResultForm - Classe abstraite qui sert de base aux ResultForm plus
 * spécifiques
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @param <T> classe de spécification de la classe héritée
 * @see Resultat
 * @date 03/12/2015
 */
public abstract class ResultForm<T> {

    protected Metadata metadata;
    protected Resultat result;

    /**
     * resultSize() - Récupère le nombre d'éléments de la liste data qui est
 un champ de result
     *
     * @return le nombre d'éléments de la liste
     * @see Resultat
     * @date 04/12/2015
     */
    public int resultSize() {
        return result.dataSize();
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Resultat getResult() {
        return result;
    }
    
    public void setStatus(List<Status> status){
        metadata.setStatus(status);
    }
}

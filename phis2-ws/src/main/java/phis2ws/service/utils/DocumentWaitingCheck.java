//**********************************************************************************************
//                                       DocumentWaitingCheck.java 
//
// Author(s): Arnaud CHARLEROY, Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: may 2016
// Contact:arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  March, 2017
// Subject: A class which permit to start a thread for documents sending
//***********************************************************************************************
package phis2ws.service.utils;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.resources.DocumentResourceService;

public class DocumentWaitingCheck implements Callable<Boolean> {
    final static Logger logger = LoggerFactory.getLogger(DocumentWaitingCheck.class);
    final static String PROPS_FILE_NAME = "service";
    final static int DEFAUT_WAITING_FILE_TIME = 30;
    final private String annotationsUri;

    public DocumentWaitingCheck(String annotationsUri) {
        this.annotationsUri = annotationsUri;
    }
    
    @Override
    public Boolean call() {
//        logger.debug("start");
        // Temps d'attente
        int waitingFileTime = DEFAUT_WAITING_FILE_TIME;
        try{
            waitingFileTime = Integer.valueOf(PropertiesFileManager.getConfigFileProperty(PROPS_FILE_NAME, "waitingFileTime"));
        }catch(Exception e){
            logger.info("Can't parse waitingFileTime properties in " + PROPS_FILE_NAME + " properties file. Default value is "+ DEFAUT_WAITING_FILE_TIME +" seconds.", e);
        }

        try {
            Thread.sleep(waitingFileTime * 1000);
        } catch (InterruptedException ex) {
             logger.error(ex.getMessage(), ex);
        }
        // Si l'uri n'est pas présente dans la Map ou si sa valeur est à false 
//        (aucun envoi de fichier en cours), on la supprime.
//         Sinon on ne fait rien
        
  
        boolean find = DocumentResourceService.waitingAnnotFileCheck.containsKey(annotationsUri);
        if(find){
            final Boolean running = DocumentResourceService.waitingAnnotFileCheck.get(annotationsUri);
            if(!running){
                // Suppression tableau d'attente
                DocumentResourceService.waitingAnnotFileCheck.remove(annotationsUri);
                //Suppression de l'information
                DocumentResourceService.waitingAnnotInformation.remove(annotationsUri);
//                logger.debug("Remove : " + annotationsUri);
            } 
//            logger.debug("sending : " + annotationsUri);
        }
//        logger.debug("thread Stopped : " + annotationsUri);
        return null;
    }
}

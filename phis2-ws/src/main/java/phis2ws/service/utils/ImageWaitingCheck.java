//**********************************************************************************************
//                                       ImageWaitingCheck.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Decembler, 11 2017
// Subject: A class which allows to start a thread for image sending
//***********************************************************************************************
package phis2ws.service.utils;

//SILEX:todo

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.resources.ImageResourceService;

//Factoriser cette classe avec la classe DocumentWaitingCheck
//\SILEX:todo
public class ImageWaitingCheck implements Callable<Boolean> {
    final static Logger logger = LoggerFactory.getLogger(ImageWaitingCheck.class);
    final static String PROPS_FILE_NAME = "service";
    final static int DEFAUT_WAITING_FILE_TIME = 30;
    final private String annotationsUri;

    public ImageWaitingCheck(String annotationsUri) {
        this.annotationsUri = annotationsUri;
    }
    
    @Override
    public Boolean call() {
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
        boolean find = ImageResourceService.waitingMetadataFileCheck.containsKey(annotationsUri);
        if(find){
            final Boolean running = ImageResourceService.waitingMetadataFileCheck.get(annotationsUri);
            if(!running){
                // Suppression tableau d'attente
                ImageResourceService.waitingMetadataFileCheck.remove(annotationsUri);
                //Suppression de l'information
                ImageResourceService.waitingMetadataInformation.remove(annotationsUri);
            } 
        }
        return null;
    }
}

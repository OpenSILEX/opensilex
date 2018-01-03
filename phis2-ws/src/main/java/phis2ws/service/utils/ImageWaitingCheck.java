//**********************************************************************************************
//                                       ImageWaitingCheck.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 03 2018
// Subject: A class which allows to start a thread for image sending
//***********************************************************************************************
package phis2ws.service.utils;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.resources.ImageResourceService;

/**
 * allows to start a thread for image sending
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ImageWaitingCheck implements Callable<Boolean> {
    
    //SILEX:todo
    //Factorization with DocumentWaitingCheck
    //\SILEX:todo
    
    final static Logger LOGGER = LoggerFactory.getLogger(ImageWaitingCheck.class);
    final static String PROPS_FILE_NAME = "service";
    final static int DEFAUT_WAITING_FILE_TIME = 30;
    final private String annotationsUri;

    public ImageWaitingCheck(String annotationsUri) {
        this.annotationsUri = annotationsUri;
    }
    
    @Override
    public Boolean call() {
        try{
            int waitingFileTime = Integer.valueOf(PropertiesFileManager.getConfigFileProperty(PROPS_FILE_NAME, "waitingFileTime"));
            Thread.sleep(waitingFileTime * 1000);
        } catch (InterruptedException ex) {
             LOGGER.error(ex.getMessage(), ex);
        } catch (NumberFormatException e) {
            LOGGER.info("Can't parse waitingFileTime properties in " + PROPS_FILE_NAME + " properties file. Default value is "+ DEFAUT_WAITING_FILE_TIME +" seconds.", e);
        }
        //If the image uri is unknown or if it's value is false (no file sending)
        //it is deleted
        //else do nothing
        boolean find = ImageResourceService.WAITING_METADATA_FILE_CHECK.containsKey(annotationsUri);
        if (find) {
            final Boolean running = ImageResourceService.WAITING_METADATA_FILE_CHECK.get(annotationsUri);
            if(!running){
                // Delete the waiting array
                ImageResourceService.WAITING_METADATA_FILE_CHECK.remove(annotationsUri);
                //Delete the information
                ImageResourceService.WAITING_METADATA_INFORMATION.remove(annotationsUri);
            } 
        }
        return null;
    }
}

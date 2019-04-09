//******************************************************************************
//                            ImageWaitingCheck.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 11 December 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.PropertiesFileManager;
import opensilex.service.resource.ImageResourceService;

/**
 * Allows to start a thread for image sending. 
 * When an image is sent to the web service, it is sent in two stages: 
 * 1. the image metadata is sent
 * 2. the file is sent
 * This is why we need this class (to wait for the image file).
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ImageWaitingCheck implements Callable<Boolean> {
    
    //SILEX:todo
    //Factorization with DocumentWaitingCheck
    //\SILEX:todo
    
    final static Logger LOGGER = LoggerFactory.getLogger(ImageWaitingCheck.class);
    final static String PROPS_FILE_NAME = "service";
    
    //Waiting file time default
    final static int DEFAUT_WAITING_FILE_TIME = 30;
    
    //The uris of the image metadata
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
                // Delete the information
                ImageResourceService.WAITING_METADATA_INFORMATION.remove(annotationsUri);
            } 
        }
        return null;
    }
}

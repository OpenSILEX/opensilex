//******************************************************************************
//                              DocumentWaitingCheck.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.PropertiesFileManager;
import opensilex.service.resource.DocumentResourceService;

/**
 * Permits to start a thread for document sending.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class DocumentWaitingCheck implements Callable<Boolean> {
    final static Logger LOGGER = LoggerFactory.getLogger(DocumentWaitingCheck.class);
    final static String PROPS_FILE_NAME = "service";
    final static int DEFAUT_WAITING_FILE_TIME = 30;
    final private String annotationsUri;

    public DocumentWaitingCheck(String annotationsUri) {
        this.annotationsUri = annotationsUri;
    }
    
    @Override
    public Boolean call() {
        
        // Waiting time
        int waitingFileTime = DEFAUT_WAITING_FILE_TIME;
        try{
            waitingFileTime = Integer.valueOf(PropertiesFileManager.getConfigFileProperty(PROPS_FILE_NAME, "waitingFileTime"));
        }catch(Exception e){
            LOGGER.info("Can't parse waitingFileTime properties in " + PROPS_FILE_NAME + " properties file. Default value is "+ DEFAUT_WAITING_FILE_TIME +" seconds.", e);
        }

        try {
            Thread.sleep(waitingFileTime * 1000);
        } catch (InterruptedException ex) {
             LOGGER.error(ex.getMessage(), ex);
        }
        
        /*
        If the URI isn't in the map or its value is false (no running file 
        sending), it is deleted. Otherwise, nothing is done.
        */  
        boolean find = DocumentResourceService.WAITING_ANNOT_FILE_CHECK.containsKey(annotationsUri);
        if(find){
            final Boolean running = DocumentResourceService.WAITING_ANNOT_FILE_CHECK.get(annotationsUri);
            if(!running){
                // Delete waiting list
                DocumentResourceService.WAITING_ANNOT_FILE_CHECK.remove(annotationsUri);
                // Delete information
                DocumentResourceService.WAITING_ANNOT_INFORMATION.remove(annotationsUri);
            } 
        }
        return null;
    }
}

//******************************************************************************
//                                       ResponseFormSensor.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  14 mars 2018
// Subject: Allows the formating of the result of the request about Sensor
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultSensor;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Sensor;

/**
 * Allows the formating of the result of the request about Sensor
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormSensor extends ResultForm<Sensor> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormSensor(int pageSize, int currentPage, ArrayList<Sensor> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultSensor(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultSensor(list);
        }
    }
    
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     * @param totalCount number of result
     */
    public ResponseFormSensor(int pageSize, int currentPage, ArrayList<Sensor> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultSensor(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultSensor(list);
        }
    }
}

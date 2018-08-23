//**********************************************************************************************
//                                       ResponseFormDocumentType.java 
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: March 2016
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  March, 2017
// Subject: extends ResultForm. Adapted to the labelViews list informations used in the documents
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.results.ResultDocumentType;
import phis2ws.service.view.manager.ResultForm;

public class ResponseFormDocumentType extends ResultForm<String> {
    public ResponseFormDocumentType(int pageSize, int currentPage, ArrayList<String> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultDocumentType(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultDocumentType(list);
        }
        
    }
    
    public ResponseFormDocumentType(int pageSize, int currentPage, ArrayList<String> list, boolean paginate, Integer totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultDocumentType(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultDocumentType(list);
        }
    }

    public ResponseFormDocumentType(int pageSize, int currentPage, ArrayList<String> list, boolean paginate, Status status) {
        metadata = new Metadata(pageSize, currentPage, list.size(),status);
        if (list.size() > 1) {
            result = new ResultDocumentType(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultDocumentType(list);
        }
    }
    
    public ResponseFormDocumentType(int pageSize, int currentPage, ArrayList<String> list, boolean paginate, ArrayList<Status> status) {
        metadata = new Metadata(pageSize, currentPage, list.size(),status);
        if (list.size() > 1) {
            result = new ResultDocumentType(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultDocumentType(list);
        }
    }

    public ResponseFormDocumentType(int pageSize, int currentPage, ArrayList<String> list, boolean paginate, Integer totalCount, ArrayList<Status> status) {
        metadata = new Metadata(pageSize, currentPage, totalCount,status);
        if (list.size() > 1) {
            result = new ResultDocumentType(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultDocumentType(list);
        }
    }
}

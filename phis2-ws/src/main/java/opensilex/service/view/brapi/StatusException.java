/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.view.brapi;

/**
 *
 * @author charlero
 */
public class StatusException {

    public String type;
    public String href;
    public String details;

    public StatusException(String type, String details) {
        this.type = type;
        this.details = details;
    }

    public StatusException(String type, String href, String details) {
        this.type = type;
        this.href = href;
        this.details = details;
    }
}

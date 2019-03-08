//******************************************************************************
//                             AnnotationDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 14 June 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.annotation;

import org.joda.time.format.DateTimeFormat;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.view.model.phis.Annotation;

/**
 * DTO representing an annotation
 * @update [Andréas Garcia] Make AnnotationDTO only concerns GET calls of annotations
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AnnotationDTO extends AnnotationPostDTO {

    /**
     * URI
     * @example http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2
     */
    @URL
    private String uri;

    /** 
     * Creation date string format yyyy-MM-ddTHH:mm:ssZ
     * @example 2018-06-25T15:13:59+0200
     */
    private String creationDate;
    
    /**
     * Constructor to create a DTO from an annotation model
     * @param annotation 
     */
    public AnnotationDTO(Annotation annotation) {
        super(annotation);
        this.uri = annotation.getUri();
        this.creationDate = DateTimeFormat
                .forPattern(DateFormat.YMDTHMSZZ.toString())
                .print(annotation.getCreated());
    }

    @Override
    public Annotation createObjectFromDTO() {
        Annotation annotation = super.createObjectFromDTO();
        annotation.setCreated(Dates.stringToDateTimeWithGivenPattern(this.creationDate, DateFormat.YMDTHMSZZ.toString()));
        annotation.setUri(uri);
        return annotation;
    }

    public String getUri() {
        return uri;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String created) {
        this.creationDate = created;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}

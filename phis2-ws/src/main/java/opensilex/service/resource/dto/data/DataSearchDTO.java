//******************************************************************************
//                                DataSearchDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 21 mai 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.data;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import opensilex.service.configuration.DateFormat;
import opensilex.service.model.Data;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDTO;
import org.bson.types.Decimal128;

/**
 * Data search DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DataSearchDTO extends AbstractVerifiedClass {
    //URI of the data
    //@example http://www.opensilex.org/demo/id/data/kwmj24y77kzggorkhjldkpf4xalthhc4exmwcpbhjs6abwubudtqf4685ba6819741ce8f5abf007f6a78aa
    protected String uri;
    //Provenance of the data
    protected RdfResourceDTO provenance;
    //Object concerned by the data
    protected ObjectResourceDTO object;
    //Variable of the data
    protected RdfResourceDTO variable;
    //Date of the data. The format should be yyyy-MM-ddTHH:mm:ssZ
    //@example 2017-05-21 23:51:00.000Z
    protected String date;
    //Value of the data
    //@example 1.3
    protected Object value;

    public DataSearchDTO(Data data, String provenanceLabel, List<String> objectLabels, String variableLabel) {
        uri = data.getUri();
        provenance = new RdfResourceDTO(data.getProvenanceUri(), provenanceLabel);
        if (data.getObjectUri() != null) {
            object = new ObjectResourceDTO(data.getObjectUri(), objectLabels);
        }
        variable = new RdfResourceDTO(data.getVariableUri(), variableLabel);
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        if (data.getDate() != null) {
            date = df.format(data.getDate());
        }
        Object dataValue = data.getValue();
        if (dataValue != null && dataValue instanceof Date) {
            value = df.format(dataValue);
        } else if (dataValue != null && dataValue instanceof Decimal128) {
            value = new BigDecimal(dataValue.toString());
        } else {
            value = dataValue;
        }
    }
    
    @Override
    public Object createObjectFromDTO() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

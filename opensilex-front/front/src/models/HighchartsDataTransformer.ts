import {DataGetDTO} from "opensilex-core/model/dataGetDTO";
import Highcharts from "highcharts";

/**
 * @author Valentin RIGOLLE
 */
export interface HighchartsDataTransformerOptions {
    deviceUri?: string,
    scientificObjectUri?: string
}

/**
 * @author Valentin RIGOLLE
 */
export interface OpenSilexPointOptionsObject extends Highcharts.PointOptionsObject {
    provenanceUri: string,
    data: DataGetDTO,
    dataUri: string,
    dateWithOffset: string,
    deviceUri?: string,
    objectUri?: string
}

/**
 * Transform data points into Highcharts-compatible data points. An x value is provided using the timestamp of the
 * date, and the y value is the value of the data. A 'data' property is passed to keep a reference to the object.
 *
 * Optional parameters allow to pass a device and/or a scientific object URI along with the other information. Available
 * options are :
 *
 * - deviceUri
 * - scientificObjectUri
 *
 * @param data
 * @param options an optional HighchartsDataTransformerOptions object
 */
function transformDataForHighcharts(data: Array<DataGetDTO>, options?: HighchartsDataTransformerOptions): Array<OpenSilexPointOptionsObject> {
    return data.map(element => {
        let date = new Date(element.date);
        let timestamp = date.getTime();
        return {
            x: timestamp,
            y: element.value,
            provenanceUri: element.provenance.uri,
            data: element,
            dataUri: element.uri,
            dateWithOffset: date.toISOString(),
            deviceUri: options?.deviceUri,
            objectUri: options?.scientificObjectUri
        } as OpenSilexPointOptionsObject;
    });
}

const HighchartsDataTransformer = {
    transformDataForHighcharts
};

export default HighchartsDataTransformer;
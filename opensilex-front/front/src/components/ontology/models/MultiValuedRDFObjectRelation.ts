import {VueRDFTypePropertyDTO} from "opensilex-core/model/vueRDFTypePropertyDTO";

export interface MultiValuedRDFObjectRelation{
    property: VueRDFTypePropertyDTO,
    value: string | string[];
}
import {VueRDFTypePropertyDTO} from "../../../lib";

export interface MultiValuedRDFObjectRelation{
    property: VueRDFTypePropertyDTO,
    value: string | string[];
}
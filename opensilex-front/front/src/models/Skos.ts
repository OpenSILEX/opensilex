
export class Skos {

    // TODO: get relations from web service
    // labels
    static narrower: string = "component.skos.narrower";
    static broader: string = "component.skos.broader";
    static closeMatch: string = "component.skos.closeMatch";
    static exactMatch: string = "component.skos.exactMatch";

    static getSkosObject(): any {
        return {
            exactMatch: [],
            closeMatch: [],
            broader: [],
            narrower: []
        }
    }

    static getSkosRelationsMap(): Map<string, string> {
        return new Map(Object.entries(Skos));
    }

    static getSkosRelationsEntries(): any[] {
        return Object.entries(Skos);
    }
}

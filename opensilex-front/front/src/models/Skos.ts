
export class Skos {

    // TODO: get relations from web service
    // labels
    static narrow_match: string = "component.skos.narrowMatch";
    static broad_match: string = "component.skos.broadMatch";
    static close_match: string = "component.skos.closeMatch";
    static exact_match: string = "component.skos.exactMatch";

    static getSkosObject(): any {
        return {
            exact_match: [],
            close_match: [],
            broad_match: [],
            narrow_match: []
        }
    }

    static getSkosRelationsMap(): Map<string, string> {
        return new Map(Object.entries(Skos));
    }

    static getSkosRelationsEntries(): any[] {
        return Object.entries(Skos);
    }
}

import {injectable} from "inversify";
import IGeocodingService, {GeocodingAddressResult, GeocodingOptions} from "./IGeocodingService";
import {FeatureCollection} from "opensilex-core/model/featureCollection";

const SCHEME = "https"
const BASE_URI = "api-adresse.data.gouv.fr";
const API_ENDPOINT = "search"
const QUERY_PARAM = "q";
const LIMIT_PARAM = "limit";
const AUTOCOMPLETE_PARAM = "autocomplete"

const COUNTRY = "France";

const DEFAULT_OPTIONS: GeocodingOptions = {
    limit: 5
};

/**
 * An implementation of the GeocodingService using the data.gouv.fr external API. See {@link https://adresse.data.gouv.fr/api-doc/adresse}.
 */
@injectable()
class DataGouvGeocodingService implements IGeocodingService {
    constructor() {

    }

    async search(partialAddress: string, options?: GeocodingOptions): Promise<Array<GeocodingAddressResult>> {
        if (!options) {
            options = DEFAULT_OPTIONS;
        } else {
            options = {
                ...DEFAULT_OPTIONS,
                ...options
            };
        }
        let url = this.buildQueryUrl(partialAddress, options.limit);

        let response = await fetch(url.toString(), {
            method: "GET"
        });

        if (!response.ok) {
            return Promise.reject(response);
        }

        let featureCollection: FeatureCollection = await response.json();

        let addresses: Array<GeocodingAddressResult> = featureCollection.features.map(feature => {
            let address: GeocodingAddressResult = {
                country: COUNTRY,
                city: feature.properties.city,
                state: feature.properties.context,
                postcode: feature.properties.postcode,
                street: feature.properties.street,
                houseNumber: feature.properties.housenumber,
                name: feature.properties.name,
                readableAddress: feature.properties.label
            };
            return address;
        });

        return addresses;
    }

    private buildQueryUrl(partialAddress: string, limit: number): URL {
        let url = new URL(`${SCHEME}://${BASE_URI}/${API_ENDPOINT}`);
        url.searchParams.append(QUERY_PARAM, partialAddress);
        url.searchParams.append(AUTOCOMPLETE_PARAM, "1");

        if (limit > 0) {
            url.searchParams.append(LIMIT_PARAM, String(limit));
        }

        return url;
    }
}

export default DataGouvGeocodingService;

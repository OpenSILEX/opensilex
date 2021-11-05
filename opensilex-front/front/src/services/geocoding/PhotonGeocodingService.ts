import {injectable} from "inversify";
import IGeocodingService, {GeocodingAddressResult, GeocodingOptions} from "./IGeocodingService";
import {FeatureCollection} from "opensilex-core/model/featureCollection";

const SCHEME = "https"
const BASE_URI = "photon.komoot.io";
const API_ENDPOINT = "api"
const QUERY_PARAM = "q";
const LANG_PARAM = "lang";
const LIMIT_PARAM = "limit";

const AVAILABLE_LANGUAGES = [
    "en", "de", "fr"
];

const DEFAULT_OPTIONS: GeocodingOptions = {
    lang: "en",
    limit: 5
};

/**
 * An implementation of the GeocodingService using the Photon external API. See {@link https://photon.komoot.io/}.
 */
@injectable()
class PhotonGeocodingService implements IGeocodingService {
    constructor() {

    }

    async search(partialAddress: string, options?: GeocodingOptions): Promise<any> {
        if (!options) {
            options = DEFAULT_OPTIONS;
        } else {
            options = {
                ...DEFAULT_OPTIONS,
                ...options
            };
        }
        let url = this.buildQueryUrl(partialAddress, options.lang, options.limit);

        let response = await fetch(url.toString(), {
            method: "GET"
        });

        if (!response.ok) {
            return Promise.reject(response);
        }

        let featureCollection: FeatureCollection = await response.json();

        let addresses: Array<GeocodingAddressResult> = featureCollection.features.sort((a, b) => {
            if (!!a.properties.street && !b.properties.street) {
                return -1;
            } else if (!a.properties.street && !!b.properties.street) {
                return 1;
            }
            return 0;
        }).map(feature => {
            let address: GeocodingAddressResult = {
                country: feature.properties.country,
                state: feature.properties.state,
                county: feature.properties.county,
                city: feature.properties.city,
                postcode: feature.properties.postcode,
                street: feature.properties.street,
                houseNumber: feature.properties.housenumber,
                name: feature.properties.name
            };
            address.readableAddress = [
                address.name,
                address.houseNumber,
                address.street,
                address.postcode,
                address.city,
                address.county,
                address.state,
                address.country
            ].filter(s => !!s && s.length > 0).join(", ");
            return address;
        });

        return addresses;
    }

    private buildQueryUrl(partialAddress: string, lang: string, limit: number): URL {
        let url = new URL(`${SCHEME}://${BASE_URI}/${API_ENDPOINT}`);
        url.searchParams.append(QUERY_PARAM, partialAddress);

        let lowerCaseLang = lang.toLowerCase();
        if (lang && AVAILABLE_LANGUAGES.findIndex(l => l === lowerCaseLang) >= 0) {
            url.searchParams.append(LANG_PARAM, lowerCaseLang);
        }
        if (limit > 0) {
            url.searchParams.append(LIMIT_PARAM, String(limit));
        }

        return url;
    }
}

export default PhotonGeocodingService;

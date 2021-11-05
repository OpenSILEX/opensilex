/**
 * An address returned by the search request. All the fields are optional.
 */
export interface GeocodingAddressResult {
    country?: string,
    state?: string,
    county?: string,
    city?: string,
    postcode?: string,
    street?: string,
    houseNumber?: string,
    name?: string,

    readableAddress?: string
}

/**
 * Options for the geocoding search API.
 */
export interface GeocodingOptions {
    lang?: string,
    limit?: number
}

/**
 * This service calls an external geocoding API to search for a partial address and return close matches. Its intended
 * use is for autocompletion of partial addresses in an address selector.
 */
interface IGeocodingService {
    /**
     * Searches for matching addresses given a partial address.
     *
     * @param partialAddress
     * @param options
     */
    search(partialAddress: string, options?: GeocodingOptions): Promise<Array<GeocodingAddressResult>>;
}

export default IGeocodingService;
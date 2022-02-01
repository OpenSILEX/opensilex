import {Container, interfaces} from "inversify";
import IGeocodingService from "./geocoding/IGeocodingService";
import AdresseDataGouvGeocodingService from "./geocoding/AdresseDataGouvGeocodingService";
import PhotonGeocodingService from "./geocoding/PhotonGeocodingService";
import OpenSilexVuePlugin from "../models/OpenSilexVuePlugin";
import Context = interfaces.Context;

const PHOTON_SERVICE = "Photon";
const ADRESSE_DATA_GOUV_SERVICE = "Adresse";

const geocodingServices = new Map<string, new (...args: any[]) => IGeocodingService>([
    [PHOTON_SERVICE, PhotonGeocodingService],
    [ADRESSE_DATA_GOUV_SERVICE, AdresseDataGouvGeocodingService]
]);

const DEFAULT_GEOCODING_SERVICE = PHOTON_SERVICE;

export class ServiceBinder {
    public static with(container: Container) {
        container.bind<IGeocodingService>("IGeocodingService").toDynamicValue((context: Context) => {
            const $opensilex: OpenSilexVuePlugin = context.container.get(OpenSilexVuePlugin);

            let geocodingServiceKey = $opensilex.getConfig().geocodingService;
            if (typeof geocodingServiceKey !== "string" || !geocodingServices.has(geocodingServiceKey)) {
                geocodingServiceKey = DEFAULT_GEOCODING_SERVICE;
            }

            console.debug("Using geocoding service : " + geocodingServiceKey);

            let geocodingServiceConstructor = geocodingServices.get(geocodingServiceKey);

            return new geocodingServiceConstructor();
        }).inSingletonScope();
    }
}
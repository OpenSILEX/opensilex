import { Container } from "inversify";
import IGeocodingService from "./geocoding/IGeocodingService";
import AdresseDataGouvGeocodingService from "./geocoding/AdresseDataGouvGeocodingService";
import PhotonGeocodingService from "./geocoding/PhotonGeocodingService";
import OpenSilexVuePlugin from "../models/OpenSilexVuePlugin";

const PHOTON_SERVICE = "Photon";
const ADRESSE_DATA_GOUV_SERVICE = "Adresse";

const geocodingServices = new Map<string, new (...args: any[]) => IGeocodingService>([
  [PHOTON_SERVICE, PhotonGeocodingService],
  [ADRESSE_DATA_GOUV_SERVICE, AdresseDataGouvGeocodingService]
]);

const DEFAULT_GEOCODING_SERVICE = PHOTON_SERVICE;

export class ServiceBinder {
  public static with(container: Container) {
    container
      .bind<IGeocodingService>("IGeocodingService")
      .toDynamicValue(() => {
        const $opensilex = container.get(OpenSilexVuePlugin);

        let geocodingServiceKey = $opensilex.getConfig().geocodingService;
        if (typeof geocodingServiceKey !== "string" || !geocodingServices.has(geocodingServiceKey)) {
          geocodingServiceKey = DEFAULT_GEOCODING_SERVICE;
        }

        console.debug("Using geocoding service : " + geocodingServiceKey);

        const Ctor = geocodingServices.get(geocodingServiceKey) ?? PhotonGeocodingService;
        return new Ctor();
      })
      .inSingletonScope();
  }
}

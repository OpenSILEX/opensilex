import {Container} from "inversify";
import IGeocodingService from "./geocoding/IGeocodingService";
import PhotonGeocodingService from "./geocoding/PhotonGeocodingService";

export class ServiceBinder {
    public static with(container: Container) {
        container.bind<IGeocodingService>("IGeocodingService").to(PhotonGeocodingService).inSingletonScope();
    }
}
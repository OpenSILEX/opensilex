import {Container} from "inversify";
import IGeocodingService from "./geocoding/IGeocodingService";
import DataGouvGeocodingService from "./geocoding/DataGouvGeocodingService";

export class ServiceBinder {
    public static with(container: Container) {
        container.bind<IGeocodingService>("IGeocodingService").to(DataGouvGeocodingService).inSingletonScope();
    }
}
import {interfaces} from "inversify";

import { AngularService } from './api/angular.service';

export class ApiServiceBinder {
    public static with(container: interfaces.Container) {
        container.bind<AngularService>("AngularService").to(AngularService).inSingletonScope();
    }
}

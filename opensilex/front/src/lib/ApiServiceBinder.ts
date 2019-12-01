import {interfaces} from "inversify";

import { AuthenticationService } from './api/authentication.service';
import { UsersService } from './api/users.service';

export class ApiServiceBinder {
    public static with(container: interfaces.Container) {
        container.bind<AuthenticationService>("AuthenticationService").to(AuthenticationService).inSingletonScope();
        container.bind<UsersService>("UsersService").to(UsersService).inSingletonScope();
    }
}

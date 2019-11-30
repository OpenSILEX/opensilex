import {interfaces} from "inversify";

import { AuthenticationService } from './api/authentication.service';
import { HelloService } from './api/hello.service';
import { ProjectsService } from './api/projects.service';
import { UsersService } from './api/users.service';
import { VariablesService } from './api/variables.service';

export class ApiServiceBinder {
    public static with(container: interfaces.Container) {
        container.bind<AuthenticationService>("AuthenticationService").to(AuthenticationService).inSingletonScope();
        container.bind<HelloService>("HelloService").to(HelloService).inSingletonScope();
        container.bind<ProjectsService>("ProjectsService").to(ProjectsService).inSingletonScope();
        container.bind<UsersService>("UsersService").to(UsersService).inSingletonScope();
        container.bind<VariablesService>("VariablesService").to(VariablesService).inSingletonScope();
    }
}

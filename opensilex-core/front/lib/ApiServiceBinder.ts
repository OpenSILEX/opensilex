import {interfaces} from "inversify";

import { HelloService } from './api/hello.service';
import { ProjectsService } from './api/projects.service';
import { VariablesService } from './api/variables.service';

export class ApiServiceBinder {
    public static with(container: interfaces.Container) {
        container.bind<HelloService>("HelloService").to(HelloService).inSingletonScope();
        container.bind<ProjectsService>("ProjectsService").to(ProjectsService).inSingletonScope();
        container.bind<VariablesService>("VariablesService").to(VariablesService).inSingletonScope();
    }
}

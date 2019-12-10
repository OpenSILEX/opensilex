import { Route } from './Route';

export class Menu {

    id: string = "";

    label: string = "";

    children: Array<Menu> = [];

    route?: Route;
}
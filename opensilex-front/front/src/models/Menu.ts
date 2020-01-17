import { MenuItemDTO, RouteDTO } from "../lib";

export class Menu implements MenuItemDTO {
    
    /**
     * Menu identifier
     */
    id: string;
    
    /**
     * Menu label
     */
    label: string;
    
    /**
     * List of sub menu items
     */
    children: Array<MenuItemDTO>;
    
    /**
     * Optional route definition
     */
    route?: RouteDTO;

    /**
     * 
     */
    showChildren: boolean = false;

    public constructor(menu: MenuItemDTO) {
        this.id = menu.id;
        this.label = menu.label;
        this.children = menu.children;
        this.route = menu.route;
        this.showChildren = false;
    }

    public static fromMenuItemDTO(items: Array<MenuItemDTO>): Array<Menu> {
        return items.map(item => new Menu(item));
    }

    public hasChildren(): boolean {
        return this.children.length > 0;
    }

}
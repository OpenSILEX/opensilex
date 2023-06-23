// Declare markdown files as vue components
declare module '*.md' {
    import Vue from 'vue'
    export default Vue
}

declare module "tabulator-tables" {
    export default Tabulator;
}

declare module 'vue-grid-layout' {
    import Vue from 'vue';

    export class GridLayout extends Vue {}

    export class GridItem extends Vue {}

    export interface GridItemData {
        x: number;
        y: number;
        w: number;
        h: number;
        i: string;
        content: any;
    }
}
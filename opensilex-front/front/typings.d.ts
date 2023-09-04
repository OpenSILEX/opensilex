// Declare markdown files as vue components
declare module '*.md' {
    import Vue from 'vue'
    export default Vue
}

declare module "tabulator-tables" {
    export default Tabulator;
}

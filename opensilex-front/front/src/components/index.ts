
// charger dynamiquement tous les composants Vue
const components: { [key: string]: any } = {};

const modules = import.meta.glob('./**/*.vue', { eager: true });

for (const path in modules) {
  const component = (modules[path] as { default : any}).default;
  const componentName : string | undefined = path
  .split("/").pop()
    // .replace(/^\.\/components\//, '')
    .replace(/\.vue$/, '')
    // .replace(/\//g, '-');
  
  components[`opensilex-${componentName}`] = component;
}

export default components;

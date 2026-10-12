// charger dynamiquement tous les composants de la librairie
const components: { [key: string]: any } = {};

const modules = import.meta.glob('./components/**/*.vue', { eager: true });

for (const path in modules) {
  const component = (modules[path] as { default: any }).default;
  const componentName: string = path.split("/").pop()!.replace(/\.vue$/, '');
  components[`opensilex-library-${componentName}`] = component;
}

export default components;

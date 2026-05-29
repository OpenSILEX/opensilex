import { readdirSync, statSync, writeFileSync, existsSync, mkdirSync } from 'fs';
import { join, basename, extname } from 'path';

// Directory containing Vue components
import { fileURLToPath } from 'url';
const __filename = fileURLToPath(import.meta.url);
const __dirname = new URL('.', import.meta.url).pathname;
const componentsDir = join(__dirname, '..', 'opensilex-front', 'front', 'src');
// Output folder for generated stories (parallel to components)
const storiesDir = join(componentsDir, 'stories');
if (!existsSync(storiesDir)) {
  mkdirSync(storiesDir, { recursive: true });
}

function walk(dir: string) {
  const entries = readdirSync(dir);
  for (const entry of entries) {
    const fullPath = join(dir, entry);
    if (statSync(fullPath).isDirectory()) {
      walk(fullPath);
    } else if (extname(entry) === '.vue') {
      const componentName = basename(entry, '.vue');
      const relativePath = fullPath.split('src')[1]; // path after 'src'
      const importPath = `../opensilex-front/front/src${relativePath}`;
      const storyFile = join(storiesDir, `${componentName}.stories.ts`);
      if (existsSync(storyFile)) continue; // skip existing stories
      const storyContent = `import ${componentName} from '${importPath}';

export default {
  title: 'Components/${componentName}',
  component: ${componentName},
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ${componentName} },
  setup() { return { args }; },
  template: '<${componentName} v-bind=\"args\" />',
});

export const Default = Template.bind({});
Default.args = {};
`;
      writeFileSync(storyFile, storyContent);
      console.log(`Created ${storyFile}`);
    }
  }
}

walk(componentsDir);

import { defineConfig } from 'vitepress'
import { withSidebar } from 'vitepress-sidebar'

export default defineConfig(
  withSidebar(
    {
      title: 'OpensilexDev-Specs',
      description: 'A VitePress Site',
      srcExclude: ['template/spec_template.md', 'scientific-object/scientific-objects-import.md'],
      base: '/opensilex-dev/specifications/',
      ignoreDeadLinks: true,

      themeConfig: {
        nav: [
          { text: 'Home', link: '/' },
          { text: 'Examples', link: '/markdown-examples' }
        ],
        // DO NOt define sidebar, plugin will generate it
        socialLinks: [
          { icon: 'github', link: 'https://github.com/vuejs/vitepress' }
        ],
        search: {
          provider: 'local'
        }
      }
    },
    {
      // vitepress-sidebar plugin options
      documentRootPath: '.',
      collapsed: false,
      capitalizeFirst: true
    }
  )
)
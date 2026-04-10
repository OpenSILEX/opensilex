import { defineConfig } from 'vitepress'
import { withSidebar } from 'vitepress-sidebar'

export default defineConfig(
  withSidebar(
    {
      title: 'Opensilex specifications',
      description: 'A VitePress Site',
      base: '/opensilex-dev/technical-documentation/',
      ignoreDeadLinks: true,

      themeConfig: {
        nav: [
          { text: 'Home', link: '/' },
        ],
        // DO NOt define sidebar, plugin will generate it
        socialLinks: [
          { icon: 'github', link: 'https://github.com/OpenSILEX/opensilex' }
        ],
        search: {
          provider: 'local'
        }
      }
    },
    {
      // vitepress-sidebar plugin options
      documentRootPath: '.',
      collapsed: true,
      capitalizeFirst: true,
      hyphenToSpace: true,
      underscoreToSpace: true,
    }
  )
)
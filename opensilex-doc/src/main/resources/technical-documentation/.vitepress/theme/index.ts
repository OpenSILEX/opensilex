import DefaultTheme from 'vitepress/theme'

export default {
  extends: DefaultTheme,
  enhanceApp() {
    if (typeof window !== 'undefined') {
      document.addEventListener('click', (e) => {
        const link = (e.target as HTMLElement)?.closest('a')
        if (!link) return

        const href = link.getAttribute('href')
        if (!href || href.startsWith('#') || href.startsWith('http') || link.target === '_blank') return

        const needsHardNav =
            href.includes('/technical-documentation/') ||
            href.includes('/functional-specifications/')

        // permet de forcer une navigation dure quand on passe d'un dossier à l'autre, sinon il l'interprète comme un fichier interne et il faut refresh pour voir la page
        if (!needsHardNav) return

        e.preventDefault()
        e.stopImmediatePropagation()
        window.location.href = href
      }, true)
    }
  }
}
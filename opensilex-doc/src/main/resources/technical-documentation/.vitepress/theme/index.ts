import DefaultTheme from 'vitepress/theme'

// À renseigner : le nouveau host de destination (sans slash final)
// ex: 'https://mondomaine.example.com'
const NEW_HOST = 'https://opensilex.pages-forge.inrae.fr/opensilex-dev'
const PREFIXES = ['/technical-documentation/', '/functional-specifications/']

export default {
  extends: DefaultTheme,
  enhanceApp() {
    if (typeof window !== 'undefined') {
      document.addEventListener('click', (e) => {
        const link = (e.target as HTMLElement)?.closest('a')
        if (!link) return
        const href = link.getAttribute('href')
        if (!href || href.startsWith('#') || href.startsWith('http') || link.target === '_blank') return
        /* fix la navigation entre les deux différentes docs (specification et technique)
         permet de forcer une navigation dure quand on passe d'un dossier à l'autre, sinon il l'interprète comme un fichier interne et il faut refresh pour voir la page
        */
        const matchedPrefix = PREFIXES.find((prefix) => href.includes(prefix))
        if (!matchedPrefix) return
        e.preventDefault()
        e.stopImmediatePropagation()
        // Retire le premier préfixe rencontré (et tout ce qui précède, y compris un éventuel doublon), puis reconstruit l'url sur le bon host
        const pathAfterPrefix = href.slice(href.indexOf(matchedPrefix) + matchedPrefix.length)
        const targetUrl = `${NEW_HOST}/${matchedPrefix}/${pathAfterPrefix}`
        window.location.href = targetUrl
      }, true)
    }
  }
}
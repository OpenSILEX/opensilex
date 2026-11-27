<template>
  <!-- The content is our own markdown files, not user input -->
  <div class="markdown-content" v-html="renderedHelp"></div>
</template>

<script setup lang="ts">
import {computed, ComputedRef} from "vue";
import {useI18n} from "vue-i18n";
import {Marked, Token, Tokens} from "marked";
import DataVisuHelpEN from "@/components/visualization/doc/DataVisuHelpEN.md?raw";
import DataVisuHelpFR from "@/components/visualization/doc/DataVisuHelpFR.md?raw";

/**
 * Help of the DataVisuGraphic: its interactions (click, right-click, zoom, menu), in the current language.
 *
 * The markdown files reference their screenshots as `![description](./name.png)`, next to them in the doc folder.
 */

//#region Private
//#region Plugins & services
const {locale} = useI18n();

/**
 * URL of each screenshot of the doc folder, by its path in the markdown. The markdown is read as raw text, so Vite
 * doesn't resolve its image paths by itself.
 */
const screenshotUrls: Map<string, string> = new Map(
    Object.entries(import.meta.glob<string>("./doc/*.png", {eager: true, query: "?url", import: "default"}))
        .map(([path, url]: [string, string]) => [path.replace("./doc/", "./"), url])
);

/**
 * Own instance of marked, so that the image paths are only replaced in this help, not in the other markdown pages
 */
const helpMarked: Marked = new Marked({walkTokens: resolveScreenshot});
//#endregion

//#region Data & Computed
const renderedHelp: ComputedRef<string> = computed<string>(() =>
    helpMarked.parse(locale.value === "fr" ? DataVisuHelpFR : DataVisuHelpEN, {async: false}) as string
);
//#endregion

//#region Methods
/**
 * Called by marked on each piece of the parsed markdown, before it is rendered as HTML: gives the images the real URL
 * of their screenshot
 */
function resolveScreenshot(token: Token): void {
  if (token.type === "image") {
    const image: Tokens.Image = token as Tokens.Image;
    image.href = screenshotUrls.get(image.href) ?? image.href;
  }
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
/* Screenshots of any size: never wider than the modal, proportions kept, framed to stand out from the white modal */
.markdown-content :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 8px auto 24px;
  border: 1px solid #e0e0e6;
  border-radius: 4px;
}
</style>

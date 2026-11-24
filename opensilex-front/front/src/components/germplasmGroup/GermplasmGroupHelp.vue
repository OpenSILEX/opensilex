<template>
  <div class="markdown-content" v-html="renderedMarkdown"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useStore } from 'vuex';
import { marked } from 'marked';
import GroupGermplasmHelpEN from './doc/GroupGermplasmHelpEN.md?raw';
import GroupGermplasmHelpFR from './doc/GroupGermplasmHelpFR.md?raw';

const store = useStore();
const lang = computed(() => store.state.lang);

const renderedMarkdown = computed(() =>
  marked.parse(lang.value === 'fr' ? GroupGermplasmHelpFR : GroupGermplasmHelpEN),
);
</script>

<style scoped lang="scss">
.markdown-content :deep(*) {
  max-width: 100%;
}
</style>

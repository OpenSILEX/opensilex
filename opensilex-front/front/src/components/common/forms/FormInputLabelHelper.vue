<template>
  <span>
    <label class="form-label" :for="labelFor">
      {{ labelText }}
      &nbsp;
      <n-tooltip
        v-if="resolvedHelpMessage"
        trigger="hover"
        placement="top"
      >
        <template #trigger>
          <i
            class="bi bi-question-circle-fill text-secondary"
            tabindex="0"
            role="button"
          />
        </template>
        <!-- Pour autoriser du HTML -->
        <span v-html="resolvedHelpMessage"></span>
      </n-tooltip>
    </label>
  </span>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { NTooltip } from 'naive-ui';

const props = defineProps<{
  helpMessage?: string;
  label: string;
  labelFor?: string;
}>();

const { t } = useI18n();

/**
 * Essaie de traduire si ça ressemble à une clé i18n,
 * sinon renvoie le texte tel quel.
 */
function resolveMaybeKey(msg: string): string {
  if (!msg) return '';

  //  une clé contient un point et pas d'espace, identifie si c'est le cas
  const looksLikeKey = msg.includes('.') && !msg.includes(' ');

  if (looksLikeKey) {
    const translated = t(msg) as string;
    if (!translated || translated === msg) {
      return msg;
    }
    return translated;
  }

  return msg;
}

const labelText = computed(() => resolveMaybeKey(props.label));
const resolvedHelpMessage = computed(() =>
  props.helpMessage ? resolveMaybeKey(props.helpMessage) : ''
);
</script>

<style lang="scss">
// tooltips d'aide aggroportal, sans quoi ecriture noire sur fond noir 
.tootltipLinks a {
  color: #36ad6a !important;
}
</style>

<style scoped lang="scss">
.form-label {
  display: inline-block;
  min-width: 100%;
  margin-bottom: 5px;
  cursor: default;
}
</style>

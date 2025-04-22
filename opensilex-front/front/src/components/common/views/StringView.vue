<template>
  <div>
    <div v-if="props.allowCopy" class="static-field">
      <span :class="['field-view-title', customClass]">{{ t(props.label) }}</span>

      <span>
        <a
          href="#"
          @click.prevent="$emit('click', props.value)"
          :title="props.value"
          :class="'uri ' + (props.underlineTextForCopy ? 'uri-display-none' : 'uri-copy-string')"
        >
          <span>{{ props.value }}</span>
          &nbsp;
          <button
            v-if="props.allowCopy"
            @click.prevent.stop="copyURI(copySource)"
            :class="'uri-copy ' + (props.underlineTextForCopy ? 'uri-display-none' : 'uri-copy-string')"
            :title="t(props.copyTextMessage)"
          >
            <opensilex-Icon icon="bi#bi-clipboard" />
          </button>
        </a>
      </span>
      
    </div>
    <div v-else class="static-field">
      <span :class="['field-view-title', customClass]">{{ t(props.label) }}</span>
      <span class="static-field-line capitalize-first-letter">
        <slot>{{ props.value }}</slot>
      </span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { inject, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import copy from 'copy-to-clipboard';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';

// i18n
const { t } = useI18n();

// Props
const props = defineProps<{
  label: string;
  value: string;
  copyTextMessage?: string;
  allowCopy?: boolean;
  underlineTextForCopy?: boolean;
  copyValue?: string;
  customClass?: string;
}>();

// Emits
const emit = defineEmits<{
  (e: 'click', value: string): void;
}>();

// OpenSilex
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex');
const customClass = props.customClass || '';

// Valeur à copier
const copySource = computed(() => props.copyValue || props.value);

// Methode de copie
function copyURI(value: string) {
  copy(value);
  $opensilex?.showSuccessToast(`${t(props.copyTextMessage || 'component.copyToClipboard.copyText')}: ${value}`);
}
</script>

<style scoped lang="scss">
.uri-copy {
  text-decoration: none !important;
  background-color: transparent !important;
}

.uri {
  display: inline-flex;
  max-width: 400px;
  padding-right: 30px;
  position: relative;
}
.uri-copy-string {
  color: black !important;
  text-decoration: none;
}
.uri-copy-string:hover {
  color: black !important;
  text-decoration: none !important;
}

.uri > span {
  display: inline-block;
  max-width: 370px;
  word-break: keep-all;
  text-overflow: ellipsis;
  overflow: hidden;
  word-wrap: normal;
  white-space: nowrap;
}

.uri .uri-copy {
  border: 1px solid #d8dde5;
  border-radius: 5px;
  color: #212121;
  padding: 3px 5px 0;
  position: absolute;
  right: 0;
  top: -3px;
}

.uri .uri-display-none {
  display: none;
}
.uri-display-none:hover {
  color: #212121 !important;
  text-decoration: underline !important;
}
a span {
  color: black;
}
.uri:hover .uri-copy,
.uri:focus .uri-copy {
  display: inline;
}

.uri:hover {
  color: #212121;
}

// .field-view-title {
//   font-weight: bold;
// }
.sectionTitle {
  font-weight: bold;
  min-width: 60px;
  display:inline-block
}
</style>

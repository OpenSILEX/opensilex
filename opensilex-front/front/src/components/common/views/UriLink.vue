<template>
  <span>
    <!-- Redirection on click -->
    <router-link
      v-if="to"
      :target="target"
      :title="uri"
      :to="to"
      class="uri"
      @focus="storePrevious"
      @click="handleUriLinkClicked"
    >
      <span v-html="value || uri"></span>

      &nbsp;
      <button
        v-if="allowCopy"
        @click.prevent.stop="copyURI(uri)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="bi#bi-clipboard" />
      </button>
    </router-link>

    <!-- Redirection to URL or external URI -->
    <a
      v-else-if="computeURL"
      :href="computeURL"
      class="uri"
      :title="uri"
      target="about:blank"
    >
      <span>{{ value || uri }}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        @click.prevent.stop="copyURI(computeURL)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUrl')"
      >
        <opensilex-Icon icon="bi#bi-clipboard" />
      </button>
    </a>

    <!-- No redirection, only copy is possible -->
    <span
      v-else-if="!isClickable"
      @click.prevent.stop="copyURI(uri)"
      :title="uri"
      class="uri onlyCopyAllowed"
    >
      <span>{{ value || uri }}</span>
      &nbsp;

      <button
        v-if="allowCopy"
        @click.prevent.stop="copyURI(uri)"
        class="uri-copy-visible"
        :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="bi#bi-clipboard" />
      </button>
    </span>

    <!-- No redirection but can open something -->
    <a
      v-else
      href="#"
      @click.prevent="$emit('click', uri)"
      :title="uri"
      class="uri"
    >
      <span>{{ value || uri }}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        @click.prevent.stop="copyURI(uri)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="bi#bi-clipboard" />
      </button>
    </a>
  </span>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";
import { useI18n } from 'vue-i18n';
import copy from "copy-to-clipboard";
import OpenSilexVuePlugin from '../../models/OpenSilexVuePlugin';

const props = defineProps<{
  uri: string;
  value?: string;
  url?: string;
  to?: string | { path: string };
  noExternalLink?: boolean;
  allowCopy?: boolean;
  target?: string;
  isClickable?: boolean;
}>();

const { t } = useI18n();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");

const emit = defineEmits<{
  (event: "click", uri: string): void;
  (event: "linkClicked"): void;
}>();

const computeURL = computed(() => {
  if (props.to) return null;
  if (props.url) return props.url;
  if (props.uri) {
    return !props.noExternalLink &&
      (props.uri.startsWith("http://") || props.uri.startsWith("https://"))
      ? props.uri
      : null;
  }
  return null;
});

const copyURI = (address: string) => {
  copy(address);
  alert(`Copié : ${address}`); // Remplace `$opensilex.showSuccessToast()`
   $opensilex.showSuccessToast(
      address.startsWith("http://") || address.startsWith("https://")
        ? t("component.common.url-copy") + ": " + address
        : t("component.common.uri-copy") + ": " + address
    );

};

const storePrevious = () => {
  console.log("Stocker la page précédente");
};

const handleUriLinkClicked = () => {
  console.log("Lien cliqué, stocker la page");
  emit("linkClicked");
};
</script>

<style scoped lang="scss">
.uri-copy,
.uri-copy-visible {
  text-decoration: none !important;
  background-color: transparent !important;
}

.uri-copy:hover,
.uri-copy-visible:hover {
  background: #e6eceb !important;
}

.uri {
  display: inline-flex;
  max-width: 400px;
  padding-right: 30px;
  position: relative;
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
  display: none;
  border: 1px solid #d8dde5;
  border-radius: 5px;
  color: #212121;
  padding: 3px 5px 0;
  position: absolute;
  right: 0;
  top: -3px;
}

.uri .uri-copy-visible {
  border: 1px solid #d8dde5;
  border-radius: 5px;
  color: #212121;
  padding: 5px 6px 3px;
  position: absolute;
  right: 0;
  // top: -3px;
}

.uri:hover .uri-copy,
.uri:focus .uri-copy,
.uri:hover .uri-copy-visible,
.uri:focus .uri-copy-visible {
  display: inline;
}

.uri:hover {
  color: #212121;
  text-decoration: underline;
}

.onlyCopyAllowed {
  color: #000 !important;
  cursor: pointer;
}

.onlyCopyAllowed:hover {
  text-decoration: none !important;
  color: #018371 !important;
}
</style>

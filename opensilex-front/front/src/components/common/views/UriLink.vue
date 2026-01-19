<template>
  <span>
    <!-- Redirection on click -->
    <router-link
      v-if="to"
      :target="target"
      :title="uri"
      :to="to"
      :class="['uri', { 'uri-in-table': inTable }]"
      @focus="storePrevious"
      @click="handleUriLinkClicked"
    >
      <span v-html="value || uri"></span>
      &nbsp;
      <button
        v-if="allowCopy"
        @click.prevent.stop="copyURI(uri)"
        class="uri-copy"
        :title="t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="bi#bi-clipboard" />
      </button>
    </router-link>

    <!-- Redirection to URL or external URI -->
    <a
      v-else-if="computeURL"
      :href="computeURL"
      :class="['uri', { 'uri-in-table': inTable }]"
      :title="uri"
      target="about:blank"
    >
      <span>{{ value || uri }}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        @click.prevent.stop="copyURI(computeURL)"
        class="uri-copy"
        :title="t('component.copyToClipboard.copyUrl')"
      >
        <opensilex-Icon icon="bi#bi-clipboard" />
      </button>
    </a>

    <!-- No redirection, only copy is possible -->
    <span
      v-else-if="!isClickable"
      @click.prevent.stop="copyURI(uri)"
      :title="uri"
      :class="['uri', 'onlyCopyAllowed', { 'uri-in-table': inTable }]"
    >
      <span>{{ value || uri }}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        @click.prevent.stop="copyURI(uri)"
        class="uri-copy-visible"
        :title="t('component.copyToClipboard.copyUri')"
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
        :title="t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="bi#bi-clipboard" />
      </button>
    </a>
  </span>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";
import { useI18n } from "vue-i18n";
import copy from "copy-to-clipboard";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

export type UriLinkDestination = string | { path: string };

type Props = {
  uri: string;
  value?: string;
  url?: string;
  to?: UriLinkDestination;
  noExternalLink?: boolean;
  allowCopy?: boolean;
  target?: string;
  isClickable?: boolean;
  inTable?: boolean;
};

const props = defineProps<Props>();

const { t } = useI18n();
const $opensilex = inject<OpenSilexVuePlugin | undefined>("$opensilex");

const emit = defineEmits<{
  (event: "click", uri: string): void;
  (event: "linkClicked"): void;
}>();

const computeURL = computed<string | null>(() => {
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
  // Guard si l'injection n'est pas dispo
  $opensilex?.showSuccessToast?.(
    address.startsWith("http://") || address.startsWith("https://")
      ? t("component.common.url-copy") + ": " + address
      : t("component.common.uri-copy") + ": " + address
  );
};

const storePrevious = () => {
};

const handleUriLinkClicked = () => {
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
  //  Changements de style pour la colonne "nom" de la table des variables qui n'etait pas reductible sous une certaine taille ⬇️
  // finalement remplacé une prop inTable (booleen) passée par la colonne à uriLink.vue, qui ajoute une classe dynamique uri-in-table'
  // cela permet de conserver le style des autres cas de figure hors tables, et eviter les effets de bord
  padding-right: 30px; // necessaire dans les listes pour que le bouton de copie de l'uri ne chevauche pas l'uri 
  max-width: 400px;
  //  max-width: 100%;
  position: relative;
}

.uri > span {
  display: inline-block;
  //  max-width: 100%; /
  max-width: 370px;
  word-break: keep-all;
  white-space: nowrap;
  // white-space: normal; /* <-- Permet le retour à la ligne */
  // word-break: break-word; /* <-- Permet de couper les longs mots/URI */
  text-overflow: ellipsis;
  overflow: hidden;
  word-wrap: normal;
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

.uri-in-table {
  max-width: 100% !important;
}

.uri-in-table > span {
  max-width: 100% !important;
  white-space: normal !important;
  word-break: break-word !important;
}


</style>

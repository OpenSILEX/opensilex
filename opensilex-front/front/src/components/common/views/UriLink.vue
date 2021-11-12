<template>
  <span>
    <router-link
      v-if="to"
      :target="target"
      :title="uri"
      :to="to"
      class="uri"
      @focus.native="storePrevious"
      @click.native="storeReturnPage"
    >
      <span v-html="value">{{ value || uri }}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        v-on:click.prevent.stop="copyURI(uri)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="ik#ik-copy" />
      </button>
    </router-link>
    <a v-if="computeURL" :href="computeURL" class="uri" :title="uri" target="about:blank">
      <span>{{value || uri}}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        v-on:click.prevent.stop="copyURI(uri)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUrl')"
      >
        <opensilex-Icon icon="ik#ik-copy" />
      </button>
    </a>
    <a
      v-if="!computeURL && !to"
      href="#"
      @click.prevent="$emit('click', uri)"
      :title="uri"
      class="uri"
    >
      <span>{{value || uri}}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        v-on:click.prevent.stop="copyURI(uri)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="ik#ik-copy" />
      </button>
    </a>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import copy from "copy-to-clipboard";
import Vue from "vue";

export type UriLinkDestination = string | {
  path: string
};

@Component
export default class UriLink extends Vue {
  $opensilex: any;
  $t: any;
  $router: any;

  @Prop()
  uri: string;

  @Prop()
  value: string;

  @Prop()
  url: string;

  @Prop()
  to: UriLinkDestination;

  @Prop({
    default: false
  })
  noExternalLink: boolean;

  @Prop({
    default: true
  })
  allowCopy: boolean;

  @Prop({
    default: "_self"
  })
  target: string;

  get computeURL() {
    if (this.to) {
      return null;
    } else if (this.url) {
      return this.url;
    } else if (this.uri) {
      return !this.noExternalLink &&
        (this.uri.startsWith("http://") || this.uri.startsWith("https://"))
        ? this.uri
        : null;
    } else {
      return null;
    }
  }

  copyURI(address) {
    copy(address);
    this.$opensilex.showSuccessToast(
      address.startsWith("http://") || address.startsWith("https://")
        ? this.$t("component.common.url-copy") + ": " + address
        : this.$t("component.common.uri-copy") + ": " + address
    );
  }

  storePrevious() {
    this.$store.commit("storeCandidatePage", this.$router);
  }

  storeReturnPage() {
    this.$store.commit("validateCandidatePage", this.$router);
  }
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

.uri:hover .uri-copy,
.uri:focus .uri-copy,
.uri:hover .uri-copy {
  display: inline;
}

.uri:hover {
  color: #212121;
  text-decoration: underline;
}
</style>



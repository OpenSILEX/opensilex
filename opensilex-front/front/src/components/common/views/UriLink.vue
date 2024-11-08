<template>
  <span>
    <!-- Redirection on click -->
    <router-link
      v-if="to"
      :target="target"
      :title="uri"
      :to="to"
      class="uri"
      @focus.native="storePrevious"
      @click.native="handleUriLinkClicked"
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

    <!-- Redirection to url or external uri -->
    <a
      v-if="computeURL"
      :href="computeURL"
      class="uri"
      :title="uri"
      target="about:blank"
    >
      <span>{{value || uri}}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        v-on:click.prevent.stop="copyURI(computeURL)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUrl')"
      >
        <opensilex-Icon icon="ik#ik-copy" />
      </button>
    </a>
    
    <!-- No redirection only copy is possible -->
    <span
      v-if="!computeURL && !to && !isClickable"
      href="#"
      v-on:click.prevent.stop="copyURI(uri)"
      :title="uri"
      class="uri onlyCopyAllowed"
    >
      <span>{{value || uri}}</span>
      &nbsp;
      <button
        v-if="allowCopy"
        v-on:click.prevent.stop="copyURI(uri)"
        class="uri-copy-visible"
        :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="ik#ik-copy" />
      </button>
    </span>

    <!-- No redirection but can open something -->
    <a
      v-if="isClickable"
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
  //#region: data

  $opensilex: any;
  $t: any;
  $router: any;

  //#endregion
  //region: props

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

  @Prop({
    default: false
  })
  isClickable: boolean;

  //#endregion
  //#region: computed

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

  //#endregion
  //#region: EventHandlers

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

  handleUriLinkClicked(){
    this.storeReturnPage();
    this.$emit('linkClicked');
  }

  //#endregion
  //#region: private functions

  private storeReturnPage() {
    this.$store.commit("validateCandidatePage", this.$router);
  }
  //#endregion

}
</script>

<style scoped lang="scss">
.uri-copy, .uri-copy-visible{
  text-decoration: none !important;
  background-color: transparent !important;
}

.uri-copy:hover, .uri-copy-visible:hover{
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
  padding: 3px 5px 0;
  position: absolute;
  right: 0;
  top: -3px;
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


.onlyCopyAllowed  {
  color: #000 !important;
  cursor: pointer;
}

.onlyCopyAllowed:hover {
  text-decoration: none !important;
  color: #018371 !important;
}
</style>



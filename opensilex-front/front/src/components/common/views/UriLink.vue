<template>
  <span>
    <router-link v-if="to" class="uri" :to="to">
      <span>{{uri}}</span>
      &nbsp;
      <button
        v-on:click.prevent="copyURI(uri)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="ik#ik-copy" />
      </button>
    </router-link>
    <a v-if="url" :href="url" class="uri" target="about:blank">
      <span>{{uri}}</span>
      &nbsp;
      <button
        v-on:click.prevent="copyURI(uri)"
        class="uri-copy"
        :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="ik#ik-copy" />
      </button>
    </a>
    <a v-if="!url && !to" href="#" @click.prevent="$emit('click', uri)" class="uri">
      <span>{{uri}}</span>
      &nbsp;
      <button
        v-on:click.prevent="copyURI(uri)"
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

@Component
export default class UriLink extends Vue {
  $opensilex: any;
  $t: any;

  @Prop()
  uri: string;

  @Prop()
  url: string;

  @Prop()
  to: string;

  copyURI(address) {
    copy(address);
    this.$opensilex.showSuccessToast(
      this.$t("component.common.uri-copy") + ": " + address
    );
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
  max-width: 300px;
  padding-right: 30px;
  position: relative;
}

.uri > span {
  display: inline-block;
  max-width: 270px;
  word-break: keep-all;
  text-overflow: ellipsis;
  overflow: hidden;
}

.uri .uri-copy {
  display: none;
  border: 1px solid #d8dde5;
  border-radius: 5px;
  color: #212121;
  display: none;
  padding: 3px 3px 0 3px;
  padding-left: 5px;
  padding-right: 5px;
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



<template>
  <div>
    <div v-if="allowCopy" class="static-field">
      <span class="field-view-title">{{ $t(label) }}</span>
      <span>
        <a 
          href="#"
          @click.prevent="$emit('click', uri)"
          :title="uri"
          :class="'uri ' + (underlineTextForCopy ? 'uri-display-none':'uri-copy-string')"
        >
          <span>{{ value }}</span>
          &nbsp;
          <button
            v-if="allowCopy"
            v-on:click.prevent.stop="copyURI(copyValue|| value)"
            :class="'uri-copy  ' + (underlineTextForCopy ? 'uri-display-none':'uri-copy-string')"
            :title="$t(copyTextMessage)"
          >
            <opensilex-Icon icon="ik#ik-copy" />
          </button>
        </a>
      </span>
    </div>
    <div v-else class="static-field">
      <span class="field-view-title">{{ $t(label) }}</span>
      <span class="static-field-line capitalize-first-letter">
        <slot>{{ value }}</slot>
      </span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import copy from "copy-to-clipboard";
import Vue from "vue";

@Component
export default class UriLink extends Vue {
  $opensilex: any;
  $t: any;
  $router: any;

  @Prop()
  label: string;

  @Prop()
  value: string;

  @Prop({
    default: 'component.copyToClipboard.copyText',
  })
  copyTextMessage: string;

  @Prop({
    default: false,
  })
  allowCopy: boolean;

  @Prop({
    default: false,
  })
  underlineTextForCopy: boolean;

  @Prop()
  copyValue: string;

  copyURI(value) {
    copy(value);
    this.$opensilex.showSuccessToast(
      this.$t(this.copyTextMessage) + ": " + value
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
  max-width: 400px;
  padding-right: 30px;
  position: relative;
}
.uri-copy-string{
  color: black !important;
  text-decoration: none;
}
.uri-copy-string:hover{
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

.uri .uri-display-none{
  display: none;
}
.uri-display-none:hover {
  color: #212121  !important;
  text-decoration: underline !important;;
}
a span{
  color: black;
}
.uri:hover .uri-copy,
.uri:focus .uri-copy,
.uri:hover .uri-copy {
  display: inline;
}

.uri:hover {
  color: #212121;
}
</style>
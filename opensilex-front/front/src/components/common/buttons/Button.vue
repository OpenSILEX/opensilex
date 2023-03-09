<template>
  <b-button v-if="href"
            :title="$t(label)"
            :disabled="disabled"
            :href="href"
            variant="outline-primary"
            target="_blank"
  >
    <slot name="icon">
      <opensilex-Icon icon="fa#external-link-alt"/>
    </slot>
    <span class="button-label" :title="tooltip" v-if="size==='md'">{{ $t(label) }}</span>
  </b-button>

  <b-button v-else
            @click.prevent="$emit('click')"
            :title="$t(label)"
            :variant="variant"
            :disabled="disabled"
  >
    <slot name="icon">
      <opensilex-Icon :icon="icon"/>
    </slot>
    <span class="button-label" :title="tooltip" v-if="size==='md'">{{ $t(label) }}</span>
  </b-button>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class Button extends Vue {
  @Prop()
  label: string;

  @Prop()
  variant: string;

  @Prop()
  small: boolean;

  @Prop({
    default: false,
  })
  disabled: boolean;

  @Prop()
  icon: string;

  @Prop()
  helpMessage: string;

  @Prop()
  href: string;

  get tooltip() {
    if (!this.helpMessage || this.helpMessage.length == 0) {
      return this.label ? this.$t(this.label) : undefined;
    }
    return this.$t(this.helpMessage);
  }

  get size() {
    if (this.small == undefined) {
      return undefined;
    }
    return this.small ? 'sm' : 'md';
  }
}
</script>

<style scoped lang="scss">
.button-label {
  margin-left: 5px;
  padding-right: 13px;
}
</style>


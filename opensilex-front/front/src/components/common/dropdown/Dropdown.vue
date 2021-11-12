<template>
  <b-dropdown
    :text="$t(label)"
    :variant="variant"
    :disabled="disabled"
    :title="tooltip"
    :right="right"
    :size="size"
    :class="toggleClass"
  >
    <template v-slot:button-content>
      <opensilex-Icon
          v-if="icon"
          :icon="icon" />
    </template>
    <b-dropdown-item-button
        v-for="option in options"
        :key="option.id"
        @click="click(option)"
        :variant="variant"
    >
      <opensilex-Icon
          v-if="option.icon"
          :icon="option.icon" />
      <span>
        {{ option.label }}
      </span>
    </b-dropdown-item-button>
  </b-dropdown>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "vue-class-component";
import {Prop} from "vue-property-decorator";

export interface DropdownButtonOption {
  label: string,
  id: string,
  icon?: string,
  data?: any
}

@Component
export default class Dropdown extends Vue {
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

  @Prop({
    default: undefined
  })
  right: boolean;

  @Prop()
  helpMessage: string;

  @Prop()
  options: Array<DropdownButtonOption>;

  get tooltip() {
    if (!this.helpMessage || this.helpMessage.length == 0) {
      return this.label ? this.$t(this.label) : undefined;
    }
    return this.$t(this.helpMessage);
  }

  get size() {
    if (!this.small) {
      return undefined;
    }
    if (this.small === true) {
      return 'sm';
    } else {
      return 'md';
    }
  }

  get toggleClass() {
    if (!this.small) {
      return undefined;
    }
    return 'btn-group-sm';
  }

  click(option: DropdownButtonOption) {
    this.$emit("click", option);
  }
}
</script>

<style scoped lang="scss">

</style>
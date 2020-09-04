<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <input :id="field.id" type="hidden" :value="stringValue" />
      <treeselect
        valueFormat="node"
        :options="paginated"
        :placeholder="$t(placeholder)"
        @open="onOpen"
        @close="onClose"
      >
        <template v-slot:option-label="{node}">
          <opensilex-Icon :icon="node.label" />&nbsp;
          <span>({{node.label}})</span>
        </template>
        <template v-slot:value-label="{node}">
          <opensilex-Icon :icon="node.label" />&nbsp;
          <span>({{node.label}})</span>
        </template>
        <template v-slot:after-list>
          <span ref="loadingElement"></span>
        </template>
      </treeselect>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Ref
} from "vue-property-decorator";
import Vue from "vue";
import AsyncComputedProp from "vue-async-computed-decorator";

@Component
export default class IconForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  stringValue: string;

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @PropSync("required")
  isRequired: boolean;

  @Prop()
  disabled: boolean;

  @Prop({
    default: false
  })
  rules: string | Function;

  @Prop()
  multiple: boolean;

  observer = null;
  listOffset = 20;
  limit = 20;
  search = "";

  icons = [];

  @Ref("loadingElement") readonly loadingElement!: any;

  mounted() {
    this.icons = this.$opensilex.getSelectIconIDs();

    this.observer = new IntersectionObserver((entries, observer) => {
      entries.forEach(entry => {
        let isVisible = entry.intersectionRatio > 0;
        if (isVisible) {
          this.limit += this.listOffset;
        }
      });
    });
  }

  get filtered() {
    return this.icons.filter(icon => icon.id.indexOf(this.search) >= 0 || icon.id == this.stringValue);
  }

  get paginated() {
    return this.filtered.slice(0, this.limit);
  }

  get hasNextPage() {
    return this.paginated.length < this.filtered.length;
  }

  async onOpen() {
    if (this.hasNextPage) {
      await this.$nextTick();
      this.observer.observe(this.loadingElement);
    }
  }

  onClose() {
    this.observer.disconnect();
  }
}
</script>

<style scoped lang="scss">
</style>


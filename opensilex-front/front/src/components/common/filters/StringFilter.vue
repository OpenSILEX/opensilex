<template>
  <b-input-group size="sm">
     <opensilex-FormInputLabelHelper
      class="mt-2 mr-2"
      v-if="label"
      :label="label"
      :helpMessage="helpMessage"
      :labelFor="id"
    ></opensilex-FormInputLabelHelper>
    <b-form-input
      :id="id"
      class="filter"
      :disabled="disabled"
      v-model="filterValue"
      @update="$emit('update', $event)"
      debounce="300"
      :placeholder="$t(placeholder)"
    ></b-form-input>
    <template v-slot:append>
      <b-btn :disabled="!filterValue" variant="primary" @click="clear()">
        <opensilex-Icon icon="fa#times" />
      </b-btn>
    </template>
  </b-input-group>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class StringFilter extends Vue {
  $opensilex :any;

  @PropSync("filter")
  filterValue: string;

  @Prop()
  placeholder: string;

  @Prop()
  label: string;

  @Prop({default:false})
  disabled: boolean;

  @Prop()
  helpMessage: string;

  clear() {
    this.filterValue = '';
    this.$emit('update', this.filterValue);
  }
  id: string;

  created() {
    this.id = this.$opensilex.generateID();
  }

}
</script>

<style scoped lang="scss">
.filter {
  font-size: 13px;
}

.stringFilterLabel {
  margin-bottom : 0px !important;
}
</style>


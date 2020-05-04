<template>
  <b-input-group size="sm">
    <b-form-input
      class="filter"
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
  @PropSync("filter")
  filterValue: string;

  @Prop()
  placeholder: string;

  clear() {
    this.filterValue = '';
    this.$emit('update', this.filterValue);
  }
}
</script>

<style scoped lang="scss">
.filter {
  font-size: 13px;
}
</style>


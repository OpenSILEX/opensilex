<template>
  <b-input-group size="sm">
    <b-form-input class="filter" v-model="filterValue" debounce="300" :placeholder="$t(placeholder)"></b-form-input>
    <template v-slot:append>
      <b-btn :disabled="!filterValue" variant="primary" @click="filterValue = ''">
        <font-awesome-icon icon="times" size="sm" />
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
}
</script>

<style scoped lang="scss">
.filter {
  font-size: 13px;
}
</style>


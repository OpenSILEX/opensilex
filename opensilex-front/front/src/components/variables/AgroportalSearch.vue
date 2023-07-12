<template>
  <b-input
      type="text"
      v-model="searchBar"
      placeholder="search"
      v-on:change="foo"/>
</template>


<script lang="ts">

import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from 'vue';

@Component
export default class AgroportalSearch extends Vue {

  searchBar;

  created() {
    this.searchBar = "";
  }

  foo() {
    console.log(this.searchBar);
    this.$emit('change', this.searchBar);
  }
}
</script>


<style scoped>

</style>


<i18n>

</i18n>
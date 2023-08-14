<template>
  <b-input
      type="text"
      v-model="searchBar"
      placeholder="search"
      v-on:change="$emit('change', searchBar)"/>
</template>


<script lang="ts">

import {Component} from "vue-property-decorator";
import Vue from 'vue';

@Component
export default class AgroportalSearch extends Vue {

  searchBar;

  created() {
    this.searchBar = "";
  }

}
</script>


<style scoped>

</style>


<i18n>

</i18n>
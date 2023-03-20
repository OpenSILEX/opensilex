<template>
  <div class="container-fluid">
    <opensilex-Dashboard></opensilex-Dashboard>
  </div>
</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';

@Component
export default class HomeView extends Vue {
  get user() {
    return this.$store.state.user;
  }
}
</script>

<style scoped lang="scss">
</style>

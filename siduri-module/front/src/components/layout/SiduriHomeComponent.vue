<template>
  <div class="main-content">
    <div class="container-fluid">
      <div class="center">
        <img v-bind:src="$opensilex.getResourceURI('images/siduri-logo.jpg')" />
        <h2>.welcome-message')}} <br/><br/> .title')}}</h2>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DefaultHomeComponent extends Vue {}
</script>

<style scoped lang="scss">

</style>

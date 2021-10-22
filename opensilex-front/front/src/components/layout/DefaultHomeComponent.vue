<template>
  <div class="main-content">
    <div fluid="sm">
      <b-row>
        <b-col md="6">
          <b-img v-b-popover.hover.top="$t('component.header.download-user-guide')" @click="$opensilex.getGuideFile()" thumbnail v-bind:src="$opensilex.getResourceURI('images/userguide.png')" alt="component.header.download-user-guide"></b-img>
        </b-col>
      </b-row>
    </div>
  </div> 
</template>

<script lang="ts">
import { Component, Ref } from 'vue-property-decorator';
import Vue from 'vue';

@Component
export default class DefaultHomeComponent extends Vue {
}
</script>

<style scoped lang="scss">
.jumbotron{   
    padding-top: 0px;
    padding-bottom:0px;
    height:560px;
  }
</style>

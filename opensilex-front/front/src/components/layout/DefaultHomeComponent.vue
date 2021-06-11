<template>
  <div class="main-content">
      <div class="container-fluid">
          
      </div>
     
        <opensilex-Elasticsearch  
         ref="tableRef"
      :searchMethod="loadData"
      :fields="fields"
      :isSelectable="true"
      :maximumSelectedRows="maximumSelectedRows">
          

        </opensilex-Elasticsearch>
   
  </div> 
</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';

@Component
export default class DefaultHomeComponent extends Vue {
}
</script>

<style scoped lang="scss">
</style>

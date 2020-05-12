<template>
  <div class="card-vertical-group">
    <div class="card">
      <div class="card-header">
        <h3 class="mr-3">
          <opensilex-Icon icon="ik#ik-search" />
          {{$t(label)}}
        </h3>
      </div>
      <div class="mt-3">
        <slot name="filters"></slot>
      </div>
      <div class="card-footer text-right mr-3">
        <slot name="clear">
          <b-button @click="$emit('clear',$event)" class="btn btn-light mr-3">
            <opensilex-Icon icon="ik#ik-x" />
            {{$t('component.common.search.clear-button')}}
          </b-button>
        </slot>
        <slot name="search">
          <b-button @click="$emit('search',$event)" class="btn btn-primary">
            <opensilex-Icon icon="ik#ik-search" />
            {{$t('component.common.search.search-button')}}
          </b-button>
        </slot>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class SearchFilterField extends Vue {
  @Prop({ default: "component.search.label" })
  label: string;
}
</script>


<style scoped lang="scss">
</style>


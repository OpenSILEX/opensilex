<template>
  <div class="card-vertical-group">
    <div class="card">
      <div class="card-body container-fluid">
        <div class="row">
          <opensilex-Icon class="search-icon" icon="ik#ik-search" />
          <slot name="filters"></slot>
        </div>
      </div>
      <div class="container-fluid button-group" v-if="withButton">
        <div class="row">
          <div class="col-md-12 text-right">
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

  @Prop({ default: true })
  withButton: boolean;
}
</script>


<style scoped lang="scss">
.button-group {
  padding-bottom: 15px;
}

.card-body {
  padding-left: 45px !important;
  padding-right: 15px !important;
  padding-bottom: 0 !important;
}

.search-icon {
  position: absolute;
  font-size: 24px;
  left: 15px;
  top: 15px;
}
</style>


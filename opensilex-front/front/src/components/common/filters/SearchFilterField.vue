<template>
  <div class="card-vertical-group">
    <div class="card">
      <div v-if="showTitle" class="card-header">
          <h3 class="mr-3"><opensilex-Icon class="search-icon" icon="ik#ik-search" />{{ $t(label) }}</h3>
      </div>

      <div class="card-body row">
        <slot name="filters"></slot>
      </div>

      <div class="card" v-if="showAdvancedSearch">
          <div class="card-header sub-header">
              <h3 class="mr-3">{{ $t(advancedSearchLabel) }}</h3>
              <div class="card-header-right">
                  <div class="card-option">
                      <li><i class="ik minimize-card ik-plus" v-on:click="toogleAdvancedSearch($event)"></i></li>
                  </div>
              </div>
          </div>
          <div class="card-body advancedSearch" style="background-color: rgb(246, 248, 251);" v-bind:class="{ 'open': advancedSearchOpen}">
              <slot name="advancedSearch"></slot>
          </div>
      </div>
    </div>
    <div class="card" v-if="withButton">
        <div class="card-footer text-right">
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
  PropSync,
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class SearchFilterField extends Vue {
  @Prop({ default: "SearchFilter.searchlabel" })
  label: string;

  @Prop({ default: "SearchFilter.advancedSearchLabel" })
  advancedSearchLabel: string;

  @Prop({ default: true })
  withButton: boolean;

  @Prop({ 
    default: false
  })
  showTitle;

  @Prop({ 
    default: false 
  })
  showAdvancedSearch;

  advancedSearchOpen = false;

  toogleAdvancedSearch() {
      this.advancedSearchOpen = !this.advancedSearchOpen;
  }
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
</style>

<i18n>

en:
  SearchFilter:
    searchlabel: Search
    advancedSearchLabel: Advanced Search
fr:
  SearchFilter:
    searchlabel: Recherche
    advancedSearchLabel: Recherche Avancée

</i18n>


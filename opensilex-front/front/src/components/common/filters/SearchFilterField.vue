<template>
  <div class="card-vertical-group">
    <div class="card">
      <div v-if="showTitle" class="card-header">
        <h3 class="mr-3">
          <opensilex-Icon class="search-icon" icon="ik#ik-search" />
          {{ $t(label) }}
        </h3>
      </div>

      <div class="card-body row">
        <slot name="filters"></slot>
      </div>

      <div class="card" v-if="showAdvancedSearch">
        <div class="card-header sub-header">
          <h3 class="mr-3">{{ $t(advancedSearchLabel) }}</h3>
          <div class="card-header-right">
            <div class="card-option">
              <li>
                <i v-if="!advancedSearchOpen" class="ik minimize-card ik-plus" v-on:click="toogleAdvancedSearch($event)"></i>
                <i v-if="advancedSearchOpen" class="ik minimize-card ik-minus" v-on:click="toogleAdvancedSearch($event)"></i>
              </li>
            </div>
          </div>
        </div>
        <div
          class="card-body advancedSearch row"
          style="background-color: rgb(246, 248, 251);"
          v-bind:class="{ 'open': advancedSearchOpen}"
        >
          <slot name="advancedSearch"></slot>
        </div>
      </div>
    </div>

    <div class="container-fluid button-group" v-if="withButton" v-bind:class="{ 'advancedSearchClosed': !advancedSearchOpen}">
      <div class="row">
        <div class="col-md-12 text-right">
          <slot name="clear">
            <opensilex-Button
              label="component.common.search.clear-button"
              icon="ik#ik-x"
              @click="$emit('clear',$event)"
              variant="light"
              class="mr-3"
              :small="false"
            ></opensilex-Button>
          </slot>
          <slot name="search">
            <opensilex-Button
              label="component.common.search.search-button"
              @click="$emit('search',$event)"
              icon="ik#ik-search"
              variant="primary"
              :small="false"
            ></opensilex-Button>
          </slot>
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

.advancedSearchClosed {
  padding-top: 15px;
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


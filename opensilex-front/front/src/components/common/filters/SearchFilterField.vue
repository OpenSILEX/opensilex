<template>
  <div class="card-vertical-group">
    <ValidationObserver ref="validatorRef">
      <div class="card">
        <div v-if="showTitle" class="card-header">
          <h3 class="mr-3">
            <opensilex-Icon class="search-icon" icon="ik#ik-search"/>
            {{ $t(label) }}
          </h3>
        </div>

        <div class="card-body">
          <div class="container-full">
            <div class="row">
              <slot name="filters"></slot>
            </div>
          </div>
        </div>

        <div class="card" v-if="showAdvancedSearch">
          <div
            class="card-header sub-header advanceSearchBlock"
            v-on:click="toogleAdvancedSearch($event)"
          >
            <!-- <i v-if="!advancedSearchOpen" class="ik minimize-card ik-plus"></i>
          <i v-if="advancedSearchOpen" class="ik minimize-card ik-minus"></i>
          <span>&nbsp;</span> -->
            <h3 class="mr-3">
              {{ $t(advancedSearchLabel) }}
            </h3>
            <div class="card-header-right">
              <div class="card-option">
                <li>
                  <i
                    v-if="!advancedSearchOpen"
                    class="ik minimize-card ik-plus"
                  ></i>
                  <i
                    v-if="advancedSearchOpen"
                    class="ik minimize-card ik-minus"
                  ></i>
                </li>
              </div>
            </div>
          </div>
          <div
            class="card-body advancedSearch row"
            style="background-color: transparent"
            v-bind:class="{ open: advancedSearchOpen }"
          >
            <slot name="advancedSearch"></slot>
          </div>
        </div>
      </div>

      <div
        class="container-fluid button-group"
        v-if="withButton"
        v-bind:class="{ withAdvancedSearch: showAdvancedSearch }"
      >
        <div class="row">
          <div class="col-md-12 text-right">
            <slot name="clear">
              <opensilex-Button
                label="component.common.search.clear-button"
                icon="ik#ik-x"
                @click="$emit('clear', $event)"
                variant="light"
                class="mr-3"
                :small="false"
              ></opensilex-Button>
            </slot>
            <slot name="search">
              <opensilex-Button
                :label="searchButtonLabel"
                @click="validateAndSearch($event)"
                icon="ik#ik-search"
                class="greenThemeColor createButton"
                :small="false"
              ></opensilex-Button>
            </slot>
          </div>
        </div>
      </div>
    </ValidationObserver>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Ref,
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class SearchFilterField extends Vue {
  @Ref("validatorRef") readonly validatorRef!: any;

  @Prop({ default: "SearchFilter.searchlabel" })
  label: string;

  @Prop({ default: "component.common.search.search-button" })
  searchButtonLabel: string;

  @Prop({ default: "SearchFilter.advancedSearchLabel" })
  advancedSearchLabel: string;

  @Prop({ default: true })
  withButton: boolean;

  @Prop({default: true })
  withIcon: boolean;

  @Prop({
    default: false,
  })
  
  @Prop({ default: true })
  showTitle: boolean;

  @Prop({
    default: false,
  })
  showAdvancedSearch;

  advancedSearchOpen = false;

  toogleAdvancedSearch() {
    this.advancedSearchOpen = !this.advancedSearchOpen;
  }

  validateAndSearch($event) {
    this.validatorRef.validate().then((isValid) => {
      if (isValid) {
        this.$emit("search", $event);
      }
    });
  }
}
</script>


<style scoped lang="scss">
.button-group {
  padding-bottom: 15px;
}

.card-body {
  padding-bottom: 0 !important;
}

.advancedSearch {
  padding-top: 15px;
}

.sub-header:hover {
  cursor: pointer;
  background-color: #eeeeee;
}

.button-group.withAdvancedSearch {
  padding-top: 0;
}

.advanceSearchBlock {
  padding-top: 5px !important;
  padding-bottom: 5px !important;
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


<template>
  <div class="card-vertical-group">
    <div class="card">
      <div v-if="showTitle" class="card-header">
        <h3 class="mr-3">
          <Icon class="search-icon" icon="ik#ik-search"/>
          {{ translatedLabel }}
        </h3>
      </div>

      <div class="card-body">
        <div class="container-full">
          <div class="row">
            <slot name="filters"></slot>
          </div>
        </div>
      </div>

      <div class="card" v-if="advancedSearchOpen">
        <div
            class="card-header sub-header advanceSearchBlock"
            @click="toggleAdvancedSearch"
        >
          <h3 class="mr-3">
            {{ translatedAdvancedSearchLabel }}
          </h3>
          <div class="card-header-right">
            <div class="card-option">
              <i
                  v-if="!advancedSearchOpen"
                  class="ik minimize-card ik-plus"
              ></i>
              <i
                  v-if="advancedSearchOpen"
                  class="ik minimize-card ik-minus"
              ></i>
            </div>
          </div>
        </div>
        <div
            class="card-body advancedSearch row"
            style="background-color: transparent"
            :class="{ open: advancedSearchOpen }"
        >
          <slot name="advancedSearch"></slot>
        </div>
      </div>
    </div>

    <div
        class="container-fluid button-group"
        v-if="withButton"
        :class="{ withAdvancedSearch: advancedSearchOpen }"
    >
      <div class="row">
        <div class="col-md-12 text-right">
          <slot name="clear">
            <Button
                label="component.common.search.clear-button"
                icon="ik#ik-x"
                @click="$emit('clear')"
                variant="light"
                class="mr-3"
                :small="false"
            ></Button>
          </slot>
          <slot name="search">
            <Button
                :label="translatedSearchButtonLabel"
                @click="$emit('search')"
                icon="ik#ik-search"
                class="greenThemeColor createButton"
                :small="false"
            ></Button>
          </slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
//#region Public
import {useI18n} from "vue-i18n";
import Icon from "@/components/common/views/Icon.vue";
import Button from "@/components/common/buttons/Button.vue";
import {computed} from "vue";

const props = withDefaults(defineProps<{
  label?: string
  searchButtonLabel?: string
  advancedSearchLabel?: string
  withButton?: boolean
  showTitle?: boolean
}>(), {
  withButton: true,
  showTitle: true
});

const advancedSearchOpen = defineModel<boolean>("advancedSearchOpen");

defineEmits<{
  clear: [],
  search: []
}>();
//#endregion

//#region Private
const {t} = useI18n();

const translatedLabel = computed(() => props.label || t("searchLabel"));
const translatedSearchButtonLabel = computed(() => props.label || t("component.common.search.search-button"));
const translatedAdvancedSearchLabel = computed(() => props.label || t("advancedSearchLabel"));

function toggleAdvancedSearch() {
  advancedSearchOpen.value = !advancedSearchOpen.value;
}
//#endregion
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
  searchLabel: Search
  advancedSearchLabel: Advanced Search
fr:
  searchLabel: Recherche
  advancedSearchLabel: Recherche Avancée
</i18n>
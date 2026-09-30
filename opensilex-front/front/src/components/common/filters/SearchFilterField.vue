<template>
  <div class="card-vertical-group">
    <n-form
        ref="validatorRef">
      <div class="card">
        <div v-if="showTitle" class="card-header">
          <h3 class="mr-3">
            <Icon class="search-icon" icon="ik#ik-search"/>
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
            v-on:click="toggleAdvancedSearch($event)"
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
              <Button
                label="component.common.search.clear-button"
                icon="ik#ik-x"
                @click="$emit('clear', $event)"
                variant="light"
                class="mr-3"
                :small="false"
              ></Button>
            </slot>
            <slot name="search">
              <Button
                :label="searchButtonLabel"
                @click="validateAndSearch($event)"
                icon="ik#ik-search"
                class="greenThemeColor createButton"
                :small="false"
              ></Button>
            </slot>
          </div>
        </div>
      </div>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import Button from "@/components/common/buttons/Button.vue";
import Icon from "@/components/common/views/Icon.vue";
import {NForm} from "naive-ui";
import {ref, useTemplateRef} from "vue";

const validatorRef = useTemplateRef<InstanceType<typeof NForm>>('validatorRef')
const advancedSearchOpen = ref<boolean>(false)

const props = withDefaults(defineProps<{
  label: string
  searchButtonLabel: string
  advancedSearchLabel: string
  withButton: boolean
  withIcon: boolean
  showTitle: boolean
  showAdvancedSearch?: boolean
}>(), {
  label : "SearchFilter.searchlabel",
  searchButtonLabel: "component.common.search.search-button",
  advancedSearchLabel: "SearchFilter.advancedSearchLabel",
  withButton: true,
  withIcon: true,
  showTitle: true,
  showAdvancedSearch: false,
})

const emit = defineEmits<{
  toggleAdvancedSearch: [],
  search: []
}>()

 function toggleAdvancedSearch($event) {
    advancedSearchOpen.value = !advancedSearchOpen.value;
    emit("toggleAdvancedSearch",$event);
  }

  function validateAndSearch($event) {
    validatorRef.value.validate().then((isValid) => {
      if (isValid) {
        emit("search", $event);
      }
    });
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


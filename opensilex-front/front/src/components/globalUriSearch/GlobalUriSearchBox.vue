<template>
  <div>
    <div class="header-container">
      <div>
        <div class="input-group">
          <opensilex-StringFilter
            v-model:filter="uriSearchValue"
            placeholder="component.header.uri-search-placeholder"
            class="searchFilter"
            @handlingEnterKey="launchUriGlobalSearch"
          />
          <button class="btn greenThemeColor" @click="launchUriGlobalSearch">
            <i class="bi bi-search"></i>
          </button>
        </div>
      </div>
      <opensilex-Button
        :label="t('component.common.close')"
        icon="bi-x"
        class="closeResultBox"
        :small="true"
        @click="$emit('hideUriSearch')"
      />
    </div>

    <br />

    <!-- Résultats -->
    <div v-if="uriSearchResultVisible">
      <opensilex-GlobalUriSearchResult
        v-if="resultsFound"
        :searchResult="uriSearchResult"
        @hideUriSearch="$emit('hideUriSearch')"
      />
      <div v-else class="no-results">
        {{ t('GlobalUriSearchBox.noResultsMessage') }}
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, inject } from "vue";
import { useI18n } from "vue-i18n";
import { URIGlobalSearchDTO } from "opensilex-core/index";
import { UriSearchService } from "opensilex-core/api/uriSearch.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

const { t } = useI18n();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");

const uriSearchResult = ref<URIGlobalSearchDTO | null>(null);
const uriSearchValue = ref<string>("");
const uriSearchService = ref<UriSearchService>();
const uriSearchResultVisible = ref<boolean>(false);

onMounted(() => {
  uriSearchService.value = $opensilex.getService("opensilex.UriSearchService");
});

const resultsFound = computed(() => !!uriSearchResult.value);

function launchUriGlobalSearch() {
    console.log("sservice : ", uriSearchService.value)
    console.log("uriSearchVALUE ", uriSearchValue.value)
  uriSearchService.value.searchByUri(uriSearchValue.value)
    .then((res) => {
        console.log("response launchUriGlobalSearch ")
      uriSearchResult.value = res.response.result;
      uriSearchResultVisible.value = true;
    })
    .catch((err) => {
        console.log("error launchUriGlobalSearch ", err)
      uriSearchResult.value = null;
      uriSearchResultVisible.value = true;
    });
}
</script>

<style scoped lang="scss">
.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-group .btn {
  height: 100%;
  padding-top: 0.6rem;
  padding-bottom: 0.6rem;
}

.closeResultBox {
  border: none;
  margin-top: -10px;
  font-size: 1.5em !important;
  color: rgba(101, 101, 101, 0.5);
  font-weight: bolder;
  cursor: pointer;
  background: none;
}

.no-results {
  font-style: italic;
  font-size: 1.3em;
  text-align: center;
  padding: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #666666;
}
</style>

<i18n>
en:
  GlobalUriSearchBox:
    noResultsMessage: Not found

fr:
  GlobalUriSearchBox:
    noResultsMessage: Pas trouvé
</i18n>

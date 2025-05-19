<!--
  - ******************************************************************************
  -                         NbElementPerPageSelector.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 04/12/2024 09:00
  - Contact: sebastien.prado@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  - ******************************************************************************
  -->
<template>
  <span class="numberOfElementsPerPageSelector">
    <select
        v-model="itemsPerPage"
        @change="onItemsPerPageChange"
        :title="$t('component.common.list.pagination.numberOfElementsPerPageSelector')"
    >
      <option value="10">{{$t('component.common.list.pagination.tenElements')}}</option>
      <option value="20">{{$t('component.common.list.pagination.twentyElements')}}</option>
      <option value="50">{{$t('component.common.list.pagination.fiftyElements')}}</option>
      <option value="100">{{$t('component.common.list.pagination.hundredElements')}}</option>
    </select>
  </span>
</template>

<script setup lang="ts">
import { ref, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin"

const $opensilex = inject<OpenSilexVuePlugin>('opensilex');

const itemsPerPage = ref("20")

const emit = defineEmits(['change'])

// Méthode de changement du nombre d'éléments par page
function onItemsPerPageChange(): void {
  // Stockage local
  localStorage.setItem("numberOfElements", itemsPerPage.value)
  
  // Mise à jour du paramètre d'URL
  $opensilex?.updateURLParameter("page_size", itemsPerPage.value, 20)
  emit('change', itemsPerPage.value)
}

onMounted(() => {
  // Récupération de la valeur depuis le stockage local si existante
  const storedNumberOfElements = localStorage.getItem("numberOfElements")
  if (storedNumberOfElements) {
    itemsPerPage.value = storedNumberOfElements
  }
  onItemsPerPageChange()
})
</script>

<style scoped lang="scss">
.numberOfElementsPerPageSelector {
  padding-left: 10px;
  margin-left: auto;
  margin-right: 10px;

  select {
    color: #00A38D;
    background-color: #fff;
    border: 1px solid #ccc;
    border-radius: 4px;
    padding: 5px;
    width: 100px;
    font-size: 13px;
    cursor: pointer;

    &::-ms-expand {
      color: #fff; /* Arrow color for IE/Edge */
    }

    &:hover {
      border-color: #00A38D;
    }

    option {
      background-color: #fff;
      color: #00A38D;
      font-weight: bold;

      &:checked {
        background-color: #00A38D;
        color: #fff;
      }
    }
  }
}
</style>
<!--
  - ******************************************************************************
  -                         NbElementPerPageSelector.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 04/12/2024 09:00
  - Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
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

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class NbElementPerPageSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;

  private itemsPerPage: string = "20";

  created() {
    if (localStorage.getItem("numberOfElements") !== null) {
      this.itemsPerPage = localStorage.getItem("numberOfElements");
    }
    this.onItemsPerPageChange()
  }

  private onItemsPerPageChange(): void {
    localStorage.setItem("numberOfElements", this.itemsPerPage);
    this.$opensilex.updateURLParameter("page_size", this.itemsPerPage, 20);
    this.emitChange();
  }

  private emitChange(): void {
    this.$emit('change', this.itemsPerPage);
  }

}
</script>

<style scoped lang="scss">
.numberOfElementsPerPageSelector{
  padding-left: 10px;
  margin-left: auto;
  margin-right: 10px;
}

.numberOfElementsPerPageSelector select {
  color: #00A38D;
  background-color: #fff;
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 5px;
  width: 100px;
  font-size: 13px;
  cursor: pointer;
}

.numberOfElementsPerPageSelector select::-ms-expand {
  color: #fff; /* Arrow color for IE/Edge */
}

.numberOfElementsPerPageSelector select:hover {
  border-color: #00A38D;
}

.numberOfElementsPerPageSelector select option{
  background-color: #fff;
  color: #00A38D;
  font-weight: bold;
}

.numberOfElementsPerPageSelector select option:checked {
  background-color: #00A38D;
  color: #fff;
}
</style>
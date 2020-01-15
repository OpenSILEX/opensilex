<template>
  <div>
    <b-button
      :class="showSearchComponent ? null : 'collapsed'"
      :aria-expanded="showSearchComponent ? 'true' : 'false'"
      aria-controls="collapse-1"
      @click="onSearchButtonClick"
      variant="primary"
    >
      <font-awesome-icon icon="sliders-h" size="sm" />Search
    </b-button>

    <b-collapse id="collapse-1" v-model="showSearchComponent" class="mt-2">
      <phis2ws-ImageSearch @onSearchFormSubmit="onSearchFormSubmit"></phis2ws-ImageSearch>
    </b-collapse>

    <b-collapse id="collapse-2" v-model="showImageListComponent" class="mt-2">
      <div v-if="totalImages>0">
        <p>Total Image {{totalImages}}</p>
        <p class="mt-3">Current Page: {{ currentPage }}</p>
        <phis2ws-ImageGrid :images="images" ref="imageGrid"></phis2ws-ImageGrid>
        <b-pagination
          v-model="currentPage"
          :total-rows="totalImages"
          :per-page="pageSize"
          @change="loadDataWithDelay()"
        ></b-pagination>
      </div>
      <div v-else>No images to dispay</div>
    </b-collapse>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { DataService } from "../../lib/api/data.service";
import { FileDescriptionDTO } from "../../lib/model/fileDescriptionDTO";

import VueRouter from "vue-router";
@Component
export default class ImageView extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }

  showSearchComponent: boolean = true;
  showImageListComponent: boolean = false;

  images = [];

  currentPage: number = 1;
  pageSize = 3;
  totalImages = 0;
  private searchImagesFields: any = {
    rdfType: undefined,
    startDate: undefined,
    endDate: undefined,
    provenance: undefined,
    concernedItems: undefined,
    jsonValueFilter: undefined,
    orderByDate: true
  };
  set searchObject(value: any) {
    this.searchImagesFields = value;
  }
  get searchObject() {
    return this.searchImagesFields;
  }

  onSearchFormSubmit(form) {
    console.log(form);
    this.showSearchComponent = false;
    this.showImageListComponent = true;
    this.searchImagesFields.rdfType = form.rdfType;
    if (form.soUri) {
      this.searchImagesFields.concernedItems = [];
      this.searchImagesFields.concernedItems.push(form.soUri);
    }
    if (form.startDate) {
      const date = form.startDate;
      console.log(this.format(date));
      this.searchImagesFields.startDate = this.format(date);
    }
    if (form.endDate) {
      const date = form.endDate;
      console.log(this.format(date));
      this.searchImagesFields.endDate = this.format(date);
    }
    this.loadData();
  }

  format(date) {
    var day = ("0" + date.getDate()).slice(-2);
    var month = ("0" + (date.getMonth() + 1)).slice(-2);
    var year = date.getFullYear();
    return year + "-" + month + "-" + day;
  }
  onSearchButtonClick() {
    this.showSearchComponent = !this.showSearchComponent;
    this.showImageListComponent = !this.showImageListComponent;
  }
  loadDataWithDelay() {
    setTimeout(() => {
      this.loadData();
    }, 0);
  }

  loadData() {
    this.images = [];
    let dataService: DataService = this.$opensilex.getService(
      "opensilex.DataService"
    );
    if (this.searchImagesFields.rdfType != undefined) {
      dataService
        .getDataFileDescriptionsBySearch(
          this.user.getAuthorizationHeader(),
          this.searchImagesFields.rdfType,
          this.searchImagesFields.startDate,
          this.searchImagesFields.endDate,
          this.searchImagesFields.provenance,
          this.searchImagesFields.concernedItems,
          this.searchImagesFields.jsonValueFilter,
          this.searchImagesFields.orderByDate,
          this.pageSize,
          this.currentPage - 1
        )
        .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<FileDescriptionDTO>>>
          ) => {
            this.searchImagesFields.concernedItems = undefined;
            this.totalImages = http.response.metadata.pagination.totalCount;
            this.pageSize = http.response.metadata.pagination.pageSize;
            const res = http.response.result as any;
            const data = res.data;
            this.images = data;
          }
        )
        .catch(error => {
          this.searchImagesFields.concernedItems = undefined;
          this.totalImages = 0;
          console.log(error);
        });
    }
  }
}
</script>

<style scoped lang="scss">
</style>


<template>
  <div>
    <b-button
      :class="showSearchComponent ? null : 'collapsed'"
      :aria-expanded="showSearchComponent ? 'true' : 'false'"
      aria-controls="collapse-1"
      @click="onSearchButtonClick"
      variant="primary"
    >Image Search</b-button>

    <b-collapse id="collapse-1" v-model="showSearchComponent" class="mt-2">
      <phis2ws-ImageSearch @onSearchFormSubmit="onSearchFormSubmit"></phis2ws-ImageSearch>
    </b-collapse>

    <b-collapse id="collapse-2" v-model="showImageListComponent" class="mt-2">
      <phis2ws-ImageGrid :images="images" ref="imageGrid"></phis2ws-ImageGrid>
    </b-collapse>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { DataService } from "../../lib/api/data.service";
import { FileDescriptionDTO } from "../../lib/model/fileDescriptionDTO";

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

  currentPage: number = 0;
  pageSize = 800;
  totalImages = 0;
  private searchImagesFields: any = {
    rdfType: undefined,
    startDate: undefined,
    endDate: undefined,
    provenance: undefined,
    concernedItems: [],
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
    this.showSearchComponent = false;
    this.showImageListComponent = true;
    this.searchImagesFields.rdfType = form.rdfType;
    this.loadData();
  }

  onSearchButtonClick() {
    this.showSearchComponent = !this.showSearchComponent;
    this.showImageListComponent = !this.showImageListComponent;
  }

  loadData() {
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
          this.currentPage
        )
        .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<FileDescriptionDTO>>>
          ) => {
            this.totalImages = http.response.metadata.pagination.totalCount;
            this.pageSize = http.response.metadata.pagination.pageSize;
            const res = http.response.result as any;
            const data = res.data;
               var promises = [];
            var promise;
            var vm = this;
            // data.forEach(function(value, i) {
            //   promise = dataService.getDataFile(data[i].uri).catch(error => { return error });
            //   promises.push(promise);
            // });
            // Promise.all(promises).then(function(values) {
            //   console.log(values);
            // });
            this.images = data;
          }
        )
        .catch(this.$opensilex.errorHandler);
    }
  }
}
</script>

<style scoped lang="scss">
</style>


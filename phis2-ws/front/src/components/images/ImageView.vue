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
      <phis2ws-ImageSearch @onSearchFormSubmit="onSearchFormSubmit" :form="searchImagesFields"></phis2ws-ImageSearch>
    </b-collapse>

    <div v-if="totalImages>0">
      <p>Total Image {{totalImages}}</p>
      <phis2ws-ImageGrid :images="images" ref="imageGrid"></phis2ws-ImageGrid>

      <b-pagination
        v-model="currentPage"
        :total-rows="totalImages"
        :per-page="pageSize"
        @change="loadDataWithDelay"
      ></b-pagination>
    </div>
    <div v-else>No images to dispay</div>
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
  $router: VueRouter;
  get user() {
    return this.$store.state.user;
  }

  showSearchComponent: boolean = true;

  images = [];

  currentPage: number = 1;
  pageSize = 20;
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
  created() {
    let query: any = this.$route.query;
    if (query.pageSize) {
      this.pageSize = parseInt(query.pageSize);
    }
    if (query.currentPage) {
      this.currentPage = parseInt(query.currentPage);
    }
    if (query.concernedItems) {
      this.searchImagesFields.concernedItems = query.concernedItems;
    }
    if (query.startDate) {
      this.searchImagesFields.startDate = query.startDate;
    }
    if (query.endDate) {
      this.searchImagesFields.endDate = query.endDate;
    }
    if (query.rdfType) {
      this.searchImagesFields.rdfType = query.rdfType;
      this.showImage();
      this.loadData();
    }
  }

  showImage() {
    this.showSearchComponent = false;
  }

  onSearchFormSubmit(form) {
    console.log("form");
    console.log(form);
    this.currentPage = 1;
    this.showImage();
    this.searchImagesFields.rdfType = form.rdfType;
    this.$router
      .push({
        path: this.$route.fullPath,
        query: {
          rdfType: encodeURI(this.searchImagesFields.rdfType)
        }
      })
      .catch(function() {});
    this.searchImagesFields.concernedItems = form.concernedItems;
    this.$router
      .push({
        path: this.$route.fullPath,
        query: {
          concernedItems: this.searchImagesFields.concernedItems
        }
      })
      .catch(function() {});
    if (form.startDate) {
      this.searchImagesFields.startDate = this.format(form.startDate);
      this.$router
        .push({
          path: this.$route.fullPath,
          query: {
            startDate: this.searchImagesFields.startDate
          }
        })
        .catch(function() {});
    }
    if (form.endDate) {
      this.searchImagesFields.endDate = this.format(form.endDate);
      this.$router
        .push({
          path: this.$route.fullPath,
          query: {
            endDate: this.searchImagesFields.endDate
          }
        })
        .catch(function() {});
    }

    this.loadData();
  }

  format(date) {
    var d = new Date(date),
      month = "" + (d.getMonth() + 1),
      day = "" + d.getDate(),
      year = d.getFullYear();

    if (month.length < 2) month = "0" + month;
    if (day.length < 2) day = "0" + day;

    return [year, month, day].join("-");
  }
  onSearchButtonClick() {
    this.showSearchComponent = !this.showSearchComponent;
  }
  loadDataWithDelay() {
    // Fix: on Pagination component: currentPage is changed after the change event call
    setTimeout(() => {
      this.loadData();
    }, 0);
  }

  loadData() {
    this.images = [];
    var concernedItems = undefined;
    let dataService: DataService = this.$opensilex.getService(
      "opensilex.DataService"
    );
    if (this.searchImagesFields.concernedItems) {
      concernedItems = [];
      concernedItems.push(this.searchImagesFields.concernedItems);
    }

    if (this.searchImagesFields.rdfType != undefined) {
      dataService
        .getDataFileDescriptionsBySearch(
          this.user.getAuthorizationHeader(),
          this.searchImagesFields.rdfType,
          this.searchImagesFields.startDate,
          this.searchImagesFields.endDate,
          this.searchImagesFields.provenance,
          concernedItems,
          this.searchImagesFields.jsonValueFilter,
          this.searchImagesFields.orderByDate,
          this.pageSize,
          this.currentPage - 1
        )
        .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<FileDescriptionDTO>>>
          ) => {
            this.totalImages = http.response.metadata.pagination.totalCount;
            this.pageSize = http.response.metadata.pagination.pageSize;
            const res = http.response.result as any;
            const data = res.data;
            this.images = data;

            this.$router
              .push({
                path: this.$route.fullPath,
                query: {
                  currentPage: "" + this.currentPage
                }
              })
              .catch(function() {});
          }
        )
        .catch(error => {
          this.totalImages = 0;
          console.log(error);
        });
    }
  }
}
</script>

<style scoped lang="scss">
</style>


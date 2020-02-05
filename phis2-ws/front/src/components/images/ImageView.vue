<template>
  <div>
    <b-button
      :class="showSearchComponent ? null : 'collapsed'"
      :aria-expanded="showSearchComponent ? 'true' : 'false'"
      aria-controls="collapse-1"
      @click="onSearchButtonClick"
      variant="primary"
    >
      <font-awesome-icon icon="search" size="sm" />
    </b-button>

    <b-collapse id="collapse-1" v-model="showSearchComponent" class="mt-2">
      <phis2ws-ImageSearch @onSearchFormSubmit="onSearchFormSubmit"></phis2ws-ImageSearch>
    </b-collapse>

    <b-collapse id="collapse-2" v-model="showImageComponent" class="mt-2">
      <div v-if="totalImages>0">
        <p>Total Image {{totalImages}}</p>
        <phis2ws-ImageList :images="images"></phis2ws-ImageList>

        <b-pagination
          v-model="currentPage"
          :total-rows="totalImages"
          :per-page="pageSize"
          @change="loadDataWithDelay"
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
import { Image } from "./image";

@Component
export default class ImageView extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  get user() {
    return this.$store.state.user;
  }

  showSearchComponent: boolean = true;
  showImageComponent: boolean = false;
  images = [];

  currentPage: number = 1;
  pageSize = 20;
  totalImages = 0;
  private searchImagesFields: any = {
    rdfType: undefined,
    startDate: undefined,
    endDate: undefined,
    provenance: undefined,
    jsonValueFilter: undefined,
    orderByDate: true,
    concernedItemsValue: [],
    objectType: null,
    experiment: null
  };

  created() {
    let query: any = this.$route.query;
    if (query.pageSize) {
      this.pageSize = parseInt(query.pageSize);
    }
    if (query.currentPage) {
      this.currentPage = parseInt(query.currentPage);
    }
  }

  showImage() {
    this.showSearchComponent = false;
    this.showImageComponent = true;
  }

  onSearchFormSubmit(form) {
    if (form.rdfType !== null) {
      this.currentPage = 1;
      this.searchImagesFields.experiment = form.experiment;
      this.searchImagesFields.objectType = form.objectType;
      this.searchImagesFields.rdfType = form.rdfType;
      this.searchImagesFields.startDate = form.startDate;
      this.searchImagesFields.endDate = form.endDate;
      this.searchImagesFields.concernedItemsValue = [];
      if (form.objectList) {
        form.objectList.forEach(element => {
          this.searchImagesFields.concernedItemsValue.push(element);
        });
      }

      this.loadData();
    }
  }

  loadDataWithDelay() {
    // Fix: on Pagination component: currentPage is changed after the change event call
    setTimeout(() => {
      this.loadData();
    }, 0);
  }

  loadData() {
    this.images = [];
    let dataService: DataService = this.$opensilex.getService(
      "opensilex.DataService"
    );

    if (
      (this.searchImagesFields.objectType !== null ||
        this.searchImagesFields.experiment !== null) &&
      this.searchImagesFields.concernedItemsValue.length === 0
    ) {
      this.images = [];
      this.totalImages = 0;
      this.showImage();
    } else {
      dataService
        .getDataFileDescriptionsBySearch(
          this.user.getAuthorizationHeader(),
          this.searchImagesFields.rdfType,
          this.searchImagesFields.startDate,
          this.searchImagesFields.endDate,
          this.searchImagesFields.provenance,
          this.searchImagesFields.concernedItemsValue,
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
            const data = res.data as Array<FileDescriptionDTO>;
            console.log("data");
            console.log(data);

            //this.images = data; //before filter
            this.imagesFilter(data);

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
          this.images = [];
          console.log(error);
          this.showImage();
        });
    }
  }

  imagesFilter(data: Array<FileDescriptionDTO>) {
    //pour chaque concerneditem une nouvelle image ..
    const images = [];
    if (this.searchImagesFields.objectType !== null) {
      data.forEach(element => {
        element.concernedItems.forEach(concernedItem => {
          if (this.searchImagesFields.objectType === concernedItem.typeURI) {
            const image = <Image>{};
            image.objectType = concernedItem.typeURI;
            image.uri =this.$opensilex.getBaseAPI()+"/data/file/"+encodeURIComponent(element.uri);
            image.type = element.rdfType;
            image.objectUri = concernedItem.uri;
            image.date = element.date;
            image.provenanceUri = element.provenanceUri;
            images.push(image);
          }
        });
      });
    } else {
      data.forEach(element => {
        element.concernedItems.forEach(concernedItem => {
          const image = <Image>{};
          image.objectType = concernedItem.typeURI;
          image.uri =this.$opensilex.getBaseAPI() +"/data/file/" +encodeURIComponent(element.uri);
          image.type = element.rdfType;
          image.objectUri = concernedItem.uri;
          image.date = element.date;
          image.provenanceUri = element.provenanceUri;
          images.push(image);
        });
      });
    }

    this.images = images;
    this.showImage();
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
    this.showImageComponent = !this.showImageComponent;
  }
}
</script>

<style scoped lang="scss">
</style>


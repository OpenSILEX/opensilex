<template>
    <div id="metadata">
      <span v-if="!publicationDate && !lastUpdatedDate">
        {{ onlyPublisher }}
      </span>
      <span v-else-if="publicationDate && !lastUpdatedDate">
        {{ withoutUpdate}}
      </span>
      <span v-else-if="!publicationDate && lastUpdatedDate">
        {{ withoutPublication}}
      </span>
      <span v-else>
        {{ completeMetadata}}
      </span>
    </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import { UserGetDTO } from "../../../../../../opensilex-security/front/src/lib";

@Component
export default class MetadataView extends Vue {
  $i18n: any;
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  publisher: UserGetDTO;

  @Prop()
  publicationDate: string;

  @Prop()
  lastUpdatedDate: string;

  get completeMetadata() {
    return this.$t("MetadataView.complete-sentence", {
      datePublication: this.$opensilex.$dateTimeFormatter.formatLocaleDate(this.publicationDate, {timeStyle: 'medium'}),
      publisher: this.publisher.first_name && this.publisher.last_name ? this.publisher.first_name + " " + this.publisher.last_name : this.publisher.uri,
      lastUpdateDate: this.$opensilex.$dateTimeFormatter.formatLocaleDate(this.lastUpdatedDate, {timeStyle: 'medium'})
    });
  }

  get withoutPublication() {
    return this.$t("MetadataView.without-publication-with-update-sentence", {
      publisher: this.publisher.first_name && this.publisher.last_name ? this.publisher.first_name + " " + this.publisher.last_name : this.publisher.uri,
      lastUpdateDate: this.$opensilex.$dateTimeFormatter.formatLocaleDate(this.lastUpdatedDate, {timeStyle: 'medium'})
    });
  }
  
  get withoutUpdate() {
    return this.$t("MetadataView.without-update-with-publication-sentence", {
      datePublication: this.$opensilex.$dateTimeFormatter.formatLocaleDate(this.publicationDate, {timeStyle: 'medium'}),
      publisher: this.publisher.first_name && this.publisher.last_name ? this.publisher.first_name + " " + this.publisher.last_name : this.publisher.uri
    });
  }

  get onlyPublisher() {
    return this.$t("MetadataView.only-publisher-sentence", {
      publisher: this.publisher.first_name && this.publisher.last_name ? this.publisher.first_name + " " + this.publisher.last_name : this.publisher.uri
    });
  }
}
</script>

<style scoped lang="scss">

#metadata {
  color: #717b8e;
  font-style: italic;
}

</style>

<i18n>
  en:
    MetadataView:
      complete-sentence: "Published on {datePublication} by {publisher}, updated on {lastUpdateDate}"
      without-publication-with-update-sentence: "Published by {publisher}, updated on {lastUpdateDate}"
      without-update-with-publication-sentence: "Published on {datePublication} by {publisher}"
      only-publisher-sentence: "Published by {publisher}"
  fr:
    MetadataView:
      complete-sentence: "Publié le {datePublication} par {publisher}, modifié le {lastUpdateDate}"
      without-publication-with-update-sentence: "Publié par {publisher}, modifié le {lastUpdateDate}"
      without-update-with-publication-sentence: "Publié le {datePublication} par {publisher}"
      only-publisher-sentence: "Publié par {publisher}"
</i18n>
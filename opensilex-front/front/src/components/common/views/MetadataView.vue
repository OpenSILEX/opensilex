<template>
    <div id="metadata">
      <span v-if="lastUpdatedDate">
        {{ completeMetadata }}
      </span>
      <span v-else>
        {{ withoutUpdate}}
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

  get withoutUpdate() {
    return this.$t("MetadataView.without-update-sentence", {
      datePublication: this.$opensilex.$dateTimeFormatter.formatLocaleDate(this.publicationDate, {timeStyle: 'medium'}),
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
      without-update-sentence: "Published on {datePublication} by {publisher}."
  fr:
    MetadataView:
      complete-sentence: "Publié le {datePublication} par {publisher}, modifié le {lastUpdateDate}"
      without-update-sentence: "Publié le {datePublication} par {publisher}."
</i18n>
<template>
  <div id="metadata">
    <span v-if="!publicationDate && !lastUpdatedDate">
      {{ onlyPublisher }}
    </span>
    <span v-else-if="publicationDate && !lastUpdatedDate">
      {{ withoutUpdate }}
    </span>
    <span v-else-if="!publicationDate && lastUpdatedDate">
      {{ withoutPublication }}
    </span>
    <span v-else>
      {{ completeMetadata }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { inject, computed } from "vue";
import { useI18n , Composer} from "vue-i18n";
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import type { UserGetDTO } from "opensilex-security";
import DateTimeFormatter from "./../../../models/DateTimeFormatter";

const { locale, t } = useI18n();
const formatter = new DateTimeFormatter({ locale, t } as Composer);

// Props
const props = defineProps<{
  publisher: UserGetDTO;
  publicationDate?: string;
  lastUpdatedDate?: string;
}>();

// i18n & plugin

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");

// Format helper
const getName = (publisher: UserGetDTO) =>
  publisher.first_name && publisher.last_name
    ? `${publisher.first_name} ${publisher.last_name}`
    : publisher.uri;

// Computed metadata
const completeMetadata = computed(() =>
  t("MetadataView.complete-sentence", {
    datePublication: $opensilex?.$dateTimeFormatter.formatLocaleDate(props.publicationDate, { timeStyle: "medium" }),
    publisher: getName(props.publisher),
    lastUpdateDate: $opensilex?.$dateTimeFormatter.formatLocaleDate(props.lastUpdatedDate, { timeStyle: "medium" })
  })
);

const withoutPublication = computed(() =>
  t("MetadataView.without-publication-with-update-sentence", {
    publisher: getName(props.publisher),
    lastUpdateDate: $opensilex?.$dateTimeFormatter.formatLocaleDate(props.lastUpdatedDate, { timeStyle: "medium" })
  })
);

const withoutUpdate = computed(() =>
  t("MetadataView.without-update-with-publication-sentence", {
    datePublication: $opensilex?.$dateTimeFormatter.formatLocaleDate(props.publicationDate, { timeStyle: "medium" }),
    publisher: getName(props.publisher)
  })
);

const onlyPublisher = computed(() =>
  t("MetadataView.only-publisher-sentence", {
    publisher: getName(props.publisher)
  })
);
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

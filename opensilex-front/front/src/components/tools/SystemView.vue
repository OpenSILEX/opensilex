<template>
  <div class="container-fluid">
    <PageContent>
      <Card icon="bi-info-circle" :label="t('component.system.details')">
        <template #body>
            <h4>{{ t("component.system.info") }} </h4>

            <div class="row">
              <div class="col-12 col-lg-4">
                <StringView
                    :label="t('component.system.title')"
                    :value="versionInfo.title"
                />

                <UriView
                    :title="t('component.system.version')"
                    :uri="getVersion()"
                    :value="versionInfo.version"
                    target="_blank"
                />

                <UriView
                    :key="`api-docs-${locale}`"
                    :title="t('component.system.api-docs')"
                    :uri="versionInfo.api_docs?.url"
                    target="_blank"
                />


                <StringView
                    :label="t('component.system.git-commit')"
                    :copyValue="versionInfo.git_commit?.commit_id"
                    :value="versionInfo.git_commit?.commit_message"
                    :allowCopy="true"
                    :copyTextMessage="t('component.system.git-commit-copy')"
                />

                <StringView
                    :label="t('component.system.copyright')"
                    value=" © 2021 INRAE – Tous droits réservés "
                />
              </div>

              <div class="col-12 col-lg">
                <TextView
                    :label="t('component.system.description')"
                    :value="versionInfo.description"
                />

                <UriView
                    :key="`contact-${locale}`"
                    :title="t('component.system.contact')"
                    :uri="versionInfo.contact?.email"
                    :value="versionInfo.contact?.email"
                    :href="versionInfo.contact?.email ? `mailto:${versionInfo.contact.email}` : undefined"
                />

                <UriView
                    :key="`project-${locale}`"
                    :title="t('component.system.project')"
                    :uri="versionInfo.contact?.homepage"
                    value="OpenSILEX homepage"
                    target="_blank"
                />

                <LabelUriView
                    :label="t('component.system.license')"
                    :uri="versionInfo.license?.url"
                    :value="versionInfo.license?.name"
                    target="_blank"
                    :allowCopy="false"
                />
              </div>
            </div>

            <hr/>

            <h4>{{ t("component.system.loaded-modules") }}</h4>

            <TableView
                v-if="hasModules"
                :items="versionInfo.modules_version"
                :fields="modulesFields"
                :showCount="false"
                :withPagination="false"
                :fixedPageSize="30"
            />
        </template>
      </Card>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import {computed, inject} from "vue";
import {useI18n} from "vue-i18n";
import PageContent from "@/components/layout/PageContent.vue";
import StringView from "@/components/common/views/StringView.vue";
import UriView from "@/components/common/views/UriView.vue";
import TableView from "@/components/common/views/TableView.vue";
import LabelUriView from "@/components/common/views/LabelUriView.vue";
import TextView from "@/components/common/views/TextView.vue";
import Card from "@/components/common/views/Card.vue";
import {VersionInfoDTO} from "opensilex-core/model/versionInfoDTO";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";

const {t, locale} = useI18n();

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");

const versionInfo = computed<VersionInfoDTO>(() => $opensilex.versionInfo ?? {});

const modulesFields = computed(() => [
  {
    key: "name",
    label: t("component.system.name"),
    sortable: false,
  },
  {
    key: "version",
    label: t("component.system.version"),
    sortable: false,
  },
]);

const hasModules = computed(() => {
  return (
      versionInfo.value.modules_version !== undefined &&
      versionInfo.value.modules_version.length > 0
  );
});

function getVersion(): string | undefined {
  const version = versionInfo.value.version;
  const githubPage = versionInfo.value.github_page;

  if (!githubPage) {
    return undefined;
  }

  if (version !== undefined && version.includes("SNAPSHOT")) {
    return `${githubPage}/releases`;
  }

  return `${githubPage}/releases/tag/${version}`;
}
</script>
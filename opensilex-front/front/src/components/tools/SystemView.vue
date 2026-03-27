<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <n-card content-style="padding: 0;">
        <template #header>
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-info-circle system-card-icon" aria-hidden="true"></i>
            <span>{{ t("SystemView.details") }}</span>
          </div>
        </template>

        <div class="container-fluid py-3">
          <h4>{{ t("SystemView.info") }}</h4>

          <div class="row">
            <div class="col-12 col-lg-4">
              <opensilex-StringView
                :label="t('SystemView.title')"
                :value="versionInfo.title"
              />

              <opensilex-UriView
                :title="t('SystemView.version')"
                :uri="getVersion()"
                :value="versionInfo.version"
                target="_blank"
              />

              <opensilex-UriView
                :key="`api-docs-${locale}`"
                :title="t('SystemView.api-docs')"
                :uri="versionInfo.api_docs?.url"
                target="_blank"
              />
              

              <opensilex-StringView
                :label="t('SystemView.git-commit')"
                :copyValue="versionInfo.git_commit?.commit_id"
                :value="versionInfo.git_commit?.commit_message"
                :allowCopy="true"
                :copyTextMessage="t('SystemView.git-commit-copy')"
              />

              <opensilex-StringView
                :label="t('SystemView.copyright')"
                value=" © 2021 INRAE – Tous droits réservés "
              />
            </div>

            <div class="col-12 col-lg">
              <opensilex-TextView
                :label="t('SystemView.description')"
                :value="versionInfo.description"
              />

              <opensilex-UriView
                :key="`contact-${locale}`"
                :title="t('SystemView.contact')"
                :uri="versionInfo.contact?.email"
                :value="versionInfo.contact?.email"
                :href="versionInfo.contact?.email ? `mailto:${versionInfo.contact.email}` : undefined"
              />

              <opensilex-UriView
                :key="`project-${locale}`"
                :title="t('SystemView.project')"
                :uri="versionInfo.contact?.homepage"
                value="OpenSILEX homepage"
                target="_blank"
              />

              <opensilex-LabelUriView
                :label="t('SystemView.license')"
                :uri="versionInfo.license?.url"
                :value="versionInfo.license?.name"
                target="_blank"
                :allowCopy="false"
              />
            </div>
          </div>

          <hr />

          <h4>{{ t("SystemView.loaded-modules") }}</h4>

            <opensilex-TableView
            v-if="hasModules"
            :items="versionInfo.modules_version"
            :fields="modulesFields"
            :showCount="false"
            :withPagination="false"
            :fixedPageSize="30"
            />
        </div>
      </n-card>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance } from "vue";
import { NCard } from "naive-ui";
import { useI18n } from "vue-i18n";
// @ts-ignore
import type { versionInfoDTO } from "opensilex-core/index";

const { t, locale } = useI18n();

const instance = getCurrentInstance();
const $opensilex = instance?.appContext.config.globalProperties.$opensilex;

const versionInfo = computed<versionInfoDTO>(() => {
  return $opensilex?.versionInfo ?? {};
});

const modulesFields = computed(() => [
  {
    key: "name",
    label: t("SystemView.name"),
    sortable: false,
  },
  {
    key: "version",
    label: t("SystemView.version"),
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

<style scoped lang="scss">
.system-card-icon {
  font-size: 1.25rem;
  line-height: 1;
}
</style>

<i18n>
en:
  SystemView:
    title: System
    details: System details
    info: Informations
    version: Version
    project: Project homepage
    contact: Contact e-mail
    api-docs: Web API
    git-commit: Last commit Id
    git-commit-copy: Copy last commit Id
    description: Description
    title-description: Informations about information system
    license: Software license
    loaded-modules: Loaded modules
    copyright: Copyright
    name: Name

fr:
  SystemView:
    title: Système
    details: Détails du système
    info: Informations
    version: Version
    project: Page du projet
    contact: E-mail du contact
    api-docs: API Web
    git-commit: Dernier id commit
    git-commit-copy: Copier le dernier id commit
    description: Description
    title-description: Informations à propos du système d'information
    license: Licence logicielle
    loaded-modules: Modules chargés
    copyright: Copyright
    name: Nom
</i18n>
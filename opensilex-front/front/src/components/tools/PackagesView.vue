<template>
  <div>
    <opensilex-PageContent>
      <n-card content-style="padding: 0;">
        <template #header>
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-github package-card-icon" aria-hidden="true"></i>
            <span>{{ t("PackagesView.details", { version: versionInfo.version }) }}</span>
          </div>
        </template>

        <div class="container-fluid">
          <div class="row align-items-center py-2 border-top">
            <div class="col-2 col-md-1">
              <img
                src="https://www.python.org/static/community_logos/python-logo-generic.svg"
                alt="Python logo"
                class="package-logo"
              />
            </div>
            <div class="col-4 col-md-3 fw-bold">
              {{ t("PackagesView.python-generated") }}
            </div>
            <div class="col">
              <opensilex-UriView
                :title="t('PackagesView.python-generated')"
                :uri="pythonVersion"
                :value="`Version ${versionInfo.version ?? ''}`"
                target="_blank"
              />
            </div>
          </div>

          <div class="row align-items-center py-2">
            <div class="col-2 col-md-1">
              <img
                src="https://www.r-project.org/logo/Rlogo.svg"
                alt="R logo"
                class="package-logo"
              />
            </div>
            <div class="col-4 col-md-3 fw-bold">
              {{ t("PackagesView.r-generated") }}
            </div>
            <div class="col">
              <opensilex-UriView
                :title="t('PackagesView.r-generated')"
                :uri="rVersion"
                :value="`Version ${versionInfo.version ?? ''}`"
                target="_blank"
              />
            </div>
          </div>

          <div class="row align-items-center py-2">
            <div class="col-2 col-md-1">
              <img
                src="https://upload.wikimedia.org/wikipedia/commons/f/fe/Dart_programming_language_logo.svg"
                alt="Dart logo"
                class="package-logo"
              />
            </div>
            <div class="col-4 col-md-3 fw-bold">
              {{ t("PackagesView.dart-generated") }}
            </div>
            <div class="col">
              <opensilex-UriView
                :title="t('PackagesView.dart-generated')"
                :uri="dartVersion"
                :value="`Version ${versionInfo.version ?? ''}`"
                target="_blank"
              />
            </div>
          </div>
        </div>
      </n-card>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance } from "vue";
import { NCard } from "naive-ui";
// @ts-ignore
import type { VersionInfoDTO } from "opensilex-core/index";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

const instance = getCurrentInstance();
const $opensilex = instance?.appContext.config.globalProperties.$opensilex;

const versionInfo = computed<VersionInfoDTO>(() => {
  return $opensilex?.versionInfo ?? {};
});

const versionSuffix = "/tags";

const pythonVersion = computed(
  () => `https://github.com/OpenSILEX/opensilexClientToolsPython${versionSuffix}`
);

const rVersion = computed(
  () => `https://github.com/OpenSILEX/opensilexClientToolsR${versionSuffix}`
);

const dartVersion = computed(
  () => `https://github.com/OpenSILEX/opensilexClientToolsDart${versionSuffix}`
);
</script>

<style scoped lang="scss">
.package-card-icon {
  font-size: 1.25rem;
  line-height: 1;
}

.package-logo {
  display: block;
  max-height: 32px;
  max-width: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
}
</style>

<i18n>
en:
  PackagesView:
    title: Packages
    description: Set of packages that allows access to the webservice
    details: Packages compatible with version {version}
    python-generated: Python Package
    r-generated: R Package
    dart-generated: Dart Package
    version: Version
fr:
  PackagesView:
    title: Librairies
    description: Ensemble de bibliothèque logicielles qui permet l'accès au webservice
    details: Librairies compatibles avec la version {version}
    python-generated: Librairie Python
    r-generated: Librairie R
    dart-generated: Librairie Dart
    version: Version
</i18n>
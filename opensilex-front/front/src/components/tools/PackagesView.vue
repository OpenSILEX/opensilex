<template>
  <div>
    <opensilex-PageContent>
      <template v-slot>
        <h3></h3>
        <opensilex-Card
          icon="ik#ik-github"
          :label="
            $t('PackagesView.details', {
              version: versionInfo.version,
            })
          "
        >
          <template v-slot:body>
            <b-row>
              <b-col cols="3">
                <b-img
                  height="30"
                  src="https://www.python.org/static/community_logos/python-logo-generic.svg"
                ></b-img>
              </b-col>
              <b-col>
                <opensilex-UriView
                  title="PackagesView.python-generated"
                  :uri="pythonVersion"
                  :value="'Version ' + versionInfo.version"
                  target="_blank"
                ></opensilex-UriView>
              </b-col>
            </b-row>
            <b-row>
              <b-col cols="3">
                <b-img
                  height="25"
                  src="https://www.r-project.org/logo/Rlogo.svg"
                ></b-img>
              </b-col>
              <b-col>
                <opensilex-UriView
                  title="PackagesView.r-generated"
                  :uri="rVersion"
                  :value="'Version ' + versionInfo.version"
                  target="_blank"
                ></opensilex-UriView>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="3">
                <b-img
                  height="25"
                  src="https://upload.wikimedia.org/wikipedia/commons/f/fe/Dart_programming_language_logo.svg"
                ></b-img>
              </b-col>
              <b-col>
                <opensilex-UriView
                  title="PackagesView.dart-generated"
                  :uri="dartVersion"
                  :value="'Version ' + versionInfo.version"
                  target="_blank"
                ></opensilex-UriView>
              </b-col>
            </b-row>
          </template>
        </opensilex-Card>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { VersionInfoDTO } from "opensilex-core/index";

@Component
export default class PackagesView extends Vue {
  $opensilex: any;
  versionInfo: VersionInfoDTO = {};

  getVersion() {
    return "/tags";
  }

  get pythonVersion() {
    return (
      "https://github.com/OpenSILEX/opensilexClientToolsPython" +
      this.getVersion()
    );
  }

  get rVersion() {
    return (
      "https://github.com/OpenSILEX/opensilexClientToolsR" + this.getVersion()
    );
  }

  get dartVersion() {
    return (
      "https://github.com/OpenSILEX/opensilexClientToolsDart" +
      this.getVersion()
    );
  }

  created() {
    this.versionInfo = this.$opensilex.versionInfo;
  }
}
</script>

<style scoped lang="scss">
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

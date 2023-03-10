<template>
  <opensilex-Card :no-footer="true" icon="fa#newspaper" label="Twitter" class="twitterCard">

    <template v-slot:rightHeader>
      <opensilex-Button
        label="TwitterButtonLabel"
        href="https://twitter.com/OpenSilex"
        class="greenThemeColor"
      ></opensilex-Button>
    </template>

    <template v-slot:body>
      <a
        class="twitter-timeline"
        data-height="458"
        data-chrome="noheader nofooter noborders"
        data-dnt="true"
        href="https://twitter.com/OpenSilex"
      ></a>
    </template>

  </opensilex-Card>
</template>
<script lang="ts">

import { Component } from 'vue-property-decorator';
import Vue from 'vue';

@Component
export default class Twitter extends Vue {

  dataHeight;

  created() {
    let twitterFeed = document.createElement("script");
    twitterFeed.setAttribute("src", "https://platform.twitter.com/widgets.js");
    twitterFeed.setAttribute("charset", "utf-8");
    document.head.appendChild(twitterFeed);
  }
}
</script>

<style scoped lang="scss">
  @media (max-width: 1180px) {
    .twitterCard {
      max-height: 600px;
      overflow: hidden;
    }
  }
</style>

<i18n>
en:
  TwitterButtonLabel: Opensilex Twitter

fr:
  TwitterButtonLabel: Twitter OpenSilex
</i18n>

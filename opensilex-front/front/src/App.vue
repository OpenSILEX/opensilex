
<template>
  <!-- <div id="main-content"> -->
    <!-- <main class="main-content"> -->
      <section id="content-wrapper" class="page-wrap"  v-bind:class="{ 'hidden-menu': !menuVisible }" >
                    <div id="main-content">
              <main class="main-content">
      <opensilex-DefaultMenuComponent></opensilex-DefaultMenuComponent>
      <opensilex-DefaultHeaderComponent></opensilex-DefaultHeaderComponent>
      <router-view></router-view>
      </main>
      </div>
      </section>

<!-- <div v-if="this.$route.meta.public"> -->


          <component v-bind:is="headerComponent"></component>
 
          <!-- <section id="content-wrapper" class="page-wrap"  v-bind:class="{ 'hidden-menu': !menuVisible }" > -->
            <!-- <div id="main-content">
              <main class="main-content"> -->
                <!-- <router-view :key="$route.fullPath" /> -->
              <!-- </main> -->

            <!-- </div> -->
          <!-- </section> -->
              <!-- </main> -->
            <!-- </div> -->
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, inject } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';
import { Carousel, Dropdown} from "bootstrap";
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';

// Props definitions
const props = defineProps({
  embed: Boolean,
  headerComponent: [String, Object],
  loginComponent: [String, Object],
  menuComponent: [String, Object],
  footerComponent: [String, Object]
});

// Setup composition API variables
const route = useRoute();
const store = useStore();
const { t } = useI18n();
// const opensilex = inject('opensilex');
// const bvToast = inject('bvToast');

// Data properties
const notificationMessage = ref(undefined);
const notificationMessageDisplayed = ref("");
const notificationEndDate = ref("");
const notificationColorTheme = ref("");
const displayNotificationMessage = ref(false);
const uriSearchBoxVisible = ref(false);

// Computed properties
const lang = computed(() => store.state.lang);
const disconnected = computed(() => store.state.disconnected);
const user = computed(() => store.state.user);
const isLoaderVisible = computed(() => store.state.loaderVisible);
const menuVisible = computed(() => store.state.menuVisible);

// Computed for notification color class
const notificationColorClass = computed(() => {
  const theme = notificationColorTheme.value.toLowerCase();

  if (theme === 'information') {
    return 'notificationMessageInfoColor';
  } else if (theme === 'warning') {
    return 'notificationMessageWarningColor';
  } else {
    return 'notificationMessageDefaultColor';
  }
});

// Methods
const closeNotification = () => {
  displayNotificationMessage.value = false;
};

const handleUriGlobalSearchPressed = () => {
  toggleUriSearchBox();
};

const handleHideUriSearch = () => {
  toggleUriSearchBox(false);
};

const toggleUriSearchBox = (visible) => {
  uriSearchBoxVisible.value = visible !== undefined ? visible : !uriSearchBoxVisible.value;
};

// Lifecycle hooks
onMounted(() => {
  // Setup language watcher
  // watch(() => store.getters.language, () => {
  //   notificationMessageDisplayed.value = notificationMessage.value[opensilex.i18n.locale];
  // });
});

// Created lifecycle - initialize data
// opensilex.bvToast = bvToast;
const currentDate = new Date();
// const formattedCurrentDate = currentDate.toISOString().slice(0, 10);
// notificationMessage.value = opensilex.getConfig().notificationMessage;
// notificationEndDate.value = opensilex.getConfig().notificationEndDate;
// notificationColorTheme.value = opensilex.getConfig().notificationColorTheme;

// try {
//   if (notificationEndDate.value) {
//     new Date(notificationEndDate.value);
//   }
//   if (!notificationEndDate.value || notificationEndDate.value > formattedCurrentDate) {
//     displayNotificationMessage.value = true;
//     notificationMessageDisplayed.value = notificationMessage.value[opensilex.i18n.locale];
//   }
// } catch {
//   // opensilex.showErrorToast(t("component.header.bad-notification-end-date"));
// }
</script>

<style lang="scss">
@import "./../theme/opensilex/main.css";

// header {
//   display: flex;
// }

// main {
//   background-color: getVar(--defaultColorLight);
//   color: getVar(--defaultColorDark);
// }

// #header-content {
//   max-width: 1600px;
//   margin: auto;
//   display: flex;
//   width: 100%;
// }

// #header-content .header-logo {
//   width: 70%;
// }

// #header-content .header-login {
//   width: 30%;
//   text-align: right;
// }

// section#content-wrapper {
//   display: flex;
//   margin: 0 auto;
//   height: 100%;
//   flex-grow: 1;
//   width: 100%;
// }


// .header-top.logged-out {
//   box-shadow: none;
// }

.wrapper.embed .page-wrap .main-content {
  margin-top: 0px;
  margin-left: 0px;
  padding: 15px;
}

.uri-search-box {
  position: fixed;
  max-width: 500px;
  top: 70px;
  right: 8%;
  padding: 20px;
  background-color: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  z-index: 1030;
}

// .notificationMessageContainer{
//   display: flex;
//   position: relative;
//   z-index: 1029; // header has an index of 1030, must be lower to allow the display of dropdown buttons above
//   width: 100%;
//   min-height: 35px;
//   top: 65px;
//   padding: 0 10px 0 10px;
//   justify-content: center;
//   align-items: center;
//   font-family: sans-serif;
//   font-size: small;
//   font-style: oblique;
//   font-weight: bold;
//   color: darkslategray;
// }

// .notificationMessageDefaultColor {
//   background: linear-gradient(45deg, #CFD8D5, #B1D8CB);
// }
// .notificationMessageInfoColor {
//   background: linear-gradient(45deg, #c8e7eb, #91c8db);
// }
// .notificationMessageWarningColor {
//     background: linear-gradient(45deg, #ebc8c8, #db9191);
// }

// .notificationWithMenu {
//   left: 241px;
//   max-width: calc(100% - 241px);
// }

// .notificationButtonContainer {
//   position: inherit;
//   right: 0;
// }

// .closeNotificationButton{
//   border: none;
//   padding: 1px 8px;
//   margin: 4px;
//   font-size: 1.5em !important;
//   color:rgba(101, 101, 101, 0.5);
//   font-weight: bolder;
//   cursor: pointer;
//   background: none;
// }

// .closeNotificationButton:hover{
//   color : #00A28C;
//   background: none;
// }

// .notificationText {
//   padding-right: 25px;
//   max-width: calc(100% - 50px);
// }
</style>

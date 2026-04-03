<template>
  <div>
    <!-- Hamburger -->
    <div class="hamburger-container">
      <button
        class="hamburger hamburger--collapse"
        :class="{ 'is-active': menuVisible }"
        type="button"
        @click="toggleMenu"
      >
        <span class="hamburger-box">
          <span class="hamburger-inner"></span>
        </span>
      </button>
    </div>

    <!-- Sections menu -->
    <div class="app-sidebar">
      <div class="sidebar-content">
        <div class="nav-container">
          <nav id="main-menu-navigation" class="navigation-main">
          
            <div
              v-for="item in menu"
              :key="item.id"
              class="nav-item"
              :class="{
                'has-sub': item.hasChildren(),
                open: item.showChildren,
                active: isActive(item),
              }"
            >
            <a
              v-if="item.hasChildren()"
              href="#"
              @click.prevent="toggle(item)"
              class="menu-link"
            >
              <span class="menu-link-left">
                <i class="bi" :class="getIcon(item)"></i>
                <span>{{ t(item.label) }}</span>
              </span>

              <i class="bi bi-chevron-right submenu-chevron"></i>
            </a>
              <router-link v-else :to="{ path: item.route.path }">
                <i class="bi" :class="getIcon(item)"></i>
                <span>{{ t(item.label) }}</span>
              </router-link>
              <transition
                @before-enter="beforeEnter"
                @enter="enter"
                @before-leave="beforeLeave"
                @leave="leave"
              >
                <div v-if="item.showChildren" class="submenu-content">
                  <span @click="toggleMenuOnSelect">
                    <router-link
                      v-for="itemChild in item.children"
                      :key="itemChild.id"
                      :class="{ active: isActive(itemChild) }"
                      class="menu-item"
                      :to="itemChild.route.path"
                    >
                      {{ t(itemChild.label) }}
                    </router-link>
                  </span>
                </div>
              </transition>
            </div>

            <div class="nav-item">
              <a
                v-if="versionInfo?.api_docs?.url"
                :href="versionInfo.api_docs.url"
                target="_blank"
                class="router-link-exact-active router-link-active"
              >
                <img
                  fluid
                  width="25"
                  src="https://cdn.svgporn.com/logos/swagger.svg"
                />
                &nbsp;&nbsp;&nbsp;
                <span class="ml-1">{{ t("component.menu.web-api") }}</span>
              </a>
            </div>
          </nav>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject } from "vue";
import { useStore } from "vuex"; 
import { useRoute } from "vue-router";
import { Menu } from "../../models/Menu";
import { versionInfoDTO } from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { useI18n } from 'vue-i18n';
import en from './../../lang/message-en.json';
import fr from './../../lang/message-fr.json';

import { useRouter } from "vue-router";
const router = useRouter();
const { t } = useI18n(); 
const store = useStore();
const route = useRoute();
const $opensilex= inject<OpenSilexVuePlugin>("$opensilex");

const menu = computed(() => store.state.menu);
const user = computed(() => store.state.user);
const menuVisible = computed(() => store.state.menuVisible);

const versionInfo = ref<versionInfoDTO | null>(null);

onMounted(() => {
  versionInfo.value = $opensilex.versionInfo;
});

const toggleMenu = () => {
  store.commit("toggleMenu");
};

const toggleMenuOnSelect = () => {
  if (document.body.clientWidth < 1040) {
    store.commit("toggleMenuOnSelect");
  }
};

const toggle = (item: Menu) => {
  if (item.hasChildren()) {
    item.showChildren = !item.showChildren;
  }
};

const getIcon = (item: Menu): string => {
  const code = "icon." + item.label;
  const result = t(code);
  return result !== code ? result.toString() : "bi-folder";
};

const isActive = (item: Menu): boolean => {
  return item.route && route.path.indexOf(item.route.path) === 0;
};

const beforeEnter = (el: Element) => {
  const htmlEl = el as HTMLElement;
  htmlEl.style.height = "0";
  htmlEl.style.opacity = "0";
  htmlEl.style.transform = "translateY(-6px)";
  htmlEl.style.overflow = "hidden";
};

const enter = (el: Element) => {
  const htmlEl = el as HTMLElement;

  htmlEl.style.transition =
    "height 0.5s ease, opacity 0.5s ease, transform 0.5s ease";

  requestAnimationFrame(() => {
    htmlEl.style.height = htmlEl.scrollHeight + "px";
    htmlEl.style.opacity = "1";
    htmlEl.style.transform = "translateY(0)";
  });

  const cleanup = (event: TransitionEvent) => {
    if (event.propertyName !== "height") return;
    htmlEl.style.height = "auto";
    htmlEl.style.overflow = "";
    htmlEl.removeEventListener("transitionend", cleanup);
  };

  htmlEl.addEventListener("transitionend", cleanup);
};

const beforeLeave = (el: Element) => {
  const htmlEl = el as HTMLElement;
  htmlEl.style.height = htmlEl.scrollHeight + "px";
  htmlEl.style.opacity = "1";
  htmlEl.style.transform = "translateY(0)";
  htmlEl.style.overflow = "hidden";
};

const leave = (el: Element) => {
  const htmlEl = el as HTMLElement;

  htmlEl.style.transition =
    "height 0.5s ease, opacity 0.18s ease, transform 0.18s ease";

  requestAnimationFrame(() => {
    htmlEl.style.height = "0";
    htmlEl.style.opacity = "0";
    htmlEl.style.transform = "translateY(-6px)";
  });
};


</script>

<style scoped lang="scss">

.menu-link {
  display: flex !important;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.menu-link-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.submenu-chevron {
  font-size: 0.7rem !important;
  opacity: 0.6;
  margin-right: 15px !important;
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.nav-item.open .submenu-chevron {
  transform: rotate(90deg);
  opacity: 0.9;
}

.submenu-content.open {
  max-height: 500px;
  opacity: 1;
  transform: translateY(0);
}

.submenu-content {
  overflow: hidden;
  will-change: height, opacity, transform;
}

.hamburger-container {
  position: fixed;
  z-index: 1030;
  -webkit-user-select: none;
  user-select: none;
  height: 65px;
  width: 60px;
  left: 0px;
  top: 0px;
  background-color: #00a38d;
}

.hamburger {
  position: relative;
  height: 55px;
  top: 1px;
  outline: none;
}

.nav-item a { // viens du style global sur les liens (main.scss ?) ..
  text-decoration: none;
}

.hamburger .hamburger-inner,
.hamburger .hamburger-inner::after,
.hamburger .hamburger-inner::before,
.hamburger.is-active .hamburger-inner,
.hamburger.is-active .hamburger-inner:after ,
.hamburger.is-active .hamburger-inner:before {
  background-color: #fff;
}

.hamburger:hover .hamburger-inner,
.hamburger:hover .hamburger-inner::after,
.hamburger:hover .hamburger-inner::before {
  background-color: rgb(221, 221, 221);
}

.hamburger-box {
  width: 35px;
}

.hamburger-inner,
.hamburger-inner:after,
.hamburger-inner:before {
  width: 41px;
}

@media (min-width: 250px) and (max-width: 1150px) {
  .hamburger-inner,
  .hamburger-inner:after,
  .hamburger-inner:before {
    width: 25px;
  }
}
</style>
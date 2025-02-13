///////////////////////////////////////////////////
// import { App } from "vue";

// import TestPage from './TestPage.vue';
// import DefaultLoginComponentVue from "./layout/DefaultLoginComponent.vue";
// const components = (app: App): void => {
//     app.component('TestPage', TestPage);
//     app.component('DefaultLoginComponentVue', DefaultLoginComponentVue);
// }
// export default components;
///////////////////////////////////////////////////


// let components = {};

// Layout
// import DefaultFooterComponent from './layout/DefaultFooterComponent.vue';
// components["opensilex-DefaultFooterComponent"] = DefaultFooterComponent;
// import DefaultHeaderComponent from './layout/DefaultHeaderComponent.vue';
// components["opensilex-DefaultHeaderComponent"] = DefaultHeaderComponent;
// import DefaultLoginComponent from './layout/DefaultLoginComponent.vue';
// components["opensilex-DefaultLoginComponent"] = DefaultLoginComponent;
// import TestPage from './TestPage.vue';
// components["opensilex-TestPage.vue"] = TestPage;
// import DefaultMenuComponent from './layout/DefaultMenuComponent.vue';
// components["opensilex-DefaultMenuComponent"] = DefaultMenuComponent;
// import DefaultHomeComponent from './layout/DefaultHomeComponent.vue';
// components["opensilex-DefaultHomeComponent"] = DefaultHomeComponent;
// import DefaultNotFoundComponent from './layout/DefaultNotFoundComponent.vue';
// components["opensilex-DefaultNotFoundComponent"] = DefaultNotFoundComponent;
// import PageHeader from './layout/PageHeader.vue'
// components["opensilex-PageHeader"] = PageHeader;
// import PageActions from './layout/PageActions.vue'
// components["opensilex-PageActions"] = PageActions;
// import PageContent from './layout/PageContent.vue'
// components["opensilex-PageContent"] = PageContent;
// import Overlay from './layout/Overlay.vue'
// components["opensilex-Overlay"] = Overlay;
// import ToDoComponent from './layout/ToDoComponent.vue';
// components["opensilex-ToDoComponent"] = ToDoComponent;
// import ResetPassword from './layout/ResetPassword.vue';
// components["opensilex-ResetPassword"] = ResetPassword;
// import ForgotPassword from './layout/ForgotPassword.vue';
// components["opensilex-ForgotPassword"] = ForgotPassword;


// // Users
// import AccountForm from './account/AccountForm.vue';
// components["opensilex-AccountForm"] = AccountForm;
// import AccountList from './account/AccountList.vue';
// components["opensilex-AccountList"] = AccountList;
// import AccountView from './account/AccountView.vue';
// components["opensilex-AccountView"] = AccountView;
// import AccountSelector from './account/AccountSelector.vue';
// components["opensilex-AccountSelector"] = AccountSelector;


// export default components;





const components: { [key: string]: any } = {};

// charger dynamiquement tous les composants Vue
const modules = import.meta.glob('./**/*.vue', { eager: true });

console.log("index.ts - modules ", modules)
for (const path in modules) {
  const component = (modules[path] as { default : any}).default;
//   console.log("index.ts  - component : ", component)
//   console.log("---------------")
  const componentName = path
  .split("/").pop()
    // .replace(/^\.\/components\//, '')
    .replace(/\.vue$/, '')
    // .replace(/\//g, '-');
  components[`opensilex-${componentName}`] = component;
}
console.log("index.ts - components : ", components)

export default components;

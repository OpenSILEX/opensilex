import SiteList from '../components/organizations/site/SiteList.vue';

export default {
  title: 'Components/SiteList',
  component: SiteList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SiteList },
  setup() { return { args }; },
  template: '<SiteList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

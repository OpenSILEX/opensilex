import SiteDetail from '../components/organizations/site/SiteDetail.vue';

export default {
  title: 'Components/SiteDetail',
  component: SiteDetail,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SiteDetail },
  setup() { return { args }; },
  template: '<SiteDetail v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

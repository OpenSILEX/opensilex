import SiteDetailView from '../components/organizations/site/SiteDetailView.vue';

export default {
  title: 'Components/SiteDetailView',
  component: SiteDetailView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SiteDetailView },
  setup() { return { args }; },
  template: '<SiteDetailView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

import SiteView from '../components/organizations/site/SiteView.vue';

export default {
  title: 'Components/SiteView',
  component: SiteView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SiteView },
  setup() { return { args }; },
  template: '<SiteView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

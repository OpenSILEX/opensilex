import PackagesView from '../components/tools/PackagesView.vue';

export default {
  title: 'Components/PackagesView',
  component: PackagesView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { PackagesView },
  setup() { return { args }; },
  template: '<PackagesView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

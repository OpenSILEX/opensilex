import MetadataView from '../components/common/views/MetadataView.vue';

export default {
  title: 'Components/MetadataView',
  component: MetadataView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { MetadataView },
  setup() { return { args }; },
  template: '<MetadataView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

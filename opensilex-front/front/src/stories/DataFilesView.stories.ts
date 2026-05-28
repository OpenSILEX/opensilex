import DataFilesView from '../components/data/DataFilesView.vue';

export default {
  title: 'Components/DataFilesView',
  component: DataFilesView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DataFilesView },
  setup() { return { args }; },
  template: '<DataFilesView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

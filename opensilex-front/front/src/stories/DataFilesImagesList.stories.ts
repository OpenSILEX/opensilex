import DataFilesImagesList from '../components/data/DataFilesImagesList.vue';

export default {
  title: 'Components/DataFilesImagesList',
  component: DataFilesImagesList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DataFilesImagesList },
  setup() { return { args }; },
  template: '<DataFilesImagesList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

import DataFilesList from '../components/data/DataFilesList.vue';

export default {
  title: 'Components/DataFilesList',
  component: DataFilesList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DataFilesList },
  setup() { return { args }; },
  template: '<DataFilesList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

import DataFileForm from '../components/data/form/DataFileForm.vue';

export default {
  title: 'Components/DataFileForm',
  component: DataFileForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DataFileForm },
  setup() { return { args }; },
  template: '<DataFileForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

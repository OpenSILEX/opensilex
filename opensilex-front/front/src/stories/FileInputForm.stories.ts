import FileInputForm from '../components/common/forms/FileInputForm.vue';

export default {
  title: 'Components/FileInputForm',
  component: FileInputForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FileInputForm },
  setup() { return { args }; },
  template: '<FileInputForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

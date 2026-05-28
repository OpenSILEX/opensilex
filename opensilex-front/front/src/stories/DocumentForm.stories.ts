import DocumentForm from '../components/documents/DocumentForm.vue';

export default {
  title: 'Components/DocumentForm',
  component: DocumentForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DocumentForm },
  setup() { return { args }; },
  template: '<DocumentForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

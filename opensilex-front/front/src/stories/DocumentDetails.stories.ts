import DocumentDetails from '../components/documents/DocumentDetails.vue';

export default {
  title: 'Components/DocumentDetails',
  component: DocumentDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DocumentDetails },
  setup() { return { args }; },
  template: '<DocumentDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

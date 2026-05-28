import DocumentList from '../components/documents/DocumentList.vue';

export default {
  title: 'Components/DocumentList',
  component: DocumentList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DocumentList },
  setup() { return { args }; },
  template: '<DocumentList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

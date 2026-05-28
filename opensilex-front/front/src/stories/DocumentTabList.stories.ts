import DocumentTabList from '../components/documents/DocumentTabList.vue';

export default {
  title: 'Components/DocumentTabList',
  component: DocumentTabList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DocumentTabList },
  setup() { return { args }; },
  template: '<DocumentTabList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

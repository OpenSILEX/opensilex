import DocumentView from '../components/documents/DocumentView.vue';

export default {
  title: 'Components/DocumentView',
  component: DocumentView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DocumentView },
  setup() { return { args }; },
  template: '<DocumentView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

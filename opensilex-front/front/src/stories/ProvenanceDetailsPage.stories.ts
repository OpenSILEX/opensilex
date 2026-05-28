import ProvenanceDetailsPage from '../components/data/ProvenanceDetailsPage.vue';

export default {
  title: 'Components/ProvenanceDetailsPage',
  component: ProvenanceDetailsPage,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProvenanceDetailsPage },
  setup() { return { args }; },
  template: '<ProvenanceDetailsPage v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

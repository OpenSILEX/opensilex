import NamespacesView from '../components/ontology/NamespacesView.vue';

export default {
  title: 'Components/NamespacesView',
  component: NamespacesView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { NamespacesView },
  setup() { return { args }; },
  template: '<NamespacesView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

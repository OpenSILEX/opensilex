import TreeView from '../components/common/views/TreeView.vue';

export default {
  title: 'Components/TreeView',
  component: TreeView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { TreeView },
  setup() { return { args }; },
  template: '<TreeView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

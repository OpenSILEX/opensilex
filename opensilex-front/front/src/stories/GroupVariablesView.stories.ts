import GroupVariablesView from '../components/groupVariable/GroupVariablesView.vue';

export default {
  title: 'Components/GroupVariablesView',
  component: GroupVariablesView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GroupVariablesView },
  setup() { return { args }; },
  template: '<GroupVariablesView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

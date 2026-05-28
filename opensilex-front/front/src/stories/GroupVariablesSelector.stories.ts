import GroupVariablesSelector from '../components/groupVariable/GroupVariablesSelector.vue';

export default {
  title: 'Components/GroupVariablesSelector',
  component: GroupVariablesSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GroupVariablesSelector },
  setup() { return { args }; },
  template: '<GroupVariablesSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

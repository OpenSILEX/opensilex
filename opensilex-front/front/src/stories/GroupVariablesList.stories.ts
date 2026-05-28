import GroupVariablesList from '../components/groupVariable/GroupVariablesList.vue';

export default {
  title: 'Components/GroupVariablesList',
  component: GroupVariablesList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GroupVariablesList },
  setup() { return { args }; },
  template: '<GroupVariablesList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

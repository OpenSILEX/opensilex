import GroupVariablesModalList from '../components/groupVariable/GroupVariablesModalList.vue';

export default {
  title: 'Components/GroupVariablesModalList',
  component: GroupVariablesModalList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GroupVariablesModalList },
  setup() { return { args }; },
  template: '<GroupVariablesModalList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

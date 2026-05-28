import GroupVariablesForm from '../components/groupVariable/GroupVariablesForm.vue';

export default {
  title: 'Components/GroupVariablesForm',
  component: GroupVariablesForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GroupVariablesForm },
  setup() { return { args }; },
  template: '<GroupVariablesForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

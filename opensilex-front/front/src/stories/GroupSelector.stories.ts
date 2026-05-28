import GroupSelector from '../components/groups/GroupSelector.vue';

export default {
  title: 'Components/GroupSelector',
  component: GroupSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GroupSelector },
  setup() { return { args }; },
  template: '<GroupSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

import CreateButton from '../components/common/buttons/CreateButton.vue';

export default {
  title: 'Components/CreateButton',
  component: CreateButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CreateButton },
  setup() { return { args }; },
  template: '<CreateButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

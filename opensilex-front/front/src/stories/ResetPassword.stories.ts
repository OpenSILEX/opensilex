import ResetPassword from '../components/layout/ResetPassword.vue';

export default {
  title: 'Components/ResetPassword',
  component: ResetPassword,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ResetPassword },
  setup() { return { args }; },
  template: '<ResetPassword v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

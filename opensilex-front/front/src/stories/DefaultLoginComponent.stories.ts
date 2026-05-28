import DefaultLoginComponent from '../components/layout/DefaultLoginComponent.vue';

export default {
  title: 'Components/DefaultLoginComponent',
  component: DefaultLoginComponent,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DefaultLoginComponent },
  setup() { return { args }; },
  template: '<DefaultLoginComponent v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

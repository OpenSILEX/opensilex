import DefaultHomeComponent from '../components/layout/DefaultHomeComponent.vue';

export default {
  title: 'Components/DefaultHomeComponent',
  component: DefaultHomeComponent,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DefaultHomeComponent },
  setup() { return { args }; },
  template: '<DefaultHomeComponent v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

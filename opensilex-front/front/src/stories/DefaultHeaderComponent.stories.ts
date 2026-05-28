import DefaultHeaderComponent from '../components/layout/DefaultHeaderComponent.vue';

export default {
  title: 'Components/DefaultHeaderComponent',
  component: DefaultHeaderComponent,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DefaultHeaderComponent },
  setup() { return { args }; },
  template: '<DefaultHeaderComponent v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

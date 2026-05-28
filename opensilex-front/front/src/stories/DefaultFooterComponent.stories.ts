import DefaultFooterComponent from '../components/layout/DefaultFooterComponent.vue';

export default {
  title: 'Components/DefaultFooterComponent',
  component: DefaultFooterComponent,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DefaultFooterComponent },
  setup() { return { args }; },
  template: '<DefaultFooterComponent v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

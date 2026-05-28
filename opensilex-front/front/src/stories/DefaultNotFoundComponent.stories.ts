import DefaultNotFoundComponent from '../components/layout/DefaultNotFoundComponent.vue';

export default {
  title: 'Components/DefaultNotFoundComponent',
  component: DefaultNotFoundComponent,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DefaultNotFoundComponent },
  setup() { return { args }; },
  template: '<DefaultNotFoundComponent v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

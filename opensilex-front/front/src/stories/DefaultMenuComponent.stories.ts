import DefaultMenuComponent from '../components/layout/DefaultMenuComponent.vue';

export default {
  title: 'Components/DefaultMenuComponent',
  component: DefaultMenuComponent,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DefaultMenuComponent },
  setup() { return { args }; },
  template: '<DefaultMenuComponent v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

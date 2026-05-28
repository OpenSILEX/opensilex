import EntitySelector from '../components/variables/form/EntitySelector.vue';

export default {
  title: 'Components/EntitySelector',
  component: EntitySelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EntitySelector },
  setup() { return { args }; },
  template: '<EntitySelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

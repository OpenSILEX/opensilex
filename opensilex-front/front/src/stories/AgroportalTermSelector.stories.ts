import AgroportalTermSelector from '../components/common/external-references/agroportal/AgroportalTermSelector.vue';

export default {
  title: 'Components/AgroportalTermSelector',
  component: AgroportalTermSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalTermSelector },
  setup() { return { args }; },
  template: '<AgroportalTermSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

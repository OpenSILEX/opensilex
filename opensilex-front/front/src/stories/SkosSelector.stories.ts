import SkosSelector from '../components/common/external-references/skos/SkosSelector.vue';

export default {
  title: 'Components/SkosSelector',
  component: SkosSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SkosSelector },
  setup() { return { args }; },
  template: '<SkosSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

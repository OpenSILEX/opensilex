import NbElementPerPageSelector from '../components/common/views/NbElementPerPageSelector.vue';

export default {
  title: 'Components/NbElementPerPageSelector.vue',
  component: NbElementPerPageSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { NbElementPerPageSelector},
  setup() { return { args }; },
  template: '<NbElementPerPageSelector.vue v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

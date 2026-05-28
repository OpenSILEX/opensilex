import TextView from '../components/common/views/TextView.vue';

export default {
  title: 'Components/TextView',
  component: TextView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { TextView },
  setup() { return { args }; },
  template: '<TextView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

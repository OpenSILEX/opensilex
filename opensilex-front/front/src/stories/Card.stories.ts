import Card from '../components/common/views/Card.vue';

export default {
  title: 'Components/Card',
  component: Card,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Card },
  setup() { return { args }; },
  template: '<Card v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

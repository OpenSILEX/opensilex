import Tutorial from '../components/common/views/Tutorial.vue';

export default {
  title: 'Components/Tutorial',
  component: Tutorial,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Tutorial },
  setup() { return { args }; },
  template: '<Tutorial v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

import Icon from '../components/common/views/Icon.vue';

export default {
  title: 'Components/Icon',
  component: Icon,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Icon },
  setup() { return { args }; },
  template: '<Icon v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

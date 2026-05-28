import Overlay from '../components/layout/Overlay.vue';

export default {
  title: 'Components/Overlay',
  component: Overlay,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Overlay },
  setup() { return { args }; },
  template: '<Overlay v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

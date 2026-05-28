import ImageSingle from '../components/visualization/ImageSingle.vue';

export default {
  title: 'Components/ImageSingle',
  component: ImageSingle,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ImageSingle },
  setup() { return { args }; },
  template: '<ImageSingle v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

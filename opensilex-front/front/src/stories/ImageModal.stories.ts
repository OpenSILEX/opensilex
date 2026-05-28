import ImageModal from '../components/data/ImageModal.vue';

export default {
  title: 'Components/ImageModal',
  component: ImageModal,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ImageModal },
  setup() { return { args }; },
  template: '<ImageModal v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

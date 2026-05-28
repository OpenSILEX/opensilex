import AnnotationModalForm from '../components/annotations/list/form/AnnotationModalForm.vue';

export default {
  title: 'Components/AnnotationModalForm',
  component: AnnotationModalForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AnnotationModalForm },
  setup() { return { args }; },
  template: '<AnnotationModalForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};

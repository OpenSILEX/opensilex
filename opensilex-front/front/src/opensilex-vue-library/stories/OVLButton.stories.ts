import type { Meta, StoryObj } from "@storybook/vue3-vite";
import { fn } from "storybook/test";
import OVLButton from "../components/OVLButton.vue";

const meta = {
  title: "Library/OVLButton",
  component: OVLButton,
  tags: ["common"],
  argTypes: {
    variant: {
      control: "select",
      options: ["primary", "outline-primary", "secondary", "outline-info", "outline-success", "danger"],
    },
    icon: { control: "text", description: "`fa#<nom>` (FontAwesome) ou `bi-<nom>` (Bootstrap Icons)" },
    label: { control: "text" },
    helpMessage: { control: "text" },
    small: { control: "boolean", description: "Masque le label (icône seule)" },
    disabled: { control: "boolean" },
  },
  args: {
    onClick: fn(),
  },
} satisfies Meta<typeof OVLButton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const GreenTheme: Story = {
  args: {
    label: "Ajouter",
    icon: "bi-plus-lg",
    class: "greenThemeColor",
  },
};

export const OutlinePrimary: Story = {
  args: {
    label: "Modifier",
    icon: "fa#pencil-alt",
    variant: "outline-primary",
  },
};

export const IconOnly: Story = {
  args: {
    label: "Modifier",
    icon: "fa#pencil-alt",
    variant: "outline-primary",
    small: true,
  },
};

export const Disabled: Story = {
  args: {
    label: "Ajouter",
    icon: "bi-plus-lg",
    class: "greenThemeColor",
    disabled: true,
  },
};

export const WithHelpMessage: Story = {
  args: {
    label: "Trait",
    helpMessage: "Message d'aide au survol",
    icon: "fa#globe-americas",
    variant: "outline-primary",
  },
};

package org.jdownloader.myjdownloader.client.bindings;

public class ExtensionStorable {
        private final String id;
        private boolean      installed;
        private boolean      enabled;
        private String       name;
        private String       iconKey;
        private String       description;
        private String       configInterface;

        public ExtensionStorable(String id) {
            this.id = id;
        }

        public boolean isInstalled() {
            return installed;
        }

        public void setInstalled(boolean installed) {
            this.installed = installed;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIconKey() {
            return iconKey;
        }

        public void setIconKey(String iconKey) {
            this.iconKey = iconKey;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getConfigInterface() {
            return configInterface;
        }

        public void setConfigInterface(String configInterface) {
            this.configInterface = configInterface;
        }


}

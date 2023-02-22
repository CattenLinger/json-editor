package net.catten.property.editor.app

import net.catten.property.editor.framework.EnvironmentPropertyResolver

class UIApplicationEnvironment : EnvironmentPropertyResolver() {
    val configDir by provider {
        property("net.catten.PropertyEditor.configDir")
        environment("PROPERTY_EDITOR_CONFIG_DIR")
        ifNotFoundThenLog("Application settings will not be persistent.")
    }
}
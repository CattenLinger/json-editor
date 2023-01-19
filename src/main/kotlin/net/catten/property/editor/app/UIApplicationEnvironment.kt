package net.catten.property.editor.app

class UIApplicationEnvironment : EnvironmentPropertyResolver() {
    val configDir by provider {
        property("net.catten.PropertyEditor.configDir")
        environment("PROPERTY_EDITOR_CONFIG_DIR")
        ifNotFoundThenLog("Application settings will not be persistent.")
    }
}
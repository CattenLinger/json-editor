package net.catten.property.editor.app.views.editor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

interface ConfigValueContainerFormat {
    val objectMapper : ObjectMapper
    val name : String

    companion object {
        val Json = object : ConfigValueContainerFormat {
            override val name = "json"
            override val objectMapper = ObjectMapper()
        }

        val Yaml = object : ConfigValueContainerFormat {
            override val name = "yaml"
            override val objectMapper = ObjectMapper(YAMLFactory())
        }
    }
}
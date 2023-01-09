package net.catten.property.editor.app.editor

import javax.swing.tree.DefaultMutableTreeNode
import kotlin.properties.Delegates
import kotlin.properties.Delegates.observable

interface ConfigFragment {
    class Value : ConfigFragment {
        enum class Type {
            STRING, INTEGER, DECIMAL, BOOLEAN, NULL
        }


    }

    class Object : ConfigFragment
}

class ConfigFragmentTreeNode(fragment : ConfigFragment) : DefaultMutableTreeNode() {
    val fragment : ConfigFragment by observable(fragment) { _, _, new ->
        userObject = new
    }
}
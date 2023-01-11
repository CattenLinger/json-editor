package net.catten.property.editor.utils

import com.formdev.flatlaf.extras.FlatSVGUtils
import java.util.*
import javax.swing.ImageIcon

class SvgIconCache(val location : String, val width : Int, val height : Int) {
    private val cache = WeakHashMap<String, ImageIcon>()

    operator fun get(name : String): ImageIcon = cache.computeIfAbsent("$location/$name.svg") {
        ImageIcon(FlatSVGUtils.svg2image(it, width, height))
    }
}
package cn.apisium.eim.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.utils.OBJECT_MAPPER
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Path
import kotlin.io.path.name

interface ProjectInformation {
    val root: Path
    var name: String
    var author: String
    var description: String
    var timeCost: Int

    fun save()
}

class DefaultProjectInformation(@JsonIgnore override val root: Path): ProjectInformation {
    override var name by mutableStateOf(root.name)
    override var author by mutableStateOf("")
    override var description by mutableStateOf("")
    override var timeCost by mutableStateOf(0)

    @JsonIgnore
    private val jsonFile = root.resolve("eim.json").toFile()

    init {
        var flag = false
        if (jsonFile.exists()) {
            try {
                val map = OBJECT_MAPPER.readValue<Map<String, String>>(jsonFile)
                name = map["name"] ?: root.name
                author = map["author"] ?: ""
                description = map["description"] ?: ""
                timeCost = map["timeCost"]?.toIntOrNull() ?: 0
                flag = true
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        if (!flag) save()
    }

    override fun save() {
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(jsonFile, this)
    }
}

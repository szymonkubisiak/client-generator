package readback

import models.*
import utils.PackageConfig
import java.io.File
import java.io.FileNotFoundException

/**
 * Simple hack to read back POJOs written previously
 * only names of fields and their order are recovered
 */
class KotlinReadbackTransport(val pkg: PackageConfig) {

	private lateinit var fieldsOrdering: Map<String, List<String>>

/*
	fun readbackStructs(models: List<Struct>) {
		val structs = HashMap<String, List<String>>()
		models.forEach { struct ->
			(struct as? StructActual)?.also { struct ->
				val fields = readbackStruct(struct)
				fields?.takeIf { it.isNotEmpty() }?.also {
					structs[struct.key] = it
				}
			}
		}
		fieldsOrdering = structs
	}

	private fun readbackStruct(struct: StructActual): List<String>? {
		val fileName = fileName(struct.type)
		return readbackStruct("$fileName.kt")
	}

	private fun fileName(type: RefTypeDescr): String = type.transportFinalName()
*/

	private val rgx = "^\tvar ([a-zA-Z0-9_-]+): (.*)\\? = null(?:\t//(.*))?$".toRegex(RegexOption.MULTILINE)
	private val fileSuffix = "Pojo.kt"

	private fun readbackStructsByDirectory() {
		val structs = HashMap<String, List<String>>()
		val directory = File(pkg.asDir)
		val files = directory.listFiles()
		files?.forEach { file ->
			val fileName = file.name
			if (fileName.endsWith(fileSuffix)) {
				readbackStruct(fileName)?.also { fields ->
					val structName = fileName.substring(0, fileName.length - fileSuffix.length)
					structs[structName] = fields
				}
			}
		}
		fieldsOrdering = structs
	}

	private fun readbackStruct(fileName: String): List<String>? {
		try {
			val file = File("${pkg.asDir}/$fileName")

			file.bufferedReader().use { reader ->
				val matches = rgx.findAll(reader.readText()).toList()

				val fields = matches.map { it.groupValues.drop(1) }
					.map { it[0] }

				return fields.takeIf { it.isNotEmpty() }
			}

		} catch (e: FileNotFoundException) {
			//println("An error occurred.")
			//e.printStackTrace();
			return null
		}
	}

	fun reorderStructFields(structs: List<Struct>): List<Struct> {
		//re-sort fields of structs to match previous version
		//this is to circumvent a bug that shuffles api-doc once in a while
		return structs.map {
			when (it) {
				is StructEnum -> it
				is StructActual -> {
					val oldOrder = fieldsOrdering[it.key] ?: return@map it
					val orderById = oldOrder.withIndex().associate { it.value to it.index }
					val sortedFields = it.fields.sortedBy { orderById[it.key] ?: 0xFFFF }
					it.copy(fields = sortedFields)
				}
			}
		}
	}

	init {
		readbackStructsByDirectory()
	}
}
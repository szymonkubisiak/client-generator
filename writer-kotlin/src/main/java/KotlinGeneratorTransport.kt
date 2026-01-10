import Namer.transportFinalName
import models.*
import utils.PackageConfig

@Suppress("NAME_SHADOWING")
class KotlinGeneratorTransport(pkg: PackageConfig) : KotlinGeneratorBaseStructs(pkg) {

	override fun fileName(type: RefTypeDescr): String = type.transportFinalName()
	override fun isWriteable(type: Struct): Boolean {
		return (type !is StructEnum && (type.incoming || type.outgoing))
	}

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
		when (model) {
			is StructActual -> writeActualStruct(writer, model)
			is StructEnum -> return //enums use built-in transport types
		}
	}

	fun writeActualStruct(writer: GeneratorWriter, model: StructActual) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("class ${model.type.transportFinalName()} {")
		IndentedWriter(writer).use { writer ->
			model.fields.forEach {
				writeField(writer, it)
			}
		}
		writer.writeLine("}")
	}

	fun writeField(writer: GeneratorWriter, field: Field) {
		val name = Namer.kotlinizeVariableName(field.key)
		val rawType = field.type.transportFinalName()
		val type = when {
			field.isArray -> "List<$rawType>"
			field.isStringmap -> "Map<String, $rawType>"
			else -> rawType
		}
		val description = field.description.convertToSingleLine()?.let { "\t//$it" } ?: ""

		//all transport fields are made nullable to work around parser not detecting missing mandatories
		writer.writeLine("var $name: $type? = null$description")
	}

}
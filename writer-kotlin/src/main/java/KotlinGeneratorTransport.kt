import Namer.transportFinalName
import models.*
import utils.PackageConfig

@Suppress("NAME_SHADOWING")
class KotlinGeneratorTransport(pkg: PackageConfig) : KotlinGeneratorBase(pkg) {

	override fun fileName(type: StructTypeDescr): String = type.transportFinalName()
	override fun isWriteable(type: Struct): Boolean {
		return type !is StructEnum
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

	override fun writeField(writer: GeneratorWriter, field: Field) {
		val name = field.transportName
		val rawType = field.type.transportFinalName()
		val type = if (!field.isArray) rawType else "List<$rawType>"
		val description = field.description?.let { "\t//$it" } ?: ""

		//all transport fields are made nullable to work around parser not detecting missing mandatories
		writer.writeLine("var $name: $type? = null$description")
	}

}
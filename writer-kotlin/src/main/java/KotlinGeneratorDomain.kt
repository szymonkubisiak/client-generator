import Namer.domainFinalName
import models.*
import utils.PackageConfig

@Suppress("NAME_SHADOWING")
class KotlinGeneratorDomain(pkg: PackageConfig): KotlinGeneratorBaseStructs(pkg) {

	override fun fileName(type: RefTypeDescr): String = type.domainFinalName()

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		when(model){
			is StructActual -> writeActualStruct(writer, model)
			is StructEnum -> writeActualEnum(writer, model)
		}
	}

	fun writeActualEnum(writer: GeneratorWriter, model: StructEnum) {
		writer.writeLine("enum class ${model.type.domainFinalName()}{")
		IndentedWriter(writer).use { writer ->
			model.values.forEach {
				writer.writeLine("$it,")
			}
		}
		writer.writeLine("}")
	}

	fun writeActualStruct(writer: GeneratorWriter, model: StructActual) {
		writer.writeLine("class ${model.type.domainFinalName()}(")
		IndentedWriter(writer).use { writer ->

			model.fields.forEach {
				writeField(writer, it)
			}
		}
		writer.writeLine(")")

		if (model.artificialID) {
			writer.writeLine("{")
			writer.writeLine("\tdata class ID(val internal: Long)")
			writer.writeLine("}")
		}
	}

	fun writeField(writer: GeneratorWriter, field: Field) {
//		field.description?.also {
//			writer.writeLine("//$it")
//		}

		val name = field.key
		val rawType = field.type.domainFinalName()
		var type = when {
			field.isArray -> "List<$rawType>"
			field.isStringmap -> "Map<String, $rawType>"
			else -> rawType
		}
		if (!field.mandatory)
			type = "$type?"
		val description = field.description?.let { "\t//$it" } ?: ""

		writer.writeLine("val $name: $type,$description")
	}
}
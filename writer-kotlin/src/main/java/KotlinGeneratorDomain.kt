import Namer.domainFinalName
import models.*
import utils.PackageConfig

@Suppress("NAME_SHADOWING")
class KotlinGeneratorDomain(
	pkg: PackageConfig,
	val manualModels: PackageConfig,
) : KotlinGeneratorBaseStructs(pkg) {

	override fun fileName(type: RefTypeDescr): String = type.domainFinalName()
	override fun isWriteable(type: Struct): Boolean {
		return (type is StructEnum || (type.incoming || type.outgoing || type.outgoingAsForm))
	}

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import " + manualModels.toPackage() + ".*")
		if(needDates(model))
			writer.writeLine("import java.time.*")
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

		model.artificialID?.also {
			writer.writeLine("{")
			writer.writeLine("\tdata class ID(val internal: ${it.domainFinalName()})")
			writer.writeLine("}")
		}
	}

	fun writeField(writer: GeneratorWriter, field: Field) {
//		field.description?.also {
//			writer.writeLine("//$it")
//		}

		val name = Namer.kotlinizeVariableName(field.key)
		val rawType = field.type.domainFinalName()
		var type = when {
			field.isArray -> "List<$rawType>"
			field.isStringmap -> "Map<String, $rawType>"
			else -> rawType
		}
		if (!field.mandatory)
			type = "$type?"
		val description = field.description.convertToSingleLine()?.let { "\t//$it" } ?: ""

		writer.writeLine("val $name: $type,$description")
	}
}
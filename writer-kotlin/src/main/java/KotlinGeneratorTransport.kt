import Namer.transportFinalName
import models.*

@Suppress("NAME_SHADOWING")
class KotlinGeneratorTransport: KotlinGeneratorBase() {

	override fun fileName(type: TypeDescr): String = type.transportFinalName()

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
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
		val rawType = typeResolver.resolveTransportType(field.type)
		val type = if (!field.isArray) rawType else "List<$rawType>"
		val description = field.description?.let { "\t//$it" } ?: ""

		//all transport fields are made nullable to work around parser not detecting missing mandatories
		writer.writeLine("var $name: $type? = null$description")
	}

}
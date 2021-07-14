import models.Field
import models.Struct
import java.io.Writer

@Suppress("NAME_SHADOWING")
class KotlinGenerator {

	private val typeResolver = TypeResolver()

	fun writeStructs(models: List<Struct>, callback: (String) -> Writer) {
		models.forEach { struct ->
			callback(struct.transportName).use { writer ->
				writeStruct(BaseWriter(writer), struct)
				writer.flush()
			}
		}
	}

	fun writeStruct(writer: GeneratorWriter, model: Struct) {

		writer.writeLine("class ${model.transportName} {")
		IndentedWriter(writer).use { writer ->
			model.fields.forEach {
				writeField(writer, it)
			}
		}
		writer.writeLine("}")
	}

	fun writeField(writer: GeneratorWriter, field: Field) {
//		field.description?.also {
//			writer.writeLine("//$it")
//		}

		val name = field.transportName
		val rawType = typeResolver.resolveTransportType(field.type)
		val type = if (!field.isArray) rawType else "List<$rawType>"
		val description = field.description?.let{"\t//$it"} ?: ""

		writer.writeLine("var $name: $type? = null$description")
	}
}
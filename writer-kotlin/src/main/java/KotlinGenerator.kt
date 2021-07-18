import Namer.domainFinalName
import Namer.transportFinalName
import Namer.adapterT2DName
import models.*
import java.io.Writer

@Suppress("NAME_SHADOWING")
class KotlinGenerator() {

	private val typeResolver = TypeResolver()

	fun writeStructs(models: List<Struct>, callback: (String) -> Writer) {
		models.forEach { struct ->
			callback(struct.transportName).use { writer ->
				writeTransportStruct(BaseWriter(writer), struct)
				writer.flush()
			}
		}
	}

	fun writeTransportStruct(writer: GeneratorWriter, model: Struct) {
		writer.writeLine("class ${model.type.transportFinalName()} {")
		IndentedWriter(writer).use { writer ->
			model.fields.forEach {
				writeTransportField(writer, it)
			}
		}
		writer.writeLine("}")
	}

	fun writeTransportField(writer: GeneratorWriter, field: Field) {
		val name = field.transportName
		val rawType = typeResolver.resolveTransportType(field.type)
		val type = if (!field.isArray) rawType else "List<$rawType>"
		val description = field.description?.let { "\t//$it" } ?: ""

		//all transport fields are made nullable to work around parser not detecting missing mandatories
		writer.writeLine("var $name: $type? = null$description")
	}

	fun writeDomainStruct(writer: GeneratorWriter, model: Struct) {
		writer.writeLine("class ${model.type.domainFinalName()} (")
		IndentedWriter(writer).use { writer ->
			model.fields.forEach {
				writeDomainField(writer, it)
			}
		}
		writer.writeLine(")")
	}

	fun writeDomainField(writer: GeneratorWriter, field: Field) {
//		field.description?.also {
//			writer.writeLine("//$it")
//		}

		val name = field.transportName
		var type = typeResolver.resolveDomainType(field.type)
		if (field.isArray)
			type = "List<$type>"
		if (!field.mandatory)
			type = "$type?"
		val description = field.description?.let { "\t//$it" } ?: ""

		writer.writeLine("val $name: $type,$description")
	}

	fun writeTransportToDomainAdapter(writer: GeneratorWriter, model: Struct) {
		val transportTypeName = model.type.transportFinalName()
		val domainTypeName = model.type.domainFinalName()
		writer.writeLine("fun ${model.type.adapterT2DName()}(input: $transportTypeName): $domainTypeName {")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("val retval = $domainTypeName (")
			IndentedWriter(writer).use { writer ->
				model.fields.forEach {
					writeTransportToDomainField(writer, it)
				}
			}
			writer.writeLine(")")
			writer.writeLine("return retval")
		}
		writer.writeLine("}")
	}

	fun writeTransportToDomainField(writer: GeneratorWriter, field: Field) {
		val name = field.transportName
		// val type = typeResolver.resolveTransportType(field.type)
		val conversion = typeResolver.resolveTransportToDomainConversion(field.type)
		val conversionIt = conversion.format("it")

		var expression = "input.$name"

		expression = if (field.mandatory) {
			"($expression ?: throw MandatoryIsNullException(\"$name\"))"
		} else {
			"$expression?"
		}

		expression = if (field.isArray) {
			"$expression.map { $conversionIt }"
		} else {
			"$expression.let { $conversionIt }"
		}

		writer.writeLine("$expression,")
	}
}
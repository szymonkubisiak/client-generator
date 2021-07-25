import Namer.domainFinalName
import Namer.transportFinalName
import Namer.adapterT2DName
import models.*
import java.io.Writer

class KotlinGeneratorDomain : KotlinGeneratorBase() {

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
		writer.writeLine("import java.util.*")
		writer.writeLine("")
		writer.writeLine("class ${model.type.domainFinalName()} (")
		IndentedWriter(writer).use { writer ->

			model.fields.forEach {
				writeField(writer, it)
			}
		}
		writer.writeLine(")")
	}

	override fun writeField(writer: GeneratorWriter, field: Field) {
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
}
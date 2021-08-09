import Namer.domainFinalName
import models.*
import utils.PackageConfig

@Suppress("NAME_SHADOWING")
class KotlinGeneratorDomain(pkg: PackageConfig): KotlinGeneratorBase(pkg) {

	override fun fileName(type: TypeDescr): String = type.domainFinalName()

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("class ${model.type.domainFinalName()}(")
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
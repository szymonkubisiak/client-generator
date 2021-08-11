import Namer.domainFinalName
import Namer.transportFinalName
import Namer.adapterT2DName
import models.*
import utils.PackageConfig
import java.io.Writer

@Suppress("NAME_SHADOWING")
class KotlinGeneratorT2D(
	pkg: PackageConfig,
	val conn: PackageConfig,
	val domain: PackageConfig,
) : KotlinGeneratorBase(pkg) {

	override fun fileName(type: TypeDescr): String = type.adapterT2DName()

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
		when (model) {
			is StructActual -> writeActualStruct(writer, model)
			is StructEnum -> return
		}
	}

	fun writeActualStruct(writer: GeneratorWriter, model: StructActual) {
		writeImports(writer)
		val transportTypeName = model.type.transportFinalName()
		val domainTypeName = model.type.domainFinalName()
		writer.writeLine("fun ${model.type.adapterT2DName()}(input: $transportTypeName): $domainTypeName {")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("val retval = $domainTypeName (")
			IndentedWriter(writer).use { writer ->
				model.fields.forEach {
					writeField(writer, it)
				}
			}
			writer.writeLine(")")
			writer.writeLine("return retval")
		}
		writer.writeLine("}")
	}

	override fun writeField(writer: GeneratorWriter, field: Field) {
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

	private fun writeImports(writer: GeneratorWriter) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import " + conn.toPackage() + ".*")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("")
	}
}
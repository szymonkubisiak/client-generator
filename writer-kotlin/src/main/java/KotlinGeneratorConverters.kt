import Namer.adapterD2TName
import Namer.domainFinalName
import Namer.transportFinalName
import Namer.adapterT2DName
import models.*
import utils.PackageConfig
import java.io.PrintWriter

@Suppress("NAME_SHADOWING")
class KotlinGeneratorConverters(
	pkg: PackageConfig,
	val conn: PackageConfig,
	val domain: PackageConfig,
) : KotlinGeneratorBase(pkg) {

	override fun fileName(type: StructTypeDescr): String = type.key
	override fun isWriteable(type: Struct): Boolean {
		return type !is StructEnum
	}

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
		when (model) {
			is StructActual -> {
				writeImports(writer)
				writeActualStructT2D(writer, model)
				writer.writeLine("")
				writeActualStructD2T(writer, model)
			}
			is StructEnum -> return
		}
	}

	fun writeActualStructT2D(writer: GeneratorWriter, model: StructActual) {
		val transportTypeName = model.type.transportFinalName()
		val domainTypeName = model.type.domainFinalName()
		writer.writeLine("fun ${model.type.adapterT2DName()}(input: $transportTypeName): $domainTypeName {")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("val retval = $domainTypeName (")
			IndentedWriter(writer).use { writer ->
				model.fields.forEach {
					writeFieldT2D(writer, it)
				}
			}
			writer.writeLine(")")
			writer.writeLine("return retval")
		}
		writer.writeLine("}")
	}

	fun writeFieldT2D(writer: GeneratorWriter, field: Field) {
		val name = field.transportName
		// val type = typeResolver.resolveTransportType(field.type)
		val conversion = resolveTransportToDomainConversion(field.type)
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

	private fun writeActualStructD2T(writer: GeneratorWriter, model: StructActual) {
		val transportTypeName = model.type.transportFinalName()
		val domainTypeName = model.type.domainFinalName()
		writer.writeLine("fun ${model.type.adapterD2TName()}(input: $domainTypeName): $transportTypeName {")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("val retval = $transportTypeName ().apply {")
			IndentedWriter(writer).use { writer ->
				model.fields.forEach {
					writeFieldD2T(writer, it)
				}
			}
			writer.writeLine("}")
			writer.writeLine("return retval")
		}
		writer.writeLine("}")
	}

	private fun writeFieldD2T(writer: GeneratorWriter, field: Field) {
		val name = field.transportName
		// val type = typeResolver.resolveTransportType(field.type)
		val conversion = resolveDomainToTransportConversion(field.type)
		val conversionIt = conversion.format("it")

		var expression = "this.$name = input.$name"

		expression = if (field.mandatory) {
			expression
		} else {
			"$expression?"
		}


		expression = if (field.isArray) {
			"$expression.map { $conversionIt }"
		} else {
			"$expression.let { $conversionIt }"
		}

		writer.writeLine(expression)
	}

	private fun writeImports(writer: GeneratorWriter) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import " + conn.toPackage() + ".*")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("")
	}

	override fun writeExtras(directory: String) {
		PrintWriter("$directory/MandatoryIsNullException.kt").use { writer ->
			writeMandatoryIsNullException(BaseWriter(writer))
			writer.flush()
		}
	}

	private fun writeMandatoryIsNullException(writer: GeneratorWriter) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import java.lang.Exception")
		writer.writeLine("")
		writer.writeLine("class MandatoryIsNullException(fieldName: String): Exception(\"Mandatory field \$fieldName is null\")")
	}

	companion object {
		fun resolveTransportToDomainConversion(type: TypeDescr): String {
			return when (type) {
				is BuiltinTypeDescr -> TypeResolver.instance.resolveTransportToDomainConversion(type)
				is StructTypeDescr -> when (type.definition!!) {
					is StructActual -> "${type.adapterT2DName()}(%s)"
					is StructEnum -> type.key + ".valueOf(%s)"
				}
			}
		}

		fun resolveDomainToTransportConversion(type: TypeDescr): String {
			return when (type) {
				is BuiltinTypeDescr -> TypeResolver.instance.resolveDomainToTransportConversion(type)
				is StructTypeDescr -> when (type.definition!!) {
					is StructActual -> "${type.adapterD2TName()}(%s)"
					is StructEnum -> "%s.name"
				}
			}
		}
	}
}
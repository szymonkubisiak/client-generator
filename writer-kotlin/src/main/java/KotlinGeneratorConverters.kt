import Namer.adapterD2MapName
import Namer.adapterD2TName
import Namer.domainFinalName
import Namer.transportFinalName
import Namer.adapterT2DName
import models.*
import utils.PackageConfig

@Suppress("NAME_SHADOWING")
class KotlinGeneratorConverters(
	pkg: PackageConfig,
	val conn: PackageConfig,
	val domain: PackageConfig,
	val manualModels: PackageConfig,
) : KotlinGeneratorBaseStructs(pkg) {

	override fun fileName(type: RefTypeDescr): String = type.key
	override fun isWriteable(type: Struct): Boolean {
		return (type !is StructEnum && (type.incoming || type.outgoing || type.outgoingAsForm))
	}

	override fun writeStruct(writer: GeneratorWriter, model: Struct) {
		when (model) {
			is StructActual -> {
				writeImports(writer, model)
				if(model.incoming)
					writeActualStructT2D(writer, model)
				if(model.outgoing)
					writeActualStructD2T(writer, model)
				if(model.outgoingAsForm)
					writeActualStructD2Map(writer, model)
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
		val name = field.key
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
		} else if (field.isStringmap) {
			"$expression.mapValues { " + conversion.format("it.value") + " }"
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
		val name = field.key
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
		} else if (field.isStringmap) {
			"$expression.mapValues { " + conversion.format("it.value") + " }"
		} else {
			"$expression.let { $conversionIt }"
		}

		writer.writeLine(expression)
	}

	private fun writeActualStructD2Map(writer: GeneratorWriter, model: StructActual) {
		try {
			model.fields.forEach {
				if(it.isArray || it.isStringmap) return
				resolveDomainToMapFieldConversion(it.type)
			}
		} catch (ex: InvalidFieldException) {
			return
		}

		writer.writeLine("")
		val transportTypeName = "Map<String, String?>"
		val domainTypeName = model.type.domainFinalName()
		writer.writeLine("fun ${model.type.adapterD2MapName()}(input: $domainTypeName): $transportTypeName {")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("val retval = HashMap<String, String?>().apply {")
			IndentedWriter(writer).use { writer ->
				model.fields.forEach {
					writeFieldD2Map(writer, it)
				}
			}
			writer.writeLine("}")
			writer.writeLine("return retval")
		}
		writer.writeLine("}")
	}

	private fun writeFieldD2Map(writer: GeneratorWriter, field: Field) {
		val name = field.key
		val conversion = resolveDomainToMapFieldConversion(field.type)
		val conversionIt = conversion.format("it")

		if (field.isArray) {
			throw InvalidFieldException("array type ${field.key} cannot be converted to map")
		}

		val expression = if (field.mandatory) {
			"this[\"$name\"] = input.$name.let { $conversionIt }"
		} else {
			"input.$name?.also { this[\"$name\"] = $conversionIt }"
		}

		writer.writeLine(expression)
	}

	private fun writeImports(writer: GeneratorWriter, model: Struct) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import " + conn.toPackage() + ".*")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("import " + manualModels.toPackage() + ".*")
		if(needDates(model))
			writer.writeLine("import java.time.*")
		writer.writeLine("")
	}

	override fun writeExtras() {
		pkg.openFile("MandatoryIsNullException.kt").use { writer ->
			writeMandatoryIsNullException(writer)
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
				is RefTypeDescr -> when (type.definition!!) {
					is StructActual -> "${type.adapterT2DName()}(%s)"
					is StructEnum -> type.key + ".valueOf(%s)"
				}
			}
		}

		fun resolveDomainToTransportConversion(type: TypeDescr): String {
			return when (type) {
				is BuiltinTypeDescr -> TypeResolver.instance.resolveDomainToTransportConversion(type)
				is RefTypeDescr -> when (type.definition!!) {
					is StructActual -> "${type.adapterD2TName()}(%s)"
					is StructEnum -> "%s.name"
				}
			}
		}

		private fun resolveDomainToMapFieldConversion(type: TypeDescr): String {
			return when (type) {
				is BuiltinTypeDescr -> TypeResolver.instance.resolveDomainToTransportConversion(type) + ".toString()"
				is RefTypeDescr -> when (type.definition!!) {
					is StructActual -> throw InvalidFieldException("complex type ${type.key} cannot be flattened")
					is StructEnum -> "%s.name"
				}
			}
		}

		fun resolveDomainToMapConversion(type: TypeDescr): String {
			return when (type) {
				is BuiltinTypeDescr -> throw InvalidFieldException("simple type ${type.key} cannot be converted to map")
				is RefTypeDescr -> when (type.definition!!) {
					is StructActual -> "${type.adapterD2MapName()}(%s)"
					is StructEnum -> throw InvalidFieldException("enum type ${type.key} cannot be converted to map")
				}
			}
		}

		class InvalidFieldException(message: String) : Exception(message)
	}
}
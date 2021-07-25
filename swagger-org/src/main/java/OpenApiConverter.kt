import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import models.*

class OpenApiConverter {

	private val typeFactory = TypeDescrFactory()

	fun swagger2api(input: OpenAPI): Api {
		val structs = input.components.schemas.map { oneModel ->
			val structType = typeFactory.getRefType(oneModel.key)
			model2struct(structType, oneModel.value)
		}
		val retval = Api(structs)
		return retval
	}

	fun model2struct(type: StructTypeDescr, input: Schema<*>): Struct {
		if (input !is ObjectSchema || input.type != "object")
			throw NotImplementedError("not an object")
		val requireds : List<String> = input.required ?: emptyList()
		val fields = input.properties.map { oneField ->
			property2field(oneField.key, oneField.value, requireds.contains(oneField.key))
		}

		val retval = Struct(type, fields, input.description)
		return retval
	}


	fun property2field(name: String, input: Schema<*>, required: Boolean): Field {
		val retval = Field(
			transportName = name,
			type = resolveType(input),
			isArray = input is ArraySchema,
			mandatory = required,
			description = input.description,
		)

		return retval
	}

	fun resolveType(input: Schema<*>): TypeDescr {
		//by design handle only known types and throw exception if anything unknown cames by
		val retval = when (input.type) {
			"array" -> {
				val inp = input as ArraySchema
				resolveType(inp.items)
			}
			"string", "number", "integer", "boolean" ->
				typeFactory.getSimpleType(input.type, input.format)
			"object", "ref", null -> {
				val ref = input.`$ref`
				if(ref.startsWith(objPrefix)) {
					typeFactory.getRefType(ref.removePrefix(objPrefix))
				} else {
					throw NotImplementedError("erroneous \$ref type")
				}
			}
			else ->
				throw NotImplementedError("unknown type")
		}
		return retval
	}

	val objPrefix = "#/components/schemas/"
}
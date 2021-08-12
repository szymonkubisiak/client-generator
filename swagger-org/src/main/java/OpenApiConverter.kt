import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import models.*

class OpenApiConverter {

	private val typeFactory = TypeDescrFactory()
	private lateinit var securityDefs : List<Security>

	fun swagger2api(input: OpenAPI): Api {
		val structs = input.components.schemas.map { oneModel ->
			model2struct(oneModel.key, oneModel.value)
		}
		securityDefs = input.components.securitySchemes.map {
			Security(it.key, parseLocation(it.value.`in`.toString()))
		}
		val paths = input.paths.flatMap { onePath ->
			path2Endpoints(onePath.key, onePath.value)
		}

		val retval = Api(structs, paths)
		return retval
	}

	fun path2Endpoints(path: String, input: PathItem): List<Endpoint> {
		return input.readOperationsMap().map { operation2Endpoint(path, it.key.toString(), it.value) }
	}

	fun operation2Endpoint(path: String, operation: String, input: Operation): Endpoint {
		val ap: ApiResponse? = input.responses?.get("200")
		val response = ap?.content?.let(::pickOneContent)
		val security = input.security
			?.flatMap { it.keys }
			?.map { key -> securityDefs.first { it.key == key } }

		return Endpoint(
			input.operationId,
			path,
			operation,
			(input.parameters?.mapNotNull(::parameter2Param) ?: emptyList()) + (input.requestBody?.let(::body2Param)
				?: emptyList()),
			response?.second,
			response?.first,
			security,
		)
	}

	//TODO: make something that encapsulates TypeDescr + isArray, because it's 3rd place this shows up
	fun pickOneContent(input: Content): Pair<String, Field>? {
		val entries = input.map {
			Pair(
				it.key,
				property2field(it.key, it.value.schema, true)//all params except schema are made up
			)
		}
		val retval = if (entries.size == 1) {
			entries.first()
		} else {
//			entries.sortedBy { if (it.first == "application/json") 0 else 1 }
//				.firstOrNull()
			entries.firstOrNull { it.first == "application/json" }
				?: entries.firstOrNull { it.first == "application/x-www-form-urlencoded" }
				?: entries.firstOrNull { it.first == "*/*" }
				?: entries.firstOrNull { it.first == "multipart/form-data" }
				?: null
		}

		return retval
	}

	fun body2Param(input: RequestBody): List<Param>? {
		if (input.`$ref` != null) {
			throw IllegalArgumentException("Direct definition of input body type is not supported: " + input.`$ref`)
		}
		val bodyType = pickOneContent(input.content) ?: return null

		val retval = Param(
			transportName = "body",
			type = bodyType.second.type,
			isArray = bodyType.second.isArray,
			mandatory = true,
			description = input.description,
			location = Param.Location.BODY(bodyType.first)
		)
		return listOf(retval)
	}

	fun parameter2Param(input: Parameter): Param? {
		val location = parseLocation(input.`in`)
		val retval = Param(
			transportName = input.name,
			type = resolveType(input.schema),
			isArray = input is ArraySchema,
			mandatory = input.required,
			description = input.description,
			location = location
		)
		return retval
	}

	private fun parseLocation(_in: String): Param.Location {
		return when (_in) {
			"path" -> Param.Location.PATH
			"query" -> Param.Location.QUERY
			"body" -> throw IllegalArgumentException("BODY argument defined in unexpected place")
			"header" -> Param.Location.HEADER
			"cookie" -> Param.Location.COOKIE
			else -> throw IllegalArgumentException("Unrecognized parameter location: $_in")
		}
	}

	fun model2struct(typeStr: String, input: Schema<*>): Struct {

		if (input is StringSchema && input.type == "string"){
			val type = typeFactory.getRefType(typeStr)
			val transportType = typeFactory.getSimpleType(input.type, null)
			val values = input.enum
			val retval = StructEnum(type, transportType, values, input.description)
			type.definition = retval
			return retval
		}

		if (input is ObjectSchema && input.type == "object") {
			val type = typeFactory.getRefType(typeStr)
			val requireds: List<String> = input.required ?: emptyList()
			val fields = input.properties.map { oneField ->
				property2field(oneField.key, oneField.value, requireds.contains(oneField.key))
			}

			val retval = StructActual(type, fields, input.description)
			type.definition = retval
			return retval
		}
		throw NotImplementedError("unknown type")
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

	@JvmName("resolveTypeNullable")
	fun resolveType(input: Schema<*>?): TypeDescr? {
		return if (input != null)
			resolveType(input)
		else null
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
				val ref = input.`$ref` ?: (input.additionalProperties as? Schema<*>)?.`$ref`
				refToStructTypeDescr(ref!!)
			}
			else ->
				throw NotImplementedError("unknown type")
		}
		return retval
	}

	private fun refToStructTypeDescr(ref: String) = if (ref.startsWith(objPrefix)) {
		typeFactory.getRefType(ref.removePrefix(objPrefix))
	} else {
		throw NotImplementedError("erroneous \$ref type")
	}

	val objPrefix = "#/components/schemas/"
}
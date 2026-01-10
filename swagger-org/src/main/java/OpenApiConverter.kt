import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import models.*

private const val jwtToken = "JWT" //duplication in KotlinGeneratorBase

class OpenApiConverter {

	private val typeFactory = TypeDescrFactory()
	private lateinit var securityDefs: List<Security>

	fun swagger2api(input: OpenAPI): Api {
		val structs = input.components.schemas.map { oneModel ->
			model2struct(oneModel.key, oneModel.value)
		}
		securityDefs = input.components.securitySchemes.map {
			Security(it.key, parseLocation(it.value.`in`.toString()))
		}.let { securityDefs ->
			val optionalJwt = securityDefs.firstOrNull { it.key == jwtToken }?.let { Security(it.key, it.location, false) }
			securityDefs + listOfNotNull(optionalJwt)
		}
		val paths = input.paths.flatMap { onePath ->
			path2Endpoints(onePath.key, onePath.value)
		}

		val retval = Api(structs, paths)
		return retval
	}

	fun path2Endpoints(path: String, input: PathItem): List<Endpoint> {
		return input.readOperationsMap().mapNotNull {
			try {
				operation2Endpoint(path, it.key.toString(), it.value)
			} catch (ex: Exception) {
				null
			}
		}
	}

	fun operation2Endpoint(path: String, operation: String, input: Operation): Endpoint {
		val ap: ApiResponse? = input.responses?.get("200")
		val response = ap?.content?.let(::pickOneContent)
		val securityIsOptional = input.security?.any { it.containsKey("none") } ?: false
		val security = input.security
			?.flatMap { it.keys }
			?.mapNotNull { key -> securityDefs.firstOrNull { it.key == key && (it.mandatory != securityIsOptional || it.key != jwtToken) } }

		val concatenatedDescription =
			listOf(input.summary, input.description).filterNotNull().takeIf { it.isNotEmpty() }?.joinToString("\n")

		val deprecationNotice = if(input.deprecated == true) {
			val rgx = """^\*\*DEPRECATION (?:NOT.*)\*\*\s*(.*)$""".toRegex(RegexOption.MULTILINE)
			val retval = rgx.findAll(input.description).map { it.groupValues.drop(1) }.firstOrNull()?.firstOrNull()
			retval
		} else {
			null
		}

		return Endpoint(
			input.operationId,
			path,
			input.tags.map(::Tag),
			operation,
			(input.parameters?.mapNotNull(::parameter2Param) ?: emptyList()) + (input.requestBody?.let(::body2Param)
				?: emptyList()),
			response?.second,
			response?.first,
			security,
			concatenatedDescription,
			deprecationNotice,
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
			key = "body",
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
			key = input.name,
			type = resolveType(input.schema),
			isArray = input.schema is ArraySchema,
			mandatory = input.required,
			description = input.description,
			location = location,
			defaultValue = input.schema.default,
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

		if (input is StringSchema && input.type == "string") {
			val type = typeFactory.getRefType(typeStr)
			val transportType = typeFactory.getSimpleType(input.type, null)
			val values = input.enum
			val retval = StructEnum(type, transportType, values, input.description)
			type.definition = retval
			return retval
		}

		if (input is ObjectSchema && input.type == "object") {
			val artificialID = input.extensions?.let {
				it.get("x-artificialID") as? String
			}?.let {
				typeFactory.getSimpleType(it, null)
			}
			val type = typeFactory.getRefType(typeStr)
			val requireds: List<String> = input.required ?: emptyList()
			val fields = (input.properties ?: emptyMap()).mapNotNull { oneField ->
				try {
					property2field(oneField.key, oneField.value, requireds.contains(oneField.key))
						.forceTypeOnID(artificialID, typeStr)
				} catch (ex: Exception) {
					null
				}
			}

			val retval = StructActual(type, fields, input.description, artificialID)
			type.definition = retval
			return retval
		}
		throw NotImplementedError("unknown type")
	}

	//For some structs the id field cannot be properly annotated in api-docs (eg, because it's inherited),
	//so I've arbitrarily decided that if a struct has artificial ID and a field named "id" that's a mistake to be fixed
	private fun Field.forceTypeOnID(artificialID: BuiltinTypeDescr?, encompassingType: String): Field {
		val field = this

		if (artificialID == null //the encompassing type doesn't have own id type
			|| field.key != "id" //field name doesn't look like id
			|| field.type !is BuiltinTypeDescr //field is not of a simple type
			|| field.type.key != artificialID.key //"id" field is of different type than artificial ID (eg one is number and another string)
		)
			return field

		val IDFormat = "ID:${encompassingType}.ID"

		//this one is correctly described
		if ((field.type as BuiltinTypeDescr).format == IDFormat)
			return field

		val retval = field.copy(type = typeFactory.getSimpleType(field.type.key, IDFormat))
		return retval
	}

	fun property2field(name: String, input: Schema<*>, required: Boolean): Field {
		val originalType = resolveType(input)

		val retval = Field(
			key = name,
			type = originalType,
			isArray = input is ArraySchema,
			mandatory = required,
			description = input.description,
			isStringmap = input.additionalProperties != null
		)

		return retval
	}

	fun resolveType(input: Schema<*>, parent: Schema<*>? = null): TypeDescr {
		//by design handle only known types and throw exception if anything unknown comes by
		val retval = when (input.type) {
			"array" -> {
				val inp = input as ArraySchema
				//workaround on Springdoc bug: extensions defined on list members are moved one level up
				resolveType(inp.items, inp)
			}
			"string", "number", "integer", "boolean" ->
				typeFactory.getSimpleType(getEffectiveType(input, parent), getEffectiveFormat(input, parent))
			"object", "ref", null -> {
				input.`$ref`?.also {
					return refToStructTypeDescr(it)
				}

				if (input is MapSchema) {
					(input.additionalProperties as? Schema<*>)?.also {
						return resolveType(it)
					}
				}
				throw IllegalArgumentException("unsupported type: " + input.type)
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

	private fun getEffectiveType(input: Schema<*>, parent: Schema<*>?): String {
		return input.getExtension("x-type")
			?: parent?.getExtension("x-type")
			?: input.type
	}

	private fun getEffectiveFormat(input: Schema<*>, parent: Schema<*>?): String? {
		return input.getExtension("x-format")
			?: parent?.getExtension("x-format")
			?: input.format
	}

	private inline fun <reified U> Schema<*>.getExtension(key: String) : U? {
		return this.extensions?.get(key) as? U
	}
}
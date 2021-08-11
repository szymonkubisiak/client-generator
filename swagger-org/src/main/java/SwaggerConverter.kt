import io.swagger.models.Model
import io.swagger.models.ModelImpl
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.Property
import io.swagger.models.properties.RefProperty
import io.swagger.models.refs.RefFormat
import models.*


class SwaggerConverter {

	private val typeFactory = TypeDescrFactory()

//	fun swagger2api(input: Swagger): Api {
//		val structs = input.definitions.map { oneModel ->
//			val structType = typeFactory.getRefType(oneModel.key)
//			model2struct(structType, oneModel.value)
//		}
//		val retval = Api(structs)
//		return retval
//	}

	fun model2struct(type: StructTypeDescr, input: Model): Struct {
		if (input !is ModelImpl || input.type != "object")
			throw NotImplementedError("not an object")
		val fields = input.properties.map { oneField ->
			property2field(oneField.key, oneField.value)
		}

		val retval = StructActual(type, fields, input.description)
		return retval
	}


	fun property2field(name: String, input: Property): Field {
		val retval = Field(
			transportName = name,
			type = resolveType(input),
			isArray = input is ArrayProperty,
			mandatory = input.required,
			description = input.description,
		)
		return retval
	}

	fun resolveType(input: Property): TypeDescr {
		//by design handle only known types and throw exception if anything unknown cames by
		val retval = when (input.type) {
			"array" -> {
				val inp = input as ArrayProperty
				resolveType(inp.items)
			}
			"ref" -> {
				val inp = input as RefProperty
				if (inp.refFormat != RefFormat.INTERNAL || inp.originalRef != "#/definitions/${inp.simpleRef}") {
					throw NotImplementedError("unknown ref type")
				}
				typeFactory.getRefType(inp.simpleRef)
			}
			"string", "number", "integer", "boolean" ->
				typeFactory.getSimpleType(input.type, input.format)
			else ->
				throw NotImplementedError("unknown type")
		}
		return retval
	}
}
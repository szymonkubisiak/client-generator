import io.swagger.models.Model
import io.swagger.models.ModelImpl
import io.swagger.models.Swagger
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.Property
import io.swagger.models.properties.RefProperty
import models.*


class SwaggerConverter {

    private val typeFactory = TypeDescrFactory()

    fun swagger2api(input: Swagger) : Api {
        val structs = input.definitions.map { oneModel ->
            model2struct(oneModel.key, oneModel.value)
        }
        val retval = Api (structs)
        return retval
    }

    fun model2struct(name: String, input: Model) : Struct {
        if(input !is ModelImpl || input.type != "object")
            throw NotImplementedError("not an object")
        val fields = input.properties.map { oneField ->
            property2field(oneField.key, oneField.value)
        }
        val retval = Struct ( name, fields, input.description)
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
        if (input.type == "array" ) {
            val retval = resolveType((input as ArrayProperty).items)
            return retval
        }
        if (input.type == "ref" ) {
            val retval = typeFactory.getType((input as RefProperty).simpleRef, null)
            return retval
        }
        val retval = typeFactory.getType(input.type, input.format)
        return retval
    }
}
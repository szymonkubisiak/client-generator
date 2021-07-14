import models.TypeDescr
import models.TypeDescrFactory

class TypeResolver {

	private val transportTypes = HashMap<TypeDescr, String>()
	private val typeResolver = TypeDescrFactory()

	//TODO: jak resolvować typy lokalne?
	// 1) Dodać do TypeDescr flagę że jest lokalny
	// 2) dodawać każdego Structa do listy znanych typów i tu przeszukiwać też tą listę
	fun resolveTransportType(t: TypeDescr): String {
		//val retval = transportTypes.getOrDefault(t, "Any")

		return transportTypes[t] ?: t.transportName
//		transportTypes.get(t)?.also { return it }
//		return t.transportName
	}

	init {
		//transportTypes[typeResolver.getType()]
//		addType("number",null, throw NotImplementedError())	//Any numbers.
		addType("number", "float", "Float")    //Floating-point numbers.
		addType("number", "double", "Double")    //Floating-point numbers with double precision.
		addType("integer",null, "Long") 	//Integer numbers.
		addType("integer", "int32", "Int")    //Signed 32-bit integers (commonly used integer type).
		addType("integer", "int64", "Long")    //Signed 64-bit integers (long type).

//		addType("string",

		addType("string", null, "String") // –
		addType("string", "date", "String", "Date") // – full-date notation as defined by RFC 3339, section 5.6, for example, 2017-07-21
		addType("string", "date-time", "String", "Date") //– the date-time notation as defined by RFC 3339, section 5.6, for example, 2017-07-21T17:32:28Z
		addType("string", "password", "String") //– a hint to UIs to mask the input
		addType("string", "byte", "String", "ByteArray") //– base64-encoded characters, for example, U3dhZ2dlciByb2Nrcw ==
		addType("string", "binary", "String") //– binary data, used to describe files (see Files below)

		addType("boolean", null, "Boolean") // type: boolean represents two values: true and false. Note that truthy and falsy values such as "true", "", 0 or null are not considered boolean values.

	}
	private fun addType(transportName: String, format: String?, kotlinTransport: String, kotlinModel:String = kotlinTransport){
		transportTypes[typeResolver.getType(transportName, format)] = kotlinTransport
	}
}

/*
"number", null, ??  	//Any numbers.
"number","float", "Float" 	//Floating-point numbers.
"number","double", "Double" 	//Floating-point numbers with double precision.
"integer",null 	//Integer numbers.
"integer","int32" 	//Signed 32-bit integers (commonly used integer type).
"integer","int64" 	//Signed 64-bit integers (long type).
 */

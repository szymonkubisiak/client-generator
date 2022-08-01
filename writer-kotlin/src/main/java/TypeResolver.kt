import models.BuiltinTypeDescr
import models.TypeDescrFactory
import java.lang.Exception

class TypeResolver private constructor(){
	private val typeFactory = TypeDescrFactory()

	private val transportTypes = HashMap<BuiltinTypeDescr, String>()
	private val domainTypes = HashMap<BuiltinTypeDescr, String>()
	private val adaptersT2D = HashMap<BuiltinTypeDescr, String>()
	private val adaptersD2T = HashMap<BuiltinTypeDescr, String>()

	fun resolveTransportType(type: BuiltinTypeDescr): String {
		return transportTypes[type] ?: throw Exception("missing type [$type]")
	}

	fun resolveDomainType(type: BuiltinTypeDescr): String {
		return domainTypes[type] ?: throw Exception("missing type [$type]")
	}

	fun resolveTransportToDomainConversion(type: BuiltinTypeDescr): String {
		return adaptersT2D[type] ?: throw Exception("missing type [$type]")
	}

	fun resolveDomainToTransportConversion(type: BuiltinTypeDescr): String {
		return adaptersD2T[type] ?: throw Exception("missing type [$type]")
	}

	init {
		//Any numbers. This type is forbidden in Kotlin, as no type can represent both floating and fixed point at the same time
		//addType("number",null, throw NotImplementedError())
		//Floating-point numbers.
		addType("number", "float", "Float")
		//Floating-point numbers with double precision.
		addType("number", "double", "Double")
		//Integer numbers.
		addType("integer", null, "Long")
		//Signed 32-bit integers (commonly used integer type).
		addType("integer", "int32", "Int")
		//Signed 64-bit integers (long type).
		addType("integer", "int64", "Long")
		// type: boolean represents two values: true and false. Note that truthy and falsy values such as "true", "", 0 or null are not considered boolean values.
		addType("boolean", null, "Boolean")

		//Any String
		addType("string", null, "String")
		// – full-date notation as defined by RFC 3339, section 5.6, for example, 2017-07-21
		addType("string", "Date", "String")
		addType("string", "date", "String")
		// – the date-time notation as defined by RFC 3339, section 5.6, for example, 2017-07-21T17:32:28Z
		addType("string", "date-time", "String")
		// – a hint to UIs to mask the input
		addType("string", "password", "String")
		// – base64-encoded characters, for example, U3dhZ2dlciByb2Nrcw ==
		addType("string", "byte", "String",)
		// – binary data, used to describe files (see Files below)
		addType("string", "binary", "String")
	}

	/**
	 * add straight-through type
	 * example: String to String
	 */
	private fun addType(
		transportName: String, format: String?,
		kotlinTransport: String,
	) = addType(
		transportName, format,
		kotlinTransport,
		kotlinTransport,
		"%s",
		"%s"
	)

	/**
	 * add a type that's represented by one type in JSON and another one in Kotlin
	 * example: String to Date
	 */
	private fun addType(
	fun addType(
		transportName: String, format: String?,
		kotlinTransport: String,
		kotlinModel: String,
		transportToModelAdapter: String,
		modelToTransportAdapter: String
	) {
		val type = typeFactory.getSimpleType(transportName, format)
		transportTypes[type] = kotlinTransport
		domainTypes[type] = kotlinModel
		adaptersT2D[type] = transportToModelAdapter
		adaptersD2T[type] = modelToTransportAdapter
	}

	companion object{
		val instance = TypeResolver()
	}
}

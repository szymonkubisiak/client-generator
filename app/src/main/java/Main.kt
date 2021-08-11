//import SwaggerConverter.swagger2api
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import io.swagger.parser.util.InlineModelResolver
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import models.Api
import utils.Package
import utils.PackageConfig
import java.io.PrintWriter

object Main {

	val kotlinT: KotlinGeneratorTransport
	val kotlinD: KotlinGeneratorDomain
	val kotlinT2D: KotlinGeneratorT2D
	val kotlinRetrofit: KotlinGeneratorRetrofit
	val kotlinRetrofitModule: KotlinGeneratorRetrofitModule

	init {
		//TODO: move those to some config
		val master = PackageConfig(rootDir = Package("out"), project = Package("com", "example"))
		val generatedPrefix = "generated"
		val transport = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "models"))
		kotlinT = KotlinGeneratorTransport(transport)

		val domain = master.copy(module = Package("domain"), suffix = Package(generatedPrefix, "models"))
		kotlinD = KotlinGeneratorDomain(domain)

		val t2d = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "converters"))
		kotlinT2D = KotlinGeneratorT2D(t2d, kotlinT.pkg, kotlinD.pkg)

		val retrofit = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "retrofit"))
		kotlinRetrofit = KotlinGeneratorRetrofit(retrofit, kotlinT.pkg)

		val retrofitModule = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "dagger"))
		kotlinRetrofitModule = KotlinGeneratorRetrofitModule(retrofitModule, kotlinRetrofit.pkg)
	}

	@JvmStatic
	fun main(args: Array<String>) {
		println("Hello World!")
		val openAPI: OpenAPI = OpenAPIV3Parser().read("src/test/resources/3.0/petstore.json")
//		val openAPI: OpenAPI = OpenAPIV3Parser().read("src/test/resources/3.0/types.json")
		val api = OpenApiConverter().swagger2api(openAPI)

		writeAllToFiles(api)
		//writeSampleToOut(api)
		println("Goodbye World!")
	}

	private fun writeSampleToOut(api: Api) {
		val outWriter = PrintWriter(System.out)
		api.structs.firstOrNull { it.type.name == "BasicTypes" }?.also {
			kotlinT.writeStruct(BaseWriter(outWriter), it)
			kotlinD.writeStruct(BaseWriter(outWriter), it)
			kotlinT2D.writeStruct(BaseWriter(outWriter), it)
		}
		outWriter.flush()
	}

	private fun writeAllToFiles(api: Api) {
		kotlinT.writeStructs(api.structs)
		kotlinD.writeStructs(api.structs)
		kotlinT2D.writeStructs(api.structs)
		kotlinRetrofit.writeEndpoits(api.paths)
		kotlinRetrofitModule.writeEndpoints(api.paths)
	}

	private fun parseAndPrepareSwagger(path: String): Swagger {
		val swagger = SwaggerParser().read(path)
		// resolve inline models
		InlineModelResolver().flatten(swagger)
		return swagger
	}
}
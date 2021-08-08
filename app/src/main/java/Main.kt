//import SwaggerConverter.swagger2api
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import io.swagger.parser.util.InlineModelResolver
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import models.Api
import java.io.PrintWriter

object Main {

	val kotlinT = KotlinGeneratorTransport()
	val kotlinD = KotlinGeneratorDomain()
	val kotlinT2D = KotlinGeneratorT2D()
	val kotlinRetrofit = KotlinGeneratorRetrofit()

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
		api.structs.firstOrNull { it.type.transportName == "BasicTypes" }?.also {
			kotlinT.writeStruct(BaseWriter(outWriter), it)
			kotlinD.writeStruct(BaseWriter(outWriter), it)
			kotlinT2D.writeStruct(BaseWriter(outWriter), it)
		}
		outWriter.flush()
	}

	private fun writeAllToFiles(api: Api) {
		//TODO: move those to some config
		val projectDir = "out"
		val projectPackage = "com.example"

		val transportDir = "$projectDir/conn/src/main/java/$projectPackage/conn/models"
		val domainDir = "$projectDir/domain/src/main/java/$projectPackage/domain/models"
		val converterInDir = "$projectDir/conn/src/main/java/$projectPackage/conn/converters"
		val converterOutDir = "$projectDir/conn/src/main/java/$projectPackage/conn/converters"
		val retrofitDir = "$projectDir/conn/src/main/java/$projectPackage/conn/retrofit"

		//TODO: add packages
		kotlinT.writeStructs(api.structs, transportDir)
		kotlinD.writeStructs(api.structs, domainDir)
		kotlinT2D.writeStructs(api.structs, converterInDir)
		kotlinRetrofit.writeEndpoits(api.paths, retrofitDir)
	}

	private fun parseAndPrepareSwagger(path: String): Swagger {
		val swagger = SwaggerParser().read(path)
		// resolve inline models
		InlineModelResolver().flatten(swagger)
		return swagger
	}
}
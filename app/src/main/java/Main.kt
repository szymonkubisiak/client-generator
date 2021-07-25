//import SwaggerConverter.swagger2api
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import io.swagger.parser.util.InlineModelResolver
import models.Api
import java.io.PrintWriter

object Main {

	val kotlinT = KotlinGeneratorTransport()
	val kotlinD = KotlinGeneratorDomain()
	val kotlinT2D = KotlinGeneratorT2D()

	@JvmStatic
	fun main(args: Array<String>) {
		println("Hello World!")
		val swg = parseAndPrepareSwagger("src/test/resources/types.json")
		val api = SwaggerConverter().swagger2api(swg)


		writeAllToFiles(api)
		writeSampleToOut(api)
		println("Goodbye World!")
	}

	private fun writeSampleToOut(api: Api) {
		val outWriter = PrintWriter(System.out)
		api.structs.first { it.transportName == "BasicTypes" }
			.also { kotlinT.writeStruct(BaseWriter(outWriter), it) }
			.also { kotlinD.writeStruct(BaseWriter(outWriter), it) }
			.also { kotlinT2D.writeStruct(BaseWriter(outWriter), it) }
		outWriter.flush()
	}

	private fun writeAllToFiles(api: Api) {
		val transportDir = "out/transport"
		val domainDir = "out/domain"
		val converterInDir = "out/convin"
		val converterOutDir = "out/convout"

		//TODO: dodać package
		kotlinT.writeStructs(api.structs, transportDir)
		kotlinD.writeStructs(api.structs, domainDir)
		kotlinT2D.writeStructs(api.structs, converterInDir)
	}

	private fun parseAndPrepareSwagger(path: String): Swagger {
		val swagger = SwaggerParser().read(path)
		// resolve inline models
		InlineModelResolver().flatten(swagger)
		return swagger
	}
}
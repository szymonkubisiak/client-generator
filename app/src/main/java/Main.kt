//import SwaggerConverter.swagger2api
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import io.swagger.parser.util.InlineModelResolver
import java.io.PrintWriter

object Main {
	@JvmStatic
	fun main(args: Array<String>) {
		println("Hello World!")
		val swg = parseAndPrepareSwagger("src/test/resources/types.json")
		val api = SwaggerConverter().swagger2api(swg)
		//new KotlinGenerator().writeStructs(api.getStructs(), name -> new PrintWriter(System.out));

		val writer = PrintWriter(System.out)
		val kotlin = KotlinGenerator()
		api.structs.forEach { kotlin.writeStruct(BaseWriter(writer), it) }
		writer.flush()
		println("Goodbye World!")
	}

	private fun parseAndPrepareSwagger(path: String): Swagger {
		val swagger = SwaggerParser().read(path)
		// resolve inline models
		InlineModelResolver().flatten(swagger)
		return swagger
	}
}
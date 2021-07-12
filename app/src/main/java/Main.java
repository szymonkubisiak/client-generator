import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.InlineModelResolver;
import models.Api;

public class Main {
	public static void main(String[] args) {
		System.out.println("Hello World!");
//		Swagger swg = parseAndPrepareSwagger("src/test/resources/2_0/allOfProperties.yaml");
		Swagger swg = parseAndPrepareSwagger("src/test/resources/szymon1.json");
		Api api = new SwaggerConverter().swagger2api(swg);
	}

	private static Swagger parseAndPrepareSwagger(String path) {
		Swagger swagger = new SwaggerParser().read(path);
		// resolve inline models
		new InlineModelResolver().flatten(swagger);
		return swagger;
	}

}
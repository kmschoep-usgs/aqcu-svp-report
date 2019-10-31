package gov.usgs.aqcu.serializer;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import springfox.documentation.spring.web.json.Json;

public class SwaggerGsonSerializer implements JsonSerializer<Json> {
	@Override
	public JsonElement serialize(Json json, Type type, JsonSerializationContext context) {
		//This is needed to fix a known bug with Swagger and the Spring GsonHttpMessageConverter
		//See: https://github.com/springfox/springfox/issues/1608
		return JsonParser.parseString(json.value());
	}
}
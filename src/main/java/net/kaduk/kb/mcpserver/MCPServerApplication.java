package net.kaduk.kb.mcpserver;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MCPServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MCPServerApplication.class, args);
    }

    // @Bean
	// public ToolCallbackProvider weatherTools(WeatherService weatherService) {
	// 	return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
	// }

    // @Bean
	// public ToolCallbackProvider knowledgeTools(DBpediaService knowledgeService) {
	// 	return MethodToolCallbackProvider.builder().toolObjects(knowledgeService).build();
	// }
	// public record TextInput(String input) {
	// }

	// @Bean
	// public ToolCallback toUpperCase() {
	// 	return FunctionToolCallback.builder("toUpperCase", (TextInput input) -> input.input().toUpperCase())
	// 		.inputType(TextInput.class)
	// 		.description("Put the text to upper case")
	// 		.build();
	// }
}
package net.kaduk.kb.mcpserver.config;

import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransport;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ServerMcpTransport;
import net.kaduk.kb.mcpserver.server.DBpediaService;
import net.kaduk.kb.mcpserver.server.WeatherService;

@Configuration
public class McpServerConfig {

	// STDIO transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
	public StdioServerTransport stdioServerTransport() {
		return new StdioServerTransport();
	}

	// SSE transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public WebFluxSseServerTransport sseServerTransport() {
		return new WebFluxSseServerTransport(new ObjectMapper(), "/mcp/message/");
	}
    
    // Default transport if no specific transport mode is configured
    @Bean
    @ConditionalOnMissingBean(ServerMcpTransport.class)
    public ServerMcpTransport defaultServerTransport() {
        // You can choose which transport to use as default
        // return new StdioServerTransport();
        return new WebFluxSseServerTransport(new ObjectMapper(), "/mcp/message/");
    }

	// Router function for SSE transport used by Spring WebFlux to start an HTTP
	// server.
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public RouterFunction<?> mcpRouterFunction(WebFluxSseServerTransport transport) {
		return transport.getRouterFunction();
	}

	@Bean
	public WeatherService weatherApiClient() {
		return new WeatherService();
	}

    @Bean
	public DBpediaService knowledgeApiClient() {
		return new DBpediaService();
	}

	@Bean
	public McpSyncServer mcpWeatherServer(ServerMcpTransport transport, WeatherService weatherApiClient) { // @formatter:off

		// Configure server capabilities with resource support
		var capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true) // Tool support with list changes notifications
			.logging() // Logging support
			.build();

		// Create the server with both tool and resource capabilities
		McpSyncServer server = McpServer.sync(transport)
			.serverInfo("MCP Demo Weather Server", "1.0.0")
			.capabilities(capabilities)
			.tools(McpToolUtils.toSyncToolRegistration(ToolCallbacks.from(weatherApiClient))) // Add @Tools
			.build();
		
		return server; // @formatter:on
	} // @formatter:on


    @Bean
	public McpSyncServer mcpKnowledgeServer(ServerMcpTransport transport, DBpediaService knowledgeApiClient) { // @formatter:off

		// Configure server capabilities with resource support
		var capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true) // Tool support with list changes notifications
			.logging() // Logging support
			.build();

		// Create the server with both tool and resource capabilities
		McpSyncServer server = McpServer.sync(transport)
			.serverInfo("MCP Knowledge Server", "1.0.0")
			.capabilities(capabilities)
			.tools(McpToolUtils.toSyncToolRegistration(ToolCallbacks.from(knowledgeApiClient))) // Add @Tools
			.build();
		
		return server; // @formatter:on
	} // @formatter:on
}
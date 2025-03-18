package net.kaduk.kb.mcpserver;

import java.time.Duration;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.ClientMcpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

/**
 * @author Christian Tzolov
 */

public class SampleClient {

	private final ClientMcpTransport transport;

	public SampleClient(ClientMcpTransport transport) {
		this.transport = transport;
	}

	public void run() {

		var client = McpClient.sync(this.transport).requestTimeout(Duration.ofSeconds(600)).build();

		client.initialize();

		client.ping();

		// List and demonstrate tools
		ListToolsResult toolsList = client.listTools();
        System.out.println("=============================");
		System.out.println("Available Tools = " + toolsList);

		CallToolResult weatherForcastResult = client.callTool(new CallToolRequest("getWeatherForecastByLocation",
				Map.of("latitude", "47.6062", "longitude", "-122.3321")));
        System.out.println("=============================");
		System.out.println("Weather Forcast: " + weatherForcastResult);

		CallToolResult alertResult = client.callTool(new CallToolRequest("getAlerts", Map.of("state", "NY")));
        System.out.println("=============================");
		System.out.println("Alert Response = " + alertResult);

	
		CallToolResult entityLiResult = client.callTool(new CallToolRequest("findRelatedEntities", Map.of("entityName", "Albert_Einstein", "limit", 5)));
		System.out.println("=============================Knowledge======");
		System.out.println("Enity list = " + entityLiResult);

		CallToolResult entityInfo = client.callTool(new CallToolRequest("getEntityInfo", Map.of("entityName", "Berlin")));
		System.out.println("=============================Knowledge======");
		System.out.println("Enity Info = " + entityInfo.toString());




		client.closeGracefully();

	}

}
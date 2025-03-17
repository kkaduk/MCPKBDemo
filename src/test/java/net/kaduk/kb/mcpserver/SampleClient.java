package net.kaduk.kb.mcpserver;

import java.time.Duration;

/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

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

	
		CallToolResult entityLiResult = client.callTool(new CallToolRequest("findRelatedEntities", Map.of("Albert_Einstein", 5)));
		System.out.println("=============================Knowledge======");
		System.out.println("Enity list = " + entityLiResult);

		CallToolResult entityInfo = client.callTool(new CallToolRequest("getEntityInfo", Map.of("Berlin", "")));
		System.out.println("=============================Knowledge======");
		System.out.println("Enity Info = " + entityInfo.toString());




		client.closeGracefully();

	}

}
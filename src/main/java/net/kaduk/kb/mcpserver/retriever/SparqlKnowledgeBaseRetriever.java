package net.kaduk.kb.mcpserver.retriever;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kaduk.kb.mcpserver.service.SparqlKnowledgeBaseService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SparqlKnowledgeBaseRetriever {

    private final SparqlKnowledgeBaseService sparqlService;
    private static final int DEFAULT_LIMIT = 10;

    /**
     * Retrieve information based on a query
     */
    public List<Document> retrieve(String query) {
        log.info("Retrieving information for query: {}", query);
        
        // Using the SPARQL service to search for entities related to the query
        List<Map<String, String>> searchResults = sparqlService.searchEntities(query, DEFAULT_LIMIT);
        
        // Convert the SPARQL results to Documents
        return searchResults.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
    }
    
    private Document convertToDocument(Map<String, String> result) {
        // Extract entity URI and its label
        String entityUri = result.getOrDefault("entity", "");
        String label = result.getOrDefault("label", "Unknown");
        String description = result.getOrDefault("description", "");
        
        // Get additional properties for this entity if we have a valid URI
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("Entity: ").append(label).append("\n");
        
        if (!description.isEmpty()) {
            contentBuilder.append("Description: ").append(description).append("\n");
        }
        
        if (!entityUri.isEmpty()) {
            List<Map<String, String>> properties = sparqlService.getEntityProperties(entityUri, 20);
            
            contentBuilder.append("Properties:\n");
            properties.forEach(prop -> {
                String property = prop.getOrDefault("property", "");
                String value = prop.getOrDefault("value", "");
                if (!property.isEmpty() && !value.isEmpty()) {
                    // Extract the property name from the URI
                    String propertyName = property;
                    if (property.contains("#") || property.contains("/")) {
                        propertyName = property.substring(Math.max(
                                property.lastIndexOf('#'), property.lastIndexOf('/')) + 1);
                    }
                    contentBuilder.append("- ").append(propertyName).append(": ").append(value).append("\n");
                }
            });
        }
        
        // Create a document with the content and metadata
        Map<String, Object> metadata = Map.of(
                "entityUri", entityUri,
                "label", label,
                "source", "SPARQL Knowledge Base"
        );
        
        return new Document(contentBuilder.toString(), metadata);
    }
}
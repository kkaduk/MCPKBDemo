package net.kaduk.kb.mcpserver.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.*;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SparqlKnowledgeBaseService {

    @Value("${sparql.endpoint.url}")
    private String sparqlEndpointUrl;

    @Value("${sparql.default-graph-uri}")
    private String defaultGraphUri;

    /**
     * Execute a SPARQL query against the configured endpoint
     *
     * @param sparqlQuery The SPARQL query to execute
     * @return List of results as maps
     */
    public List<Map<String, String>> executeQuery(String sparqlQuery) {
        log.info("Executing SPARQL query: {}", sparqlQuery);
        List<Map<String, String>> results = new ArrayList<>();

        try {
            Query query = QueryFactory.create(sparqlQuery);
            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpointUrl, query, defaultGraphUri)) {
                ResultSet resultSet = qexec.execSelect();
                
                while (resultSet.hasNext()) {
                    QuerySolution solution = resultSet.nextSolution();
                    Map<String, String> row = new HashMap<>();
                    
                    solution.varNames().forEachRemaining(varName -> {
                        if (solution.get(varName) != null) {
                            row.put(varName, solution.get(varName).toString());
                        } else {
                            row.put(varName, null);
                        }
                    });
                    
                    results.add(row);
                }
            }
        } catch (QueryExceptionHTTP e) {
            log.error("SPARQL endpoint error: {}", e.getMessage(), e);
            throw new RuntimeException("Error accessing SPARQL endpoint: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error executing SPARQL query: {}", e.getMessage(), e);
            throw new RuntimeException("Error executing SPARQL query: " + e.getMessage(), e);
        }

        log.info("Query returned {} results", results.size());
        return results;
    }

    /**
     * Search for entities in the knowledge base
     *
     * @param term The search term
     * @param limit Maximum number of results to return
     * @return List of entities matching the search term
     */
    public List<Map<String, String>> searchEntities(String term, int limit) {
        String sparqlQuery = String.format(
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT DISTINCT ?entity ?label ?type ?description " +
                "WHERE { " +
                "  ?entity rdfs:label ?label . " +
                "  FILTER(LANG(?label) = 'en') " +
                "  FILTER(CONTAINS(LCASE(?label), LCASE('%s'))) " +
                "  OPTIONAL { ?entity rdf:type ?type } " +
                "  OPTIONAL { ?entity rdfs:comment ?description . FILTER(LANG(?description) = 'en') } " +
                "} " +
                "LIMIT %d", 
                term, limit);
        
        return executeQuery(sparqlQuery);
    }

    /**
     * Get properties of a specific entity
     * 
     * @param entityUri The URI of the entity
     * @param limit Maximum number of properties to return
     * @return List of property-value pairs for the entity
     */
    public List<Map<String, String>> getEntityProperties(String entityUri, int limit) {
        String sparqlQuery = String.format(
                "SELECT ?property ?value " +
                "WHERE { " +
                "  <%s> ?property ?value . " +
                "} " +
                "LIMIT %d", 
                entityUri, limit);
        
        return executeQuery(sparqlQuery);
    }
}
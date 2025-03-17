package net.kaduk.kb.mcpserver.server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.springframework.ai.tool.annotation.Tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DBpediaClient {

    private static final String ENDPOINT_URL = "http://dbpedia.org/sparql";

    public static void main(String[] args) {
        DBpediaClient client = new DBpediaClient();
        System.out.println(client.getEntityInfo("California"));
        System.out.println(client.findRelatedEntities("Albert_Einstein", 5));
        System.out.println(client.searchByCategory("Nobel_Prize_winners", 3));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EntityInfo(@JsonProperty("uri") String uri, 
                            @JsonProperty("label") String label,
                            @JsonProperty("abstract") String description,
                            @JsonProperty("type") List<String> types) {
        
        public String toText() {
            return String.format("""
                    URI: %s
                    Label: %s
                    Types: %s
                    Description: %s
                    """, uri, label, String.join(", ", types), description);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Relation(@JsonProperty("subject") String subject,
                          @JsonProperty("predicate") String predicate,
                          @JsonProperty("object") String object) {
        
        public String toText() {
            return String.format("%s -> %s -> %s", subject, predicate, object);
        }
    }

    /**
     * Get information about a specific entity from DBpedia
     * 
     * @param entityName Name of the entity to search for
     * @return Information about the entity
     */
    @Tool(description = "Get information about a specific entity from DBpedia")
    public String getEntityInfo(String entityName) {
        String query = String.format("""
                PREFIX dbo: <http://dbpedia.org/ontology/>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                
                SELECT DISTINCT ?uri ?label ?abstract (GROUP_CONCAT(DISTINCT ?type; SEPARATOR=", ") AS ?types)
                WHERE {
                  ?uri rdfs:label ?label .
                  ?uri dbo:abstract ?abstract .
                  OPTIONAL { ?uri rdf:type ?type . FILTER(STRSTARTS(STR(?type), "http://dbpedia.org/ontology/")) }
                  FILTER(LANG(?label) = 'en')
                  FILTER(LANG(?abstract) = 'en')
                  FILTER(REGEX(?label, "%s", "i"))
                }
                GROUP BY ?uri ?label ?abstract
                LIMIT 1
                """, entityName);

        List<EntityInfo> results = executeQuery(query, rs -> {
            List<EntityInfo> entities = new ArrayList<>();
            
            while (rs.hasNext()) {
                QuerySolution solution = rs.next();
                String uri = solution.getResource("uri").getURI();
                String label = solution.getLiteral("label").getString();
                String description = solution.getLiteral("abstract").getString();
                
                List<String> types = new ArrayList<>();
                if (solution.contains("types") && solution.getLiteral("types") != null) {
                    String typeStr = solution.getLiteral("types").getString();
                    if (typeStr != null && !typeStr.isEmpty()) {
                        types = List.of(typeStr.split(", "));
                    }
                }
                
                entities.add(new EntityInfo(uri, label, description, types));
            }
            
            return entities;
        });

        if (results.isEmpty()) {
            return "No information found for entity: " + entityName;
        }

        return results.stream()
                .map(EntityInfo::toText)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Find entities related to a specific entity
     * 
     * @param entityName Resource name (using DBpedia URI format, e.g. 'Albert_Einstein' not 'Albert Einstein')
     * @param limit Maximum number of results to return
     * @return Related entities and their relationships
     */
    @Tool(description = "Find entities related to a specific entity in DBpedia. Entity should be in DBpedia resource format (e.g. 'Albert_Einstein')")
    public String findRelatedEntities(String entityName, int limit) {
        String query = String.format("""
                PREFIX dbo: <http://dbpedia.org/ontology/>
                PREFIX dbr: <http://dbpedia.org/resource/>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                
                SELECT ?predicate ?object ?objectLabel
                WHERE {
                  dbr:%s ?predicate ?object .
                  ?object rdfs:label ?objectLabel .
                  FILTER(LANG(?objectLabel) = 'en')
                  FILTER(STRSTARTS(STR(?object), "http://dbpedia.org/resource/"))
                }
                LIMIT %d
                """, entityName, limit);

        List<Relation> results = executeQuery(query, rs -> {
            List<Relation> relations = new ArrayList<>();
            
            while (rs.hasNext()) {
                QuerySolution solution = rs.next();
                String subject = "dbr:" + entityName;
                String predicate = solution.getResource("predicate").getLocalName();
                String object = solution.getLiteral("objectLabel").getString();
                
                relations.add(new Relation(subject, predicate, object));
            }
            
            return relations;
        });

        if (results.isEmpty()) {
            return "No related entities found for: " + entityName;
        }

        return "Related entities for " + entityName + ":\n" +
                results.stream()
                        .map(Relation::toText)
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Search for entities by category
     * 
     * @param category Category name in DBpedia format (e.g. 'Nobel_Prize_winners')
     * @param limit Maximum number of results to return
     * @return Entities in the specified category
     */
    @Tool(description = "Search for entities by category in DBpedia. Category should be in DBpedia format (e.g. 'Nobel_Prize_winners')")
    public String searchByCategory(String category, int limit) {
        String query = String.format("""
                PREFIX dct: <http://purl.org/dc/terms/>
                PREFIX dbc: <http://dbpedia.org/resource/Category:>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX dbo: <http://dbpedia.org/ontology/>
                
                SELECT DISTINCT ?entity ?label ?abstract
                WHERE {
                  ?entity dct:subject dbc:%s .
                  ?entity rdfs:label ?label .
                  ?entity dbo:abstract ?abstract .
                  FILTER(LANG(?label) = 'en')
                  FILTER(LANG(?abstract) = 'en')
                }
                LIMIT %d
                """, category, limit);

        List<EntityInfo> results = executeQuery(query, rs -> {
            List<EntityInfo> entities = new ArrayList<>();
            
            while (rs.hasNext()) {
                QuerySolution solution = rs.next();
                String uri = solution.getResource("entity").getURI();
                String label = solution.getLiteral("label").getString();
                String description = solution.getLiteral("abstract").getString();
                
                entities.add(new EntityInfo(uri, label, description, List.of()));
            }
            
            return entities;
        });

        if (results.isEmpty()) {
            return "No entities found in category: " + category;
        }

        return "Entities in category " + category + ":\n" +
                results.stream()
                        .map(EntityInfo::toText)
                        .collect(Collectors.joining("\n"));
    }

    private <T> List<T> executeQuery(String queryString, ResultSetMapper<T> mapper) {
        try {
            // Set default HTTP headers for all SPARQL requests
            // HttpEnv.setUserAgent("DBpediaClientJena/1.0 (application/research)");
            
            // Create and configure the query execution
            QueryExecution qe = QueryExecution.service(ENDPOINT_URL)
                .query(QueryFactory.create(queryString))
                .timeout(10000)
                .build();
            
            try (qe) {
                ResultSet results = qe.execSelect();
                return mapper.map(results);
            }
        } catch (Exception e) {
            System.err.println("Query execution error: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    @FunctionalInterface
    private interface ResultSetMapper<T> {
        List<T> map(ResultSet resultSet);
    }
}
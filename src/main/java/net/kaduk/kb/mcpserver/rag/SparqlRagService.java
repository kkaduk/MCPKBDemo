// package net.kaduk.kb.mcpserver.rag;

// import net.kaduk.kb.mcpserver.service.SparqlKnowledgeBaseService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.ai.chat.messages.Message;
// import org.springframework.ai.chat.messages.SystemMessage;
// import org.springframework.ai.chat.messages.UserMessage;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.ai.chat.prompt.Prompt;
// import org.springframework.ai.document.Document;
// import org.springframework.stereotype.Service;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// @Service
// @RequiredArgsConstructor
// public class SparqlRagService {

//     private final SparqlKnowledgeBaseService sparqlService;
//     private final ChatModel chatModel;
    
//     public String generateResponse(String userQuery) {
//         // 1. Retrieve relevant information from SPARQL
//         List<Map<String, String>> results = sparqlService.searchEntities(userQuery, 5);
        
//         // 2. Convert to documents
//         List<Document> documents = results.stream()
//                 .map(this::convertToDocument)
//                 .collect(Collectors.toList());
        
//         // 3. Build context from documents
//         String context = documents.stream()
//                 .map(Document::getContent)
//                 .collect(Collectors.joining("\n\n"));
        
//         // 4. Create messages for the prompt
//         List<Message> messages = new ArrayList<>();
//         messages.add(new SystemMessage(
//                 "You are a helpful assistant with access to a knowledge base. " +
//                 "Use the following information to answer the user's question:\n\n" +
//                 context + 
//                 "\n\nIf the knowledge base doesn't contain relevant information, " +
//                 "you can answer based on your general knowledge."));
//         messages.add(new UserMessage(userQuery));
        
//         // 5. Create prompt and call model
//         Prompt prompt = new Prompt(messages);
//         return chatModel.call(prompt).getResult().getOutput().getContent();
//     }
    
//     private Document convertToDocument(Map<String, String> result) {
//         // Same implementation as before
//         String entityUri = result.getOrDefault("entity", "");
//         String label = result.getOrDefault("label", "Unknown");
//         String description = result.getOrDefault("description", "");
        
//         StringBuilder contentBuilder = new StringBuilder();
//         contentBuilder.append("Entity: ").append(label).append("\n");
        
//         if (!description.isEmpty()) {
//             contentBuilder.append("Description: ").append(description).append("\n");
//         }
        
//         if (!entityUri.isEmpty()) {
//             List<Map<String, String>> properties = sparqlService.getEntityProperties(entityUri, 20);
            
//             contentBuilder.append("Properties:\n");
//             properties.forEach(prop -> {
//                 String property = prop.getOrDefault("property", "");
//                 String value = prop.getOrDefault("value", "");
//                 if (!property.isEmpty() && !value.isEmpty()) {
//                     String propertyName = property;
//                     if (property.contains("#") || property.contains("/")) {
//                         propertyName = property.substring(Math.max(
//                                 property.lastIndexOf('#'), property.lastIndexOf('/')) + 1);
//                     }
//                     contentBuilder.append("- ").append(propertyName).append(": ").append(value).append("\n");
//                 }
//             });
//         }
        
//         return new Document(contentBuilder.toString(), Map.of(
//                 "entityUri", entityUri,
//                 "label", label,
//                 "source", "SPARQL Knowledge Base"
//         ));
//     }
// }
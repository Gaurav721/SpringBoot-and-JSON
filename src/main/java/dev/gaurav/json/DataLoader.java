package dev.gaurav.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gaurav.json.post.Post;
import dev.gaurav.json.post.PostRepository;
import org.springframework.asm.TypeReference;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Component
public class DataLoader implements CommandLineRunner {

    List<Post> posts=new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;
    private static final String DATE_PATTERN = "MM/dd/yyyy";

    public DataLoader(ObjectMapper objectMapper, PostRepository postRepository) {
        this.objectMapper = objectMapper;
        this.postRepository = postRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        JsonNode json;
        try(InputStream inputStream= TypeReference.class.getResourceAsStream("/data/blog.json")){
            json = objectMapper.readValue(inputStream,JsonNode.class);
        }catch (IOException e){
            throw new RuntimeException("failed to read Json Data",e);
        }

        JsonNode edges = getEdges(json);
        for(JsonNode edge:edges){
            posts.add(createPostFromNode(edge));
        }
        postRepository.saveAll(posts);
    }

    private Post createPostFromNode(JsonNode edge) {
        JsonNode node = edge.get("node");
        String id = node.get("id").asText();
        String title = node.get("title").asText();
        String slug = node.get("slug").asText();
        String date = node.get("date").asText();
        int timeToRead = node.get("timeToRead").asInt();
        String tags = extractTags(node);

        return new Post(id, title, slug, LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN)), timeToRead, tags, null);
    }

    private JsonNode getEdges(JsonNode json) {
       return Optional.ofNullable(json)
               .map(j -> j.get("data"))
               .map(j -> j.get("allPost"))
               .map(j -> j.get("edges"))
               .orElseThrow(() -> new IllegalArgumentException("Invalid JSON Object"));
    }

    private String extractTags(JsonNode node) {
        StringBuilder sb = new StringBuilder();

        JsonNode tags = node.get("tags");
        for (int i = 0; i < tags.size(); i++) {
            sb.append(tags.get(i).get("title").asText());
            if(i < tags.size() - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }
}

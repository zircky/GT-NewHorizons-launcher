package zi.zircky.gtnhlauncher.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpUtils {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String USER_AGENT = "GTNHLauncher/1.0";
  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  public static JsonNode postJson(String url, String body) throws IOException, InterruptedException {
    return postJson(url, body, null);
  }

  public static JsonNode postJson(String url, String body, Map<String, String> extraHeaders) throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(REQUEST_TIMEOUT)
        .header("User-Agent", USER_AGENT)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body));

    if (extraHeaders != null) {
      extraHeaders.forEach(builder::header);
    }
    return sendJsonRequest(builder.build());
  }

  public static JsonNode getJson(String url, String bearerToken) throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(REQUEST_TIMEOUT)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json");

    if (bearerToken != null && !bearerToken.isBlank()) {
      builder.header("Authorization", "Bearer " + bearerToken);
    }

    return sendJsonRequest(builder.GET().build());
  }

  private static JsonNode sendJsonRequest(HttpRequest request) throws IOException, InterruptedException {
    HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    HttpHeaders headers = response.headers();

    int statusCode = response.statusCode();
    if (statusCode < 200 || statusCode >= 300) {
      throw new IOException(
          "HTTP " + statusCode + " from " + request.uri() + ": " + bodyPreview(response.body()));
    }

    String contentType = headers.firstValue("Content-Type").orElse("");
    if (!contentType.toLowerCase().contains("application/json")) {
      throw new IOException("Expected JSON but got '" + contentType + "' from " + request.uri());
    }

    try {
      return mapper.readTree(response.body());
    } catch (IOException e) {
      throw new IOException(
          "Failed to parse JSON from " + request.uri() + ": " + bodyPreview(response.body()), e);
    }
  }

  private static String bodyPreview(String body) {
    if (body == null) {
      return "<no body>";
    }

    String trimmed = body.strip();
    if (trimmed.length() <= 300) {
      return trimmed;
    }

    return trimmed.substring(0, 300) + "_";
  }


}

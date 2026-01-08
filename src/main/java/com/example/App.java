package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
	private static String GITHUB_TOKEN;
	private static String REPO_OWNER;
	private static String REPO_NAME;

	public static Map<String, String> loadEnv(String filePath) throws Exception {
		try (Stream<String> env = Files.lines(Paths.get(filePath))) {
			return env.filter(line -> line.contains("=") && !line.startsWith("#"))
					.map(line -> line.split("=", 2))
					.collect(Collectors.toMap(
							parts -> parts[0].trim(),
							parts -> parts[1].trim()
					));
		}
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> env = loadEnv(".env");

		GITHUB_TOKEN = env.get("GITHUB_TOKEN");
		REPO_OWNER = env.get("REPO_OWNER");
		REPO_NAME = env.get("REPO_NAME");
		System.out.println("GITHUB_TOKEN: " + GITHUB_TOKEN);
		System.out.println("REPO_OWNER: " + REPO_OWNER);
		System.out.println("REPO_NAME: " + REPO_NAME);

		triggerWorkflow("Hello from my Java App!");
	}

	public static void triggerWorkflow(String message) {
		String url = String.format("https://api.github.com/repos/%s/%s/dispatches", REPO_OWNER, REPO_NAME);

		String jsonPayload = """
				{
				  "event_type": "trigger",
				  "client_payload": {
				    "user": "JavaAppUser",
				    "message": "%s"
				  }
				}
				""".formatted(message);

		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Authorization", "Bearer " + GITHUB_TOKEN)
					.header("Accept", "application/vnd.github+json")
					.header("X-GitHub-Api-Version", "2022-11-28")
					.header("User-Agent", "Java-HttpClient")
					.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
					.build();

			client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
					.thenAccept(response -> {
						if (response.statusCode() == 204) {
							System.out.println("Workflow triggered successfully!");
						} else {
							System.err.println("Failed to trigger. Status code: " + response.statusCode());
							System.err.println("body: " + response.body());
						}
					})
					.join();
		}
	}
}
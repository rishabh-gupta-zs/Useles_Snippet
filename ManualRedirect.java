import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class ManualRedirectLogin {

	private RestTemplate restTemplate;

	public ManualRedirect() {
		CloseableHttpClient httpClient = HttpClients.custom()
			.setDefaultRequestConfig(RequestConfig.custom()
				.setRedirectsEnabled(false) // Disable automatic redirect
				.build())
			.build();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		restTemplate = new RestTemplate(factory);
		restTemplate.getInterceptors().add(new RedirectLoggingInterceptor());
	}

	private final Map<String, String> cookies = new HashMap<>();
	private final String SERVICE_URL = "https://URL";

	private ResponseEntity<String> httpCallExecutor(URI uri, HttpMethod httpMethod, HttpEntity<String> requestEntity)
		throws URISyntaxException {

		ResponseEntity<String> response = restTemplate.exchange(uri, httpMethod, requestEntity, String.class);
		if(Objects.nonNull(response.getHeaders().get("Set-Cookie"))){
			String cookie = response.getHeaders().get("Set-Cookie").stream().map(el->el.split(";")[0]).reduce((a,b)->a+b).orElse("; ");
			cookies.put(uri.getHost(), cookie);
		}
		if(response.getStatusCode().is3xxRedirection()){
			String location = response.getHeaders().get("location").get(0);
			URI locationUri = new URI(location);
			HttpHeaders headers = new HttpHeaders();
			if(Objects.nonNull(cookies.get(locationUri.getHost())))
				headers.set("Cookie", cookies.get(locationUri.getHost()));
			HttpEntity<String> httpEntity = new HttpEntity<>( headers);
			return httpCallExecutor(locationUri, HttpMethod.GET, httpEntity);
		}
		return response;
	}

	public String performLogin() throws URISyntaxException {
		URI ServiceUri = new URI(SERVICE_URL);
		ResponseEntity<String> response = httpCallExecutor(ServiceUri, HttpMethod.GET, null);
		String token = getToken(response);

		String loginPayload = "username=#####&password=####&_token="+token;

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<String> loginRequest = new HttpEntity<>(loginPayload, headers);

		httpCallExecutor(new URI("https://Login_service"),HttpMethod.POST, loginRequest);

		ResponseEntity<String> finalResponse = httpCallExecutor(
			new URI("https://MY_serbvice"),
			HttpMethod.GET, null);

		URI uri = new URI("https://MY_serbvice");
		headers = new HttpHeaders();
		if(Objects.nonNull(cookies.get(uri.getHost())))
			headers.set("Cookie", cookies.get(uri.getHost()));
		HttpEntity<String> httpEntity = new HttpEntity<>( headers);
		response = httpCallExecutor(uri, HttpMethod.GET, httpEntity);
		System.out.println("---");
		System.out.println(response.getBody());
		System.out.println("---");
		return response.getBody();

	}

	private String getToken(ResponseEntity<String> htmlRes) {
		Document document = Jsoup.parse(htmlRes.getBody());

		return document.select("input[name=_token]").attr("value");
	}
}
class RedirectLoggingInterceptor implements ClientHttpRequestInterceptor
{
	@Override
	public org.springframework.http.client.ClientHttpResponse intercept(
		org.springframework.http.HttpRequest request, byte[] body,
		org.springframework.http.client.ClientHttpRequestExecution execution) throws java.io.IOException {

		System.out.println("Making request to: " + request.getURI());

		org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);

		// If a redirect is detected, log it
		if (response.getStatusCode().is3xxRedirection()) {
			System.out.println("Redirect detected to: " + response.getHeaders().getLocation());
		}

		return response;
	}
}
public class WorkspaceApplication {

	public static void main(String[] args) throws URISyntaxException
	{
		System.out.println(new ManualRedirectLogin().performLogin());
	}
}

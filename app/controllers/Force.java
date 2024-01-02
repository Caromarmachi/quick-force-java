package controllers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.index;
import views.html.setup;
import views.html.indexcontacts;
import java.util.Set;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class Force {
@Inject
WSClient ws;

@Inject
Config config;

private String recherche;


public String getRecherche() {
	return this.recherche;
}

public void setRecherche(String recherche) {
	this.recherche = recherche;
}


String consumerKey() {
	System.out.println("Classe Force Methode consumerKey");

    return config.getString("consumer.key");
}

String consumerSecret() {
	System.out.println("Classe Force Methode consumerSecret");
	
    return config.getString("consumer.secret");
}

CompletionStage<AuthInfo> getToken(String code, String redirectUrl) {
	System.out.println("Classe Force Methode getToken");

    final CompletionStage<WSResponse> responsePromise = ws.url("https://login.salesforce.com/services/oauth2/token")
            .addQueryParameter("grant_type", "authorization_code")
            .addQueryParameter("code", code)
            .addQueryParameter("client_id", consumerKey())
            .addQueryParameter("client_secret", consumerSecret())
            .addQueryParameter("redirect_uri", redirectUrl)
            .execute(Http.HttpVerbs.POST);

    return responsePromise.thenCompose(response -> {
        final JsonNode jsonNode = response.asJson();

        if (jsonNode.has("error")) {
            CompletableFuture<AuthInfo> completableFuture = new CompletableFuture<>();
            completableFuture.completeExceptionally(new AuthException(jsonNode.get("error").textValue()));
            return completableFuture;
        } else {
            return CompletableFuture.completedFuture(Json.fromJson(jsonNode, AuthInfo.class));
        }
    });
}




	CompletionStage<List<Account>> getAccounts(AuthInfo authInfo) {
		System.out.println(" Methode getAccounts");

		CompletionStage<WSResponse> responsePromise = ws.url(authInfo.instanceUrl + "/services/data/v59.0/query/")
				.addHeader("Authorization", "Bearer " + authInfo.accessToken)
				.addQueryParameter("q", "SELECT Id, Name, Active__c, Type, Industry, Rating FROM Account").get();

		return responsePromise.thenCompose(response -> {
			final JsonNode jsonNode = response.asJson();
			if (jsonNode.has("error")) {
				CompletableFuture<List<Account>> completableFuture = new CompletableFuture<>();
				completableFuture.completeExceptionally(new AuthException(jsonNode.get("error").textValue()));
				return completableFuture;
			} else {
				QueryResultAccount queryResultAccount = Json.fromJson(jsonNode, QueryResultAccount.class);
				return CompletableFuture.completedFuture(queryResultAccount.records);
			}
		});
	}



	CompletionStage<List<Contact>> getContacts(AuthInfo authInfo) {
		System.out.println("Methode getContacts");

//    System.out.println(request().getQueryString("email"));
		String soqlRecherche = "";
		if (this.recherche != null) {
			soqlRecherche = "where Email='" + this.recherche + "'";

			CompletionStage<WSResponse> responsePromise = ws.url(authInfo.instanceUrl + "/services/data/v59.0/query/")
					.addHeader("Authorization", "Bearer " + authInfo.accessToken).addQueryParameter("q",
							"SELECT Id, LastName, Email, AccountId, Phone FROM Contact " + soqlRecherche)
					.get();
			return responsePromise.thenCompose(response -> {
				final JsonNode jsonNode = response.asJson();
				if (jsonNode.has("error")) {
					CompletableFuture<List<Contact>> completableFuture = new CompletableFuture<>();
					completableFuture.completeExceptionally(new AuthException(jsonNode.get("error").textValue()));
					return completableFuture;
				} else {
					// System.out.println(jsonNode.toString());

					QueryResultContact queryResultContact = Json.fromJson(jsonNode, QueryResultContact.class);
					return CompletableFuture.completedFuture(queryResultContact.records);
				}
			});
		} else {
			List<Contact> listeVide = new ArrayList<Contact>();

			return CompletableFuture.completedFuture(listeVide);
		}
	}

}

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

public class Application extends Controller {
    @Inject
    private Force force;
    
    private boolean isSetup() {
    	System.out.println("Methode isSetup");

        try {
            force.consumerKey();
            force.consumerSecret();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String oauthCallbackUrl(Http.Request request) {
    	System.out.println("Methode oauthCallbackUrl");

        return (request.secure() ? "https" : "http") + "://" + request.host();
    }

    public CompletionStage<Result> index(String code) {
    	System.out.println("Methode index");

        if (isSetup()) {
            if (code == null) {
                // start oauth
                final String url = "https://login.salesforce.com/services/oauth2/authorize?response_type=code" +
                        "&client_id=" + force.consumerKey() +
                        "&redirect_uri=" + oauthCallbackUrl(request());
                return CompletableFuture.completedFuture(redirect(url));
            } else {
                return force.getToken(code, oauthCallbackUrl(request())).thenCompose(authInfo ->
                        force.getAccounts(authInfo).thenApply(accounts ->
                                ok(index.render(accounts))
                        )
                ).exceptionally(error -> {
                    if (error.getCause() instanceof Force.AuthException)
                        return redirect(routes.Application.index(null));
                    else
                        return internalServerError(error.getMessage());
                });
            }
        } else {
            return CompletableFuture.completedFuture(redirect(routes.Application.setup()));
        }
    }
    
    public CompletionStage<Result> indexcontacts(String code) { // C1
    	System.out.println("methode indexcontacts");
 
    	
//        final Set<Map.Entry<String,String[]>> entries = request().queryString().entrySet();
//        for (Map.Entry<String,String[]> entry : entries) {
//            final String key = entry.getKey();
//            final String value = Arrays.toString(entry.getValue());
//            System.out.println(key + " " + value);
//        }
 //       System.out.println(request().getQueryString("email"));
    	String paramRecherche = request().getQueryString("email");
    	if (paramRecherche!=null) {
    		this.force.setRecherche(paramRecherche);
    	}

    	
    	
        if (isSetup()) {
            if (code == null) {
                // start oauth
                final String url = "https://login.salesforce.com/services/oauth2/authorize?response_type=code" +
                        "&client_id=" + force.consumerKey() +
                        "&redirect_uri=" + oauthCallbackUrl(request());
                return CompletableFuture.completedFuture(redirect(url));
            } else {
                return force.getToken(code, oauthCallbackUrl(request())).thenCompose(authInfo ->
                        force.getContacts(authInfo).thenApply(contacts ->
                                ok(indexcontacts.render(contacts))
                        )
                ).exceptionally(error -> {
                    if (error.getCause() instanceof Force.AuthException)
                        return redirect(routes.Application.indexcontacts(null));
                    else
                        return internalServerError(error.getMessage());
                });
            }
        } else {
            return CompletableFuture.completedFuture(redirect(routes.Application.setup()));
        }
    }    

    public Result setup() {
    	System.out.println("Methode setup");

        if (isSetup()) {
           // return redirect(routes.Application.index(null));
            return redirect(routes.Application.indexcontacts(null));
            
        } else {
            final String maybeHerokuAppName = request().host().split(".herokuapp.com")[0].replaceAll(request().host(), "");
            return ok(setup.render(maybeHerokuAppName));
        }
    }


    @Singleton
    public static class Force {
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

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Account {
            public String Id;
            public String Name;
            public String Active__c;            
            public String Type;
            public String Industry;
            public String Rating;
        }
        
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Contact {
            public String Id;
          //  public String External_Id__c;            
            public String AccountId;
            public String Email;            
            public String LastName;
            public String Phone;
            
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class QueryResultAccount {
            public List<Account> records;
        }
        
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class QueryResultContact {
            public List<Contact> records;
        }        

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AuthInfo {
            @JsonProperty("access_token")
            public String accessToken;

            @JsonProperty("instance_url")
            public String instanceUrl;
        }

        public static class AuthException extends Exception {
            AuthException(String message) {
                super(message);
            }
        }

        CompletionStage<List<Account>> getAccounts(AuthInfo authInfo) {
            System.out.println(" Methode getAccounts");


            CompletionStage<WSResponse> responsePromise = ws.url(authInfo.instanceUrl + "/services/data/v59.0/query/")
                    .addHeader("Authorization", "Bearer " + authInfo.accessToken)
                    .addQueryParameter("q", "SELECT Id, Name, Active__c, Type, Industry, Rating FROM Account")
                    .get();

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
        
//        public String Id;
//        public String External_Id__c;            
//        public String AccountId;
//        public String Email;            
//        public String Lastname;
        
        CompletionStage<List<Contact>> getContacts(AuthInfo authInfo) {
            System.out.println("Methode getContacts");

        //    System.out.println(request().getQueryString("email"));
            String soqlRecherche = "";
            if (this.recherche!=null) {
            	soqlRecherche = "where Email='" + this.recherche + "'";	
            

	            CompletionStage<WSResponse> responsePromise = ws.url(authInfo.instanceUrl + "/services/data/v59.0/query/")
	                    .addHeader("Authorization", "Bearer " + authInfo.accessToken)
	                    .addQueryParameter("q", "SELECT Id, LastName, Email, AccountId, Phone FROM Contact " + soqlRecherche)
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

}

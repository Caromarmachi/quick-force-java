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



}

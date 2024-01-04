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

/**
 * 
 * @author Carolina
 * @description Classe principale de l'application,
 * controller du framework play
 * controle les différentes pages et l'ordonnancement des appels
 *
 */

public class Application extends Controller {
    @Inject
    private Force force;
   
    /**
     * 
     * @description Methode isSetup qui verifie si l'application dispose des 2 codes 
     * consumer.key et consumer.secret. Sinon, elle dirige vers une page d'information
     *
     */    
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

    /**
     * 
     * @description Methode qui precise l'URL de callback dans le cadre de l'authentification oauth
     *
     */      
    private String oauthCallbackUrl(Http.Request request) {
    	System.out.println("Methode oauthCallbackUrl");

        return (request.secure() ? "https" : "http") + "://" + request.host();
    }

    
    /**
     * 
     * @description Methode qui affiche la "route" index. 
     * Cette méthode utilise l'authentification oauth  (via la classe Force) puis redirige vers la route "index"
     * C'est une page qui liste tous les AccountSalesforce.
     */     
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
                    if (error.getCause() instanceof AuthException)
                        return redirect(routes.Application.index(null));
                    else
                        return internalServerError(error.getMessage());
                });
            }
        } else {
            return CompletableFuture.completedFuture(redirect(routes.Application.setup()));
        }
    }
    
    /**
     * 
     * @description Methode qui affiche la "route" indexContact. 
     * Cette méthode utilise l'authentification oauth (via la classe Force) puis redirige vers la route "indexContacts"
     * C'est une page qui affiche un formulaire de recherche de contact par email.
     */     
    public CompletionStage<Result> indexcontacts(String code) { // C1
    	System.out.println("methode indexcontacts");
 
    	
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
                    if (error.getCause() instanceof AuthException)
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
            return redirect(routes.Application.indexcontacts(null));
            
        } else {
            final String maybeHerokuAppName = request().host().split(".herokuapp.com")[0].replaceAll(request().host(), "");
            return ok(setup.render(maybeHerokuAppName));
        }
    }



}

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
 * @description Classe de mapping Java / SalesForce pour l'objet Contact (partiel)
 *
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public  class Contact {
	public String Id;
	// public String External_Id__c;
	public String AccountId;
	public String Email;
	public String LastName;
	public String Phone;

}

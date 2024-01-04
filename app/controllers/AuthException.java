package controllers;

/**
 * 
 * @author Carolina
 * @description Classe d'exception specifique au projet (utilisée pour la connexion)
 *
 */
public  class AuthException extends Exception {
	AuthException(String message) {
		super(message);
	}

}

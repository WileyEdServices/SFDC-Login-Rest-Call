import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * This program demonstrates the following basic use cases for the REST API: -
 * authentication with OAuth 2.0 (This is for example development purposes only.
 * Not a real implementation.)
 * 
 * @author Wiley Ed Services
 */
public class RestTest extends Object {
	// ---------Credentials----------
	// Credentials providing access to a specific Salesforce organization.
	private static final String userName = getProperty("username"); // Get Username from Properties
	private static final String password = getProperty("password"); // Get Password from Properties
	// ---------REST and OAuth-------
	// Portions of the URI for REST access that are re-used throughout the code
	private static String OAUTH_ENDPOINT = getProperty("OAUTH_ENDPOINT");
	private static String REST_ENDPOINT = getProperty("REST_ENDPOINT");
	// Holds URI returned from OAuth call, which is then used throughout the
	// code.
	String baseUri;
	// The oauthHeader set in the oauth2Login method, and then added to
	// each HTTP object that is used to invoke the REST API.
	Header oauthHeader;
	// Basic header information added to each HTTP object that is used
	// to invoke the REST API.
	Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
	// ----------Utility-------------
	// Used to get input from console.
	private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	// ================Code starts here===================
	public static void main(String[] args) throws ParseException {
		new RestTest();
	}
	/**
	 * This class holds all the values related to the credentials needed to make
	 * the OAuth2 request for authentication.
	 */
	class UserCredentials {
		String loginInstanceDomain = getProperty("loginInstanceDomain"); // USE YOUR SERVER INSTANCE
		String apiVersion = "37"; // USE YOUR API VERSION
		String userName = RestTest.userName;
		String password = RestTest.password;
		String consumerKey = getProperty("consumer_key"); // USE YOUR CONSUMER KEY																												// KEY
		String consumerSecret = getProperty("consumer_secret"); //USE YOUR CONSUMER SECRET
		String grantType = "password";
		String redirect = getProperty("redirect");
	}
	/**
	 * Constructor drives console interaction and calls appropriate methods.
	 * 
	 * @throws ParseException
	 */
	public RestTest() throws ParseException {
		int exitCount = 1;
		int loginCount = 0;
		while (exitCount != 0) {
			showMenu();
			boolean invalidValue = true;
			int executionOption = 99;
			String choice = getUserInput("Enter option: ");
			while (invalidValue) {
				try {
					executionOption = Integer.parseInt(choice);
					if ((executionOption < 1 || executionOption > 2) && executionOption != 99) {
						System.out.println("Please enter 1, 2 or 99.\n");
						choice = getUserInput("Enter the number of the sample to run: ");
						showMenu();
					} else {
						invalidValue = false;
					}
				} catch (Exception e) {
					System.out.println("Invalid value. Please enter 1, 2 or 99.\n");
					choice = getUserInput("Enter the number of the sample to run: ");
					showMenu();
				}
			}
			if (executionOption == 99) {
				System.out.println("No action taken");
				exitCount = 0;
			} else {
				// Login is done for option 1, as well as all other valid
				// options.
				if(executionOption == 1){
					loginCount = 0;
				}
				if (loginCount == 0) {
					this.oauth2Login();
					loginCount++;
				}
				if (executionOption == 2) {
					this.restPostExample();
				}
			}
			System.out.println("Program complete.");
		}
	}
	/**
	 * This method connects the program to the Salesforce organization using
	 * OAuth. It stores returned values for further access to organization.
	 * 
	 * @param userCredentials
	 * Contains all credentials necessary for login
	 * @return
	 * @throws ParseException
	 */
	public HttpResponse oauth2Login() throws ParseException {
		System.out.println("_______________ Login _______________");
		OAuth2Response oauth2Response = null;
		HttpResponse response = null;
		UserCredentials userCredentials = new UserCredentials();
		String loginHostUri = "https://" + userCredentials.loginInstanceDomain + OAUTH_ENDPOINT;
		try {
			// Construct the objects for making the request
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(loginHostUri);
			StringBuffer requestBodyText = new StringBuffer("grant_type=password");
			requestBodyText.append("&username=");
			requestBodyText.append(userCredentials.userName);
			requestBodyText.append("&password=");
			requestBodyText.append(userCredentials.password);
			requestBodyText.append("&client_id=");
			requestBodyText.append(userCredentials.consumerKey);
			requestBodyText.append("&client_secret=");
			requestBodyText.append(userCredentials.consumerSecret);
			System.out.println("Parameters : " + requestBodyText.toString() + "\n\n");
			StringEntity requestBody = new StringEntity(requestBodyText.toString());
			requestBody.setContentType("application/x-www-form-urlencoded");
			httpPost.setEntity(requestBody);
			httpPost.addHeader(prettyPrintHeader);
			// Make the request and store the result
			response = httpClient.execute(httpPost);
			// Parse the result if we were able to connect.
			if (response.getStatusLine().getStatusCode() == 200) {
				String response_string = EntityUtils.toString(response.getEntity());
				JSONParser parser = new JSONParser();
				JSONObject json = (JSONObject) parser.parse(response_string);
				oauth2Response = new OAuth2Response(json);
				System.out.println("JSON returned by LOGIN response: ++++++++\n" + json.toJSONString()+"\n\n");
				baseUri = oauth2Response.instance_url + REST_ENDPOINT + "/v" + userCredentials.apiVersion + ".0";
				System.out.println("The Base URI is :" + baseUri + "\n\n");
				oauthHeader = new BasicHeader("Authorization", "OAuth " + oauth2Response.access_token);
				System.out.println("The OAuth token is :" + oauthHeader + "\n\n");
				System.out.println("\nSuccessfully logged in to instance: " + baseUri + "\n\n");
			} else {
				System.out.println(
						"An error has occured. Http status: " + response.getStatusLine().getStatusCode() + "\n\n");
				System.out.println(getBody(response.getEntity().getContent()) + "\n\n");
				System.exit(-1);
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		return response;
	}
	/**
	 * This method demonstrates - How to use HTTPPost and a constructed URI to
	 * retrieve data from SFDC. - Simple creation of a JSON object.
	 */
	public void restPostExample() {
		System.out.println("\n_______________ OAuthForSFDCAccess _______________");
		String uri = baseUri + getProperty("endpoint");
		System.out.println("URI Value :  " + uri+"\n\n");
		try {
			// create the JSON object containing the new contact details.
			/*
			 * This JSON will be provided after a partner has been on-boarded
			 * 		{
			 *		"Unique_Identifier": "Unique_Identifier_Value",
			 *		"Parameter 1": [
			 *		"Value 1",
			 *		"Value 2"
			 *		],
			 *		"Authentication_Key": "Authentication_Key_Value"
			 *		}
			 */
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader("post.json"));
			JSONObject applicant = (JSONObject) obj;
			System.out.println("JSON for retriving record between dates:\n" + applicant.toString()+"\n\n");
			// Construct the objects needed for the request
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);
			// The message we are going to post
			StringEntity body = new StringEntity(applicant.toString());
			body.setContentType("application/json");
			httpPost.setEntity(body);
			// Make the request
			HttpResponse response = httpClient.execute(httpPost);
			// Process the results
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 201 || statusCode == 200) {
				String response_string = EntityUtils.toString(response.getEntity());
				JSONParser parser1 = new JSONParser();
				JSONObject json = (JSONObject) parser1.parse(response_string);
				System.out.println("Retrive Successful! Status code returned is " + statusCode+"\n\n");
				System.out.println("Response from SFDC: " + json.toJSONString()+"\n\n");
				System.out.println("httpPOST: " + httpPost+"\n\n");
				// Store the retrieved data as follows.
				// eg.....
				// contactId = json.getString("id");
			} else {
				System.out.println("Retrive unsuccessful. Status code returned is " + statusCode+"\n\n");
				System.out.println(getBody(response.getEntity().getContent()));
				System.out.println("httpPOST: " + httpPost+"\n\n");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Extend the Apache HttpPost method to implement an HttpPost method.
	 */
	private static class HttpPatch extends HttpPost {
		public HttpPatch(String uri) {
			super(uri);
		}
		public String getMethod() {
			return "PATCH";
		}
	}
	/**
	 * This class is used to hold values returned by the OAuth request.
	 */
	static class OAuth2Response {
		String id;
		String issued_at;
		String instance_url;
		String signature;
		String access_token;
		public OAuth2Response() {
		}
		public OAuth2Response(JSONObject json) {
			id = (String) json.get("id");
			issued_at = (String) json.get("issued_at");
			instance_url = (String) json.get("instance_url");
			signature = (String) json.get("signature");
			access_token = (String) json.get("access_token");
		}
	}
	// ==========utility methods=============
	/**
	 * Utility method for changing a stream into a String.
	 * 
	 * @param inputStream
	 * @return String
	 */
	private String getBody(InputStream inputStream) {
		String result = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				result += inputLine;
				result += "\n";
			}
			in.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}
	// --------------utility methods for user input----------
	/**
	 * A utility method to be used for getting user input from the console.
	 * 
	 * @param prompt
	 * @return String
	 */
	private String getUserInput(String prompt) {
		String result = "";
		try {
			System.out.print(prompt);
			result = reader.readLine();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}
	/**
	 * Outputs menu choices on console.
	 */
	private void showMenu() {
		System.out.println("\n");
		System.out.println(" 1. LOGIN ONLY");
		System.out.println(" 2. TEST API ONLY (Logs-in for the first time ONLY)");
		System.out.println("99. EXIT");
		System.out.println("   ");
	}
	/**
	 * Get Properties from login_config.properties.
	 * 
	 * @param propertyName
	 * @return String
	 */
	public static String getProperty(String propertyName) {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("login_config.properties");
			// load a properties file
			prop.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// get the property value and return it
		return prop.getProperty(propertyName);
	}
}
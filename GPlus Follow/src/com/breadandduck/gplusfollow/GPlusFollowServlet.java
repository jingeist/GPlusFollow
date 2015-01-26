package com.breadandduck.gplusfollow;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.*;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.plusDomains.PlusDomains;
import com.google.api.services.plusDomains.PlusDomainsScopes;
import com.google.api.services.plusDomains.model.Circle;
import com.google.api.services.plusDomains.model.CircleFeed;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("serial")

public class GPlusFollowServlet extends HttpServlet {
	private static final String CIRCLE_NAME = "Following";

	private static final Logger log = Logger.getLogger(GPlusFollowServlet.class.getName());
	
	private static final String APPLICATION_NAME = "jj-gplusfollow";	
	private static final String SERVICE_ACCOUNT_EMAIL = 
			"105194108261-24o5a60ufq49a3guj4ls8m9urep6fh5t@developer.gserviceaccount.com";
	private static final String SERVICE_ACCOUNT_PKCS12_FILE =
			"Plus Follow-8665b4395c21.p12";
	private static final String P12_PASSWORD = 
			"notasecret";
	private static String runAs = new String();
	
	private static PrivateKey key = null;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		String page="<form action=\"\" method=\"post\">" + 
					"<div style=\"width:250;text-align:right;float:left\">Start on page:</div><input type=\"text\" name=\"Start\" value=\"1\"><br>" + 
					"<div style=\"width:250;text-align:right;float:left\">End on page:</div><input type=\"text\" name=\"End\" value=\"1\"><br>" + 
					"<div style=\"width:250;text-align:right;float:left\">Number of users per Page:</div><input type=\"text\" name=\"PageSize\" value=\"10\"><br>" +
					"<div style=\"width:250;text-align:right;float:left\">Email address to follow:</div><input type=\"text\" name=\"Follow\"><br>" +
					"<div style=\"width:250;text-align:right;float:left\">Admin email address to run as:</div><input type=\"text\" name=\"RunAs\"><br>" +
					"<input type=\"submit\" value=\"Submit\" onclick=\"this.disabled=true;this.value='Processing...';this.form.submit();\">" +
					"</form>" +
					"<div>In order to use this application for your domain you must add the application to your <a href=\"https://admin.google.com/AdminHome?chromeless=1#OGX:ManageOauthClients\">Admin Panel</a>." + 
					"<div><b>Client Name:</b></div>" + 
					"<div>105194108261-24o5a60ufq49a3guj4ls8m9urep6fh5t.apps.googleusercontent.com</div>" + 
					"<div><b>Scopes:</b>" + 
					"<div>https://www.googleapis.com/auth/admin.directory.user,</div>" + 
					"<div>https://www.googleapis.com/auth/plus.circles.read,</div>" +
					"<div>https://www.googleapis.com/auth/plus.circles.write,</div>" + 
					"<div>https://www.googleapis.com/auth/plus.me</div>";

		resp.getWriter().println(page);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		String argStartPage = (req.getParameter("Start") != null) ? req.getParameter("Start"):"1";
		Integer startPage = new Integer(argStartPage);

		String argEndPage = (req.getParameter("End") != null) ? req.getParameter("End"):"100";
		Integer endPage = new Integer(argEndPage);

		String argPageSize = (req.getParameter("PageSize") != null) ? req.getParameter("PageSize"):"100";
		Integer pageSize = new Integer(argPageSize);

		String[] accountsToFollow = (req.getParameter("Follow") != null) ? req.getParameter("Follow").split(","):null;

		runAs = (req.getParameter("RunAs") != null) ? req.getParameter("RunAs"):"";

		startPage = (startPage>0)? startPage:1;
		endPage = (endPage>startPage)? endPage:startPage;
		Integer lastPage = 1;
		
		Integer addedUsers=0;
		Integer suspendedUsers=0;
		Integer nonUsers=0;
		String htmlAddedUsers=new String();
		String htmlNonUsers=new String();
		String htmlSuspendedUsers=new String();

		
		resp.setContentType("text/html");
		if (null==accountsToFollow) {
			resp.getWriter().println("<p>No accounts to <i>Follow</i> have been provided.</p>");
			return;
		}
		
		//GPlusService gPlus = GPlusService.getService();
		//gPlus.setRunAs(runAs);
		
		try {
			// List the scopes your app requires. These must match the scopes
			// registered in the Admin console for your Google Apps domain.
			List<String> scope=Arrays.asList(DirectoryScopes.ADMIN_DIRECTORY_USER,
					PlusDomainsScopes.PLUS_ME,
					PlusDomainsScopes.PLUS_CIRCLES_READ,
					PlusDomainsScopes.PLUS_CIRCLES_WRITE);

			HttpTransport httpTransport = new NetHttpTransport();
			JsonFactory jsonFactory = new JacksonFactory();
			
			//get credentials!
			GoogleCredential service = authenticate(httpTransport,jsonFactory,scope);;
			Directory directory = new Directory.Builder(httpTransport, jsonFactory, service)
			.setApplicationName(APPLICATION_NAME)
			.build();


			//Get Users
			List<User> allUsers = new ArrayList<User>();
			Directory.Users.List usersRequest = directory.users().list()
					.setCustomer("my_customer")
					.setMaxResults(pageSize);
			log.info("Requesting user list");
			Users currentPage = usersRequest.execute();
			while (lastPage<startPage){
				log.warning("Skipping page " + lastPage.toString());
				usersRequest.setPageToken(currentPage.getNextPageToken());
				currentPage = usersRequest.execute();
				lastPage++;
				/*if (lastPage % 15 == 0){
					try {
					    Thread.sleep(1000);                 
					} catch(InterruptedException ex) {
					    Thread.currentThread().interrupt();
					}			
				}*/
			}
			resp.getWriter().println("<p>Running as " + runAs + "</p>");
			resp.getWriter().println("<p>Start Time: " + new Date().toString() + "</p>");
			resp.getWriter().println("<p>Processing pages " + startPage.toString() + " to " + endPage.toString() + "</p>");
			resp.getWriter().println("<p>Page size: " + pageSize.toString() + "</p>");
	        allUsers.addAll(currentPage.getUsers());
	        String[] userIdsToAdd = new String[accountsToFollow.length];
	        for (int i=0; i<accountsToFollow.length; i++){
		        Directory.Users.Get userRequest = directory.users().get(accountsToFollow[i]);
		        userIdsToAdd[i] = userRequest.execute().getId();
		        log.info(accountsToFollow[i] + " user id is " + userIdsToAdd);
	        }
			// Loop until no additional pages of results are available.
			while (allUsers != null) {
			    for (User currentUser : allUsers) {
			    	
			    	//check if the user is suspended
			    	if (currentUser.getSuspended()==false){
					
						//get Circles
				    	List<Circle> allCircles = new ArrayList<Circle>();
						  log.info(String.format("Getting circles for %s", currentUser.getPrimaryEmail()));
						GoogleCredential serviceUser = authenticateUser(httpTransport, jsonFactory, scope, currentUser.getPrimaryEmail());
						PlusDomains plus = new PlusDomains.Builder(httpTransport, jsonFactory, serviceUser)
							.setApplicationName(APPLICATION_NAME)
							.build();
				    	PlusDomains.Circles.List circlesRequest = plus.circles().list(currentUser.getId());
				    	try{
					    	CircleFeed currentCircles = GPlusService.executeWithExpBackoff(circlesRequest);
							  log.info(String.format("Loaded circles for %s", currentUser.getId()));
					    	allCircles.addAll(currentCircles.getItems());
					    	
					    	//Loop through Circles
							  log.info(String.format("Looping through circles"));
							String circleId="";
							while (allCircles != null){
						    	for (Circle currentCircle : allCircles){
									if (currentCircle.getDisplayName().equals(CIRCLE_NAME)){
										log.info("Circle found");
										circleId = currentCircle.getId();
										break;
									}
									
								}
						    	if (circleId == "" && currentCircles.getNextPageToken()!=null){
								      // Prepare the next page of results
								      circlesRequest.setPageToken(currentCircles.getNextPageToken());
		
								      // Execute and process the next page request
								      currentCircles = circlesRequest.execute();
								      allCircles.clear();
								      allCircles.addAll(currentCircles.getItems());				    		
						    	} else {
						    		allCircles=null;				    		
						    	}
							}
					    	
					    	//Create Following circle if it doesn't exist
					    	if (circleId==""){
					    		Circle newCircle=new Circle();
					    		newCircle.setDisplayName(CIRCLE_NAME);
					    		circleId = GPlusService.executeWithExpBackoff(plus.circles().insert(currentUser.getId(), newCircle)).getId();
					    	} 
					    	
					    	List<String> peopleList = new ArrayList<String>();
					    	peopleList.addAll(Arrays.asList(userIdsToAdd));
					    	
					    	PlusDomains.Circles.AddPeople addRequest = plus.circles().addPeople(circleId);
					    	
					    	addRequest.setUserId(peopleList);
					    	log.info("Adding People");
					    	GPlusService.executeWithExpBackoff(addRequest);
					    	addedUsers++;
				    		htmlAddedUsers += "<tr><td>" + addedUsers.toString() + "</td>" + 
				    				"<td><a href=\"https://plus.google.com/" + currentUser.getId() +  "\" target=\"_blank\">" + 
				    				currentUser.getPrimaryEmail() + "</a></td></tr>";
					    	//try {
							//    Thread.sleep(400);
							//} catch(InterruptedException ex) {
							//    Thread.currentThread().interrupt();
							//}
										
				    	} catch (GoogleJsonResponseException e) {
				    		//user is not on G+
				    		nonUsers++;
				    		htmlNonUsers += "<tr><td>" + nonUsers.toString() + "</td><td>" + 
				    				currentUser.getPrimaryEmail() + "</a></td><td>" + 
				    				"<td><textarea disabled><![CDATA[" + e.toString() + "]]></textarea></td></tr>";
							log.warning(e.toString());
				    	}
			    	} else {
			    		//user is suspended
			    		suspendedUsers++;
			    		htmlSuspendedUsers += "<tr><td>" + suspendedUsers.toString() + "</td><td>" + 
			    				currentUser.getPrimaryEmail() + "</a></td></tr>";
			    	}
			    }
			    // When the next page token is null, there are no additional pages of
			    // results. If this is the case, break.
			    if (currentPage.getNextPageToken() != null && lastPage<endPage) {
			      // Prepare the next page of results
			      usersRequest.setPageToken(currentPage.getNextPageToken());
			      lastPage++;
			      // Execute and process the next page request
			      currentPage = usersRequest.execute();
			      allUsers.clear();
			      allUsers.addAll(currentPage.getUsers());
			    } else {
			      allUsers = null;
			    }

			}

		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			resp.getWriter().println("<p>Aborting</p>");
			resp.getWriter().println("<p>" + e.toString() + "</p>");
			log.severe(e.toString());
			e.printStackTrace();
		} catch (Exception e){
			resp.getWriter().println("<p>Aborting</p>");
			resp.getWriter().println("<p>" + e.toString() + "</p>");
			log.severe(e.toString());
			e.printStackTrace();			
		}
		resp.getWriter().println("<p>End Time: " + new Date().toString() + "</p>");
		resp.getWriter().println("<p>Last page processed was " + lastPage.toString() + "<p>");
		resp.getWriter().println("<p>Total accounts processed was " + new Integer(addedUsers+nonUsers+suspendedUsers).toString() + "</p>");
		resp.getWriter().println("<table style=\"width:400px;float:left;border:solid 1px\"><tr><td colspan=\"2\"><b>" + 
					addedUsers.toString() + " users successfully followed " + accountsToFollow.toString() + 
					"</b></td></tr>" +
					htmlAddedUsers + "</table>");
		resp.getWriter().println("<table style=\"width:500px;float:left;border:solid 1px;\"><tr><td colspan=\"3\"><b>" + 
					nonUsers.toString() + " users without G+" + 
					"</b></td></tr>" +
					htmlNonUsers + "</table>");
		resp.getWriter().println("<table style=\"width:400px;float:left;border:solid 1px\"><tr><td colspan=\"2\"><b>" + 
					suspendedUsers.toString() + " <a href=\"https://support.google.com/a/answer/33312?hl=en\" target=\"_blank\">suspended</a> users" + 
					"</b></td></tr>" +
					htmlSuspendedUsers + "</table>");
	}
		
	private static GoogleCredential authenticate(HttpTransport httpTransport,JsonFactory jsonFactory, List<String> scope) throws GeneralSecurityException, IOException, Exception {

		  log.info(String.format("Authenticate the domain for %s", runAs));

		  // Setting the sub field with USER_EMAIL allows you to make API calls using the special keyword
		  // "me" in place of a user id for that user.
		  GoogleCredential credential = new GoogleCredential.Builder()
		      .setTransport(httpTransport)
		      .setJsonFactory(jsonFactory)
		      .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
		      .setServiceAccountScopes(scope)
		      .setServiceAccountUser(runAs)
		      .setServiceAccountPrivateKey(getKey())
		      .build();

		  // Create and return the authorized API client
		  return credential;

		}

	private static GoogleCredential authenticateUser(HttpTransport httpTransport,JsonFactory jsonFactory, List<String> scope, String userEmail) throws GeneralSecurityException, IOException, Exception {

		  log.info(String.format("Authenticate the domain for %s", userEmail));

		  // Setting the sub field with USER_EMAIL allows you to make API calls using the special keyword
		  // "me" in place of a user id for that user.
		  GoogleCredential credential = new GoogleCredential.Builder()
		      .setTransport(httpTransport)
		      .setJsonFactory(jsonFactory)
		      .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
		      .setServiceAccountScopes(scope)
		      .setServiceAccountUser(userEmail)
		      .setServiceAccountPrivateKey(getKey())
		      .build();

		  // Create and return the authorized API client
		  return credential;
		}

	private static PrivateKey getKey() throws GeneralSecurityException, Exception{
		if (key!=null){
			//great! nothing to do!
		} else {
	        ClassLoader classLoader = GPlusFollowServlet.class.getClassLoader();

			log.info("Getting keystore");
	        KeyStore keystore = KeyStore.getInstance("PKCS12");
			log.info(String.format("Getting key stream for %s", SERVICE_ACCOUNT_PKCS12_FILE));

	        InputStream keyFileStream = classLoader.getResourceAsStream(SERVICE_ACCOUNT_PKCS12_FILE);
	
	        if (keyFileStream == null){
	  		  log.severe(String.format("Key file %s not found", SERVICE_ACCOUNT_PKCS12_FILE));
	            throw new Exception("Key File Not Found.");
	        }
	
	        keystore.load(keyFileStream, P12_PASSWORD.toCharArray());
	        key = (PrivateKey)keystore.getKey("privatekey", P12_PASSWORD.toCharArray());
		}
		return key;
	}
	

	
	
}
	

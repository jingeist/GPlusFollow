package com.breadandduck.gplusfollow;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.plusDomains.PlusDomains.Circles.AddPeople;
import com.google.api.services.plusDomains.PlusDomains;
import com.google.api.services.plusDomains.PlusDomainsScopes;
import com.google.api.services.plusDomains.model.Circle;
import com.google.api.services.plusDomains.model.CircleFeed;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class GPlusService {
	private static final List<String> GPLUS_SCOPE=Arrays.asList(DirectoryScopes.ADMIN_DIRECTORY_USER,
			PlusDomainsScopes.PLUS_ME,
			PlusDomainsScopes.PLUS_CIRCLES_READ,
			PlusDomainsScopes.PLUS_CIRCLES_WRITE);
	
	private static final int PAGE_SIZE=500;

	private static GPlusService instance=null;
	
	private String followCircleName = "Following";
	private String applicationName = "jj-gplusfollow";
	private List<String> followeesList = null;
	private List<String> followersList = null;
	private int startPage = 1;
	private int endPage = 0;
	private String runAs=null;



	private static final Logger log = Logger.getLogger(GPlusService.class.getName());
	
	private static final String SERVICE_ACCOUNT_EMAIL = 
			"105194108261-24o5a60ufq49a3guj4ls8m9urep6fh5t@developer.gserviceaccount.com";
	private static final String SERVICE_ACCOUNT_PKCS12_FILE =
			"Plus Follow-8665b4395c21.p12";
	private static final String P12_PASSWORD = 
			"notasecret";
	
	private static PrivateKey key = null;
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();

	
	private void GPlusService(){
		
	}
	
	public static GPlusService getService(){
		if (null==instance){
			instance=new GPlusService();
		}
		return instance;
	}
	
	public void execute(){
		if (null==this.followersList){
		}
	}
	
	private GoogleCredential getAdminService() throws GeneralSecurityException, IOException, Exception {

		  log.info(String.format("Authenticate the domain for %s", runAs));

		  // Setting the sub field with USER_EMAIL allows you to make API calls using the special keyword
		  // "me" in place of a user id for that user.
		  GoogleCredential credential = new GoogleCredential.Builder()
		      .setTransport(HTTP_TRANSPORT)
		      .setJsonFactory(JSON_FACTORY)
		      .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
		      .setServiceAccountScopes(GPLUS_SCOPE)
		      .setServiceAccountUser(this.getRunAs())
		      .setServiceAccountPrivateKey(getKey())
		      .build();

		  // Create and return the authorized API client
		  return credential;

		}

	private PrivateKey getKey() throws GeneralSecurityException, Exception{
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
	
	//private Directory getDirectory(){
		
	//}
	
	
	public String getFollowCircleName(){
		return this.followCircleName;
	}

	public GPlusService setFollowCircleName(String circleName){
		this.followCircleName=circleName;
		return instance;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public GPlusService setApplicationName(String applicationName) {
		this.applicationName = applicationName;
		return instance;
	}
	public int getStartPage() {
		return startPage;
	}

	public GPlusService setStartPage(int startPage) {
		this.startPage = startPage;
		return instance;
	}

	public int getEndPage() {
		return endPage;
	}

	public GPlusService setEndPage(int endPage) {
		this.endPage = endPage;
		return instance;
	}

	public List<String> getFolloweesList() {
		return followeesList;
	}

	public GPlusService setFolloweesList(List<String> followeesList) {
		this.followeesList = followeesList;
		return instance;
	}
	
	public GPlusService addFollowee (String followeeId){
		this.followeesList.add(followeeId);
		return instance;
	}
	
//	public GPlusService addFolloweesByEmail(List<String> followeesEmailList){
//        for (int i=0; i<followeesEmailList.size(); i++){
//	        Directory.Users.Get userRequest = directory.users().get(followeesEmailList.get(i));
//	        this.addFollowee(userRequest.execute().getId());
//        }
//		
//		return instance;
//	}

	public List<String> getFollowersList() {
		return followersList;
	}

	public GPlusService setFollowersList(List<String> followersList) {
		this.followersList = followersList;
		return instance;
	}
	
	public void addFollower(String followerId){
		this.followersList.add(followerId);
	}
	public String getRunAs() {
		if (null==runAs){
			UserService userService = UserServiceFactory.getUserService();
			User user = userService.getCurrentUser();
			runAs = user.getEmail();
		}
		return runAs;
	}

	public GPlusService setRunAs(String runAs) {
		this.runAs = runAs;
		return instance;
	}
	
	public static void executeWithExpBackoff(PlusDomains.Circles.AddPeople addRequest) throws IOException{
	    Random randomGenerator = new Random();
	    for (int n = 0; n < 36; ++n) {
	      try {
	    	addRequest.execute();
	        return;
	      } catch (GoogleJsonResponseException e) {
	        if ((e.getStatusCode() == 403
	            && (e.getDetails().getErrors().get(0).getReason().equals("rateLimitExceeded")
	                || e.getDetails().getErrors().get(0).getReason().equals("userRateLimitExceeded")
	                || e.getDetails().getErrors().get(0).getReason().equals("dailyLimitExceeded")))
	            || e.getStatusCode() == 500) {
	          // Apply exponential backoff.
		        try {
		        	int m = (n<=12? n:12);
					Thread.sleep((1 << m) * 1000 + randomGenerator.nextInt(1001));
				} catch (InterruptedException e1) {
					log.warning(e.toString());
					e1.printStackTrace();
				}
		        if (35==n){
		        	throw e;
		        }
	        } else {
	          // Other error, re-throw.
	          throw e;
	        }
	      }
	    }
	    System.err.println("There has been an error, the request never succeeded.");
	    return;
	}

	public static CircleFeed executeWithExpBackoff(PlusDomains.Circles.List listCircles) throws IOException{
	    Random randomGenerator = new Random();
	    for (int n = 0; n < 36; ++n) {
	      try {
	    	  return listCircles.execute();
	      } catch (GoogleJsonResponseException e) {
	        if ((e.getStatusCode() == 403
	            && (e.getDetails().getErrors().get(0).getReason().equals("rateLimitExceeded")
	                || e.getDetails().getErrors().get(0).getReason().equals("userRateLimitExceeded")
	                || e.getDetails().getErrors().get(0).getReason().equals("dailyLimitExceeded")))
	            || e.getStatusCode() == 500) {
	          // Apply exponential backoff.
		        try {
		        	int m = ((1 << (n<=12? n:12)) * 1000 + randomGenerator.nextInt(1001));
		        	log.warning("Limit Exceeded: Sleeping " + m + "ms");		        	
					Thread.sleep(m);
				} catch (InterruptedException e1) {
					log.warning(e.toString());
					e1.printStackTrace();
				}
		        if (35==n){
		        	throw e;
		        }
	        } else {
	          // Other error, re-throw.
	          throw e;
	        }
	      }
	    }
	    System.err.println("There has been an error, the request never succeeded.");
	    return null;
	}

	public static Circle executeWithExpBackoff(PlusDomains.Circles.Insert insertCircle) throws IOException{
	    Random randomGenerator = new Random();
	    for (int n = 0; n < 36; ++n) {
	      try {
	    	  return insertCircle.execute();
	      } catch (GoogleJsonResponseException e) {
	        if ((e.getStatusCode() == 403
	            && (e.getDetails().getErrors().get(0).getReason().equals("rateLimitExceeded")
	                || e.getDetails().getErrors().get(0).getReason().equals("userRateLimitExceeded")
	                || e.getDetails().getErrors().get(0).getReason().equals("dailyLimitExceeded")))
	            || e.getStatusCode() == 500) {
	          // Apply exponential backoff.
		        try {
		        	int m = (n<=12? n:12);
					Thread.sleep((1 << m) * 1000 + randomGenerator.nextInt(1001));
				} catch (InterruptedException e1) {
					log.warning(e.toString());
					e1.printStackTrace();
				}
		        if (35==n){
		        	throw e;
		        }
	        } else {
	          // Other error, re-throw.
	          throw e;
	        }
	      }
	    }
	    System.err.println("There has been an error, the request never succeeded.");
	    return null;
	}

}

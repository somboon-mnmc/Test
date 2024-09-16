package com.skytizens.alfresco.duw.utils;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skytizens.alfresco.utils.EasySslProtocolSocketFactory;
import com.skytizens.alfresco.utils.ExternalWorkflowConnector;

public class UnderwriterJogetConnector extends ExternalWorkflowConnector {

	private static final Logger logger = LoggerFactory.getLogger(UnderwriterJogetConnector.class);
	
	private final static String APP_ID = "duw";
	private final static String FORM_ID = "pushf";
	private final static String DEF_ID = "duw:latest:underwriter";
	
	private String protocol;
	private String host;
	private int port;
	private String context;
	private String user;
	private String password;
	
	private SearchService searchService;
	private ServiceRegistry serviceRegistry;

	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
		
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @param context the context to set
	 */
	public void setContext(String context) {
		this.context = context;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
		
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.searchService = serviceRegistry.getSearchService();
		
	}
	
	private String getBaseUrl() {
		return protocol + "://" + host + ":" + port + "/" + context + "/";
		
	}
	
	private String getLoginParameters() {
		return "j_username=" + user + "&j_password=" + password;
		
	}
/*	
	public String createCase(String processID, String applicationNo, String product, String team, String policyNo,
			Integer flag1, Integer flag2, Integer flag3, String importType) throws Exception {
		return createCase(processID, applicationNo, product, team, policyNo, flag1, flag2, flag3, null, importType);
		
	}
*/
	public String createItem(String processID, String applicationNo, String product, String policyNo, String statusURL, String status) throws Exception {
		String servletURL = getBaseUrl();
		String pid = getActiveProcess(applicationNo);
		if(pid == null) 
		{
			return createForm(processID, applicationNo, product, policyNo, statusURL, status);
		}
		return "";
		
	}
	
	public String ivictUpdate(String pid, Map<String,Object> ivictMap) throws Exception {

		String url = getBaseUrl() + "web/json/plugin/com.skytizens.joget.service.SkyCreateUpdateRecord/service?APP_ID=" + APP_ID + "&FORM_ID=" + FORM_ID + "&pkey=" + pid + "&" + getLoginParameters();
		if(logger.isDebugEnabled()) logger.debug("Update Joget form: " + url);
		
		PostMethod postMethod = new PostMethod(url);
		// Allow access even though certificate is self signed
		Protocol lEasyHttps = new Protocol("https", (ProtocolSocketFactory) new EasySslProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", lEasyHttps);
		try {
			List<Part> partss = new ArrayList<Part>();
			
//			partss.add(new StringPart("pid", processID));
//			partss.add(new StringPart("appno",  applicationNo == null? "": applicationNo.trim()));
//			partss.add(new StringPart("date", simpleDateFormat.format(new Date())));
//			partss.add(new StringPart("dataentry_fullname", "admin" /*AuthenticationUtil.getFullyAuthenticatedUser()*/));
//			partss.add(new StringPart("dataentry_date", simpleDateFormat.format(new Date())));
//			partss.add(new StringPart("req_id", "TSLI-" + processID));
//		    partss.add(new StringPart("dataentry_username", "admin"/* AuthenticationUtil.getFullyAuthenticatedUser() */));
//			partss.add(new StringPart("owner","Administrator " /*AuthenticationUtil.getFullyAuthenticatedUser()*/));
//			partss.add(new StringPart("product", product == null? "": product.trim()));
//			partss.add(new StringPart("team", team == null? "": team.trim()));
//			partss.add(new StringPart("priority", "Normal"));
//			partss.add(new StringPart("status", status));
//			partss.add(new StringPart("time", simpleDateFormat2.format(new Date())));
//			
//			if(policyNo!=null && !policyNo.isEmpty()) {
//				partss.add(new StringPart("policyno", policyNo.trim()));
//			}
//			if(flag1!=null) {
//				partss.add(new StringPart("flag1", flag1+""));
//			}
//			if(flag2!=null) {
//				partss.add(new StringPart("flag2", flag2+""));
//			}
//			if(flag3!=null) {
//				partss.add(new StringPart("flag3", flag3+""));
//			}
			
			//no need update doc_no
			//partss.add(new StringPart("disref_no", ivictMap.get("disref_no").toString()));
			
			if(ivictMap.containsKey("disinst_no") && ivictMap.get("disinst_no") != null) partss.add(new StringPart("disinst_no", ivictMap.get("disinst_no").toString()));
			if(ivictMap.containsKey("disslip_no") && ivictMap.get("disslip_no") != null) partss.add(new StringPart("disslip_no", ivictMap.get("disslip_no").toString()));
			if(ivictMap.containsKey("disdoc_date") && ivictMap.get("disdoc_date") != null) partss.add(new StringPart("disdoc_date", ivictMap.get("disdoc_date").toString()));
			if(ivictMap.containsKey("dispost_date") && ivictMap.get("dispost_date") != null) partss.add(new StringPart("dispost_date", ivictMap.get("dispost_date").toString()));
			if(ivictMap.containsKey("dispurchase_no") && ivictMap.get("dispurchase_no") != null) partss.add(new StringPart("dispurchase_no", ivictMap.get("dispurchase_no").toString()));
			if(ivictMap.containsKey("disbus_area") && ivictMap.get("disbus_area") != null) partss.add(new StringPart("disbus_area", ivictMap.get("disbus_area").toString()));
			if(ivictMap.containsKey("disven_code") && ivictMap.get("disven_code") != null) partss.add(new StringPart("disven_code", ivictMap.get("disven_code").toString()));
			if(ivictMap.containsKey("disven_name") && ivictMap.get("disven_name") != null) partss.add(new StringPart("disven_name", ivictMap.get("disven_name").toString()));
			if(ivictMap.containsKey("dispay_method") && ivictMap.get("dispay_method") != null) partss.add(new StringPart("dispay_method", ivictMap.get("dispay_method").toString()));
			if(ivictMap.containsKey("dispay_date") && ivictMap.get("dispay_date") != null) partss.add(new StringPart("dispay_date", ivictMap.get("dispay_date").toString()));
			
			//if(ivictMap.containsKey("tax_no") && ivictMap.get("tax_no") != null) partss.add(new StringPart("tax_no", ivictMap.get("tax_no").toString()));
			//if(ivictMap.containsKey("tax_date") && ivictMap.get("tax_date") != null) partss.add(new StringPart("tax_date", ivictMap.get("tax_date").toString()));
			//if(ivictMap.containsKey("tax_code") && ivictMap.get("tax_code") != null) partss.add(new StringPart("tax_code", ivictMap.get("tax_code").toString()));
			//if(ivictMap.containsKey("tax_name") && ivictMap.get("tax_name") != null) partss.add(new StringPart("tax_name", ivictMap.get("tax_name").toString()));
			//if(ivictMap.containsKey("taxorder_no") && ivictMap.get("taxorder_no") != null) partss.add(new StringPart("taxorder_no", ivictMap.get("taxorder_no").toString()));
			
			if(ivictMap.containsKey("sap01_doc") && ivictMap.get("sap01_doc") != null) partss.add(new StringPart("sap01_doc",ivictMap.get("sap01_doc").toString()));
			//if(ivictMap.containsKey("sap02_doc") && ivictMap.get("sap02_doc") != null) partss.add(new StringPart("sap02_doc",ivictMap.get("sap02_doc").toString()));
			//if(ivictMap.containsKey("sap03_doc") && ivictMap.get("sap03_doc") != null) partss.add(new StringPart("sap03_doc",ivictMap.get("sap03_doc").toString()));
			//if(ivictMap.containsKey("sap04_doc") && ivictMap.get("sap04_doc") != null) partss.add(new StringPart("sap04_doc",ivictMap.get("sap04_doc").toString()));
			
    		MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(partss.toArray(new Part[partss.size()])/*parts*/, new HttpMethodParams());
	    	postMethod.setRequestEntity(multipartRequestEntity);

	    	HttpClient httpClient = new HttpClient();

	    	httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
	    	httpClient.getHttpConnectionManager().getParams().setSoTimeout(30000);

	    	int responseCode = httpClient.executeMethod(postMethod);
	    	logger.debug("Update Joget form get response code: " + responseCode);
	    	if(logger.isDebugEnabled()) logger.debug("Create Joget form get response code: " + responseCode);
	    	if (responseCode == HttpStatus.SC_OK) {
				logger.debug(postMethod.getResponseBodyAsString());
	 	    }

    	}finally {
        		postMethod.releaseConnection();
		
    	}
    	return pid;
	}
	
	public String getMD5(String value) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
	    md.update(value.getBytes());
	    byte[] digest = md.digest();
	    return DatatypeConverter.printHexBinary(digest).toUpperCase();
	}

	public String createForm(String processID, String applicationNo, String product, String policyNo, String statusURL, String status) throws Exception {
		
			String pkey = getMD5("ESUW-" + processID + "-" + applicationNo);
			
			String url = getBaseUrl() + "web/json/data/form/store/" + APP_ID + "/" + FORM_ID + "/" + pkey + "?" + getLoginParameters();
			
			if(logger.isDebugEnabled()) logger.debug("Create Joget form: " + url);
			
			PostMethod postMethod = new PostMethod(url);
			// Allow access even though certificate is self signed
			Protocol lEasyHttps = new Protocol("https", (ProtocolSocketFactory) new EasySslProtocolSocketFactory(), 443);
			Protocol.registerProtocol("https", lEasyHttps);
			try {
				List<Part> partss = new ArrayList<Part>();
				partss.add(new StringPart("pid", processID));
				partss.add(new StringPart("appno",  applicationNo == null ? "": applicationNo.trim()));
				partss.add(new StringPart("policyno",  policyNo == null ? "": policyNo.trim()));
				partss.add(new StringPart("product", product == null ? "": product.trim()));
//				partss.add(new StringPart("team", team == null ? "": team.trim()));
//				partss.add(new StringPart("amount", amount == null ? "": amount.trim()));
//				partss.add(new StringPart("phone", phone == null ? "": phone.trim()));
				partss.add(new StringPart("priority", "normal"));
				partss.add(new StringPart("status", status));
				
				if(policyNo!=null && !policyNo.isEmpty()) {
					partss.add(new StringPart("policyno", policyNo.trim()));
				}
//				if(flag1 != null) {
//					partss.add(new StringPart("flag1", flag1+""));
//				}
//				if(flag2 != null) {
//					partss.add(new StringPart("flag2",flag2+""));
//				}
//				if(flag3 != null) {
//					partss.add(new StringPart("flag3", flag3+""));
//				}
				
			MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(partss.toArray(new Part[partss.size()])/*parts*/, new HttpMethodParams());
			//
			postMethod.setRequestEntity(multipartRequestEntity);

			HttpClient httpClient = new HttpClient();

			httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			httpClient.getHttpConnectionManager().getParams().setSoTimeout(30000);

			int responseCode = httpClient.executeMethod(postMethod);
			
			if(logger.isDebugEnabled()) logger.debug(" Create Joget form get response code: " + responseCode);
			if(responseCode == 200) {
				return pkey;
			}

		}finally {
			postMethod.releaseConnection();
		}

		return null;
	}

	public String getLatestProcessDefId(String defId) throws Exception {

		GetMethod getMethod = new GetMethod(getBaseUrl() + "web/json/workflow/process/latest/" + defId + "?" + getLoginParameters());
		Protocol lEasyHttps = new Protocol("https", (ProtocolSocketFactory) new EasySslProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", lEasyHttps);
		HttpClient client = new HttpClient();

		client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
		client.getHttpConnectionManager().getParams().setSoTimeout(30000);

		int resultHTTP = client.executeMethod(getMethod);
		if (resultHTTP == HttpStatus.SC_OK) {
			JSONObject jsonObject = new JSONObject(getMethod.getResponseBodyAsString());
			//System.out.println(jsonObject.getString("encodedId"));
			return jsonObject.getString("encodedId");
		} else {
			throw new AlfrescoRuntimeException("Send fail, response=" + HttpStatus.getStatusText(resultHTTP));
		}
	}
	
	public String startProcess(String recordId, String policyno, String importType) throws Exception {
		
		//String processDefId = getLatestProcessDefId(DEF_ID);
		String url = getBaseUrl() + "web/json/workflow/process/start/" + DEF_ID + "?" + getLoginParameters() + "&recordId=" + recordId;
		if(policyno!=null && !policyno.isEmpty()) url += "&var_policyno=" + policyno;
		if(importType != null && !importType.isEmpty()) url += "&var_importType=" + importType;
		
		//System.out.println(url);
		if(logger.isDebugEnabled()) logger.debug("Start Joget process:  " + url);
		PostMethod postMethod = new PostMethod(url);

		// Allow access even though certificate is self signed
		Protocol lEasyHttps = new Protocol("https", (ProtocolSocketFactory) new EasySslProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", lEasyHttps);

		HttpClient client = new HttpClient();
//			client.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
//					new UsernamePasswordCredentials(user, password));

		client.getHttpConnectionManager().getParams().setConnectionTimeout(60000);
		client.getHttpConnectionManager().getParams().setSoTimeout(60000);

		int resultHTTP = client.executeMethod(postMethod);
		if(logger.isDebugEnabled()) logger.debug("Start Joget process response code: " + resultHTTP);
		if (resultHTTP == HttpStatus.SC_OK) {
			return postMethod.getResponseBodyAsString();
		} else {
			throw new AlfrescoRuntimeException("Send fail, response=" + HttpStatus.getStatusText(resultHTTP));
		}
	}
	/* 
	 * processId: processId you want to restart - can get from  #assignment.processId# or #process.processId# 
	 * activityDefId: which activity that you want workflow to restart at (constant value) - activityDefId is activity id that we defined in process designer 
	 * 
	 */
	
	private void restartActivity(String processId, String activityDefId) {

		String url = getBaseUrl() + "web/json/monitoring/activity/start/" + processId + "/" + activityDefId + "?" + getLoginParameters() + "&abortCurrent=true&var_status=reopen"; 
		//System.out.println(url);
		PostMethod method = new PostMethod(url);
		try {
			HttpClient httpClient2 = new HttpClient();
			httpClient2.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			httpClient2.getHttpConnectionManager().getParams().setSoTimeout(30000);
			int responseCode2 = httpClient2.executeMethod(method);
			//System.out.println(responseCode2);
			//System.out.println(method.getResponseBodyAsString());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void setVariable(String activityId, String variable, String value) {

		String url = getBaseUrl() + "web/json/monitoring/activity/variable/" + activityId + "/" + variable + "?" + getLoginParameters() + "&value=" + value ; 
		//System.out.println(url);
		PostMethod method = new PostMethod(url);
		try {
			HttpClient httpClient2 = new HttpClient();
			httpClient2.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			httpClient2.getHttpConnectionManager().getParams().setSoTimeout(30000);
			int responseCode2 = httpClient2.executeMethod(method);
			//System.out.println(responseCode2);
			//System.out.println(method.getResponseBodyAsString());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void completeActivity(String processDefId, String processId, String activityId ) {
		String url = getBaseUrl() + "web/json/monitoring/running/activity/complete" + "?" + getLoginParameters() + "&processDefId=" + processDefId +  "&processId=" + processId + "&activityId=" + activityId; 
		//System.out.println(url);
		PostMethod method = new PostMethod(url);
		try {
			HttpClient httpClient2 = new HttpClient();
			httpClient2.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			httpClient2.getHttpConnectionManager().getParams().setSoTimeout(30000);
			int responseCode2 = httpClient2.executeMethod(method);
			//System.out.println(responseCode2);
			//System.out.println(method.getResponseBodyAsString());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
//	private void setStatus(String activityId, String value) {
//		String servletURL = getBaseUrl();
//		String url = "";
//		String restartAtv = "web/json/monitoring/activity/variable/" + activityId + "/status";
//		String login = "j_username=" + user + "&j_password=" + password;
//		url = servletURL + restartAtv + "?" + login + "&value=" + value ; 
//		//System.out.println(url);
//		PostMethod method = new PostMethod(url);
//		try {
//			HttpClient httpClient2 = new HttpClient();
//			httpClient2.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
//			httpClient2.getHttpConnectionManager().getParams().setSoTimeout(30000);
//			int responseCode2 = httpClient2.executeMethod(method);
//			//System.out.println(responseCode2);
//			//System.out.println(method.getResponseBodyAsString());
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	
	private void setStatus2(String recordId, String processId, String status) {
		
		String url = getBaseUrl() + "web/json/data/form/store/" + APP_ID + "/" + FORM_ID+ "/" + recordId + "?" + getLoginParameters()  ; 
		//System.out.println(url);
		PostMethod method = new PostMethod(url);
		
		try {
			List<Part> partss = new ArrayList<Part>();
			partss.add(new StringPart("pid", processId));
			partss.add(new StringPart("status", status));
		
			MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(partss.toArray(new Part[partss.size()])/*parts*/, new HttpMethodParams());
			method.setRequestEntity(multipartRequestEntity);
			
			HttpClient httpClient2 = new HttpClient();
			httpClient2.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			httpClient2.getHttpConnectionManager().getParams().setSoTimeout(30000);
			int responseCode2 = httpClient2.executeMethod(method);
			//System.out.println(responseCode2);
			//System.out.println(method.getResponseBodyAsString());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	private String getActivity(String processId) {
		String activityId = null;
		String url = getBaseUrl() + "web/json/monitoring/activity/list?" + getLoginParameters() + "&processId=" + processId + "&rows=1000&sort=dateCreated&desc=true";
		//System.out.println(url);
		PostMethod method = new PostMethod(url);
		try {
			HttpClient httpClient2 = new HttpClient();
			httpClient2.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			httpClient2.getHttpConnectionManager().getParams().setSoTimeout(30000);
			int resultHTTP = httpClient2.executeMethod(method);
			if (resultHTTP == HttpStatus.SC_OK) {
				String result =  method.getResponseBodyAsString();
				//System.out.println(result);
				JSONObject json = new JSONObject(result);
				JSONArray arr = json.getJSONArray("data");
				for(int i=0; i<arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					if("open.not_running.not_started".equals(obj.getString("state"))) {
						//System.out.println("Now im here : open.not_running.not_started "+ obj.getString("state"));
						//if(obj.getString("id").endsWith("_activity2")) {
						//System.out.println(obj.getString("id"));
							activityId = obj.getString("id");
						//}
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return activityId;
	}
	private String getProcessDefId(String activityId) {
		String processDefId = null;
		String url = getBaseUrl() + "web/json/monitoring/activity/view/" + activityId + "?" + getLoginParameters();
		//System.out.println(url);
		GetMethod method = new GetMethod(url);
		try {
			HttpClient httpClient2 = new HttpClient();
			httpClient2.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			httpClient2.getHttpConnectionManager().getParams().setSoTimeout(30000);
			int resultHTTP = httpClient2.executeMethod(method);
			if (resultHTTP == HttpStatus.SC_OK) {
				String result =  method.getResponseBodyAsString();
				//System.out.println(result);
				JSONObject json = new JSONObject(result);
				processDefId = json.getString("processDefId").replace("#", ":");
			} else {
				throw new AlfrescoRuntimeException("Send fail, response=" + HttpStatus.getStatusText(resultHTTP));
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return processDefId;
		
	}
	
	private String getRecordId(String activityId) {
		String processid = null;
		String servletURL = getBaseUrl();
		String listId = "datalist_inb";
		String listParamName = "d-4493092-fn_processid";
		
		String restartAtv = "web/json/data/list/" + APP_ID + "/" + listId;
		String login = "j_username=" + user + "&j_password=" + password;
		String url = servletURL + restartAtv + "?" + login + "&" + listParamName + "=" + activityId; 
		//System.out.println(url);
		GetMethod method = new GetMethod(url);
		try {
			HttpClient httpClient2 = new HttpClient();
			httpClient2.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			httpClient2.getHttpConnectionManager().getParams().setSoTimeout(30000);
			int resultHTTP = httpClient2.executeMethod(method);
			if (resultHTTP == HttpStatus.SC_OK) {
				String result =  method.getResponseBodyAsString();
				//System.out.println(result);
				JSONObject json = new JSONObject(result);
				JSONArray arr = json.getJSONArray("data");
				if(arr.length() > 0) {
					JSONObject obj = arr.getJSONObject(0);
					processid = obj.getString("id");
				}
			} else {
				throw new AlfrescoRuntimeException("Send fail, response=" + HttpStatus.getStatusText(resultHTTP));
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return processid;
		
	}
	
	private String getActiveProcess(String applicationId) throws Exception {
		String processid = null;
		String listId = "datalist_inb";
		String url = getBaseUrl() + "web/json/data/list/" + APP_ID + "/" + listId + "?" + getLoginParameters() + "&d-4493092-fn_c_appno=" + applicationId; 
		//System.out.println(url);
		GetMethod method = new GetMethod(url);
		
		HttpClient httpClient = new HttpClient();
		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
		httpClient.getHttpConnectionManager().getParams().setSoTimeout(30000);
		int resultHTTP = httpClient.executeMethod(method);
		if (resultHTTP == HttpStatus.SC_OK) {
			String result =  method.getResponseBodyAsString();
			//System.out.println(result);
			JSONObject json = new JSONObject(result);
			JSONArray arr = json.getJSONArray("data");
			if(arr.length() > 0) {
				JSONObject obj = arr.getJSONObject(0);
				processid = obj.getString("processid");
			}
		} else {
			throw new AlfrescoRuntimeException("Send fail, response=" + HttpStatus.getStatusText(resultHTTP));
		}

		return processid;
	}
	
	
	public String createCase(String processID, String applicationNo, String product, String policyNo, String statusURL) throws Exception {

		String result = "";
		
		String pid = getActiveProcess(applicationNo);
		//System.out.println(pid);
		if(pid == null) 
		{
			System.out.println("new");
			//create
			String importType = statusURL;
			// create form - input data manually
			String recordId = createForm(processID, applicationNo, product, policyNo, statusURL , "new");
			System.out.println(recordId);
			if(recordId !=null) 
			{
				// start process
				result = startProcess(recordId, policyNo, importType);
			}	
		}else{
			//update
			String activityId = getActivity(pid);
			String processDefId = getProcessDefId(activityId);
			String statusReopen = "status";
			String reopen = "reopen";
			
			System.out.println("reopen: " + activityId);

			if(activityId != null) {
				if(activityId.endsWith("_activity1")) {
					setVariable(activityId, "pid", processID);
					setVariable(activityId, statusReopen, reopen);
					completeActivity(processDefId, pid, activityId);
				}
				if(activityId.endsWith("_activity2")) {
					setVariable(activityId, "pid", processID);
					setVariable(activityId, statusReopen, reopen);
					completeActivity(processDefId, pid, activityId);
				}
				if(activityId.endsWith("_activity3")) {
					setVariable(activityId, "pid", processID);
					setVariable(activityId, statusReopen, reopen);
					completeActivity(processDefId, pid, activityId);
				}
				if(activityId.endsWith("_activity4")) {
					setVariable(activityId, "pid", processID);
					setVariable(activityId, statusReopen, reopen);
					completeActivity(processDefId, pid, activityId);
				}
//				if(activityId.endsWith("_activity5")) {
//					setVariable(activityId, "pid", processID);
//					setVariable(activityId, statusReopen, reopen);
//					completeActivity(processDefId, pid, activityId);
//				}
//				if(activityId.endsWith("_activity6")) {
//					setVariable(activityId, "pid", processID);
//					setVariable(activityId, statusReopen, reopen);
//					completeActivity(processDefId, pid, activityId);
//				}
			}

			result = "{}";
		}
		
		return result;
	}

	public String updateCase(String applicationNo, String policyNo)
		   throws Exception {
		String servletURL = getBaseUrl();

		String result = null;
		//System.out.println(servletURL + "/webapi/update");
		PostMethod postMethod = new PostMethod(servletURL + "/webapi/update");

		// Allow access even though certificate is self signed
		Protocol lEasyHttps = new Protocol("https", (ProtocolSocketFactory) new EasySslProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", lEasyHttps);

		JSONObject json = new JSONObject();
		json.put("application_no", applicationNo == null ? null : applicationNo.trim());
		json.put("policy_no", policyNo == null ? null : policyNo.trim());
//		json.put("flag1", flag1);
//		json.put("flag2", flag2);
//		json.put("flag3", flag3);

		try {
			StringRequestEntity requestEntity = new StringRequestEntity(json.toString(), "application/json", "UTF-8");

			postMethod.setRequestEntity(requestEntity);

			HttpClient client = new HttpClient();
			client.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(user, password));

			client.getHttpConnectionManager().getParams().setConnectionTimeout(60000);

			int resultHTTP = client.executeMethod(postMethod);
			if (resultHTTP == HttpStatus.SC_OK) {
				result = postMethod.getResponseBodyAsString();
			} else {
				throw new AlfrescoRuntimeException("Send fail, response=" + HttpStatus.getStatusText(resultHTTP));
			}
		} finally {
			postMethod.releaseConnection();
		}
		return result;
		
	}

	public String callLoop(int counter, String name) throws Exception {
		String servletURL = getBaseUrl();

		String result = null;
		//System.out.println(servletURL + "/webapi/" + name);
		PostMethod postMethod = new PostMethod(servletURL + "/webapi/" + name);

		// Allow access even though certificate is self signed
		Protocol lEasyHttps = new Protocol("https", (ProtocolSocketFactory) new EasySslProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", lEasyHttps);

		JSONObject json = new JSONObject();
		json.put("counter", counter);

		try {
			StringRequestEntity requestEntity = new StringRequestEntity(json.toString(), "application/json", "UTF-8");

			postMethod.setRequestEntity(requestEntity);

			HttpClient client = new HttpClient();
			client.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(user, password));

			client.getHttpConnectionManager().getParams().setConnectionTimeout(60000);

			int resultHTTP = client.executeMethod(postMethod);
			if (resultHTTP == HttpStatus.SC_OK) {
				result = postMethod.getResponseBodyAsString();
			} else {
				throw new AlfrescoRuntimeException("Send fail, response=" + HttpStatus.getStatusText(resultHTTP));
			}
		} finally {
			postMethod.releaseConnection();
		}
		return result;
	}

	public String createReinsurerNotification(String reinsurer, String applicationNo, String who, String action,
			String details, Date when) throws Exception {
		String servletURL = getBaseUrl();

		String result = null;
		//System.out.println(servletURL + "/webapi/reinsurer-notification");
		PostMethod postMethod = new PostMethod(servletURL + "/webapi/reinsurer-notification");

		// Allow access even though certificate is self signed
		Protocol lEasyHttps = new Protocol("https", (ProtocolSocketFactory) new EasySslProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", lEasyHttps);

		JSONObject json = new JSONObject();
		json.put("application_no", applicationNo == null ? null : applicationNo.trim());
		json.put("reinsurer", reinsurer == null ? null : reinsurer.trim());
		json.put("who", who == null ? null : who.trim());
		json.put("action", action == null ? null : action.trim());
		json.put("details", details == null ? null : details.trim());
		json.put("when", ISO8601DateFormat.format(when));

		try {
			StringRequestEntity requestEntity = new StringRequestEntity(json.toString(), "application/json", "UTF-8");

			postMethod.setRequestEntity(requestEntity);

			HttpClient client = new HttpClient();
			client.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(user, password));

			client.getHttpConnectionManager().getParams().setConnectionTimeout(60000);

			int resultHTTP = client.executeMethod(postMethod);
			if (resultHTTP == HttpStatus.SC_OK) {
				result = postMethod.getResponseBodyAsString();
			} else {
				throw new AlfrescoRuntimeException("Send fail, response=" + HttpStatus.getStatusText(resultHTTP));
			}
		} finally {
			postMethod.releaseConnection();
		}
		return result;
	}

	private List<NodeRef> getDocs(String query){
		SearchParameters sp = new SearchParameters();
		
		sp.addStore(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
		sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
		sp.setQuery(query);
		
		List<NodeRef> docs = new ArrayList<NodeRef>();
		ResultSet results = null;
        try {
			results = searchService.query(sp);
			if (results.length() > 0) {
				Iterator<ResultSetRow> resultsIterator = results.iterator();
				while (resultsIterator.hasNext()) {
					NodeRef result = ((ResultSetRow) resultsIterator.next()).getNodeRef();
					docs.add(result);
				}
			}
		} catch (AlfrescoRuntimeException e) {
			e.printStackTrace();
		} finally {
			if (results != null) {
				results.close();
			}
		}
        
        return docs;
	}
	
	public static void main(String[] args) throws Exception {
		UnderwriterJogetConnector appianConnector = new UnderwriterJogetConnector();
		appianConnector.setProtocol("http");
		appianConnector.setHost("192.168.10.132");
		appianConnector.setPort(8080);
		appianConnector.setContext("jw");
		appianConnector.setUser("admin");
		appianConnector.setPassword("admin");

		String result = appianConnector.createCase( 
			   String.valueOf(System.currentTimeMillis()), "A0002", "CL", null, "");
//		(String processID, String applicationNo, String product, String policyNo, String statusURL
//		JSONObject json = new JSONObject(result);
		//System.out.println(result);
		
				
	}

	@Override
	public void setParameters(Map<String, String> parameters) {
		super.setParameters(parameters);
		setProtocol(parameters.get("protocol"));
		setHost(parameters.get("host"));
		setPort(Integer.valueOf(parameters.get("port")));
		setContext(parameters.get("context"));
		setUser(parameters.get("user"));
		setPassword(parameters.get("password"));
	}

	@Override
	public Object startWorkflow(Map<String, Object> parameters) {
		
		try {
			return createCase(
					(String) parameters.get("processID"), 
					(String) parameters.get("applicationNo"),
					(String) parameters.get("product"),
					(String) parameters.get("policyNo"),				
					"");
		}catch(Exception e) {
			e.printStackTrace();
			//throw e;
		}
		return null;
	}
	
}

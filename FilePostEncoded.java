package com.skytizens.alfresco.duw.webscript;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ClassManager;
import org.alfresco.util.interfaces.CustomDeclarativeWebScript;
import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.skytizens.alfresco.duw.utils.TProcessManualAudit;
import com.skytizens.alfresco.repo.masterdata.MasterDataService;
import com.skytizens.alfresco.utils.ExternalWorkflowManager;

public class FilePostEncoded extends CustomDeclarativeWebScript {
	
	private static String MODULE_NAME = "RestAPI";
	private static String MODULE_CODE = "2437555328";
	private static String MODULE_ENCODED_CODE = "2437555328";

	private static FastDateFormat sdf = FastDateFormat.getInstance("dd/MM/yyyy", Locale.ENGLISH);
	private static FastDateFormat sdft = FastDateFormat.getInstance("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
	private static Date maxDate;
	static {
		try {
			maxDate = sdf.parse("01/01/2099");
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static String PARAM_FILEDATA = "filedata";
	private static String PARAM_OVERWRITE = "overwrite";
	private static String PARAM_NAME = "name";
	private static String PARAM_MIMETYPE = "mimetype";
	private static String PARAM_TITLE = "title";
	private static String PARAM_DESCRIPTION = "description";
	private static String PARAM_TYPE = "type";
	private static String PARAM_APPNO = "prop_duw_appno";
	private static String PARAM_PRODUCT = "prop_duw_product";
	//private static String PARAM_TEAM = "team";
	
	private ExternalWorkflowManager externalWorkflowManager;
	private String processType;
	
	private boolean useModule = false;
	
	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private ContentService contentService;
	private NamespaceService namespaceService;
	private MasterDataService masterDataService;
	private DictionaryService dictionaryService;
	
	/**
	 * @param serviceRegistry the serviceRegistry to set
	 */
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		nodeService = serviceRegistry.getNodeService();
		contentService = serviceRegistry.getContentService();
		namespaceService = serviceRegistry.getNamespaceService();
		dictionaryService = serviceRegistry.getDictionaryService();
	}

	/**
	 * @param classManager the classManager to set
	 */
	public void setClassManager(ClassManager classManager) {
		useModule = MODULE_CODE.equals(classManager.getVerifyCode(MODULE_ENCODED_CODE, this));
	}
	
	public void setParams(Map<String,String> params){
		this.processType = params.containsKey("processType") ? params.get("processType") : null;		
	}
	
	/**
	 * @param command the command to set
	 */
	public void setObjects(Map<String,Object> objects) {
		masterDataService = (MasterDataService) objects.get("masterDataService");
		externalWorkflowManager = (ExternalWorkflowManager) objects.get("externalWorkflowManager");
	}

	/**
	 * Webscript execute method
	 * 
	 */
	@Override
	public Map<String, Object> executeImplExt(WebScriptRequest req, Status status, Cache cache)
	{
		if(useModule)
		{
			Map<String, Object> model = new HashMap<String, Object>();
	
			try {
				JSONArray formArr = (JSONArray) req.parseContent();
				for(int i = 0; i<formArr.length(); i++) {
					JSONObject formData = formArr.getJSONObject(i);
					
					String destination = formData.getString("destination");
			        Boolean overwrite = formData.has(PARAM_OVERWRITE) ? false : formData.getBoolean(PARAM_OVERWRITE);

			        String fname = formData.getString(PARAM_NAME);
			        String fmimetype = formData.getString(PARAM_MIMETYPE);
			        String title = formData.getString(PARAM_TITLE);
			        String description = formData.getString(PARAM_DESCRIPTION);
			        String type = formData.getString(PARAM_TYPE);
			        String content = formData.getString(PARAM_FILEDATA);
			        byte[] decodedBytes = Base64.getDecoder().decode(content);
			        
			        Map<QName,Serializable> properties = new HashMap<QName,Serializable>();
			        for(String param : formData.keySet()) {
			        	if(param.startsWith("prop_")){
			        		String name = param.substring(5);
			        		int ind = name.indexOf("_");
			        		String prefix = name.substring(0,ind);
			        		name = name.substring(ind + 1);
			        		QName qname = QName.createQName(prefix, name, namespaceService);
			        		String value = req.getParameter(param);
			        		if(value != null && !value.isEmpty()){
			        			PropertyDefinition def = dictionaryService.getProperty(qname);
			        			if(def != null){
				        			String ptype = def.getDataType().toString().trim();
				        			ptype = ptype.substring(ptype.indexOf("}") + 1, ptype.length());
				        			
				        			if(def.isMultiValued()) {
				        				ArrayList<Serializable> obj = new ArrayList<Serializable>();
			        					String[] elems = value.split(",");
			        					for(String elem : elems) {
			                                if(ptype.equals("date")){
			                                	Date date = sdf.parse(elem);
			                                	if(date.after(maxDate)){
			                                		throw new AlfrescoRuntimeException("Wrong date value '" + value + "'. The date must be before 01/01/2099");
			                                	}
			                                	obj.add(date);
			                                }else if(ptype.equals("datetime")){
			                                	Date date = sdft.parse(elem);
			                                	if(date.after(maxDate)){
			                                		throw new AlfrescoRuntimeException("Wrong date value '" + value + "'. The date must be before 01/01/2099");
			                                	}
			                                	obj.add(date);
			                                }else if(ptype.equals("boolean")){
			                                	Boolean date = Boolean.valueOf(elem);
			                                	obj.add(date);
			                                }else{
			                                	obj.add(elem);
			                                }
			        					}
				        				properties.put(qname, obj);
				        			}else {
		                                if(ptype.equals("date")){
		                                	Date date = sdf.parse(value);
		                                	if(date.after(maxDate)){
		                                		throw new AlfrescoRuntimeException("Wrong date value '" + value + "'. The date must be before 01/01/2099");
		                                	}
		                                	properties.put(qname, date);
		                                }else if(ptype.equals("datetime")){
		                                	Date date = sdft.parse(value);
		                                	if(date.after(maxDate)){
		                                		throw new AlfrescoRuntimeException("Wrong date value '" + value + "'. The date must be before 01/01/2099");
		                                	}
		                                	properties.put(qname, date);
		                                }else if(ptype.equals("boolean")){
		                                	Boolean date = Boolean.valueOf(value);
		                                	properties.put(qname, date);
		                                }else{
		                                	properties.put(qname, value);
		                                }
				        			}
			        			}
			        		}
			        	}
			        }
			        
			        properties.put(ContentModel.PROP_NAME, fname);
					if(title != null && !title.isEmpty()){
						properties.put(ContentModel.PROP_TITLE, title);
					}
					if(description != null && !description.isEmpty()){
						properties.put(ContentModel.PROP_DESCRIPTION, description);
					}
			        
					String appno = formData.getString(PARAM_APPNO);
			        String productParam = formData.getString(PARAM_PRODUCT);
			        
			        if(type == null || type.isEmpty()){
			        	throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Type is mandatory field.");
			        }
			        QName typeQname = QName.createQName(type, namespaceService);
			        
			        if((productParam == null || productParam.isEmpty())){
			        	throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Product is mandatory field.");
			        }
			        
			        if((appno == null || appno.isEmpty())){
			        	throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Application Number is mandatory field.");
			        }
			        
			        String fileName = fname;
	        		String mimetype = fmimetype;
	                
	        		if(destination == null || destination.isEmpty()){
						throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Destination is mandatory field.");
					}
	        		
	        		NodeRef folderNode = getNodeRef(destination);
//	        		NodeRef folderNode = getFolder(getNodeRef(destination), appno);
//	                if(!nodeService.hasAspect(folderNode, TProcessModel.ASPECT_QNAME_PROCESS)){
//						nodeService.addAspect(folderNode, TProcessModel.ASPECT_QNAME_PROCESS, new HashMap<QName, Serializable>());
//						nodeService.addAspect(folderNode, aspectFolder, new HashMap<QName, Serializable>());
//						nodeService.setProperty(folderNode, asApp, appno);
//					}
	                
	                // create the file if it doesn't exist
	                NodeRef fileNode = nodeService.getChildByName(folderNode, ContentModel.ASSOC_CONTAINS, fileName);		
					if(fileNode != null){
						if(!overwrite){
							throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "File already exists.");
						}
					}else{
						fileNode = nodeService.createNode(folderNode,
							ContentModel.ASSOC_CONTAINS,
							QName.createQName(fileName),
							typeQname).getChildRef();
					}
					
					nodeService.addProperties(fileNode, properties);
					
					InputStream is = new ByteArrayInputStream(decodedBytes);
	        		if(is.available() > 0) {
						// Setting the file contents
	        			ContentWriter writer = contentService.getWriter(fileNode, ContentModel.PROP_CONTENT, true);
						writer.setMimetype(mimetype);
						writer.putContent(is);
	        		}
					
					if(!nodeService.hasAspect(fileNode, ContentModel.ASPECT_VERSIONABLE)){
						nodeService.addAspect(fileNode, ContentModel.ASPECT_VERSIONABLE, new HashMap<QName, Serializable>());
						nodeService.setProperty(fileNode, ContentModel.PROP_AUTO_VERSION, true);
					}
					
					// Ensure the externalWorkflowManager and processType are not null
					if (externalWorkflowManager == null) {
					    throw new WebScriptException("ExternalWorkflowManager is not initialized.");
					}
					if (processType == null || processType.isEmpty()) {
					    throw new WebScriptException("Process type is not specified.");
					}

					// Prepare parameters
					String result;
					Map<String, Object> params = new HashMap<String, Object>();
					params.put("processId", "");
					params.put("applicationNo", PARAM_APPNO);
					params.put("product", PARAM_PRODUCT);
					params.put("policyNo", "");
					result = (String) externalWorkflowManager.getConnector(processType).startWorkflow(params);
					if(result == null){
						throw new WebScriptException("Cannot start case.");
					}    
	                
				}

		        model.put("success", true);
		        
		        status.setCode(200);
				
			} catch (WebScriptException e) {
				status.setCode(e.getStatus());
				model.put("error", e.getStatus() + " " + e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				status.setCode(500);
				model.put("error", "500 " + e.getMessage());
			}
			return model;
		}
		else
		{
			throw new AlfrescoRuntimeException("You cannot use module: " + MODULE_NAME);
		}
	}
	
	private NodeRef getNodeRef(String path)
	{
		NodeRef pathNodeRef = null;
		
		if(path != null && !path.isEmpty())
		{
			StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	    	pathNodeRef = nodeService.getRootNode(storeRef);
	    	QName qname = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "company_home");
	    	List<ChildAssociationRef> assocRefs = nodeService.getChildAssocs(pathNodeRef, ContentModel.ASSOC_CHILDREN, qname);
	    	pathNodeRef = assocRefs.get(0).getChildRef();
	    	String[] paths = path.split("/");
	    	for(String name : paths){
	    		if(!name.isEmpty())
	    		{
	    			pathNodeRef = getFolder(pathNodeRef, name);
	    			if(pathNodeRef == null){
	    				return null;
	    			}
	    		}
	    	}
		}
		
		return pathNodeRef;
	}
	
	private NodeRef getFolder(NodeRef nodeRef, String name)
    {
    	NodeRef folderNodeRef = nodeService.getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, name);
		if(folderNodeRef == null)
		{
			try{
				folderNodeRef = serviceRegistry.getFileFolderService().create(nodeRef, name, ContentModel.TYPE_FOLDER).getNodeRef();
			}catch(Exception e){
				e.printStackTrace();
			}
        }
		
		return folderNodeRef;
    }
}

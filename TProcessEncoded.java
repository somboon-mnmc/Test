package com.skytizens.alfresco.duw.actions;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.coci.CheckOutCheckInServiceImpl;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.i18n.StaticMessageLookup;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.i18n.MessageLookup;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentAccessor;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ClassManager;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.interfaces.CustomActionExecuter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.WebScriptException;
import org.w3c.dom.NodeList;

import com.skytizens.alfresco.actions.TiffDescription;
import com.skytizens.alfresco.duw.utils.TProcessField;
import com.skytizens.alfresco.duw.utils.TProcessManualAudit;
import com.skytizens.alfresco.duw.utils.TProcessModel;
import com.skytizens.alfresco.duw.webscript.GenerateTProcessPolicy;
import com.skytizens.alfresco.repo.masterdata.MasterDataService;
import com.skytizens.alfresco.utils.ExternalWorkflowManager;
import com.skytizens.alfresco.utils.SkyAreaProperties;
import com.skytizens.alfresco.utils.SkyAreaString;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;

public class TProcessEncoded extends ActionExecuterAbstractBase implements CustomActionExecuter {
	
	private static String MODULE_NAME = "TProcess";
	private static String MODULE_CODE = "2710018887";
	private static String MODULE_ENCODED_CODE = "2710018887";
	
	private final static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
	
	private static final int XRES_TAG = 282;
	private static final int YRES_TAG = 283;
	
	private static final Set<Integer> supportedComp = new TreeSet<Integer>();
	static {
	    supportedComp.add(TIFFEncodeParam.COMPRESSION_NONE);
	    supportedComp.add(TIFFEncodeParam.COMPRESSION_GROUP3_1D);
	    supportedComp.add(TIFFEncodeParam.COMPRESSION_GROUP3_2D);
	    supportedComp.add(TIFFEncodeParam.COMPRESSION_GROUP4);
	    supportedComp.add(TIFFEncodeParam.COMPRESSION_PACKBITS);
	    supportedComp.add(TIFFEncodeParam.COMPRESSION_JPEG_TTN2);
	    supportedComp.add(TIFFEncodeParam.COMPRESSION_DEFLATE);
	}
	
	private final static String NORMALIZE_FILE = "[\"*<>?/:|\n\t]";
	private final static String NORMALIZE_FOLDER = "[\"*<>?:|\n\t]";
	
	private static final Logger logger = LoggerFactory.getLogger(TProcess.class);
	
	private boolean useModule = false;
	
	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private SiteService siteService;
	private ContentService contentService;
	private FileFolderService fileFolderService;
	private NamespaceService namespaceService;
	private DictionaryService dictionaryService;
	private TransactionService transactionService;
	private MessageLookup messageLookup;
	private MasterDataService masterDataService;
	
	private TProcessManualAudit manualAudit;
	private String duplicatesFormat;
	private Set<String> copyList;
	private ExternalWorkflowManager externalWorkflowManager;
	private GenerateTProcessPolicy generateTProcessPolicy;
	
	private String confPath;
	private String processType;
	
	boolean processEnabled = true;

	/**
	 * @param serviceRegistry the serviceRegistry to set
	 */
	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		nodeService = serviceRegistry.getNodeService();
		siteService = serviceRegistry.getSiteService();
		contentService = serviceRegistry.getContentService();
		fileFolderService = serviceRegistry.getFileFolderService();
		namespaceService = serviceRegistry.getNamespaceService();
		dictionaryService = serviceRegistry.getDictionaryService();
		transactionService = serviceRegistry.getTransactionService();
		
		messageLookup = new StaticMessageLookup();
	}
	
	@Override
	public void setClassManager(ClassManager classManager) {
		useModule = MODULE_CODE.equals(classManager.getVerifyCode(MODULE_ENCODED_CODE,this));
	}
	
	@Override
	public void setParameters(Map<String, String> parameters) {
		this.confPath = parameters.containsKey("confPath") ? parameters.get("confPath") : null;
		duplicatesFormat = parameters.containsKey("duplicatesFormat") ? parameters.get("duplicatesFormat") : null;
		if(duplicatesFormat != null && duplicatesFormat.trim().isEmpty()){
			duplicatesFormat = null;
		}
		copyList = new TreeSet<String>();
		String[] list = parameters.get("copyList").split(",");
		for(String elem : list){
			copyList.add(elem);
		}
		this.processType = parameters.containsKey("processType") ? parameters.get("processType") : null;
		this.processEnabled = parameters.containsKey("processEnabled") ? Boolean.valueOf(parameters.get("processEnabled")) : false;
	}
	
	@Override
	public void setObjects(Map<String, Object> objects) {
		manualAudit = (TProcessManualAudit) objects.get("manualAudit");
		externalWorkflowManager = (ExternalWorkflowManager) objects.get("externalWorkflowManager");
		generateTProcessPolicy = (GenerateTProcessPolicy) objects.get("generateTProcessPolicy");
		masterDataService = (MasterDataService) objects.get("masterDataService");
	}
    
	@Override
	protected void executeImpl(Action action, NodeRef nodeRef)
	{
		if(useModule)
		{
			if(nodeService.exists(nodeRef))
			{
				if (nodeService.hasAspect(nodeRef, TProcessModel.ASPECT_QNAME_PROCESS)) {
					NodeRef destRef = (NodeRef) action.getParameterValue(TProcess.DESTINATION_FOLDER);
					if(nodeService.exists(destRef)){
						
						if(logger.isDebugEnabled()) logger.debug("Init process " + nodeRef);
						
						String initiator = AuthenticationUtil.getFullyAuthenticatedUser();
						String nodeName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
						String modifier = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIER);
						
						//Create jobID
						Date now = new Date();
						Long jobID = manualAudit.initJobLog(nodeRef, nodeName, "Process", "inqueue", now, 0, 0, modifier, initiator);
						
						String status = (String) nodeService.getProperty(nodeRef, TProcessModel.PROP_QNAME_STATUS);
						boolean isDelete = false;
						
						try 
						{
							TProcess.updateProperties(transactionService, nodeService, nodeRef, "in progress");
							
							QName typeQName = null;
							Map<QName,Serializable> properties = nodeService.getProperties(nodeRef);
							
							//String status = (String) properties.get(PROP_QNAME_STATUS);
							//if(!status.equals("completed")){
							//	throw new AlfrescoRuntimeException("Files not completed");
							//}
							
							String type = (String) properties.get(TProcessModel.PROP_QNAME_TYPE);
							if(type != null && !type.isEmpty() && (type.indexOf(":") > 0)){
								typeQName = QName.createQName(type, namespaceService);
							}
							
							QName wfField = null;
							QName applicationNoField = null;
							QName processIdField = null;
							QName productField = null;
							QName teamField = null;
							QName amountField = null;
							QName phoneField = null;
							QName policyNoField = null;
							QName importType = null;
							QName flag1Field = QName.createQName("duw:flag1", namespaceService);
							QName flag2Field = QName.createQName("duw:flag2", namespaceService);
							QName chbField = QName.createQName("duw:chb", namespaceService);
							QName appTypeQName = QName.createQName("duw:application", namespaceService);
							String pathTemplate = null;
							String nameTemplate = null;
							Set<String> wfTypes = new TreeSet<String>();
							
							QName stField = null;
							boolean stFolder = false;
							Map<String,QName> stTypes = new HashMap<String,QName>();
							Map<QName, Pattern> stTypesPatterns = new HashMap<QName, Pattern>();
							Map<QName, List<QName>> stRequired = new HashMap<QName, List<QName>>();
							
							//get fields
							List<TProcessField> tfields = new ArrayList<TProcessField>();
							String site = null;
							SiteInfo siteInfo = siteService.getSite(nodeRef);
							if(siteInfo != null){
								site = siteInfo.getShortName();
							}
							if(site == null || site.isEmpty()){
								site = "default";
							}
							try {
								NodeRef confNodeRef = getNodeRef(confPath + "/" + site + "-tprocess.json");
								if(confNodeRef != null){
							        ContentReader reader = contentService.getReader(confNodeRef, ContentModel.PROP_CONTENT);
								    String content = reader.getContentString();
								    
								    JSONObject json = new JSONObject(content);
								    
								    if(json.has("subtypes") && json.has("subtypesField")){
								    	String fieldId = json.getString("subtypesField");
								    	stField = QName.createQName(fieldId, namespaceService);
								    	stFolder = json.has("subtypesFolder") ? json.getBoolean("subtypesFolder") : false;
								    	JSONArray array1 = json.getJSONArray("subtypes");
									    for(int i=0; i<array1.length(); i++){
									    	JSONObject obj = array1.getJSONObject(i);
									    	String typeId = obj.getString("id");
									    	if(obj.has("value")){
									    		String value = obj.getString("value");
									    		stTypes.put(value, QName.createQName(typeId, namespaceService));
									    	}
									    	if(obj.has("pattern")){
									    		String pattern = obj.getString("pattern");
									    		stTypesPatterns.put(QName.createQName(typeId, namespaceService), Pattern.compile(pattern));
									    	}
									    }
								    }
								    
								    if(json.has("required")){
								    	JSONArray array1 = json.getJSONArray("required");
									    for(int i=0; i<array1.length(); i++){
									    	JSONObject obj = array1.getJSONObject(i);
									    	String typeId = obj.getString("type");
									    	List<QName> prop2 = new ArrayList<QName>();
									    	JSONArray array2 = obj.getJSONArray("properties");
										    for(int j=0; j<array2.length(); j++){
										    	String propertyId = array2.getString(j);
										    	prop2.add(QName.createQName(propertyId, namespaceService));
										    }
									    	stRequired.put(QName.createQName(typeId, namespaceService), prop2);
									    }
								    }
								    
								    if(json.has("case")){
								    	JSONObject obj = json.getJSONObject("case");
								    	applicationNoField = QName.createQName(obj.getString("application_no"), namespaceService);
								    	processIdField = QName.createQName(obj.getString("process_id"), namespaceService);
								    	if(obj.has("product")) productField = QName.createQName(obj.getString("product"), namespaceService);
								    	if(obj.has("team")) teamField = QName.createQName(obj.getString("team"), namespaceService);
								    	if(obj.has("amount")) amountField = QName.createQName(obj.getString("amount"), namespaceService);
								    	if(obj.has("phone")) phoneField = QName.createQName(obj.getString("phone"), namespaceService);
								    	policyNoField = QName.createQName(obj.getString("policy_no"), namespaceService);
//								    	importType = QName.createQName(obj.getString("import_type"), namespaceService);
								    	
								    	pathTemplate = obj.getString("path");
								    	nameTemplate = obj.getString("name");
								    }
								    
								    if(json.has("path")){
								    	pathTemplate = json.getString("path");
								    }
								    if(json.has("name")){
								    	nameTemplate = json.getString("name");
								    }
								    
								    if(json.has("type")){
								    	typeQName = QName.createQName(json.getString("type"), namespaceService);
								    }
								    
								    if(json.has("typesField")){
								    	TProcessField field = new TProcessField();
								    	field.setId(TProcessModel.PROP_QNAME_TYPE);
								    	//field.setTitle(obj2.getString("title"));
								    	field.setType("text");
								    	field.setAssoc(QName.createQName(json.getString("typesField"), namespaceService));
								    	field.setRequired(true);
								    	tfields.add(field);
								    }
								    
								    if(type != null && !type.isEmpty()){
								    	if(json.has("types")){
										    JSONArray array1 = json.getJSONArray("types");
										    for(int i=0; i<array1.length(); i++){
										    	JSONObject obj = array1.getJSONObject(i);
										    	boolean contains = false;
										    	if(obj.has("values")){
										    		JSONArray array2 = obj.getJSONArray("values");
										    		for(int j=0; j<array2.length(); j++){
										    			JSONObject obj2 = array2.getJSONObject(j);
										    			String typeId = obj2.getString("id");
												    	if(type.equals(typeId)){
												    		contains = true;
												    		break;
												    	}
										    		}
										    	}else{
										    		String typeId = obj.getString("id");
											    	if(type.equals(typeId)) contains = true;
										    	}
										    	//boolean accept = obj.has("accept") ? obj.getBoolean("accept") : false;
										    	//if(accept){
										    	//	wfTypes.add(QName.createQName(typeId, namespaceService));
										    	//}
										    	if(contains){
										    		JSONArray fields = obj.getJSONArray("fields");
												    for(int j=0; j<fields.length(); j++){
												    	JSONObject obj2 = fields.getJSONObject(j);
												    	TProcessField field = new TProcessField();
												    	if(obj2.has("value")){
												    		field.setValue(obj2.getString("value"));
												    	}else{
												    		field.setId(QName.createQName(obj2.getString("id"), namespaceService));
												    	}
												    	//field.setTitle(obj2.getString("title"));
												    	field.setType(obj2.getString("type"));
												    	field.setAssoc(QName.createQName(obj2.getString("assoc"), namespaceService));
												    	field.setRequired(obj2.has("required") ? obj2.getBoolean("required") : false);
												    	field.setHidden(obj2.has("hidden") ? obj2.getBoolean("hidden") : false);
												    	tfields.add(field);
												    }
										    	}
										    }
								    	}
								    }
								    
								    if(json.has("aspects")){
									    JSONArray array2 = json.getJSONArray("aspects");
									    for(int i=0; i<array2.length(); i++){
									    	JSONObject obj = array2.getJSONObject(i);
									    	String aspectId = obj.getString("id");
									    	if(nodeService.hasAspect(nodeRef, QName.createQName(aspectId, namespaceService))){
									    		JSONArray fields = obj.getJSONArray("fields");
											    for(int j=0; j<fields.length(); j++){
											    	JSONObject obj2 = fields.getJSONObject(j);
											    	TProcessField field = new TProcessField();
											    	if(obj2.has("value")){
											    		field.setValue(obj2.getString("value"));
											    	}else{
											    		field.setId(QName.createQName(obj2.getString("id"), namespaceService));
											    	}
											    	//field.setTitle(obj2.getString("title"));
											    	field.setType(obj2.getString("type"));
											    	field.setAssoc(QName.createQName(obj2.getString("assoc"), namespaceService));
											    	field.setRequired(obj2.has("required") ? obj2.getBoolean("required") : false);
											    	field.setHidden(obj2.has("hidden") ? obj2.getBoolean("hidden") : false);
											    	tfields.add(field);
											    }
									    	}
									    }
								    }
								}
								
							} catch (Exception e) {
								e.printStackTrace();
								throw new WebScriptException("Cannot get process configuration.");
							}
							
							Map<QName,Serializable> props = new HashMap<QName,Serializable>();
							for(TProcessField tfield : tfields){
								Serializable value = null;
								if(tfield.getValue() != null){
									//value = parseTemplate(tfield.getValue(), properties, elems, null);
								}else if(properties.containsKey(tfield.getId())){
									value = properties.get(tfield.getId());
								}
								if(value != null){
									if(value instanceof String){
										if(value != null && ((String) value).isEmpty()){
											value = null;
										}
									}
								}
	            				if(value != null){
	            					if(tfield.getType().equals("text") || tfield.getType().equals("select") || tfield.getType().equals("masterdata-select") || tfield.getType().equals("masterdata-text")){
	            						props.put(tfield.getAssoc(), value);
	            					}else if(tfield.getType().equals("boolean")){
	            						props.put(tfield.getAssoc(), Boolean.valueOf((String) value));
	            					}else if(tfield.getType().equals("date")){
	            						props.put(tfield.getAssoc(), sdf.parse((String) value));
	            					}
	            				}else if(tfield.isRequired()){
	            					throw new WebScriptException("Field " + tfield.getId() + " is required.");
	            				}
							}
							
							List<NodeRef> wfNodes = new ArrayList<NodeRef>();
							List<NodeRef> appNodes = new ArrayList<NodeRef>();
							
							manualAudit.updateJobLog(jobID, "inprogress");
							
							String warningMessage = null;
							
							//System.out.println(processIdField);
							if(processIdField != null){
								props.put(processIdField, jobID);
							}
							
							boolean allowOverride = false;
							boolean allowAppend = false;
							boolean allowWorkflow = processEnabled;
							boolean allowCase = false;
							boolean allowGenerate = false;
							String mergeAction = (String) nodeService.getProperty(nodeRef, TProcessModel.PROP_QNAME_ACTION);
							if(mergeAction != null){
								if(mergeAction.equals("append")){
									allowAppend = true;
								}else if(mergeAction.equals("replace")){
									allowOverride = true;
								}else if(mergeAction.equals("attach")){
									allowOverride = true;
									allowWorkflow = false;
									allowGenerate = true;
								}else if(mergeAction.equals("case")){
									allowWorkflow = false;
									allowCase = true;
								}
							}
							
							// process and check if moved all
							if(logger.isDebugEnabled()) logger.debug("Start process");
							List<NodeRef> nodes = (List<NodeRef>) action.getParameterValue(TProcess.NODES);
							if(nodes != null){
								if(logger.isDebugEnabled()) logger.debug("Process " + nodes.size() + " selected files");
								for(NodeRef elem : nodes){
									if(processFile(elem, nodeRef, destRef, props, typeQName, wfField, wfTypes, wfNodes, stField, stFolder, stTypes, stTypesPatterns, stRequired, appNodes, allowOverride, allowAppend, pathTemplate, nameTemplate)){

									}else{
										warningMessage = "Some documents left not processed.";
									}
								}
							}else{
								// process and check if moved all
								if(processFiles(nodeRef, destRef, props, typeQName, wfField, wfTypes, wfNodes, stField, stFolder, stTypes, stTypesPatterns, stRequired, appNodes, allowOverride, allowAppend, pathTemplate, nameTemplate)){
									nodeService.deleteNode(nodeRef);
									isDelete = true;
								}else{
									warningMessage = "Some documents left not processed.";
								}
							}
							if(logger.isDebugEnabled()) logger.debug("Finish process");
							
							if(applicationNoField != null && appNodes.size() > 0){
								String applicationNo = (String) nodeService.getProperty(appNodes.get(0), applicationNoField);
								if(applicationNo == null){
									throw new WebScriptException("Cannot get application number.");
								}
								if(allowWorkflow){
									//System.out.println("Start case: " + applicationNo + " nodes " + appNodes.size());
									//System.out.println("jobID: " + jobID);
									String product = productField == null ? null : (String) nodeService.getProperty(appNodes.get(0), productField);
									String team = teamField == null ? null : (String) nodeService.getProperty(appNodes.get(0), teamField);
									Float amountValue = amountField == null ? null : (Float) nodeService.getProperty(appNodes.get(0), amountField);
									String amount = amountValue == null ? null : amountValue.toString();
									String phone = phoneField == null ? null : (String) nodeService.getProperty(appNodes.get(0), phoneField);
									String policyNo = (String) nodeService.getProperty(appNodes.get(0), policyNoField);
									String importType2 = (String) nodeService.getProperty(appNodes.get(0), QName.createQName("duw:import_type", namespaceService) /*QName.createQName("duw:import_type")*/ );
									// QName.createQName("duw:status", namespaceService)
									Integer flag1 = null;
									Integer flag2 = null;
									Integer flag3 = null;
									String chb = null;
									
									for(NodeRef appNode: appNodes){
										QName contentType = nodeService.getType(appNode);
										if(contentType.equals(appTypeQName)){
											flag1 = (Integer) nodeService.getProperty(appNode, flag1Field);
											flag2 = (Integer) nodeService.getProperty(appNode, flag2Field);
											if(flag1 != null && flag2 != null){
												flag3 = flag1 + flag2;
											}
											chb = (String) nodeService.getProperty(appNode, chbField);
											break;
										}
									}
									
									//Product API URL
									String statusURL = null;
									NodeRef productURLs = masterDataService.getMasterDataSource("GUW Product Status URL");
									if(productURLs != null)
									{
										List<Map<String,String>> data = masterDataService.getMasterData(productURLs, null, null);
										
										for(Map<String,String> item : data) {
											String value = item.get("value");
											String label = item.get("label");
											if(value != null && product != null && product.equals(value)){
												statusURL = label;
											}
										}
									}
									
									//System.out.println("product: " + product);
									//System.out.println("team: " + team);
									
									if(logger.isDebugEnabled()) logger.debug("Start case");
									String result;
									
									Map<String, Object> params = new HashMap<String, Object>();
									params.put("action", "createCase");
									params.put("processID", jobID + "");
									params.put("applicationNo", applicationNo);
									params.put("product", product);
									params.put("team", team);
									params.put("amount", amount);
									params.put("phone", phone);
									params.put("policyNo", policyNo);
									params.put("flag1", flag1);
									params.put("flag2", flag2);
									params.put("flag3", flag3);
									params.put("chb", chb);
									params.put("importType", importType2);
									result = (String) externalWorkflowManager.getConnector(processType).startWorkflow(params);
									
									//if(logger.isDebugEnabled()) logger.debug("result: " + result);
									if(result == null){
										throw new WebScriptException("Cannot start case.");
									}
									JSONObject processJSON = new JSONObject(result);
									if(processJSON.has("error")){
										throw new WebScriptException("Cannot start case: " + processJSON.getString("error"));
									}
									
									if(processJSON.has("recordId") && processJSON.has("processId")){
										String recordId = processJSON.getString("recordId");
										String processId = processJSON.getString("processId");
										if(logger.isDebugEnabled()) logger.debug("Start case with recordId:" + recordId + " and processId: "+ processId +" ,nodes " + appNodes.size());
									}
									if(processJSON.has("pp")){
										JSONObject pm = processJSON.getJSONObject("pp");
										Long id = pm.getLong("id");
										if(logger.isDebugEnabled()) logger.debug("Start case " + id + " nodes " + appNodes.size());
									}
								}else if(allowCase){
									//System.out.println("Start case: " + applicationNo + " nodes " + appNodes.size());
									//System.out.println("jobID: " + jobID);
									String product = productField == null ? null : (String) nodeService.getProperty(appNodes.get(0), productField);
									String team = teamField == null ? null : (String) nodeService.getProperty(appNodes.get(0), teamField);
									Float amountValue = amountField == null ? null : (Float) nodeService.getProperty(appNodes.get(0), amountField);
									String amount = amountValue == null ? null : amountValue.toString();
									String phone = phoneField == null ? null : (String) nodeService.getProperty(appNodes.get(0), phoneField);
									String policyNo = (String) nodeService.getProperty(appNodes.get(0), policyNoField);
									String importType2 = (String) nodeService.getProperty(appNodes.get(0), QName.createQName("duw:import_type", namespaceService) /*QName.createQName("duw:import_type")*/ );
									// QName.createQName("duw:status", namespaceService)
									Integer flag1 = null;
									Integer flag2 = null;
									Integer flag3 = null;
									String chb = null;
									
									for(NodeRef appNode: appNodes){
										QName contentType = nodeService.getType(appNode);
										if(contentType.equals(appTypeQName)){
											flag1 = (Integer) nodeService.getProperty(appNode, flag1Field);
											flag2 = (Integer) nodeService.getProperty(appNode, flag2Field);
											if(flag1 != null && flag2 != null){
												flag3 = flag1 + flag2;
											}
											chb = (String) nodeService.getProperty(appNode, chbField);
											break;
										}
									}
									
									//Product API URL
									String statusURL = null;
									NodeRef productURLs = masterDataService.getMasterDataSource("GUW Product Status URL");
									if(productURLs != null)
									{
										List<Map<String,String>> data = masterDataService.getMasterData(productURLs, null, null);
										
										for(Map<String,String> item : data) {
											String value = item.get("value");
											String label = item.get("label");
											if(value != null && product != null && product.equals(value)){
												statusURL = label;
											}
										}
									}
									
									//System.out.println("product: " + product);
									//System.out.println("team: " + team);
									
									if(logger.isDebugEnabled()) logger.debug("Start case");
									String result;
									
									Map<String, Object> params = new HashMap<String, Object>();
									params.put("action", "createItem");
									params.put("processID", jobID + "");
									params.put("applicationNo", applicationNo);
									params.put("product", product);
									params.put("team", team);
									params.put("amount", amount);
									params.put("phone", phone);
									params.put("policyNo", policyNo);
									params.put("flag1", flag1);
									params.put("flag2", flag2);
									params.put("flag3", flag3);
									params.put("chb", chb);
									params.put("importType", "Approved");
									result = (String) externalWorkflowManager.getConnector(processType).startWorkflow(params);
									
									//if(logger.isDebugEnabled()) logger.debug("result: " + result);
									if(result == null){
										throw new WebScriptException("Cannot start case.");
									}
								}
								
								if(allowGenerate){
									String wstatus = (String) nodeService.getProperty(appNodes.get(0), QName.createQName("duw:status", namespaceService));
									if(wstatus != null && (wstatus.equals("approve") || wstatus.equals("approve2"))){
										//generate policy
										JSONObject json = new JSONObject();
										json.put("application_no", nodeService.getProperty(appNodes.get(0), QName.createQName("duw:app_no", namespaceService)));
										
										generateTProcessPolicy.executeInt(json);
									}
								}
							}
							
							if(logger.isDebugEnabled()){
								if(warningMessage != null){
									logger.debug("Warning: " + warningMessage);
								}
							}
							
							//Audit complete
							manualAudit.finishJobLog(jobID, "completed", new Date(), null, null, warningMessage);
							
						} catch (Exception e) {
							e.printStackTrace();
							manualAudit.finishJobLog(jobID, "error", new Date(), null, null, e.getMessage());
							throw new AlfrescoRuntimeException(e.getMessage(), e);
						}finally{
							if(!isDelete){
								TProcess.updateProperties(transactionService, nodeService, nodeRef, status);
							}
						}
						
						if(logger.isDebugEnabled()) logger.debug("Finish process " + nodeRef);
					}
				}
			}
		}
		else
		{
			throw new AlfrescoRuntimeException("You cannot use module: " + MODULE_NAME);
		}
    }
	
	public boolean processFiles(NodeRef nodeRef, NodeRef destRef, Map<QName,Serializable> props, QName typeQName, 
			QName wfField, Set<String> wfTypes, List<NodeRef> wfNodes, 
			QName stField, boolean stFolder, Map<String,QName> stTypes, Map<QName, Pattern> stTypesPatterns, Map<QName, List<QName>> stRequired, List<NodeRef> appNodes, 
			boolean allowOverride, boolean allowAppend,
			String pathTemplate, String nameTemplate) throws Exception {
		boolean movedAll = true;
		List<ChildAssociationRef> assocRefs = nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
    	for(ChildAssociationRef assocRef : assocRefs){
    		NodeRef elem = assocRef.getChildRef();
    		
    		FileInfo info = fileFolderService.getFileInfo(elem);
    		if(info.isFolder()){
    			if(!processFiles(elem, destRef, props, typeQName, wfField, wfTypes, wfNodes, stField, stFolder, stTypes, stTypesPatterns, stRequired, appNodes, allowOverride, allowAppend, pathTemplate, nameTemplate)){
    				movedAll = false;
    			}
    		}else{
    			if(!processFile(elem, nodeRef, destRef, props, typeQName, wfField, wfTypes, wfNodes, stField, stFolder, stTypes, stTypesPatterns, stRequired, appNodes, allowOverride, allowAppend, pathTemplate, nameTemplate)){
    				movedAll = false;
    			}
    		}
    	}
    	return movedAll;
	}
	
	public boolean processFile(NodeRef elem, NodeRef nodeRef, NodeRef destRef, Map<QName,Serializable> props, QName typeQName, 
			QName wfField, Set<String> wfTypes, List<NodeRef> wfNodes, 
			QName stField, boolean stFolder, Map<String,QName> stTypes, Map<QName, Pattern> stTypesPatterns, Map<QName, List<QName>> stRequired, List<NodeRef> appNodes, 
			boolean allowOverride, boolean allowAppend,
			String pathTemplate, String nameTemplate) throws Exception {
		boolean movedAll = true;

		logger.debug("process file: " + elem);
		QName contentType = nodeService.getType(elem);
		if(contentType.equals(ContentModel.TYPE_CONTENT)){
			if(stField != null){
				String typeCode = (String) nodeService.getProperty(stFolder ? nodeRef : elem, stField);
				if(typeCode != null){
					if(stTypes.containsKey(typeCode)){
						contentType = stTypes.get(typeCode);
						nodeService.setType(elem, contentType);
					}else{
						for(QName qname : stTypesPatterns.keySet()){
							if(stTypesPatterns.get(qname).matcher(typeCode).matches()){
								contentType = qname;
								nodeService.setType(elem, contentType);
							}
						}
					}
				}
			}else{
    			if(typeQName != null){
    				nodeService.setType(elem, typeQName);
    				contentType = typeQName;
    			}
			}
			nodeService.addProperties(elem, props);
		}else{
			//setup only properties if value is empty on document
			for(QName qname : props.keySet()){
				Serializable value = props.get(qname);
				Serializable value2 = nodeService.getProperty(elem, qname);
				if(value instanceof String){
					if(value != null && ((String)value).isEmpty()) value = null;
				}
				if(value2 instanceof String){
					if(value2 != null && ((String)value2).isEmpty()) value2 = null;
				}
				
				if(value != null && value2 == null){
					nodeService.setProperty(elem, qname, value);
				}
			}
		}
		
		if(isCompleted(nodeService.getProperties(elem), contentType, stRequired)){
			String typeCode = null;
			if(wfField != null){
				typeCode = (String) nodeService.getProperty(elem, wfField);
			}
			if(typeCode != null && wfTypes.contains(typeCode)){
				//start workflow
				wfNodes.add(elem);
				movedAll = false;
				logger.debug("process - need approve: " + elem);
			}else{
				logger.debug("process - move: " + elem + " to " + destRef);
				Map<QName,Serializable> properties = nodeService.getProperties(elem);
				
				Map<String,String> elems = new HashMap<String,String>();
				String typeTitle = dictionaryService.getType(contentType).getTitle(messageLookup);
				elems.put("type", typeTitle == null ? "" : typeTitle);
				setDateVariable(null, elems);
				
				List<String> pathList = parseTemplate(pathTemplate, properties, elems, NORMALIZE_FOLDER);
				String path = pathList != null && pathList.size() > 0 ? pathList.get(0) : null;
				
				List<String> nameList = parseTemplate(nameTemplate, properties, elems, NORMALIZE_FILE);
				String name = nameList != null && nameList.size() > 0 ? nameList.get(0) : null;
				
				NodeRef folder = getFolderByPath(destRef, path);
				logger.debug("process - folder: " + folder);
				NodeRef node = move(elem, folder, name, allowOverride, allowAppend);
				logger.debug("process - path: " + path);
				logger.debug("process - name: " + name);
				//nodeService.setProperty(node, QName.createQName("duw:scan_status", namespaceService), null);
				//nodeService.setProperty(node, QName.createQName("duw:reject_reason"), null);
				
				appNodes.add(node);
				//nodeService.moveNode(elem, destRef, null, null);
			}
		}else{
			movedAll = false;
			logger.debug("process - not completed: " + elem);
		}
		    		
    	return movedAll;
	}
	
	private void setDateVariable(Date start, Map<String,String> elems){
		
		Calendar cal = Calendar.getInstance(Locale.ENGLISH);
		if(start != null){
			cal.setTime(start);
		}
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		
		elems.put("day", day < 10 ? "0" + day : "" + day);
		elems.put("month", month < 10 ? "0" + month : "" + month);
		elems.put("byy", String.valueOf(year + 543).substring(2,4));
		elems.put("byyyy", String.valueOf(year + 543));
		elems.put("gyy", String.valueOf(year).substring(2,4));
		elems.put("gyyyy", String.valueOf(year));
	}
	
	private NodeRef getFolderByPath(NodeRef nodeRef, String path)
	{
		NodeRef pathNodeRef = nodeRef;
		
		if(path != null && !path.isEmpty())
		{
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
	
	//find document by name without extension
	private NodeRef findDocument(NodeRef parent, String name){
		String p1 = name;
		int ind1 = name.lastIndexOf(".");
		if(ind1 > 0){
			p1 = name.substring(0,ind1);
		}
		
		List<ChildAssociationRef> nodeList = nodeService.getChildAssocs(parent);
		if(nodeList != null){
			for(ChildAssociationRef node : nodeList){
				NodeRef child = node.getChildRef();
				FileInfo fileInfo = fileFolderService.getFileInfo(child);
				if(fileInfo.isFolder()){
					//nothing
				}else{
					String fname = fileInfo.getName();
					int ind2 = fname.lastIndexOf(".");
					if(ind2 > 0){
						fname = fname.substring(0,ind2);
					}
					
					if(fname.equals(p1)){
						return child;
					}
				}
			}
		}
		return null;
	}
	
	private NodeRef move(NodeRef nodeRef, NodeRef folder, String name, boolean allowOverride, boolean allowAppend) throws Exception
	{
		String filename = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
		String ext = "";
		int ind = filename.lastIndexOf('.');
		if(ind > 0){
			ext = filename.substring(ind);
			filename = filename.substring(0,ind);
		}
		
		int cnt = 1;
		String fName = name + ext;
		
		//SiteInfo siteInfo = siteService.getSite(nodeRef);
		//String site = siteInfo.getShortName();
		Integer pages = (Integer) nodeService.getProperty(nodeRef, TProcessModel.PROP_QNAME_PAGES);
		if(pages == null) pages = 0;
		
		//NodeRef childNodeRef = nodeService.getChildByName(folder, ContentModel.ASSOC_CONTAINS, fName);
		NodeRef childNodeRef = findDocument(folder, fName);
		if(childNodeRef == null){
			//System.out.println("new");
			fileFolderService.move(nodeRef, folder, fName);
			childNodeRef = nodeRef;
			if (!nodeService.hasAspect(childNodeRef, ContentModel.ASPECT_VERSIONABLE)) {
				nodeService.addAspect(childNodeRef, ContentModel.ASPECT_VERSIONABLE,
						new HashMap<QName, Serializable>());
			}
		}else{
			if (!nodeService.hasAspect(childNodeRef, ContentModel.ASPECT_VERSIONABLE)) {
				nodeService.addAspect(childNodeRef, ContentModel.ASPECT_VERSIONABLE,
						new HashMap<QName, Serializable>());
			}
			if(allowOverride){
				//System.out.println("override");
				//NodeRef workingCopyNodeRef = childNodeRef;
				NodeRef workingCopyNodeRef = serviceRegistry.getCheckOutCheckInService().checkout(childNodeRef);
				try{
					//copy content
					ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
				    ContentWriter writer = contentService.getWriter(workingCopyNodeRef, ContentModel.PROP_CONTENT, true);
				    writer.setMimetype(reader.getMimetype());
				    writer.putContent(reader);
					
				    //copy type and properties
				    QName nodeType = nodeService.getType(nodeRef);
				    QName nodeType2 = nodeService.getType(workingCopyNodeRef);
				    if(!nodeType.equals(nodeType2)){
				    	nodeService.setType(workingCopyNodeRef, nodeType);
				    }
				    
				    String workingCopyLabel = CheckOutCheckInServiceImpl.getWorkingCopyLabel();
		            String copyName = CheckOutCheckInServiceImpl.createWorkingCopyName(fName, workingCopyLabel);  
				    
				    //do not copy name, version_label, content...
				    Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
				    Map<QName, Serializable> cprop = getCopyProperties(properties);
				    cprop.put(ContentModel.PROP_NAME, copyName);
				    nodeService.addProperties(workingCopyNodeRef, cprop);
				    
				    Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
				    serviceRegistry.getCheckOutCheckInService().checkin(workingCopyNodeRef, versionProperties);

				    //change name if changed extension
				    //nodeService.setProperty(childNodeRef, ContentModel.PROP_NAME, fName);
				}catch(Exception e){
					serviceRegistry.getCheckOutCheckInService().cancelCheckout(workingCopyNodeRef);
					throw e;
				}
				
			    //delete node
			    nodeService.deleteNode(nodeRef);
			}else if(allowAppend){
				//System.out.println("append");
				ContentReader reader = contentService.getReader(nodeRef,
						ContentModel.PROP_CONTENT);
				ContentReader reader2 = contentService.getReader(childNodeRef,
						ContentModel.PROP_CONTENT);
				
				// get mimetypes
		        String sourceMimetype = getMimetype(reader);
		        String sourceMimetype2 = getMimetype(reader2);
		        if(sourceMimetype.equals(MimetypeMap.MIMETYPE_IMAGE_TIFF) && sourceMimetype2.equals(MimetypeMap.MIMETYPE_IMAGE_TIFF))
		        {
		        	File file = TempFileProvider.createTempFile(
		                    "TProcess_merge_tmp_",
		                    ".tiff");
		            
		            List<BufferedImage> images = new ArrayList<BufferedImage>();
		            
		            //2nd image
	            	ImageInputStream is2 = ImageIO.createImageInputStream(reader2.getContentInputStream());
		    		if (is2 == null || is2.length() == 0){
		    			throw new WebScriptException("Image file is empty.");
		    		}
		    		Iterator<ImageReader> iterator2 = ImageIO.getImageReaders(is2);
		    		if (iterator2 == null || !iterator2.hasNext()) {
		    			throw new WebScriptException("Image file format not supported by ImageIO");
		    		}
		    		
		    		TIFFImageReader imageReader2 = null;
		    		while(iterator2.hasNext()){
		    			ImageReader ir = iterator2.next();
		    			if(ir instanceof TIFFImageReader){
		    				imageReader2 = (TIFFImageReader) ir;
		    				break;
		    			}
		    		}
		    		
		    		iterator2 = null;
		    		imageReader2.setInput(is2);
		    		
		            int numPages2 = imageReader2.getNumImages(true);
		            if(numPages2 == 0){
		    			throw new WebScriptException("Image file is empty.");
		    		}
		            
		            for(int j = 0; j < numPages2; j++)
	    	        {
	            		images.add(imageReader2.read(j));
	    	        }
		            
		        	//1st image
		            ImageInputStream is = ImageIO.createImageInputStream(reader.getContentInputStream());
		    		if (is == null || is.length() == 0){
		    			throw new WebScriptException("Image file is empty.");
		    		}
		    		Iterator<ImageReader> iterator = ImageIO.getImageReaders(is);
		    		if (iterator == null || !iterator.hasNext()) {
		    			throw new WebScriptException("Image file format not supported by ImageIO");
		    		}
		    		
		    		TIFFImageReader imageReader = null;
		    		while(iterator.hasNext()){
		    			ImageReader ir = iterator.next();
		    			if(ir instanceof TIFFImageReader){
		    				imageReader = (TIFFImageReader) ir;
		    				break;
		    			}
		    		}
		    		
		    		iterator = null;
		    		imageReader.setInput(is);
		    		
		            int numPages = imageReader.getNumImages(true);
		            if(numPages == 0){
		    			throw new WebScriptException("Image file is empty.");
		    		}
		            
		    	    //get metadata
		            IIOMetadata metadata = imageReader.getImageMetadata(0);
		            TiffDescription tiffMetadata = getTiffMetadata(metadata);
		            
		            Integer imageXRes = tiffMetadata.getxRes();
		            if(imageXRes == null) imageXRes = 0;
		            Integer imageYRes = tiffMetadata.getyRes();
		            if(imageYRes == null) imageYRes = 0;
		            
		            Integer imageComp =  tiffMetadata.getCompression();
		            if(imageComp == null) imageComp = TIFFEncodeParam.COMPRESSION_LZW;
		            
	            	for(int j = 0; j < numPages; j++)
	    	        {
	            		images.add(imageReader.read(j));
	    	        }
	            	
		            //create file
		            createTiff2(file, images, imageComp, imageXRes, imageYRes);
		        
		            //create new version
		            NodeRef workingCopyNodeRef = serviceRegistry.getCheckOutCheckInService().checkout(childNodeRef);
					try{
						ContentWriter writer = contentService.getWriter(workingCopyNodeRef, ContentModel.PROP_CONTENT, true);
						writer.putContent(file);
		        
						Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
					    serviceRegistry.getCheckOutCheckInService().checkin(workingCopyNodeRef, versionProperties);
					}catch(Exception e){
						serviceRegistry.getCheckOutCheckInService().cancelCheckout(workingCopyNodeRef);
						throw e;
					}
					
					nodeService.setProperty(childNodeRef, TProcessModel.PROP_QNAME_PAGES, Integer.valueOf(images.size()));
					
					//delete node
				    nodeService.deleteNode(nodeRef);
		        }
			}else{
				//System.out.println("duplicate");
				while(nodeService.getChildByName(folder, ContentModel.ASSOC_CONTAINS, fName) != null){
					fName = name + "-" + (duplicatesFormat == null ? cnt : String.format(duplicatesFormat, cnt)) + ext;
					cnt++;
				}
				fileFolderService.move(nodeRef, folder, fName);
				childNodeRef = nodeRef;
			}
		}
		
		return childNodeRef;
	}
	
	public void createTiff2(File file, List<BufferedImage> images, Integer imageComp, Integer imageXRes, Integer imageYRes) throws Exception 
	{
	    TIFFEncodeParam params = new TIFFEncodeParam();
	    
	    // add a default resolution to the encode parameters
	    if(imageXRes != null && imageYRes != null){
	    	com.sun.media.jai.codec.TIFFField xRes = new com.sun.media.jai.codec.TIFFField(XRES_TAG, com.sun.media.jai.codec.TIFFField.TIFF_RATIONAL, 1, new long[][] { { imageXRes, 1 } });
	    	com.sun.media.jai.codec.TIFFField yRes = new com.sun.media.jai.codec.TIFFField(YRES_TAG, com.sun.media.jai.codec.TIFFField.TIFF_RATIONAL, 1, new long[][] { { imageYRes, 1 } });
			params.setExtraFields(new com.sun.media.jai.codec.TIFFField[] { xRes, yRes });
	    }
	    
	    if(!supportedComp.contains(imageComp)){
	    	imageComp = TIFFEncodeParam.COMPRESSION_DEFLATE;
	    }
	    
	    // add compression
	    params.setCompression(imageComp);
	    
	    // set images
	    List<BufferedImage> list = new ArrayList<BufferedImage>(images.size());
	    for (int i = 1; i < images.size(); i++) {
	        list.add(images.get(i));
	    }
	    params.setExtraImages(list.iterator());
	    
	    OutputStream out = new FileOutputStream(file);
	    ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", out, params);
	    encoder.encode(images.get(0));
	    out.close();
	}
	
	private Map<QName, Serializable> getCopyProperties(Map<QName, Serializable> properties){
		Map<QName, Serializable> cprop = new HashMap<QName, Serializable>();
	    for(QName qname : properties.keySet()){
	    	String name = qname.toPrefixString(namespaceService);
	    	String prefix = "";
        	int ind = name.indexOf(":");
        	if(ind > 0){
        		prefix = name.substring(0, ind);
        	}
        	if(copyList.contains(prefix) || copyList.contains(name))
        	{
        		cprop.put(qname, properties.get(qname));
        	}
	    }
	    return cprop;
	}
	
	private boolean isCompleted(Map<QName, Serializable> after, QName type, Map<QName, List<QName>> stRequired)
	{
		if(type.equals(ContentModel.TYPE_CONTENT)){
			logger.debug("not completed: content type");
			return false;
		}
		
		boolean allRequired = true;
		
		List<QName> requiredList = stRequired.get(type);
		if(requiredList == null)
		{
			TypeDefinition tdef = dictionaryService.getType(type);
			Map<QName, PropertyDefinition> props = tdef.getProperties();
			for(QName key : props.keySet()){
				if(key.getNamespaceURI().equals(type.getNamespaceURI()))
				{
					PropertyDefinition pdef = props.get(key);
					if(pdef.isMandatory()){
						Serializable val = after.containsKey(key) ? after.get(key) : null;
						if(val == null){
							logger.debug("not completed: not filled " + key);
							allRequired = false;
							break;
						}
					}
				}
			}
		}else{
			for(QName key : requiredList){
				Serializable val = after.containsKey(key) ? after.get(key) : null;
				if(val == null){
					logger.debug("not completed: not filled " + key);
					allRequired = false;
					break;
				}
			}
		}
		
		return allRequired;
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
	    			pathNodeRef = nodeService.getChildByName(pathNodeRef, ContentModel.ASSOC_CONTAINS, name);
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
			//try{
				folderNodeRef = fileFolderService.create(nodeRef, name, ContentModel.TYPE_FOLDER).getNodeRef();
			/*}catch(Exception e){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				folderNodeRef = nodeService.getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, name);
				if(folderNodeRef == null){
					throw new FileExistsException(nodeRef, name);
				}
			}*/
        }
		
		return folderNodeRef;
    }
	
	private ArrayList<String> parseTemplate(String template, Map<QName, Serializable> properties, Map<String,String> elems, String norm)
	{
		ArrayList<String> res = null;
		if(template != null && !template.isEmpty())
		{
			res = new ArrayList<String>();
			res.add(template);
			//res = template;
			boolean isVariable = false;
			StringBuilder variable = new StringBuilder();
			Set<String> vset = new TreeSet<String>();
			for(int i=0; i<template.length(); i++){
				char ch = template.charAt(i);
				if(isVariable){
					if(ch == '}'){
						isVariable = false;
						vset.add(variable.toString());
						variable = new StringBuilder();
					}else{
						variable.append(ch);
					}
				}else if(ch == '{'){
					isVariable = true;
				}
			}
			for(String v : vset){
				String function = "";
				String key = v;
				List<String> value = new ArrayList<String>();
				
				int ind = v.indexOf(".");
				if(ind > 0){
					function = v.substring(ind+1);
					key = v.substring(0,ind);
				}
				
				ind = key.indexOf("_");
				if(ind > 0){
					String prefix = key.substring(0, ind);
					String prop = key.substring(ind + 1);
					QName qname = QName.createQName(prefix, prop, namespaceService);
					
					if(qname != null){
						Serializable obj = properties.containsKey(qname) ? properties.get(qname) : null;
						if(obj != null){
							if(obj instanceof List){
								for(Object elem : (List) obj){
									value.add(elem.toString().trim());
								}
							}else{
								value.add(obj.toString().trim());
							}
						}
					}
				}else if(elems.containsKey(key)){
					value.add(elems.get(key));
				}
				
				if(!function.isEmpty()){
					
					ScriptEngineManager man = new ScriptEngineManager();
					ScriptEngine engine = man.getEngineByName("js");
					
					Object newString = value;
					SkyAreaString saString = new SkyAreaString(value, serviceRegistry, null, null);
			        engine.put("obj", saString);
			        SkyAreaProperties saProperties = new SkyAreaProperties(properties, serviceRegistry);
			        engine.put("properties", saProperties);
			        engine.put("current", elems.get("current"));
			        //for(QName key1 : properties.keySet()){
			        //	Serializable value1 = properties.get(key1);
			        //	engine.put(key1.toPrefixString(namespaceService).replace(':', '_'), value1 == null ? "" : value1.toString());
			        //}
			        try{
			        	newString = engine.eval("obj." + function + ".getValue()");
			        }catch(Exception e){
			        	e.printStackTrace();
			        }
			        if(newString != null){
			        	value = (List<String>) newString;
			        }
				}
				
				ArrayList<String> newSet = new ArrayList<String>();
				for(String vres : res){
					for(String elem : value){
						newSet.add(vres.replace("{" + v + "}", norm != null ? normalizeName(elem, norm) : elem));
					}
				}
				res = newSet;
			}
		}

		return res;
	}
	
	public static String normalizeName(String value, String norm) {
		return value.replaceAll(norm, "");
	}
	
	protected String getMimetype(ContentAccessor content)
    {
        String mimetype = content.getMimetype();
        if (mimetype == null)
        {
            throw new AlfrescoRuntimeException("Mimetype is mandatory for transformation: " + content);
        }
        // done
        return mimetype;
    }
	
	private static TiffDescription getTiffMetadata(IIOMetadata metadata){
		
		TiffDescription res = new TiffDescription();
    	if (metadata != null) {
            IIOMetadataNode dimNode = (IIOMetadataNode) metadata.getAsTree("javax_imageio_1.0");
            NodeList nodes = dimNode.getElementsByTagName("HorizontalPixelSize");
            if (nodes.getLength() > 0) {
                float dpcWidth = Float.parseFloat(nodes.item(0).getAttributes().item(0).getNodeValue());
                res.setxRes((int) Math.round(25.4f / dpcWidth));
            }

            nodes = dimNode.getElementsByTagName("VerticalPixelSize");
            if (nodes.getLength() > 0) {
                float dpcHeight = Float.parseFloat(nodes.item(0).getAttributes().item(0).getNodeValue());
                res.setyRes((int) Math.round(25.4f / dpcHeight));
            }
            
            nodes = dimNode.getElementsByTagName("CompressionTypeName");
            if (nodes.getLength() > 0) {
                String compression = nodes.item(0).getAttributes().item(0).getNodeValue();
                //System.out.println("CompressionTypeName: " + compression);
                
                int imageComp = TIFFEncodeParam.COMPRESSION_NONE;
                if(compression.equals("CCITT RLE")) imageComp = TIFFEncodeParam.COMPRESSION_GROUP3_1D;
                if(compression.equals("CCITT T.4")) imageComp = TIFFEncodeParam.COMPRESSION_GROUP3_2D;
                if(compression.equals("CCITT T.6")) imageComp = TIFFEncodeParam.COMPRESSION_GROUP4;
                if(compression.equals("LZW")) imageComp = TIFFEncodeParam.COMPRESSION_LZW;
                if(compression.equals("Old JPEG")) imageComp = TIFFEncodeParam.COMPRESSION_JPEG_TTN2;
                if(compression.equals("JPEG")) imageComp = TIFFEncodeParam.COMPRESSION_JPEG_TTN2;
                if(compression.equals("ZLib")) imageComp = TIFFEncodeParam.COMPRESSION_DEFLATE;
                if(compression.equals("PackBits")) imageComp = TIFFEncodeParam.COMPRESSION_PACKBITS;
                if(compression.equals("Deflate")) imageComp = TIFFEncodeParam.COMPRESSION_DEFLATE;
                if(compression.equals("EXIF JPEG")) imageComp = TIFFEncodeParam.COMPRESSION_JPEG_TTN2;
                res.setCompression(imageComp);
            }
            
            /*nodes = dimNode.getElementsByTagName("ColorSpaceType");
            if (nodes.getLength() > 0) {
                String compression = nodes.item(0).getAttributes().item(0).getNodeValue();
                System.out.println(compression);
            }*/
        }
    	return res;
	}
	
	private void findPdf(Map<QName,Serializable> properties, QName appField, List<QName> copyFields, String pathTemplate, NodeRef destRef){
		String appNo = (String) properties.get(appField);
		
		Map<String,String> elems = new HashMap<String,String>();
		
		List<String> pathList = parseTemplate(pathTemplate, properties, elems, NORMALIZE_FOLDER);
		String path = pathList != null && pathList.size() > 0 ? pathList.get(0) : null;
		
		if(path != null){
			NodeRef folder = getFolderByPath(destRef, path);
			if(folder != null){
				NodeRef pdfNodeRef = nodeService.getChildByName(folder, ContentModel.ASSOC_CONTAINS, appNo + ".pdf");
				if(pdfNodeRef != null){
					for(QName qname : copyFields){
						if(!properties.containsKey(qname)){
							properties.put(qname, nodeService.getProperty(pdfNodeRef, qname));
						}
					}
				}
			}
		}
	}
    
    @Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
	{

	}
}

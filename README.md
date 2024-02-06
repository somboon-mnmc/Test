ExtDirectoryManager directoryManager = (ExtDirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");
PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
WorkflowManager wm = (WorkflowManager) pluginManager.getBean("workflowManager");
AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");  


FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
FormRowSet rows2 = formDataDao.find("role_list_set", "itreq_role_inline", null, null, null, null, null, null);
Collection assignees = new ArrayList();
List userList = new ArrayList();
String compareValues = "#form.itreq_main.req_tem_type#";
String processId = "#process.processId#";
String currentUser = "#currentUser.username#";
        
        
        System.out.println("------------------------------------------------------------------"); 
        
        if (rows2.size() > 0) {
        	    int r = 0;
            	for (int i = 0; i < rows2.size(); i++) {
            	    r = i;
            	    
                boolean vali = false;
        		
            	FormRowSet newRows = new FormRowSet();
            	FormRow newRow = new FormRow();
        
        		FormRow row2 = rows2.get(i);
        
                System.out.println("Result Loop: " + row2);
        
        		if (compareValues.equals(row2.get("req_type"))) {
        		    
        			String role_req = row2.get("role_req");
        			String role_dep = row2.get("role_dept_req");
        			String sec_role_dep = row2.get("sec_role_department");
        			String checkBox = row2.get("check_related");
        			String reqType = row2.get("req_type");
        			
        			
        			
        			System.out.println("This is role dep: "+ role_dep);
        		 
        			if(role_req != null && !role_req.isEmpty()){
        			    String usersReq = "";
            			  
            			 //   new
            			    Collection emp = directoryManager.getEmployments( (String)null,  (String)null , role_dep,  role_req,  (String)null,  (String)null,  (String)null,  (String)null);
                            for(Object e : emp){
                                Employment usersReq = (Employment) e;
                                if(usersReq.getUserId().equals(currentUser)){
                                        role = usersReq.getGrade().getId();
                                        
                                        vali = true;
                                }
                                
                                
                            }
        		}
        		
        		if(vali){
        		   //  Requester
        		    String role_req = row2.get("role_req");
        		   
            		
            		if(role_req != null && !role_req.isEmpty()){
            		    
            		  String usersReq = "";
            		    Collection emp = directoryManager.getEmployments( (String)null,  (String)null , role_dep,  role_req,  (String)null,  (String)null,  (String)null,  (String)null);
            			for(Object e : emp){
            			    Employment userEmployment = (Employment) e;
            				if(!usersReq.isEmpty()) usersReq += ";";
            				usersReq += userEmployment.getUserId();
            			}
            			wm.activityVariable(workflowAssignment.getActivityId(), "reqter", usersReq);
            		}
            		
            		// Approver 1
            		String role_appr_1 = row2.get("role_appr_1");
            		
            		
            		if(role_appr_1 != null && !role_appr_1.isEmpty()){
            			
            			newRow.put("assignee_appr1", role_appr_1);
            			newRows.add(newRow);
            			FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "user_request", newRows, processId);
            			
            			String usersAppr1 = "";
            			Collection emp = directoryManager.getEmployments( (String)null,  (String)null , role_dep,  role_appr_1,  (String)null,  (String)null,  (String)null,  (String)null);
            			for(Object e : emp){
            			    Employment userEmployment = (Employment) e;
            				if(!usersAppr1.isEmpty()) usersAppr1 += ";";
            				usersAppr1 += userEmployment.getUserId();
            			}
            			
            			wm.activityVariable(workflowAssignment.getActivityId(), "appr1", usersAppr1);
            			
            		}
            		
            // 		Approver 2
            		String role_appr_2 = row2.get("role_appr_2");
            		
            
            		if(role_appr_2 != null && !role_appr_2.isEmpty()){
            	    	
            	    	newRow.put("assignee_appr2", role_appr_2);
            			newRows.add(newRow);
            			FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "umg_approval", newRows, processId);
            		    
            		    String usersAppr2 = "";
            			Collection emp = directoryManager.getEmployments( (String)null,  (String)null , role_dep,  role_appr_2,  (String)null,  (String)null,  (String)null,  (String)null);
            			for(Object e : emp){
            			    Employment userEmployment = (Employment) e;
            				if(!usersAppr2.isEmpty()) usersAppr2 += ";";
            				usersAppr2 += userEmployment.getUserId();
        
            			}
            			wm.activityVariable(workflowAssignment.getActivityId(), "appr2", usersAppr2);
            		}
            		
            		// 		Approver 3
            		String role_appr_3 = row2.get("role_appr_3");
            		
            		if(role_appr_3 != null && !role_appr_3.isEmpty()){
            		    
            	    	newRow.put("assignee_appr3", role_appr_3);
            			newRows.add(newRow);
            			FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "div_umg_approval", newRows, processId);
            		    
            			String usersAppr3 = "";
            			Collection emp = directoryManager.getEmployments( (String)null,  (String)null , sec_role_dep,  role_appr_3,  (String)null,  (String)null,  (String)null,  (String)null);
            			for(Object e : emp){
            			    Employment userEmployment = (Employment) e;
            				if(!usersAppr3.isEmpty()) usersAppr3 += ";";
            				usersAppr3 += userEmployment.getUserId();
            			}
            			wm.activityVariable(workflowAssignment.getActivityId(), "appr3", usersAppr3);
            		}
            		
            		// 		Approver 4
            		String role_appr_4 = row2.get("role_appr_4");
            		
            		if(role_appr_4 != null && !role_appr_4.isEmpty()){
            		    
            	    	newRow.put("assignee_appr4", role_appr_4);
            			newRows.add(newRow);
            			FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "itmgr_approval", newRows, processId);
            		    
            			String usersAppr4 = "";
            			Collection emp = directoryManager.getEmployments( (String)null,  (String)null , sec_role_dep,  role_appr_4,  (String)null,  (String)null,  (String)null,  (String)null);
            			for(Object e : emp){
            			    Employment userEmployment = (Employment) e;
            				if(!usersAppr4.isEmpty()) usersAppr4 += ";";
            				usersAppr4 += userEmployment.getUserId();
            			}
            			wm.activityVariable(workflowAssignment.getActivityId(), "appr4", usersAppr4);
            		}
            		
            		// 		Approver 5
            		String role_appr_5 = row2.get("role_appr_5");
            		
            		if(role_appr_5 != null && !role_appr_5.isEmpty()){
            		    
            	    	newRow.put("assignee_appr5", role_appr_5);
            			newRows.add(newRow);
            			FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "it_work_request", newRows, processId);
            		  
            			String usersAppr5 = "";
            			Collection emp = directoryManager.getEmployments( (String)null,  (String)null , role_dep,  role_appr_5,  (String)null,  (String)null,  (String)null,  (String)null);
            			for(Object e : emp){
            			    Employment userEmployment = (Employment) e;
            				if(!usersAppr5.isEmpty()) usersAppr5 += ";";
            				usersAppr5 += userEmployment.getUserId();
            				
            			}
            			wm.activityVariable(workflowAssignment.getActivityId(), "appr5", usersAppr5);
            		}
            		
        		    
        		}
        	}
        	
            	
            }
            
            
            while (r < row2.size()){
                FormRow row2 = rows2.get(r);
                
                if(row2.get().equals("true"))
                break;
                r++;
            	    //add role to participatnt
            	    
                }
            
            
        }

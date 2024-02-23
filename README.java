import java.util.*;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joget.workflow.model.service.*;
import org.joget.directory.model.User;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.workflow.model.WorkflowProcessResult;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.app.service.AppService;
import org.joget.directory.model.Employment;
import org.joget.apps.form.model.*;




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

            System.out.println("==================================");

            if (rows2.size() > 0) {
        
            for (int i = 0; i < rows2.size(); i++) {
        
                FormRow row = rows2.get(i);
                FormRowSet newRows = new FormRowSet();
                FormRow newRow = new FormRow();
        
                boolean checkRelated = "true".equals(row.get("check_related"));
        
                String role_req = row.get("role_req");
                String role_dep = row.get("role_dept_req");
                
        
                if (!checkRelated) {
                    //main row
                    boolean vali = false;
        
                    String type = (String) row.get("req_type");
                    if (type == null || type.isEmpty() || type.equals(compareValues)) {
                        vali = true;
                    }
                    if (vali) {
        
                        if (role_req != null && !role_req.isEmpty()) {
        
                            String usersReq = "";
        
                            //   new
                            Collection emp = directoryManager.getEmployments((String) null, (String) null, role_dep, role_req, (String) null, (String) null, (String) null, (String) null);
                            System.out.println("==================================");
                            for (Object e: emp) {
                                System.out.println("==================================");
                                Employment usersReq = (Employment) e;
                                if (usersReq.getUserId().equals(currentUser)) {
                                    role = usersReq.getGrade().getId();
                                    vali = true;
                                }   
                            }
        
                        }
        
                    }
                    
                    
                    if (vali) {
                        //    we found main row
                        String dept_appr1 = row.get("role_dept_appr1");
                        String dept_appr2 = row.get("role_dept_appr2");
                        String dept_appr3 = row.get("role_dept_appr3");
                        String dept_appr4 = row.get("role_dept_appr4");
                        String dept_appr5 = row.get("role_dept_appr5");
                     
        
                        Set req = new TreeSet();
                        String role_req = row.get("role_req");
                        if (role_req != null && !role_req.isEmpty()) {
        
                            String usersReq = "";
                            Collection emp = directoryManager.getEmployments((String) null, (String) null, role_dep, role_req, (String) null, (String) null, (String) null, (String) null);
                            for (Object e: emp) {
                                Employment userEmployment = (Employment) e;
                                if (!usersReq.isEmpty()) usersReq += ";";
                                usersReq += userEmployment.getUserId();
                            }
                            req.add(usersReq);
                            String userRequester = String.join(";", req);
                            
                            wm.activityVariable(workflowAssignment.getActivityId(), "reqter", userRequester);
                           
                        }
        
                        // 		Approver 1
                        Set app1 = new TreeSet();
                        String role_appr_1 = row.get("role_appr_1");
                        
        
                        if (role_appr_1 != null && !role_appr_1.isEmpty()) {
                            
                            newRow.put("assignee_appr1", role_appr_1);
                            newRows.add(newRow);
                            FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "user_request", newRows, processId);
        
                            String usersAppr1 = "";
                            Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr1, role_appr_1, (String) null, (String) null, (String) null, (String) null);
                            for (Object e: emp) {
                                Employment userEmployment = (Employment) e;
                                if (!usersAppr1.isEmpty()) usersAppr1 += ";";
                                usersAppr1 += userEmployment.getUserId();
                            }
                            app1.add(usersAppr1);
                            String userApprover1 = String.join(";", app1);
               
                            wm.activityVariable(workflowAssignment.getActivityId(), "appr1", userApprover1);
                            
        
                        }
        
        
                        // 		Approver 2
                        Set app2 = new TreeSet();
                        String role_appr_2 = row.get("role_appr_2");
                        
        
                        if (role_appr_2 != null && !role_appr_2.isEmpty()) {
        
                            newRow.put("assignee_appr2", role_appr_2);
                            newRows.add(newRow);
                            FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "umg_approval", newRows, processId);
        
                            String usersAppr2 = "";
                            Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr2, role_appr_2, (String) null, (String) null, (String) null, (String) null);
                            for (Object e: emp) {
                                Employment userEmployment = (Employment) e;
                                if (!usersAppr2.isEmpty()) usersAppr2 += ";";
                                usersAppr2 += userEmployment.getUserId();
        
                            }
                            app2.add(usersAppr2);
                            String userApprover2 = String.join(";", app2);
                            
                            wm.activityVariable(workflowAssignment.getActivityId(), "appr2", userApprover2);
                            
                        }
        
        
                        // 		Approver 3
                        Set app3 = new TreeSet();
                        String role_appr_3 = row.get("role_appr_3");
                        
        
                        if (role_appr_3 != null && !role_appr_3.isEmpty()) {
        
                            newRow.put("assignee_appr3", role_appr_3);
                            newRows.add(newRow);
                            FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "div_umg_approval", newRows, processId);
        
                            String usersAppr3 = "";
                            Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr3, role_appr_3, (String) null, (String) null, (String) null, (String) null);
                            for (Object e: emp) {
                                Employment userEmployment = (Employment) e;
                                if (!usersAppr3.isEmpty()) usersAppr3 += ";";
                                usersAppr3 += userEmployment.getUserId();
                            }
                            app3.add(usersAppr3);
                            String userApprover3 = String.join(";", app3);
                            
                            wm.activityVariable(workflowAssignment.getActivityId(), "appr3", userApprover3);
                            
                        }
        
        
                        // 		Approver 4
                        Set app4 = new TreeSet();
                        String role_appr_4 = row.get("role_appr_4");
                        
        
                        if (role_appr_4 != null && !role_appr_4.isEmpty()) {
        
                            newRow.put("assignee_appr4", role_appr_4);
                            newRows.add(newRow);
                            FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "itmgr_approval", newRows, processId);
        
                            String usersAppr4 = "";
                            Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr4, role_appr_4, (String) null, (String) null, (String) null, (String) null);
                            for (Object e: emp) {
                                Employment userEmployment = (Employment) e;
                                if (!usersAppr4.isEmpty()) usersAppr4 += ";";
                                usersAppr4 += userEmployment.getUserId();
                            }
                            app4.add(usersAppr4);
                            String userApprover4 = String.join(";", app4);
                            
                            wm.activityVariable(workflowAssignment.getActivityId(), "appr4", userApprover4);
                            
                        }
        
        
                        // 		Approver 5
                        Set app5 = new TreeSet();
                        String role_appr_5 = row.get("role_appr_5");
                        
        
                        if (role_appr_5 != null && !role_appr_5.isEmpty()) {
        
                            newRow.put("assignee_appr5", role_appr_5);
                            newRows.add(newRow);
                            FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "it_work_request", newRows, processId);
        
                            String usersAppr5 = "";
                            Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr5, role_appr_5, (String) null, (String) null, (String) null, (String) null);
                            for (Object e: emp) {
                                Employment userEmployment = (Employment) e;
                                if (!usersAppr5.isEmpty()) usersAppr5 += ";";
                                usersAppr5 += userEmployment.getUserId();
        
                            }
                            app5.add(usersAppr5);
                            String userApprover5 = String.join(";", app5);
                            
                            
                            wm.activityVariable(workflowAssignment.getActivityId(), "appr5", userApprover5);
                            
                        }
                        
                        
        
        
        
                            //find related rows
                        for (int j = i + 1; j < rows2.size(); j++) {
                            FormRow row2 = rows2.get(j);
       
                            boolean checkRelated2 = "true".equals(row2.get("check_related"));
                         
       
        
                            if (checkRelated2) {
                                //found related row
                                
                              
                               
                                
        
                                    // 		Approver 1
                                    
                                    String role_appr_1 = row2.get("role_appr_1");
            
                                    if (role_appr_1 != null && !role_appr_1.isEmpty()) {
            
                                        newRow.put("assignee_appr1", role_appr_1);
                                        newRows.add(newRow);
                                        FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "user_request", newRows, processId);
            
                                        String usersAppr1 = "";
                                        Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr1, role_appr_1, (String) null, (String) null, (String) null, (String) null);
                                        
                                        for (Object e: emp) {
                                            Employment userEmployment = (Employment) e;
                                            
                                            if (!usersAppr1.isEmpty()) usersAppr1 += ";";
                                            usersAppr1 += userEmployment.getUserId();
                                        }
                                        app1.add(usersAppr1); // output is "approver 1 inside App1: [admin;user1, admin;admin;user1;admin;admin]"
                                        String userApprover1 = String.join(";", app1);
                                        wm.activityVariable(workflowAssignment.getActivityId(), "appr1", userApprover1);
                                        
            
                                    }
        
        
                                    //        		Approver 2
                                    
                                    String role_appr_2 = row2.get("role_appr_2");
                    
                                    if (role_appr_2 != null && !role_appr_2.isEmpty()) {
                    
                                        newRow.put("assignee_appr2", role_appr_2);
                                        newRows.add(newRow);
                                        FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "umg_approval", newRows, processId);
                    
                                        String usersAppr2 = "";
                                        Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr2, role_appr_2, (String) null, (String) null, (String) null, (String) null);
                                        for (Object e: emp) {
                                            Employment userEmployment = (Employment) e;
                                            if (!usersAppr2.isEmpty()) usersAppr2 += ";";
                                            usersAppr2 += userEmployment.getUserId();
                    
                                        }
                                        app2.add(usersAppr2);
                                        String userApprover2 = String.join(";", app2);
                                        
                                        wm.activityVariable(workflowAssignment.getActivityId(), "appr2", userApprover2);
                                        
                                    }
                    
                    
                                    // 		Approver 3
                                    
                                    String role_appr_3 = row2.get("role_appr_3");
                    
                                    if (role_appr_3 != null && !role_appr_3.isEmpty()) {
                    
                                        newRow.put("assignee_appr3", role_appr_3);
                                        newRows.add(newRow);
                                        FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "div_umg_approval", newRows, processId);
                    
                                        String usersAppr3 = "";
                                        Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr3, role_appr_3, (String) null, (String) null, (String) null, (String) null);
                                        for (Object e: emp) {
                                            Employment userEmployment = (Employment) e;
                                            if (!usersAppr3.isEmpty()) usersAppr3 += ";";
                                            usersAppr3 += userEmployment.getUserId();
                                        }
                                        app3.add(usersAppr3);
                                        String userApprover3 = String.join(";", app3);
                                        
                                        wm.activityVariable(workflowAssignment.getActivityId(), "appr3", userApprover3);
                                        
                                    }
                    
                    
                                    // 		Approver 4
                                    
                                    String role_appr_4 = row2.get("role_appr_4");
                    
                                    if (role_appr_4 != null && !role_appr_4.isEmpty()) {
                    
                                        newRow.put("assignee_appr4", role_appr_4);
                                        newRows.add(newRow);
                                        FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "itmgr_approval", newRows, processId);
                    
                                        String usersAppr4 = "";
                                        Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr4, role_appr_4, (String) null, (String) null, (String) null, (String) null);
                                        for (Object e: emp) {
                                            Employment userEmployment = (Employment) e;
                                            if (!usersAppr4.isEmpty()) usersAppr4 += ";";
                                            usersAppr4 += userEmployment.getUserId();
                                        }
                                        app4.add(usersAppr4);
                                        String userApprover4 = String.join(";", app4);
                                        
                                        wm.activityVariable(workflowAssignment.getActivityId(), "appr4", userApprover4);
                                        
                                    }
                    
                    
                                    // 		Approver 5
                                   
                                    String role_appr_5 = row2.get("role_appr_5");
                    
                                    if (role_appr_5 != null && !role_appr_5.isEmpty()) {
                    
                                        newRow.put("assignee_appr5", role_appr_5);
                                        newRows.add(newRow);
                                        FormRowSet rowSet = appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), "it_work_request", newRows, processId);
                    
                                        String usersAppr5 = "";
                                        Collection emp = directoryManager.getEmployments((String) null, (String) null, dept_appr5, role_appr_5, (String) null, (String) null, (String) null, (String) null);
                                        for (Object e: emp) {
                                            Employment userEmployment = (Employment) e;
                                            if (!usersAppr5.isEmpty()) usersAppr5 += ";";
                                            usersAppr5 += userEmployment.getUserId();
                    
                                        }
                                        app5.add(usersAppr5);
                                        String userApprover5 = String.join(";", app5);
                                        
                                        
                                        wm.activityVariable(workflowAssignment.getActivityId(), "appr5", userApprover5);
                                        
                                    }
        
                            } else {
                                    break;
                                }
                            
                        }
                        break;
                    }
        
        
        
                } //!checked
        
            } //for
        
                        
        
        } //first
                
                


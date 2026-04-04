package com.sap.sflit.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.sflit.dto.EmployeeDTO;
import com.sap.sflit.multitenancy.TenantContext;
import com.sap.sflit.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MCP (Model Context Protocol) Server Endpoint
 *
 * INTERVIEW TALKING POINTS:
 * 1. WHAT IS THIS? This is a JSON-RPC 2.0 endpoint that implements the Model Context Protocol.
 *    It allows AI Agents (like Claude or Custom LLMs) to discover and execute tools securely.
 *
 * 2. MULTI-TENANCY IN MCP: Because this is inside the Spring Boot app, it passes through 
 *    the SAME TenantInterceptor. The LLM cannot access data outside the tenant passed in the headers.
 *    This is called "Tenant-Scoped LLM Context".
 *
 * 3. NO DEPENDENCY BLOAT: By implementing the JSON-RPC spec directly, we avoid heavy,
 *    experimental beta libraries. It's fast, observable, and strictly typed.
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {

    private final EmployeeService employeeService;
    private final ObjectMapper mapper;

    @PostMapping("/rpc")
    public ResponseEntity<JsonNode> handleMcpRequest(@RequestBody JsonNode request) {
        String method = request.path("method").asText();
        String id = request.path("id").asText();

        log.info("MCP Request received. Method: {}, Tenant: {}", method, TenantContext.getCurrentTenant());

        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", id);

        try {
            switch (method) {
                case "tools/list" -> response.set("result", handleToolsList());
                case "tools/call" -> {
                    JsonNode params = request.path("params");
                    response.set("result", handleToolsCall(params));
                }
                default -> {
                    response.set("error", createError(-32601, "Method not found: " + method));
                }
            }
        } catch (Exception e) {
            log.error("MCP Execution Error", e);
            response.set("error", createError(-32000, e.getMessage()));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * tools/list
     * The AI Agent asks: "What tools do you have available?"
     */
    private JsonNode handleToolsList() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode tools = mapper.createArrayNode();

        // Register: get_employee_summary
        ObjectNode getEmployeeTool = mapper.createObjectNode();
        getEmployeeTool.put("name", "get_employee_summary");
        getEmployeeTool.put("description", "Returns a text summary of an employee's skills and performance rating by ID.");
        
        ObjectNode inputSchema = mapper.createObjectNode();
        inputSchema.put("type", "object");
        ObjectNode properties = mapper.createObjectNode();
        
        ObjectNode idProp = mapper.createObjectNode();
        idProp.put("type", "number");
        idProp.put("description", "The database ID of the employee");
        properties.set("employee_id", idProp);
        
        inputSchema.set("properties", properties);
        
        ArrayNode required = mapper.createArrayNode();
        required.add("employee_id");
        inputSchema.set("required", required);

        getEmployeeTool.set("inputSchema", inputSchema);
        tools.add(getEmployeeTool);

        // Register: search_employees_by_skill
        ObjectNode searchTool = mapper.createObjectNode();
        searchTool.put("name", "search_employees_by_skill");
        searchTool.put("description", "Search for employees who possess a specific skillset.");
        
        ObjectNode searchSchema = mapper.createObjectNode();
        searchSchema.put("type", "object");
        ObjectNode searchProps = mapper.createObjectNode();
        
        ObjectNode skillProp = mapper.createObjectNode();
        skillProp.put("type", "string");
        skillProp.put("description", "The exact skill name (e.g., Java, Kubernetes, AWS, SQL)");
        searchProps.set("skill", skillProp);
        
        searchSchema.set("properties", searchProps);
        ArrayNode searchReq = mapper.createArrayNode();
        searchReq.add("skill");
        searchSchema.set("required", searchReq);

        searchTool.set("inputSchema", searchSchema);
        tools.add(searchTool);

        result.set("tools", tools);
        return result;
    }

    /**
     * tools/call
     * The AI Agent says: "Execute tool X with parameters Y"
     */
    private JsonNode handleToolsCall(JsonNode params) {
        String toolName = params.path("name").asText();
        JsonNode args = params.path("arguments");

        ObjectNode result = mapper.createObjectNode();
        ArrayNode content = mapper.createArrayNode();
        ObjectNode contentItem = mapper.createObjectNode();
        contentItem.put("type", "text");

        if ("get_employee_summary".equals(toolName)) {
            Long employeeId = args.path("employee_id").asLong();
            var empOpt = employeeService.getEmployeeById(employeeId);
            
            if (empOpt.isPresent()) {
                EmployeeDTO emp = empOpt.get();
                String summary = String.format("%s works in %s as a %s. They have a performance score of %s. Known skills: %s.",
                        emp.name(), emp.department(), emp.title(), emp.performanceScore(), String.join(", ", emp.skills()));
                contentItem.put("text", summary);
            } else {
                contentItem.put("text", "Employee not found or you do not have permission to view them.");
            }
        } 
        else if ("search_employees_by_skill".equals(toolName)) {
            String skill = args.path("skill").asText();
            List<EmployeeDTO> matching = employeeService.filterEmployees(null, List.of(skill), null);
            
            if (matching.isEmpty()) {
                contentItem.put("text", "No employees found with skill: " + skill);
            } else {
                StringBuilder sb = new StringBuilder("Found matching employees:\n");
                for (EmployeeDTO e : matching) {
                    sb.append("- ").append(e.name()).append(" (ID: ").append(e.id()).append(")\n");
                }
                contentItem.put("text", sb.toString());
            }
        }
        else {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        content.add(contentItem);
        result.set("content", content);
        return result;
    }

    private JsonNode createError(int code, String message) {
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        return error;
    }
}

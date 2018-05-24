package com.deepoove.swagger.diff.output;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.*;
import io.swagger.models.HttpMethod;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MarkdownRender implements Render {

    static final String TEXT_PARAMETER_PEFIX = "Parameter ";
    static final String H3 = "### ";
    static final String H2 = "## ";
    static final String BLOCKQUOTE = "> ";
    static final String CODE = "`";
    static final String PRE_CODE = "    ";
    static final String PRE_LI = "";
    static final String LI = "* ";
    static final String HR = "---\n";
    static final String STRIKETHROUGH = "~~";
    static final String NESTED_LI = "  * ";

    public MarkdownRender() {
    }

    public String render(SwaggerDiff diff) {
        List<Endpoint> newEndpoints = diff.getNewEndpoints();
        String ol_newEndpoint = ol_newEndpoint(newEndpoints);

        List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
        String ol_missingEndpoint = ol_missingEndpoint(missingEndpoints);

        List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
        String ol_changed = ol_changed(changedEndpoints);

        return renderMarkdown(diff.getOldVersion(), diff.getNewVersion(), ol_newEndpoint, ol_missingEndpoint, ol_changed);
    }

    public String renderMarkdown(String oldVersion, String newVersion, String ol_new, String ol_miss,
                                 String ol_changed) {
        // see https://keepachangelog.com/en/1.0.0/
        StringBuffer sb = new StringBuffer();
        sb.append(H2).append("Version " + oldVersion + " to " + newVersion).append("\n").append(HR);
        sb.append(H3).append(":sparkles: Added").append("\n").append(HR)
            .append(ol_new).append("\n").append(H3)
            .append(":recycle: Removed").append("\n").append(HR)
            .append(ol_miss).append("\n").append(H3)
            .append(":wrench: Changed").append("\n").append(HR)
            .append(ol_changed);
        return sb.toString();
    }

    private String ol_newEndpoint(List<Endpoint> endpoints) {
        if (null == endpoints) return "";
        StringBuffer sb = new StringBuffer();
        for (Endpoint endpoint : endpoints) {
            sb.append(li_newEndpoint(endpoint.getMethod().toString(),
                endpoint.getPathUrl(), endpoint.getSummary()));
        }
        return sb.toString();
    }

    private String li_newEndpoint(String method, String path, String desc) {
        StringBuffer sb = new StringBuffer();
        sb.append(LI).append(CODE).append(method).append(CODE)
            .append(" " + path).append(" - " + desc + "\n");
        return sb.toString();
    }

    private String ol_missingEndpoint(List<Endpoint> endpoints) {
        if (null == endpoints) return "";
        StringBuffer sb = new StringBuffer();
        for (Endpoint endpoint : endpoints) {
            sb.append(li_newEndpoint(
                endpoint.getMethod().toString(),
                STRIKETHROUGH + endpoint.getPathUrl() + STRIKETHROUGH,
                endpoint.getSummary()));
        }
        return sb.toString();
    }

    private String ol_changed(List<ChangedEndpoint> changedEndpoints) {
        if (null == changedEndpoints) return "";
        StringBuffer sb = new StringBuffer();
        for (ChangedEndpoint changedEndpoint : changedEndpoints) {
            String pathUrl = changedEndpoint.getPathUrl();
            Map<HttpMethod, ChangedOperation> changedOperations = changedEndpoint
                .getChangedOperations();
            for (Entry<HttpMethod, ChangedOperation> entry : changedOperations
                .entrySet()) {
                String method = entry.getKey().toString();
                ChangedOperation changedOperation = entry.getValue();
                String desc = changedOperation.getSummary();

                StringBuffer ul_detail = new StringBuffer();
                if (changedOperation.isDiffParam()) {
                    ul_detail
                        .append(ul_param(changedOperation));
                }
                if (changedOperation.isDiffProp()) {
                    ul_detail
                        .append(ul_response(changedOperation));
                }
                sb.append(LI).append(CODE).append(method).append(CODE)
                    .append(" " + pathUrl).append(" " + desc + "  \n")
                    .append(ul_detail);
            }
        }
        return sb.toString();
    }

    private String ul_response(ChangedOperation changedOperation) {
        List<ElProperty> addProps = changedOperation.getAddProps();
        List<ElProperty> delProps = changedOperation.getMissingProps();
        StringBuffer sb = new StringBuffer();
        for (ElProperty prop : addProps) {
            sb
                .append(NESTED_LI)
                .append(li_addProp(prop) + "\n");
        }
        for (ElProperty prop : delProps) {
            sb
                .append(NESTED_LI)
                .append(li_missingProp(prop) + "\n");
        }
        return sb.toString();
    }

    private String li_missingProp(ElProperty prop) {
        Property property = prop.getProperty();
        StringBuffer sb = new StringBuffer("");

        sb.append(":x: ")
            .append("Property ")
            .append("`" + prop.getEl() + "`")
            .append(null == property.getDescription() ? ""
                : (" - " + property.getDescription()));
        return sb.toString();
    }

    private String li_addProp(ElProperty prop) {
        Property property = prop.getProperty();
        StringBuffer sb = new StringBuffer("");
        sb.append(":heavy_plus_sign: ")
            .append("Property ")
            .append("`" + prop.getEl() + "`")
            .append(null == property.getDescription() ? ""
                : (" - " + property.getDescription()));
        return sb.toString();
    }

    private String ul_param(ChangedOperation changedOperation) {
        List<Parameter> addParameters = changedOperation.getAddParameters();
        List<Parameter> delParameters = changedOperation.getMissingParameters();
        List<ChangedParameter> changedParameters = changedOperation
            .getChangedParameter();
        StringBuffer sb = new StringBuffer("");
        for (Parameter param : addParameters) {
            sb.append(NESTED_LI).append(li_addParam(param) + "\n");
        }
        for (ChangedParameter param : changedParameters) {
            List<ElProperty> increased = param.getIncreased();
            for (ElProperty prop : increased) {
                sb.append(NESTED_LI).append(li_addProp(prop) + "\n");
            }
        }
        for (ChangedParameter param : changedParameters) {
            boolean changeRequired = param.isChangeRequired();
            boolean changeDescription = param.isChangeDescription();
            if (changeRequired || changeDescription)
                sb.append(NESTED_LI).append(li_changedParam(param) + "\n");
        }
        for (ChangedParameter param : changedParameters) {
            List<ElProperty> missing = param.getMissing();
            for (ElProperty prop : missing) {
                sb.append(NESTED_LI)
                    .append(li_missingProp(prop) + "\n");
            }
        }
        for (Parameter param : delParameters) {
            sb.append(NESTED_LI)
                .append(li_missingParam(param) + "\n");
        }
        return sb.toString();
    }

    private String li_addParam(Parameter param) {
        StringBuffer sb = new StringBuffer();
        sb.append(":heavy_plus_sign: " + TEXT_PARAMETER_PEFIX);
        sb.append("`" + param.getName() + "`")
            .append(null == param.getDescription() ? ""
                : (" - " + param.getDescription()));
        return sb.toString();
    }

    private String li_missingParam(Parameter param) {
        StringBuffer sb = new StringBuffer();
        sb
            .append((":x: " + TEXT_PARAMETER_PEFIX))
            .append(STRIKETHROUGH)
            .append("`" + param.getName() + "`")
            .append(STRIKETHROUGH)
            .append(null == param.getDescription() ? ""
                : (" - " + param.getDescription()));
        return sb.toString();
    }

    private String li_changedParam(ChangedParameter changeParam) {
        boolean changeRequired = changeParam.isChangeRequired();
        boolean changeDescription = changeParam.isChangeDescription();
        Parameter rightParam = changeParam.getRightParameter();
        Parameter leftParam = changeParam.getLeftParameter();
        StringBuffer sb = new StringBuffer();
        sb.append(TEXT_PARAMETER_PEFIX);
        sb.append("`" + rightParam.getName() + "`");
        if (changeRequired) {
            sb.append(rightParam.getRequired() ? "required" : "not required");
        }
        if (changeDescription) {
            sb.append(" :memo: ").append(leftParam.getDescription()).append(" -> ")
                .append(rightParam.getDescription());
        }
        return sb.toString();
    }

}

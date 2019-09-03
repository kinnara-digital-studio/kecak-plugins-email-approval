package com.kinnara.kecakplugins.emailapproval;

import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.joget.apps.app.lib.EmailTool;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FileUtil;
import org.joget.commons.util.*;
import org.joget.plugin.base.PluginException;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.BeansException;

import javax.activation.FileDataSource;
import javax.mail.internet.MimeUtility;
import javax.sql.DataSource;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EmailApprovalNotification extends EmailTool {
    @Override
    public String getName() {
        return "Email Approval Notification";
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    private String generateButtonCssStyle() {
        return "<style type=\"text/css\">\n" +
                "    body {\n" +
                "      margin: 0;\n" +
                "    }\n" +
                "    body, table, td, p, a, li, blockquote {\n" +
                "      -webkit-text-size-adjust: none!important;\n" +
                "      font-family: Merienda, 'Times New Roman', serif;\n" +
                "      font-style: normal;\n" +
                "      font-weight: 400;\n" +
                "    }\n" +
                "    button{\n" +
                "      width:90%;\n" +
                "    }\n" +
                "    @media screen and (max-width:600px) {\n" +
                "    /*styling for objects with screen size less than 600px; */\n" +
                "    body, table, td, p, a, li, blockquote {\n" +
                "      -webkit-text-size-adjust: none!important;\n" +
                "      font-family: Merienda, 'Times New Roman', serif;\n" +
                "    }\n" +
                "    table {\n" +
                "      /* All tables are 100% width */\n" +
                "      width: 100%;\n" +
                "    }\n" +
                "    .footer {\n" +
                "      /* Footer has 2 columns each of 48% width */\n" +
                "      height: auto !important;\n" +
                "      max-width: 48% !important;\n" +
                "      width: 48% !important;\n" +
                "    }\n" +
                "    table.responsiveImage {\n" +
                "      /* Container for images in catalog */\n" +
                "      height: auto !important;\n" +
                "      max-width: 30% !important;\n" +
                "      width: 30% !important;\n" +
                "    }\n" +
                "    table.responsiveContent {\n" +
                "      /* Content that accompanies the content in the catalog */\n" +
                "      height: auto !important;\n" +
                "      max-width: 66% !important;\n" +
                "      width: 66% !important;\n" +
                "    }\n" +
                "    .top {\n" +
                "      /* Each Columnar table in the header */\n" +
                "      height: auto !important;\n" +
                "      max-width: 48% !important;\n" +
                "      width: 48% !important;\n" +
                "    }\n" +
                "    .catalog {\n" +
                "      margin-left: 0%!important;\n" +
                "    }\n" +
                "\n" +
                "    }\n" +
                "    @media screen and (max-width:480px) {\n" +
                "    /*styling for objects with screen size less than 480px; */\n" +
                "    body, table, td, p, a, li, blockquote {\n" +
                "      -webkit-text-size-adjust: none!important;\n" +
                "      font-family: Merienda, 'Times New Roman', serif;\n" +
                "    }\n" +
                "    table {\n" +
                "      /* All tables are 100% width */\n" +
                "      width: 100% !important;\n" +
                "      border-style: none !important;\n" +
                "    }\n" +
                "    .footer {\n" +
                "      /* Each footer column in this case should occupy 96% width  and 4% is allowed for email client padding*/\n" +
                "      height: auto !important;\n" +
                "      max-width: 96% !important;\n" +
                "      width: 96% !important;\n" +
                "    }\n" +
                "    .table.responsiveImage {\n" +
                "      /* Container for each image now specifying full width */\n" +
                "      height: auto !important;\n" +
                "      max-width: 96% !important;\n" +
                "      width: 96% !important;\n" +
                "    }\n" +
                "    .table.responsiveContent {\n" +
                "      /* Content in catalog  occupying full width of cell */\n" +
                "      height: auto !important;\n" +
                "      max-width: 96% !important;\n" +
                "      width: 96% !important;\n" +
                "    }\n" +
                "    .top {\n" +
                "      /* Header columns occupying full width */\n" +
                "      height: auto !important;\n" +
                "      max-width: 100% !important;\n" +
                "      width: 100% !important;\n" +
                "    }\n" +
                "    .catalog {\n" +
                "      margin-left: 0%!important;\n" +
                "    }\n" +
                "    button{\n" +
                "      width:90%!important;\n" +
                "    }\n" +
                "    }\n" +
                "  </style>\n";
    }


    private String generateButtonHtml(String label, String value, String type) {
        String targetEmail = getPropertyString("from");
        String color;
        switch (type) {
            case "revise":
                color = "FDD017";
                break;
            case "reject":
                color = "FD0000";
                break;
            default:
                color = "4CC417";
        }

        String html =
                " <td>" +
                "   <a href=\"#\" style=\"text-decoration:none\">" +
                "     <p style=\"background-color:#" + color + "; text-align:center; padding: 10px 10px 10px 10px; margin: 10px 10px 10px 10px;color: #FFFFFF;   font-family: Merienda, 'Times New Roman', serif, sans-serif; \"><a href=\"mailto:" + targetEmail + "?subject=[#assignment.processId#]["+value+"]\" style=\"color: #FFFFFF;\" data-rel=\"external\">" + label + "</a></p>" +
                "   </a>" +
                " </td>";
        return html;
    }

    @Override
    public Object execute(Map properties) {
        String actions = Optional.ofNullable(getProperty("action"))
                .filter(o -> o instanceof Object[])
                .map(o -> (Object[]) o)
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .filter(o -> o instanceof Map)
                .map(o -> (Map<String, Object>) o)
                .map(m -> generateButtonHtml(String.valueOf(m.get("label")), String.valueOf(m.get("value")), String.valueOf(m.get("type"))))
                .collect(Collectors.joining());

        properties.replace("message", properties.get("message") + generateButtonCssStyle() + "<table cellpadding=\"0\" cellspacing=\"0\" align=\"center\" width=\"84%\" style=\"margin-left:12.5%\" class=\"catalog\"><tbody><tr>" +actions + "</tr></tbody></table>");

        return super.execute(properties);
    }

    @Override
    public String getPropertyOptions() {
        try {
            // load
            JSONArray jsonEmailToolProperties = new JSONArray(super.getPropertyOptions());
            JSONArray jsonEmailApprovalNotificationProperties = new JSONArray(getPluginPropertyOptions());

            // inject default email properties with additional properties
            for (int i = 0, size = jsonEmailApprovalNotificationProperties.length(); i < size; i++) {
                jsonEmailToolProperties.put(jsonEmailApprovalNotificationProperties.getJSONObject(i));
            }

            return jsonEmailToolProperties.toString();
        } catch (JSONException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return super.getPropertyOptions();
        }
    }

    private String getPluginPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/emailApprovalNotification.json", null, false, "/messages/emailApprovalNotification");
    }
}

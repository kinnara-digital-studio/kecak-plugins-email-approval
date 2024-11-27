package com.kinnarastudio.kecakplugins.emailapproval;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import org.joget.apps.app.lib.EmailTool;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
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
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
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


    private String generateButtonHtml(String label, String value, String type, String message) {
        String targetEmail = getPropertyString("from");
        String color;
        switch (type) {
            case "complete":
                color = "59AF50";
                break;
            case "revise":
                color = "F19633";
                break;
            case "reject":
                color = "D3382F";
                break;
            default:
                color = "2A61FB";
        }

        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        String html =
                " <td>" +
                        "   <a href=\"#\" style=\"text-decoration:none\">" +
                        "     <p style=\"background-color:#" + color + "; text-align:center; padding: 10px 10px 10px 10px; margin: 10px 10px 10px 10px;color: #FFFFFF;   font-family: Merienda, 'Times New Roman', serif, sans-serif; \"><a href=\"mailto:" + targetEmail + "?subject=[" + appDefinition.getAppId() + "][#assignment.processId#][" + value + "]&body=" + message + "\" style=\"color: #FFFFFF;\" data-rel=\"external\">" + label + "</a></p>" +
                        "   </a>" +
                        " </td>";
        return html;
    }

    @Override
    public Object execute(Map properties) {
        String actions = Optional.ofNullable(getProperty("action"))
                .filter(o -> o instanceof Object[])
                .map(o -> (Object[]) o).stream()
                .flatMap(Arrays::stream)
                .filter(o -> o instanceof Map)
                .map(o -> (Map<String, Object>) o)
                .map(m -> generateButtonHtml(String.valueOf(m.get("label")), String.valueOf(m.get("value")), String.valueOf(m.get("type")), String.valueOf(m.get("message"))))
                .collect(Collectors.joining());

        properties.replace("message", properties.get("message") + generateButtonCssStyle() + "<table cellpadding=\"0\" cellspacing=\"0\" align=\"center\" width=\"84%\" style=\"margin-left:12.5%\" class=\"catalog\"><tbody><tr>" + actions + "</tr></tbody></table>");

        return super.execute(properties);
    }

    @Override
    public String getPropertyOptions() {
        try {
            Stream<JSONObject> jsonStreamEmailProperties = JSONStream.of(new JSONArray(super.getPropertyOptions()), Try.onBiFunction(JSONArray::getJSONObject));
            Stream<JSONObject> jsonStreamNotificationProperties = JSONStream.of(new JSONArray(getPluginPropertyOptions()), Try.onBiFunction(JSONArray::getJSONObject));

            return Stream.concat(jsonStreamEmailProperties, jsonStreamNotificationProperties)
                    .collect(JSONCollectors.toJSONArray())
                    .toString();

        } catch (JSONException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
    }

    private String getPluginPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/emailApprovalNotification.json", null, false, "/messages/emailApprovalNotification");
    }
}

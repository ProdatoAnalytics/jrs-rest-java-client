/*
 * Copyright (C) 2005 - 2014 Jaspersoft Corporation. All rights  reserved.
 * http://www.jaspersoft.com.
 *
 * Unless you have purchased  a commercial license agreement from Jaspersoft,
 * the following license terms  apply:
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License  as
 * published by the Free Software Foundation, either version 3 of  the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero  General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public  License
 * along with this program.&nbsp; If not, see <http://www.gnu.org/licenses/>.
 */

package com.jaspersoft.jasperserver.jaxrs.client.dto.reports;

import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.reporting.ReportOutputFormat;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "export")
public class ExportExecutionOptions {

    public static final String PARAM_NAME_PAGES = "pages";
    public static final String PARAM_NAME_ATTACHMENTS_PREFIX = "attachmentsPrefix";

    private String outputFormat;
    private String attachmentsPrefix;
    private String pages;
    private String baseUrl;
    private boolean allowInlineScripts = true;

    public ExportExecutionOptions setPages(String pages) {
        this.pages = pages;
        return this;
    }

    public String getAttachmentsPrefix() {
        return attachmentsPrefix;
    }

    public ExportExecutionOptions setAttachmentsPrefix(String attachmentsPrefix) {
        this.attachmentsPrefix = attachmentsPrefix;
        return this;
    }

    public String getPages(){
        return pages;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public ExportExecutionOptions setOutputFormat(ReportOutputFormat outputFormat) {
        this.outputFormat = outputFormat.toString().toLowerCase();
        return this;
    }

    public ExportExecutionOptions setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat.toLowerCase();
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isAllowInlineScripts() {
        return allowInlineScripts;
    }

    public void setAllowInlineScripts(boolean allowInlineScripts) {
        this.allowInlineScripts = allowInlineScripts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExportExecutionOptions that = (ExportExecutionOptions) o;

        if (pages != null ? !pages.equals(that.pages) : that.pages != null) return false;
        if (!outputFormat.equals(that.outputFormat)) return false;
        if (attachmentsPrefix != null ? !attachmentsPrefix.equals(that.attachmentsPrefix) : that.attachmentsPrefix != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = outputFormat.hashCode();
        result = 31 * result + (pages != null ? pages.hashCode() : 0);
        result = 31 * result + (attachmentsPrefix != null ? attachmentsPrefix.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return outputFormat +
                (pages != null ? ";" + PARAM_NAME_PAGES + "=" + pages.toString() : "") +
                (attachmentsPrefix != null ? ";" + PARAM_NAME_ATTACHMENTS_PREFIX + "=" + attachmentsPrefix : "");
    }
}

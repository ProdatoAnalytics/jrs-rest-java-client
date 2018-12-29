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
package com.jaspersoft.jasperserver.jaxrs.client.filters;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.NewCookie;

import com.jaspersoft.jasperserver.jaxrs.client.core.SessionStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SessionOutputFilter implements ClientRequestFilter {

    //private final String sessionId;
    //private final Map<String, NewCookie> cookies;
	private final SessionStorage storage;

    public SessionOutputFilter(SessionStorage storage) {
        this.storage = storage;
    }
    
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
    	StringBuilder cookiesString = new StringBuilder();

		for (Map.Entry<String, NewCookie> entry : storage.getCookies().entrySet()) {
			cookiesString.append(entry.getKey())
				.append("=")
				.append(entry.getValue().getValue())
				.append(";");
		}
		
        List<Object> cookies = new ArrayList<Object>() {{add(cookiesString.toString());}};
		requestContext.getHeaders().put("Cookie", cookies);
    }
}

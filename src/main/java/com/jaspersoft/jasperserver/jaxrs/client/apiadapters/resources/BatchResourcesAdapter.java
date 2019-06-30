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
package com.jaspersoft.jasperserver.jaxrs.client.apiadapters.resources;

import com.jaspersoft.jasperserver.dto.resources.ClientResourceListWrapper;
import com.jaspersoft.jasperserver.dto.resources.ClientResourceLookup;
import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.AbstractAdapter;
import com.jaspersoft.jasperserver.jaxrs.client.core.*;
import com.jaspersoft.jasperserver.jaxrs.client.core.exceptions.handling.DefaultErrorHandler;
import com.jaspersoft.jasperserver.jaxrs.client.core.operationresult.OperationResult;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static com.jaspersoft.jasperserver.jaxrs.client.core.JerseyRequest.buildRequest;

import java.util.ArrayList;
import java.util.List;

public class BatchResourcesAdapter extends AbstractAdapter {
    public static final String SERVICE_URI = "resources";
    private MultivaluedMap<String, String> params;

    public BatchResourcesAdapter(SessionStorage sessionStorage) {
        super(sessionStorage);
        this.params = new MultivaluedHashMap<String, String>();
    }

    public BatchResourcesAdapter parameter(ResourceSearchParameter param, String value){
        params.add(param.getName(), value);
        return this;
    }

    public OperationResult<ClientResourceListWrapper> search() {
        return getBuilder(ClientResourceListWrapper.class).get();
    }

    public OperationResult<ClientResourceListWrapper> searchAll() {
	
		OperationResult<ClientResourceListWrapper> result = search();
		OperationResult<ClientResourceListWrapper> finalResult = result;
		
		List<ClientResourceLookup> allResults = new ArrayList<ClientResourceLookup>();
		
		Response response = result.getResponse();
		
		Boolean firstResult = Boolean.TRUE;
		
		while (true) {
			/*
			 * response codes are:
			 * 200: OK - has results
			 * 204: no results. includes reading past max # of results
			 * 404: invalid folder or no access to folder
			 */
	    	if (response.getStatus() != Status.OK.getStatusCode()) {
	    		break;
	    	}

    		Integer startIndex = Integer.parseInt(response.getHeaderString(ResourceSearchResponseHeader.START_INDEX.getName())); 
    	    Integer resultCount = Integer.parseInt(response.getHeaderString(ResourceSearchResponseHeader.RESULT_COUNT.getName()));

    		if (resultCount == 0) {
    			break;
    		}

	    	// get the next batch if we got something in the last batch
    		// default pagination does not give full pages or correct total count
    		// due to security

    	    if (firstResult) {
    	    	allResults = result.getEntity().getResourceLookups();
    	    	firstResult = Boolean.FALSE;
    	    } else {
    	    	allResults.addAll(result.getEntity().getResourceLookups());
    	    	finalResult = result;
    	    }

    	    Integer nextOffset = startIndex + resultCount;
			/*
			 * nextOffset in header only works for forceFullPage = true
			 * response.getHeaderString(ResourceSearchResponseHeader.NEXT_OFFSET.getName());
			 */
    		
			params.remove(ResourceSearchParameter.OFFSET.getName());
			parameter(ResourceSearchParameter.OFFSET, nextOffset.toString());
			result = search();
			response = result.getResponse();
    	}
		finalResult.getEntity().getResourceLookups().clear();
		finalResult.getEntity().getResourceLookups().addAll(allResults);
    	return finalResult;
	}

    public <R> RequestExecution asyncSearch(final Callback<OperationResult<ClientResourceListWrapper>, R> callback) {
        final JerseyRequest<ClientResourceListWrapper> request = getBuilder(ClientResourceListWrapper.class);

        RequestExecution task = new RequestExecution(new Runnable() {
            @Override
            public void run() {
                callback.execute(request.get());
            }
        });

        ThreadPoolUtil.runAsynchronously(task);
        return task;
    }

    public OperationResult delete(){
        return getBuilder(Object.class).delete();
    }

    public <R> RequestExecution asyncDelete(final Callback<OperationResult, R> callback) {
        final JerseyRequest request = getBuilder(Object.class);

        RequestExecution task = new RequestExecution(new Runnable() {
            @Override
            public void run() {
                callback.execute(request.delete());
            }
        });

        ThreadPoolUtil.runAsynchronously(task);
        return task;
    }

    private <T> JerseyRequest<T> getBuilder(Class<T> responseClass) {
        JerseyRequest<T> request = buildRequest(sessionStorage, responseClass, new String[]{SERVICE_URI}, new DefaultErrorHandler());
        request.addParams(params);
        return request;
    }

}
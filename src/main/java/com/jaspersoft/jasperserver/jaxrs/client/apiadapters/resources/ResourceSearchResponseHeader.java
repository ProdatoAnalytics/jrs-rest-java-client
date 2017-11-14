package com.jaspersoft.jasperserver.jaxrs.client.apiadapters.resources;

public enum ResourceSearchResponseHeader {

	/**
	 *  This is the number of results that are contained in the current response.
	 *  It can be less than or equal to the limit.
	 */
	RESULT_COUNT("Result-Count"),
	
	/**
	 * The Start-Index in the response is equal to the offset specified in the request.
	 * With a limit=100, it will be 0 on the first page, 100 on the second page, etc.
	 */
	START_INDEX("Start-Index"), 
	
	/**
	 * This is the offset to request the next page. With forceFullPage=false, the Next-Offset is 
	 * equivalent to Start-Index+limit, except on the last page. On the last page, 
	 * the Next-Offset is omitted to indicate there are no further pages.
	 */
	
	NEXT_OFFSET("Next-Offset"),
	
	/**
	 * This is the total number of results before permissions are applied. This is not the total number
	of results for this search by this user, but it is an upper bound. Dividing this number by the limit
	gives the number of pages that will be required, though not every page will have the full
	number of results.
	This header only appears on the first response, unless forceTotalCount=true.
	 */
	
	TOTAL_COUNT("Total-Count");

    private String name;

    ResourceSearchResponseHeader(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}

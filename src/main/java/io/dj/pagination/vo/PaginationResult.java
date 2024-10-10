package io.dj.pagination.vo;

import java.util.List;

public class PaginationResult<T> {
    private List<T> data;
    private int totalCount;
    private int lastPage;

    public PaginationResult(List<T> data, int totalCount, int lastPage) {
    	this.data = data;
        this.totalCount = totalCount;
        this.lastPage = lastPage;
    }

    public List<T> getData() {
        return data;
    }

    public int getTotalCount() {
        return totalCount;
    }

	public int getLastPage() {
		return lastPage;
	}

	public void setLastPage(int lastPage) {
		this.lastPage = lastPage;
	}
    
}
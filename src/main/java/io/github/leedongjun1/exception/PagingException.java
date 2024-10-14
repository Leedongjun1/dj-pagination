package io.github.leedongjun1.exception;

public class PagingException extends RuntimeException{
	private static final long serialVersionUID = 1L;
	
	public PagingException (PagingErrorCode errorCd) {
		super(errorCd.getMessage());
	}
	public PagingException (PagingErrorCode errorCd, String message) {
		super(errorCd.getMessage()+ message);
	}
}

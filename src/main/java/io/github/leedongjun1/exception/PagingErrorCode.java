package io.github.leedongjun1.exception;

public enum PagingErrorCode {
	NOT_FOUND_REQUIRED_PARAMETER("001", "필수 파라미터가 누락되었습니다."),
    PAGING_ERROR("002", "실행 페이지가 마지막 페이지보다 높습니다."),
    SQL_SYNTAX_ERROR("003", "SQL 구문 오류가 발생했습니다."),
    UNSUPPORTED_DBMS("004", "지원하지 않는 DBMS이거나, application.properties 정보가 올바르지 않습니다.");

	private final String code;
	private final String message;
	
	PagingErrorCode(String code, String message){
		this.code = code;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
